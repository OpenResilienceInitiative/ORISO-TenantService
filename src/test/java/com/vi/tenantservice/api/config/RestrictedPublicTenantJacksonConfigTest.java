package com.vi.tenantservice.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.SmtpConfig;
import org.junit.jupiter.api.Test;

class RestrictedPublicTenantJacksonConfigTest {

  private final RestrictedPublicTenantJacksonConfig config =
      new RestrictedPublicTenantJacksonConfig();

  @Test
  void restrictedPublicTenantModule_should_omitNullSensitiveFields() throws Exception {
    Settings settings = new Settings();
    settings.setFeatureToolsOICDToken(null);
    settings.setSmtp(
        new SmtpConfig()
            .enabled(true)
            .host("smtp.example.org")
            .port(587)
            .secure(false)
            .username(null)
            .password(null)
            .from("notifications@example.org")
            .emailThemeColor("#123456"));

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(config.restrictedPublicTenantModule());

    String json = objectMapper.writeValueAsString(settings);

    assertThat(json).contains("\"smtp\"");
    assertThat(json).contains("\"host\":\"smtp.example.org\"");
    assertThat(json).contains("\"from\":\"notifications@example.org\"");
    assertThat(json).doesNotContain("featureToolsOICDToken");
    assertThat(json).doesNotContain("\"username\"");
    assertThat(json).doesNotContain("\"password\"");
  }
}
