package com.vi.tenantservice.api;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MariaDBContainer;

/** The same transactional contract against the production database engine and real changelog. */
@TestPropertySource(
    properties = {
      "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
      "spring.liquibase.enabled=true",
      "spring.liquibase.change-log=classpath:db/changelog/tenantservice-master.xml",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
class PlatformDpiaMasterDataMariaDbIT extends PlatformDpiaMasterDataPersistenceIT {
  private static final MariaDBContainer<?> DATABASE =
      new MariaDBContainer<>("mariadb:10.11.18").withDatabaseName("tenantservice");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    DATABASE.start();
    registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
    registry.add("spring.datasource.username", DATABASE::getUsername);
    registry.add("spring.datasource.password", DATABASE::getPassword);
  }

  @AfterAll
  static void stopDatabase() {
    DATABASE.stop();
  }
}
