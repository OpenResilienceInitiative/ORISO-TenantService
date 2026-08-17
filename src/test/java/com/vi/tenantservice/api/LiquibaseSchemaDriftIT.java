package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Permanent schema-drift guard (Liquibase Re-Enablement Plan 2026-07-04, package L1).
 *
 * <p>Boots the application against a throwaway MariaDB container: Liquibase applies the single
 * master changelog, then Hibernate runs {@code ddl-auto=validate} against the JPA entity model. If
 * the changelog and the entity model ever drift apart, the context fails to start and this test
 * goes red.
 *
 * <p>This is the only test in the suite that executes the Liquibase changelog at all. Every other
 * test runs on H2 {@code MODE=MySQL} with {@code spring.liquibase.enabled=false} and {@code
 * ddl-auto=create-drop} (see {@code application-testing.properties}), i.e. against a schema
 * Hibernate derives from the entities — which can never disagree with the entities and therefore
 * can never detect drift.
 *
 * <p>The container is always started, so the test always runs. It was previously gated on the
 * {@code LIQUIBASE_IT_DB_URL} environment variable, which was set nowhere in {@code .github/}: the
 * class reported as "skipped" on every CI run for as long as it existed.
 *
 * <p>The image is pinned to {@code mariadb:10.11}, matching what pre-dev actually runs (see {@code
 * ORISO-Helm/deploy/predev/images.lock.yaml}, {@code online-counseling-mariadb.imageVersion}). The
 * container is fresh on every run and is never pointed at a deployed database, so known
 * hand-patched drift on a live environment cannot make this test red.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@Testcontainers
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

  /** Throwaway, generated per JVM run; never a credential shared with any real environment. */
  private static final String THROWAWAY_PASSWORD = UUID.randomUUID().toString();

  // disabledWithoutDocker stays at its default (false): if the runner has no Docker daemon
  // this test must fail loudly, not quietly skip. Quiet skipping is the defect being fixed.
  @Container
  static final MariaDBContainer MARIADB =
      new MariaDBContainer(DockerImageName.parse("mariadb:10.11"))
          .withDatabaseName("tenantservice")
          .withUsername("tenantservice")
          .withPassword(THROWAWAY_PASSWORD);

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
    registry.add("spring.datasource.username", MARIADB::getUsername);
    registry.add("spring.datasource.password", MARIADB::getPassword);
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void freshDatabase_afterLiquibaseUpdate_shouldPassHibernateValidate() {
    // Context startup already proves the core claim: Liquibase ran the full master changelog
    // and Hibernate validated the entity model against the resulting schema. Assert the
    // changelog table is populated so a schema that appeared some other way (e.g. ddl-auto
    // silently falling back to create) cannot pass this test.
    Integer applied =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class);
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

  /** {@code <include file="...">} paths declared by the master changelog, in declaration order. */
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
}
