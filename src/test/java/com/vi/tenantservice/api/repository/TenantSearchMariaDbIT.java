package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.service.search.TenantSearchText;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MariaDBContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.liquibase.enabled=true",
      "spring.liquibase.change-log=classpath:db/changelog/tenantservice-master.xml",
      "spring.liquibase.contexts=seed"
    })
class TenantSearchMariaDbIT {
  private static final MariaDBContainer<?> DATABASE =
      new MariaDBContainer<>("mariadb:10.11.18").withDatabaseName("tenantservice");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    DATABASE.start();
    registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
    registry.add("spring.datasource.username", DATABASE::getUsername);
    registry.add("spring.datasource.password", DATABASE::getPassword);
  }

  @AfterAll
  static void closeDatabase() {
    DATABASE.stop();
  }

  @Autowired TenantRepository repository;
  @Autowired JdbcTemplate jdbc;

  @Test
  void migratedMariaDbSearchNormalizesAccentsAndLiteralsBeforeCountingAndPaging() {
    jdbc.update("DELETE FROM tenant WHERE id != 0");
    insert(7101, "Müller 50%_!", "a-domain");
    insert(7102, "Mu\u0308ller 50%_!", "b-domain");
    insert(7103, "Muller 50other", "c-domain");
    jdbc.update("UPDATE tenant SET name='Muller 50%_!' WHERE id=0");
    jdbc.update("UPDATE tenant SET contact_email='contact-only@example.test' WHERE id=7103");
    var first = PageRequest.of(0, 1, Sort.by("id"));
    var tokens = TenantSearchText.tokens("MULLER\u200350%_!");
    var result = repository.searchTenantData(tokens, Map.of(), first);
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent()).extracting("id").containsExactly(7101L);
    assertThat(repository.searchTenantData(tokens, Map.of(), first.next()).getContent())
        .extracting("id")
        .containsExactly(7102L);
    assertThat(
            repository
                .searchTenantData(TenantSearchText.tokens("710 b-domain"), Map.of(), first)
                .getContent())
        .extracting("id")
        .containsExactly(7102L);
    assertThat(
            repository
                .searchTenantData(
                    List.of("muller", "owner", "second"),
                    Map.of("owner", Set.of(7102L), "second", Set.of(7102L)),
                    first)
                .getContent())
        .extracting("id")
        .containsExactly(7102L);
    assertThat(
            repository
                .searchTenantData(List.of("muller", "missing"), Map.of(), first)
                .getTotalElements())
        .isZero();
    assertThat(repository.searchTenantData(List.of(), Map.of(), first).getTotalElements())
        .isEqualTo(3);
    assertThat(repository.findAllTenantDataExceptTechnicalByInfix("*", first).getTotalElements())
        .isEqualTo(3);
    assertThat(
            repository.findAllTenantDataExceptTechnicalByInfix("\u2003 ", first).getTotalElements())
        .isEqualTo(3);
    assertThat(
            repository
                .searchTenantData(List.of("contact-only"), Map.of(), first)
                .getTotalElements())
        .isZero();
    assertThat(
            repository
                .searchTenantData(List.of("owner"), Map.of("owner", Set.of(0L)), first)
                .getTotalElements())
        .isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class))
        .isGreaterThan(20);
    System.out.println(
        "TENANT_SEARCH_DB "
            + jdbc.queryForList(
                "SELECT COLUMN_NAME, COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tenant' AND COLUMN_NAME IN ('name','subdomain')"));
  }

  private void insert(long id, String name, String subdomain) {
    jdbc.update(
        "INSERT INTO tenant(id,name,subdomain,create_date,update_date,active) VALUES(?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,TRUE)",
        id,
        name,
        subdomain);
  }
}
