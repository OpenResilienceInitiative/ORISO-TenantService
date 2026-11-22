package com.vi.tenantservice.api.service.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The provider base URLs are explicit configuration (ORISO-Helm#368). Machine translation is an
 * optional feature, so a missing URL disables only that provider and says which variable to set; a
 * value that is set but unusable is a configuration error and stops startup.
 */
class TranslationProviderBaseUrlTest {

  @Test
  void missingMistralBaseUrl_Should_disableOnlyTheProvider_andNameTheVariable() {
    var client = new MistralClient("", "mistral-small-latest", 1000, 1000);

    assertThat(client.isConfigured()).isFalse();
    assertThatThrownBy(() -> client.translateHtml("key", "de", "en", "<p>Hallo</p>"))
        .isInstanceOfSatisfying(
            TranslationException.class,
            e -> {
              assertThat(e.getErrorCode())
                  .isEqualTo(TranslationErrorCode.TRANSLATION_NOT_CONFIGURED);
              assertThat(e.getMessage()).contains("TRANSLATION_MISTRAL_BASE_URL");
            });
  }

  @Test
  void missingOpenRouterBaseUrl_Should_disableOnlyTheProvider_andNameTheVariable() {
    var client = new OpenRouterClient(null, "openai/gpt-4o-mini", 1000, 1000);

    assertThat(client.isConfigured()).isFalse();
    assertThatThrownBy(() -> client.translateHtml("key", "de", "en", "<p>Hallo</p>"))
        .isInstanceOfSatisfying(
            TranslationException.class,
            e -> {
              assertThat(e.getErrorCode())
                  .isEqualTo(TranslationErrorCode.TRANSLATION_NOT_CONFIGURED);
              assertThat(e.getMessage()).contains("TRANSLATION_OPENROUTER_BASE_URL");
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"/v1", "api.mistral.ai/v1", "ftp://example.org", "https://"})
  void unusableBaseUrl_Should_failStartup_andNameTheVariable(String baseUrl) {
    assertThatThrownBy(() -> new MistralClient(baseUrl, "mistral-small-latest", 1000, 1000))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TRANSLATION_MISTRAL_BASE_URL");
  }

  @Test
  void configuredBaseUrl_Should_enableTheProvider() {
    assertThat(new MistralClient("https://example.org/v1", "m", 1000, 1000).isConfigured())
        .isTrue();
  }
}
