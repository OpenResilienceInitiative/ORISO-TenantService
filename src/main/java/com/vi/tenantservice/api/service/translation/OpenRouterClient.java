package com.vi.tenantservice.api.service.translation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** OpenRouter (OpenAI-compatible chat completions at openrouter.ai/api/v1). */
@Component
public class OpenRouterClient extends OpenAiCompatibleChatClient {

  public static final String PROVIDER_ID = "openrouter";

  public OpenRouterClient(
      @Value("${translation.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
      @Value("${translation.openrouter.model:openai/gpt-4o-mini}") String model,
      @Value("${translation.connect-timeout-ms:5000}") long connectTimeoutMillis,
      @Value("${translation.read-timeout-ms:60000}") long readTimeoutMillis) {
    super(baseUrl, model, connectTimeoutMillis, readTimeoutMillis);
  }

  @Override
  public String getProviderId() {
    return PROVIDER_ID;
  }
}
