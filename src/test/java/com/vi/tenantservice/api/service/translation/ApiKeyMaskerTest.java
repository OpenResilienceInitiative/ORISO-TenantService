package com.vi.tenantservice.api.service.translation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyMaskerTest {

  @Test
  void mask_Should_showPrefixAndLast4_forLongKeys() {
    assertThat(ApiKeyMasker.mask("sk-or-v1-0123456789abcd")).isEqualTo("sk-…abcd");
  }

  @Test
  void mask_Should_neverContainTheMiddleOfTheKey() {
    var masked = ApiKeyMasker.mask("sk-or-v1-SECRETSECRETSECRET1234");
    assertThat(masked).doesNotContain("SECRET").isEqualTo("sk-…1234");
  }

  @Test
  void mask_Should_fullyMaskShortKeys() {
    assertThat(ApiKeyMasker.mask("short")).isEqualTo("…");
  }

  @Test
  void mask_Should_returnNull_When_noKeyConfigured() {
    assertThat(ApiKeyMasker.mask(null)).isNull();
    assertThat(ApiKeyMasker.mask("   ")).isNull();
    assertThat(ApiKeyMasker.mask("")).isNull();
  }
}
