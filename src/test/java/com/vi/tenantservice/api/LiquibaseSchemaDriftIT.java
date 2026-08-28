package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MariaDBContainer;

/**
 * Permanent schema-drift guard (Liquibase Re-Enablement Plan 2026-07-04, package L1).
 *
 * <p>Boots the application against a real MariaDB: Liquibase applies the single master changelog,
 * then Hibernate runs {@code ddl-auto=validate} against the JPA entity model. If the changelog and
 * the entity model ever drift apart, the context fails to start and this test goes red.
 *
 * <p>This is the only test in the service that sees a real database — everything else runs on H2
 * {@code MODE=MySQL}, which cannot show drift against the engine we deploy. It therefore starts its
 * own container rather than being gated on an environment variable: a gate that nobody sets makes
 * the test report as <em>skipped</em>, and a skip is silent (ORISO-TenantService#208).
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
      "spring.liquibase.enabled=true",
      "spring.liquibase.change-log=classpath:db/changelog/tenantservice-master.xml",
      "spring.liquibase.contexts=seed",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
class LiquibaseSchemaDriftIT {

  /**
   * Keep this on the major line we actually deploy — a drift guard against a different engine
   * version proves less than it appears to. The deployed version comes from the Helm values, which
   * live outside the chart repository, so this pin is deliberate and needs re-checking when the
   * cluster's MariaDB moves.
   */
  private static final String MARIADB_IMAGE = "mariadb:10.11.18";

  private static MariaDBContainer<?> mariadb;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    // Singleton container: started once for this class and reaped by Testcontainers' Ryuk, so the
    // suite pays one container start regardless of how many tests below run.
    if (mariadb == null) {
      mariadb = new MariaDBContainer<>(MARIADB_IMAGE).withDatabaseName("tenantservice");
      mariadb.start();
    }
    registry.add("spring.datasource.url", mariadb::getJdbcUrl);
    registry.add("spring.datasource.username", mariadb::getUsername);
    registry.add("spring.datasource.password", mariadb::getPassword);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void freshDatabase_afterLiquibaseUpdate_shouldPassHibernateValidate() {
    // Context startup already proves the core claim: Liquibase ran the full master changelog
    // and Hibernate validated the entity model against the resulting schema.
    Integer applied =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class);
    assertThat(applied).as("applied Liquibase changesets").isGreaterThanOrEqualTo(20);
  }

  @Test
  void allSequences_shouldExistWithLowercaseNames() {
    // Hibernate's physical naming strategy lowercases sequence names; on case-sensitive Linux
    // MariaDB an UPPERCASE sequence breaks inserts at runtime even though ddl-auto=validate
    // does not flag it. Guard the exact (binary) names.
    for (String sequence :
        new String[] {
          "sequence_tenant",
          "sequence_tenant_admin_controls",
          "sequence_tenant_dpa_signature",
          "sequence_tenant_dpa_version",
          "sequence_tenant_dpa_admin_signature"
        }) {
      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.tables"
                  + " WHERE table_schema = DATABASE() AND table_type = 'SEQUENCE'"
                  + " AND CAST(table_name AS BINARY) = CAST(? AS BINARY)",
              Integer.class,
              sequence);
      assertThat(count).as("lowercase sequence %s", sequence).isEqualTo(1);
    }
  }

  @Test
  void allEntityTables_shouldExist() {
    for (String table :
        new String[] {
          "tenant",
          "tenant_admin_controls",
          "tenant_dpa_signature",
          "tenant_dpa_version",
          "tenant_dpa_admin_signature"
        }) {
      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.tables"
                  + " WHERE table_schema = DATABASE() AND table_name = ?",
              Integer.class,
              table);
      assertThat(count).as("table %s", table).isEqualTo(1);
    }
  }

  @Test
  void tenantPermissionPolicyForeignKey_shouldCascadeTenantDeletion() {
    String deleteRule =
        jdbcTemplate.queryForObject(
            "SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS"
                + " WHERE CONSTRAINT_SCHEMA = DATABASE()"
                + " AND CONSTRAINT_NAME = 'fk_tenant_permission_policy_tenant'",
            String.class);

    assertThat(deleteRule).as("tenant permission-policy delete rule").isEqualTo("CASCADE");
  }

  @Test
  void tenantTheming_shouldCarryBothAccentsAndTheSignalColour() {
    // ORISO-TenantService#154: the light accent and the signal colour used to have no column at
    // all, so the Admin panel's values were accepted and dropped. ddl-auto=validate would catch a
    // missing column via the entity, but only as an opaque context-startup failure - name them.
    for (String column :
        new String[] {"theming_primary_color", "theming_accent", "theming_signal"}) {
      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.columns"
                  + " WHERE table_schema = DATABASE() AND table_name = 'tenant'"
                  + " AND column_name = ?",
              Integer.class,
              column);
      assertThat(count).as("tenant column %s", column).isEqualTo(1);
    }
  }
}
