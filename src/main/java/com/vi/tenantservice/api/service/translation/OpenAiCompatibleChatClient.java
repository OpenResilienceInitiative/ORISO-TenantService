package com.vi.tenantservice.api.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Base client for OpenAI-compatible chat-completions APIs (OpenRouter, Mistral). Sends a
 * translation prompt instructing the model to return only the translated HTML while keeping tags,
 * structure and attributes untouched, and maps provider failures to typed {@link
 * TranslationException}s: 401/403 = TRANSLATION_KEY_INVALID, 402 = TRANSLATION_NO_CREDIT, 429 =
 * TRANSLATION_RATE_LIMITED, timeouts/5xx (and any other transport failure) =
 * TRANSLATION_PROVIDER_UNAVAILABLE.
 */
public abstract class OpenAiCompatibleChatClient implements TranslationProviderClient {

  private static final String SYSTEM_PROMPT =
      """
      You are a professional translator for legal and administrative texts. \
      Translate the HTML fragment provided by the user from %s to %s. \
      Return ONLY the translated HTML. Keep every HTML tag, attribute and the document \
      structure exactly as in the input; translate only the human-readable text content. \
      Do not add explanations, comments or code fences.""";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String model;

  protected OpenAiCompatibleChatClient(
      String baseUrl, String model, long connectTimeoutMillis, long readTimeoutMillis) {
    this.baseUrl = StringUtils.removeEnd(baseUrl, "/");
    this.model = model;
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMillis));
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));
    this.restTemplate = new RestTemplate(factory);
  }

  @Override
  public String getModel() {
    return model;
  }

  @Override
  public String translateHtml(String apiKey, String sourceLang, String targetLang, String html) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);
    var body =
        Map.of(
            "model",
            model,
            "temperature",
            0,
            "messages",
            List.of(
                Map.of(
                    "role", "system", "content", SYSTEM_PROMPT.formatted(sourceLang, targetLang)),
                Map.of("role", "user", "content", html)));
    try {
      var response =
          restTemplate.postForEntity(
              baseUrl + "/chat/completions", new HttpEntity<>(body, headers), String.class);
      return extractContent(response.getBody());
    } catch (HttpStatusCodeException e) {
      throw mapStatusError(e);
    } catch (ResourceAccessException e) {
      throw new TranslationException(
          TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE,
          getProviderId(),
          "Provider " + getProviderId() + " unreachable or timed out: " + e.getMessage());
    } catch (RestClientException e) {
      throw new TranslationException(
          TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE,
          getProviderId(),
          "Unexpected provider error from " + getProviderId() + ": " + e.getMessage());
    }
  }

  private TranslationException mapStatusError(HttpStatusCodeException e) {
    var status = e.getStatusCode().value();
    var errorCode =
        switch (status) {
          case 401, 403 -> TranslationErrorCode.TRANSLATION_KEY_INVALID;
          case 402 -> TranslationErrorCode.TRANSLATION_NO_CREDIT;
          case 429 -> TranslationErrorCode.TRANSLATION_RATE_LIMITED;
          default -> TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE;
        };
    return new TranslationException(
        errorCode, getProviderId(), "Provider " + getProviderId() + " returned HTTP " + status);
  }

  private String extractContent(String responseBody) {
    try {
      JsonNode content = objectMapper.readTree(responseBody).at("/choices/0/message/content");
      if (content.isMissingNode() || content.isNull()) {
        throw new TranslationException(
            TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE,
            getProviderId(),
            "Provider " + getProviderId() + " returned no translation content");
      }
      return stripCodeFences(content.asText());
    } catch (TranslationException e) {
      throw e;
    } catch (Exception e) {
      throw new TranslationException(
          TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE,
          getProviderId(),
          "Could not parse response of provider " + getProviderId());
    }
  }

  /** Defensively removes markdown code fences some models wrap their output in. */
  private static String stripCodeFences(String content) {
    var trimmed = content.strip();
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\R?", "");
      trimmed = StringUtils.removeEnd(trimmed.stripTrailing(), "```").stripTrailing();
    }
    return trimmed;
  }
}
