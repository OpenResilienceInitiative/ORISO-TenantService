package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.MariaDBContainer;

/** Runs the actual additive migration on both supported engines with legacy data. */
class PlatformDpiaSingletonMigrationIT {
  private static final String LEGACY =
      "db/changelog/changeset/0028_platform_dpia_master_data/0028-changeSet.xml";
  private static final String MIGRATION =
      "db/changelog/changeset/0034_platform_dpia_singleton/0034-changeSet.xml";
  private static final String INSERT =
      """
      INSERT INTO platform_dpia_master_data (
        id, operator_legal_name, operator_short_name, operator_address,
        operator_contact_email, operator_contact_phone, operator_dpo_name,
        operator_department, operator_responsible_person, supervisory_legal_framework,
        supervisory_authority_name, supervisory_authority_address, supervisory_authority_email,
        document_date, document_next_review_date, key_figure_tenants, key_figure_tenants_as_of,
        key_figure_counselling_centres, key_figure_counselling_centres_as_of,
        key_figure_active_counsellors, key_figure_active_counsellors_as_of,
        key_figure_registered_clients, key_figure_registered_clients_as_of, update_date)
      VALUES (?, 'Original operator', 'Original short name', 'Original address',
        'operator@example.org', '+49 123', 'Original DPO', 'Original department',
        'Original responsible person', 'GDPR', 'Original authority', 'Authority address',
        'authority@example.org', '2026-08-01', '2027-08-01', 17, '2026-07-01',
        31, '2026-07-02', 123, '2026-07-03', 456, '2026-07-04', '2026-08-02 12:34:56')
      """;
  private static MariaDBContainer<?> mariadb;

  enum Engine {
    H2,
    MARIADB
  }

  @AfterAll
  static void stopDatabase() {
    if (mariadb != null) {
      mariadb.stop();
    }
  }

  @ParameterizedTest
  @EnumSource(Engine.class)
  void emptyTable_shouldRemainEmptyAndRejectAnyOtherId(Engine engine) throws Exception {
    try (Connection connection = legacyDatabase(engine)) {
      apply(connection, MIGRATION);
      var jdbc = jdbc(connection);
      assertThat(
              jdbc.queryForObject("SELECT COUNT(*) FROM platform_dpia_master_data", Integer.class))
          .isZero();
      assertThatThrownBy(() -> jdbc.update(INSERT, 42L))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
      jdbc.update(INSERT, 1L);
      assertThatThrownBy(() -> jdbc.update(INSERT, 2L))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
      assertThatThrownBy(
              () -> jdbc.update("UPDATE platform_dpia_master_data SET id = 2 WHERE id = 1"))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
  }

  @ParameterizedTest
  @EnumSource(Engine.class)
  void singleLegacyRow_shouldKeepEveryValueExceptNormalizedId(Engine engine) throws Exception {
    for (long legacyId : new long[] {1L, 42L}) {
      try (Connection connection = legacyDatabase(engine)) {
        var jdbc = jdbc(connection);
        jdbc.update(INSERT, legacyId);
        var before = jdbc.queryForMap("SELECT * FROM platform_dpia_master_data");
        apply(connection, MIGRATION);
        var after = jdbc.queryForMap("SELECT * FROM platform_dpia_master_data");
        assertThat(jdbc.queryForObject("SELECT id FROM platform_dpia_master_data", Long.class))
            .isEqualTo(1L);
        before.remove("id");
        after.remove("id");
        assertThat(after).isEqualTo(before);
      }
    }
  }

  @ParameterizedTest
  @EnumSource(Engine.class)
  void duplicateLegacyRows_shouldHaltWithoutChangingRowsOrMigrationLedger(Engine engine)
      throws Exception {
    try (Connection connection = legacyDatabase(engine)) {
      var jdbc = jdbc(connection);
      jdbc.update(INSERT, 1L);
      jdbc.update(INSERT, 42L);
      jdbc.update(
          "UPDATE platform_dpia_master_data SET operator_legal_name = 'Different operator' WHERE id = 42");
      var before = jdbc.queryForList("SELECT * FROM platform_dpia_master_data ORDER BY id");
      var ledger = jdbc.queryForList("SELECT * FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED");
      assertThatThrownBy(() -> apply(connection, MIGRATION))
          .isInstanceOf(LiquibaseException.class)
          .hasStackTraceContaining("multiple platform DPIA master-data rows");
      assertThat(jdbc.queryForList("SELECT * FROM platform_dpia_master_data ORDER BY id"))
          .isEqualTo(before);
      assertThat(jdbc.queryForList("SELECT * FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED"))
          .isEqualTo(ledger);
    }
  }

  @ParameterizedTest
  @EnumSource(Engine.class)
  void rollback_shouldRemoveConstraintWithoutDeletingOrRevertingPayload(Engine engine)
      throws Exception {
    try (Connection connection = legacyDatabase(engine)) {
      var jdbc = jdbc(connection);
      jdbc.update(INSERT, 42L);
      apply(connection, MIGRATION);
      var migrated = jdbc.queryForList("SELECT * FROM platform_dpia_master_data");
      liquibase(connection, MIGRATION).rollback(2, "");
      assertThat(jdbc.queryForList("SELECT * FROM platform_dpia_master_data")).isEqualTo(migrated);
      jdbc.update(INSERT, 2L);
      assertThat(
              jdbc.queryForObject("SELECT COUNT(*) FROM platform_dpia_master_data", Integer.class))
          .isEqualTo(2);
    }
  }

  @ParameterizedTest
  @EnumSource(Engine.class)
  void committedConstraintWithoutLedger_shouldAllowMigrationRestart(Engine engine)
      throws Exception {
    try (Connection connection = legacyDatabase(engine)) {
      var jdbc = jdbc(connection);
      jdbc.update(INSERT, 42L);
      apply(connection, MIGRATION);
      jdbc.update("DELETE FROM DATABASECHANGELOG WHERE ID = 'constrainPlatformDpiaSingleton'");
      apply(connection, MIGRATION);
      assertThat(jdbc.queryForObject("SELECT id FROM platform_dpia_master_data", Long.class))
          .isEqualTo(1L);
      assertThatThrownBy(() -> jdbc.update(INSERT, 2L))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
  }

  private static Connection legacyDatabase(Engine engine) throws Exception {
    Connection connection;
    if (engine == Engine.H2) {
      connection =
          DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
    } else {
      if (mariadb == null) {
        mariadb = new MariaDBContainer<>("mariadb:10.11.18");
        mariadb.start();
      }
      connection =
          DriverManager.getConnection(
              mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword());
      var jdbc = jdbc(connection);
      jdbc.execute("DROP TABLE IF EXISTS platform_dpia_master_data");
      jdbc.execute("DROP SEQUENCE IF EXISTS sequence_platform_dpia_master_data");
      jdbc.execute("DROP TABLE IF EXISTS DATABASECHANGELOG");
      jdbc.execute("DROP TABLE IF EXISTS DATABASECHANGELOGLOCK");
    }
    apply(connection, engine == Engine.H2 ? "database/PlatformDpiaLegacyH2.xml" : LEGACY);
    return connection;
  }

  private static JdbcTemplate jdbc(Connection connection) {
    return new JdbcTemplate(new SingleConnectionDataSource(connection, true));
  }

  private static Liquibase liquibase(Connection connection, String path) throws Exception {
    return new Liquibase(
        path,
        new ClassLoaderResourceAccessor(),
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection)));
  }

  private static void apply(Connection connection, String path) throws Exception {
    liquibase(connection, path).update(new Contexts(), new LabelExpression());
  }
}
