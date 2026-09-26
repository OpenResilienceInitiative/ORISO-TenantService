package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TenantSmtpTestCopyTest {
  @Test
  void supportsEveryCurrentAppLanguageAndRegionalLocale() {
    for (String language : List.of("de-sie", "de-du", "en", "fr", "ru", "ti", "tr")) {
      var copy = TenantSmtpTestCopy.forLanguage(language);
      assertThat(copy.subject()).isNotBlank();
      assertThat(copy.text()).isNotBlank();
    }
    assertThat(TenantSmtpTestCopy.forLanguage("fr-FR"))
        .isEqualTo(TenantSmtpTestCopy.forLanguage("fr"));
    assertThat(TenantSmtpTestCopy.forLanguage("de"))
        .isEqualTo(TenantSmtpTestCopy.forLanguage("de-sie"));
    assertThat(TenantSmtpTestCopy.forLanguage("de-du"))
        .isNotEqualTo(TenantSmtpTestCopy.forLanguage("de-sie"));
  }
}
