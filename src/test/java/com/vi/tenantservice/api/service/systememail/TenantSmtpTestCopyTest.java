package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TenantSmtpTestCopyTest {
  @Test
  void supportsEveryCurrentAppLanguageAndRegionalLocale() {
    for (String language : List.of("de", "en", "uk", "ru", "tr", "ar", "fa")) {
      var copy = TenantSmtpTestCopy.forLanguage(language);
      assertThat(copy.subject()).isNotBlank();
      assertThat(copy.text()).isNotBlank();
    }
    assertThat(TenantSmtpTestCopy.forLanguage("fa-AF"))
        .isEqualTo(TenantSmtpTestCopy.forLanguage("fa"));
  }
}
