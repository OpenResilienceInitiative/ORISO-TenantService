package com.vi.tenantservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * The tenant directory only batches owner lookups for tokens issued to the human app client, and
 * UserService checks the same {@code azp}. Both sides must name that client in configuration, not
 * inherit it from a code default that nobody sees (#385 follow-up).
 */
class AppClientIdConfigurationTest {

  @Test
  void applicationPropertiesNameTheAppClientExplicitly() throws IOException {
    Properties properties = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
      properties.load(in);
    }

    assertThat(properties.getProperty("keycloak.config.app-client-id"))
        .isEqualTo("${KEYCLOAK_CONFIG_APP_CLIENTID:app}");
  }

  @Test
  void codeCarriesNoHiddenDefaultForTheAppClient() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/com/vi/tenantservice/api/facade/"
                    + "TenantFacadeAuthorisationService.java"));

    assertThat(source)
        .contains("${keycloak.config.app-client-id}")
        .doesNotContain("${keycloak.config.app-client-id:");
  }
}
