package com.vi.tenantservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class DeploymentSchemaConfigurationTest {

  @Test
  void prodProfile_Should_RunLiquibaseAndValidateHibernateSchema() throws IOException {
    var properties = loadProperties("application-prod.properties");

    assertThat(properties.getProperty("spring.liquibase.enabled"))
        .isEqualTo("${SPRING_LIQUIBASE_ENABLED:true}");
    assertThat(properties.getProperty("spring.liquibase.change-log"))
        .isEqualTo("classpath:db/changelog/tenantservice-prod-master.xml");
    assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto"))
        .isEqualTo("${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}");
  }

  @Test
  void stagingProfile_Should_RunLiquibaseAndValidateHibernateSchema() throws IOException {
    var properties = loadProperties("application-staging.properties");

    assertThat(properties.getProperty("spring.liquibase.enabled"))
        .isEqualTo("${SPRING_LIQUIBASE_ENABLED:true}");
    assertThat(properties.getProperty("spring.liquibase.change-log"))
        .isEqualTo("classpath:db/changelog/tenantservice-prod-master.xml");
    assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto"))
        .isEqualTo("${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}");
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
