package com.vi.tenantservice.api.service.translation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mistral chat completions at api.mistral.ai/v1 (OpenAI-compatible). */
@Component
public class MistralClient extends OpenAiCompatibleChatClient {

  public static final String PROVIDER_ID = "mistral";

  public MistralClient(
      @Value("${translation.mistral.base-url:}") String baseUrl,
      @Value("${translation.mistral.model:mistral-small-latest}") String model,
      @Value("${translation.connect-timeout-ms:5000}") long connectTimeoutMillis,
      @Value("${translation.read-timeout-ms:60000}") long readTimeoutMillis) {
    super(baseUrl, "TRANSLATION_MISTRAL_BASE_URL", model, connectTimeoutMillis, readTimeoutMillis);
  }

  @Override
  public String getProviderId() {
    return PROVIDER_ID;
  }
}
