package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    // Deliberately not a pinned count. A floor like ">= 20" only records how many changesets
    // existed the day it was written: it never fails when a changeset stops being applied, and it
    // has to be edited every time the number climbs. The real invariant — that every changelog the
    // master includes actually ran — is asserted below, derived from the master itself.
    assertThat(applied).as("applied Liquibase changesets").isPositive();
  }

  @Test
  void everyChangelogFileIncludedByTheMaster_shouldHaveBeenApplied() {
    // Derived from the master changelog itself rather than pinned to a count, so adding a
    // changeset needs no edit here, while an <include> the master still declares but that no
    // longer produces a DATABASECHANGELOG row — excluded by a context, renamed underneath the
    // master, or skipped by a precondition path that writes no row — fails loudly.
    //
    // Limitation, stated so nobody over-trusts this: the expectation is read from the same
    // master changelog, so deleting an <include> outright is NOT caught here. That case is
    // covered by ddl-auto=validate above whenever the deleted changeset backs an entity column,
    // and is not covered at all when it does not.
    List<String> includedFiles = includedChangelogFiles();
    assertThat(includedFiles).as("<include file=...> entries in the master changelog").isNotEmpty();

    List<String> appliedFiles =
        jdbcTemplate.queryForList("SELECT DISTINCT FILENAME FROM DATABASECHANGELOG", String.class);

    assertThat(appliedFiles)
        .as("every changelog file included by the master must appear in DATABASECHANGELOG")
        .containsAll(includedFiles);
  }

  @Test
  void appliedChangesets_shouldHaveUniqueIdentity() {
    // Liquibase identity is (id, author, filename). This repo has already collided on
    // changeset numbering (two 0010_* directories, one of which the master deliberately
    // excludes), so guard the invariant instead of trusting the numbering convention.
    List<String> duplicates =
        jdbcTemplate.queryForList(
            "SELECT CONCAT(ID, '::', AUTHOR, '::', FILENAME) FROM DATABASECHANGELOG"
                + " GROUP BY ID, AUTHOR, FILENAME HAVING COUNT(*) > 1",
            String.class);

    assertThat(duplicates).as("colliding Liquibase changeset identities").isEmpty();
  }

  private static List<String> includedChangelogFiles() {
    try (InputStream master =
        new ClassPathResource("db/changelog/tenantservice-master.xml").getInputStream()) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setNamespaceAware(true);
      NodeList includes =
          factory.newDocumentBuilder().parse(master).getElementsByTagNameNS("*", "include");

      List<String> files = new ArrayList<>();
      for (int i = 0; i < includes.getLength(); i++) {
        files.add(((Element) includes.item(i)).getAttribute("file"));
      }
      return files;
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new IllegalStateException("Could not read the master changelog", e);
    }
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
