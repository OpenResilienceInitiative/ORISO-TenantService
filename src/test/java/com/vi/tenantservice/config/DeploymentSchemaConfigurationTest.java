package com.vi.tenantservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Guards the single-master Liquibase configuration (Liquibase Re-Enablement Plan 2026-07-04,
 * package L1): every deployable profile must point at the one master changelog, expose the
 * SPRING_LIQUIBASE_ENABLED kill switch, validate the Hibernate schema, and activate the proper
 * Liquibase contexts (seed data only on local/dev).
 */
class DeploymentSchemaConfigurationTest {

  private static final String MASTER_CHANGELOG = "classpath:db/changelog/tenantservice-master.xml";

  @ParameterizedTest
  @CsvSource({
    "application-local.properties, ${SPRING_LIQUIBASE_CONTEXTS:seed}",
    "application-dev.properties, ${SPRING_LIQUIBASE_CONTEXTS:seed}",
    "application-staging.properties, ${SPRING_LIQUIBASE_CONTEXTS:prod}",
    "application-prod.properties, ${SPRING_LIQUIBASE_CONTEXTS:prod}"
  })
  void deployableProfiles_Should_UseSingleMasterWithKillSwitchAndValidate(
      String propertiesFile, String expectedContexts) throws IOException {
    var properties = loadProperties(propertiesFile);

    assertThat(properties.getProperty("spring.liquibase.enabled"))
        .as("%s kill switch", propertiesFile)
        .isEqualTo("${SPRING_LIQUIBASE_ENABLED:true}");
    assertThat(properties.getProperty("spring.liquibase.change-log"))
        .as("%s single master changelog", propertiesFile)
        .isEqualTo(MASTER_CHANGELOG);
    assertThat(properties.getProperty("spring.liquibase.contexts"))
        .as("%s Liquibase contexts", propertiesFile)
        .isEqualTo(expectedContexts);
    assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto"))
        .as("%s Hibernate schema validation", propertiesFile)
        .isEqualTo("${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}");
  }

  @Test
  void testingProfile_Should_KeepH2CreateDropWithoutLiquibase() throws IOException {
    var properties = loadProperties("application-testing.properties");

    assertThat(properties.getProperty("spring.liquibase.enabled")).isEqualTo("false");
    assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
  }

  private Properties loadProperties(String fileName) throws IOException {
    var properties = new Properties();
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
      assertThat(inputStream).as(fileName + " exists").isNotNull();
      properties.load(inputStream);
    }
    return properties;
  }
}
