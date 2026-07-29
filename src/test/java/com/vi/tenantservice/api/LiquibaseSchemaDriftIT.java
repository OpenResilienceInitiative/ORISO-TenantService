package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Permanent schema-drift guard (Liquibase Re-Enablement Plan 2026-07-04, package L1).
 *
 * <p>Boots the application against a real (fresh or already-migrated) MariaDB: Liquibase applies
 * the single master changelog, then Hibernate runs {@code ddl-auto=validate} against the JPA entity
 * model. If the changelog and the entity model ever drift apart, the context fails to start and
 * this test goes red.
 *
 * <p>The test is disabled unless {@code LIQUIBASE_IT_DB_URL} is set, so the default CI build is
 * unaffected. Run it locally with e.g.:
 *
 * <pre>
 * docker run -d --name l1-tenant-mariadb -p 3311:3306 \
 *   -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE=tenantservice mariadb:11.0.6
 * LIQUIBASE_IT_DB_URL='jdbc:mariadb://localhost:3311/tenantservice' \
 *   ./mvnw surefire:test -Dtest=LiquibaseSchemaDriftIT
 * </pre>
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.datasource.url=${LIQUIBASE_IT_DB_URL}",
      "spring.datasource.username=${LIQUIBASE_IT_DB_USERNAME:root}",
      "spring.datasource.password=${LIQUIBASE_IT_DB_PASSWORD:root}",
      "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
      "spring.liquibase.enabled=true",
      "spring.liquibase.change-log=classpath:db/changelog/tenantservice-master.xml",
      "spring.liquibase.contexts=seed",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@EnabledIfEnvironmentVariable(named = "LIQUIBASE_IT_DB_URL", matches = ".+")
class LiquibaseSchemaDriftIT {

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
          "sequence_tenant_dpa_version"
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
          "tenant", "tenant_admin_controls", "tenant_dpa_signature", "tenant_dpa_version"
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
