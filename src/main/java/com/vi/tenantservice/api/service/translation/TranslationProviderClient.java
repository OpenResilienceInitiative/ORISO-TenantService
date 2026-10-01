package com.vi.tenantservice.api.service.translation;

/** A machine-translation provider (LLM chat-completions API) able to translate HTML content. */
public interface TranslationProviderClient {

  /** Stable provider id used in the settings map and the API ("openrouter", "mistral"). */
  String getProviderId();

  /** False when the provider's base URL is not configured; the provider is then unusable. */
  default boolean isConfigured() {
    return true;
  }

  /** The model this client sends requests for (configurable via application properties). */
  String getModel();

  /**
   * Translates one HTML snippet, preserving tags, attributes and structure; only human-readable
   * text is translated.
   *
   * @throws TranslationException with a typed error code on any provider failure
   */
  String translateHtml(String apiKey, String sourceLang, String targetLang, String html);
}
