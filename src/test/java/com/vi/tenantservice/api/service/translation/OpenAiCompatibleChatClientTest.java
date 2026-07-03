package com.vi.tenantservice.api.service.translation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Provider tests against a local WireMock (no real network calls): both providers, every typed
 * error code mapping Frank asked for.
 */
class OpenAiCompatibleChatClientTest {

  private static final String CHAT_COMPLETIONS = "/chat/completions";
  private static final long CONNECT_TIMEOUT_MS = 1000;
  private static final long READ_TIMEOUT_MS = 1500;

  private static WireMockServer wireMock;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  @BeforeEach
  void resetWireMock() {
    wireMock.resetAll();
  }

  static Stream<Arguments> providers() {
    return Stream.of(
        Arguments.of(
            "openrouter",
            (Function<String, TranslationProviderClient>)
                baseUrl ->
                    new OpenRouterClient(
                        baseUrl, "openai/gpt-4o-mini", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)),
        Arguments.of(
            "mistral",
            (Function<String, TranslationProviderClient>)
                baseUrl ->
                    new MistralClient(
                        baseUrl, "mistral-small-latest", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)));
  }

  private static TranslationProviderClient client(
      Function<String, TranslationProviderClient> factory) {
    return factory.apply(wireMock.baseUrl());
  }

  private static void stubStatus(int status) {
    wireMock.stubFor(
        post(urlEqualTo(CHAT_COMPLETIONS))
            .willReturn(aResponse().withStatus(status).withBody("{\"error\":\"provider error\"}")));
  }

  private static void stubSuccess(String content) {
    wireMock.stubFor(
        post(urlEqualTo(CHAT_COMPLETIONS))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                            + com.fasterxml.jackson.databind.node.TextNode.valueOf(content)
                            + "}}]}")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("providers")
  void translateHtml_Should_returnTranslatedHtml_andSendBearerKeyAndModel(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubSuccess("<p>Hello world</p>");

    var result = client(factory).translateHtml("test-key", "de", "en", "<p>Hallo Welt</p>");

    assertThat(result).isEqualTo("<p>Hello world</p>");
    wireMock.verify(
        postRequestedFor(urlEqualTo(CHAT_COMPLETIONS))
            .withHeader("Authorization", equalTo("Bearer test-key"))
            .withRequestBody(matchingJsonPath("$.model"))
            .withRequestBody(matchingJsonPath("$.messages[0].content"))
            .withRequestBody(
                matchingJsonPath("$.messages[1].content", equalTo("<p>Hallo Welt</p>"))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("providers")
  void translateHtml_Should_stripCodeFences_When_modelWrapsOutput(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubSuccess("```html\n<p>Hello</p>\n```");

    var result = client(factory).translateHtml("test-key", "de", "en", "<p>Hallo</p>");

    assertThat(result).isEqualTo("<p>Hello</p>");
  }

  @ParameterizedTest(name = "{0} 401")
  @MethodSource("providers")
  void translateHtml_Should_mapUnauthorized_toKeyInvalid(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubStatus(401);
    assertTranslationError(factory, TranslationErrorCode.TRANSLATION_KEY_INVALID, providerId);
  }

  @ParameterizedTest(name = "{0} 403")
  @MethodSource("providers")
  void translateHtml_Should_mapForbidden_toKeyInvalid(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubStatus(403);
    assertTranslationError(factory, TranslationErrorCode.TRANSLATION_KEY_INVALID, providerId);
  }

  @ParameterizedTest(name = "{0} 402")
  @MethodSource("providers")
  void translateHtml_Should_mapPaymentRequired_toNoCredit(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubStatus(402);
    assertTranslationError(factory, TranslationErrorCode.TRANSLATION_NO_CREDIT, providerId);
  }

  @ParameterizedTest(name = "{0} 429")
  @MethodSource("providers")
  void translateHtml_Should_mapTooManyRequests_toRateLimited(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubStatus(429);
    assertTranslationError(factory, TranslationErrorCode.TRANSLATION_RATE_LIMITED, providerId);
  }

  @ParameterizedTest(name = "{0} 500")
  @MethodSource("providers")
  void translateHtml_Should_mapServerError_toProviderUnavailable(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubStatus(500);
    assertTranslationError(
        factory, TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE, providerId);
  }

  @ParameterizedTest(name = "{0} 503")
  @MethodSource("providers")
  void translateHtml_Should_mapServiceUnavailable_toProviderUnavailable(
      String providerId, Function<String, TranslationProviderClient> factory) {
    stubStatus(503);
    assertTranslationError(
        factory, TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE, providerId);
  }

  @ParameterizedTest(name = "{0} timeout")
  @MethodSource("providers")
  void translateHtml_Should_mapReadTimeout_toProviderUnavailable(
      String providerId, Function<String, TranslationProviderClient> factory) {
    wireMock.stubFor(
        post(urlEqualTo(CHAT_COMPLETIONS))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withFixedDelay((int) READ_TIMEOUT_MS + 2000)
                    .withBody("{}")));

    assertTranslationError(
        factory, TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE, providerId);
  }

  @ParameterizedTest(name = "{0} malformed body")
  @MethodSource("providers")
  void translateHtml_Should_mapMissingContent_toProviderUnavailable(
      String providerId, Function<String, TranslationProviderClient> factory) {
    wireMock.stubFor(
        post(urlEqualTo(CHAT_COMPLETIONS))
            .willReturn(aResponse().withStatus(200).withBody("{\"unexpected\":true}")));

    assertTranslationError(
        factory, TranslationErrorCode.TRANSLATION_PROVIDER_UNAVAILABLE, providerId);
  }

  private static void assertTranslationError(
      Function<String, TranslationProviderClient> factory,
      TranslationErrorCode expectedCode,
      String expectedProvider) {
    var client = client(factory);
    assertThatThrownBy(() -> client.translateHtml("test-key", "de", "en", "<p>x</p>"))
        .isInstanceOfSatisfying(
            TranslationException.class,
            e -> {
              assertThat(e.getErrorCode()).isEqualTo(expectedCode);
              assertThat(e.getProvider()).isEqualTo(expectedProvider);
            });
  }
}
