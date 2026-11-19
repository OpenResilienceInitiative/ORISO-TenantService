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
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.MariaDBContainer;

/** Exercises the real receipt migrations against interrupted DDL histories. */
class DpaAdminNoticeMigrationConvergenceIT {
  private static final String PROVENANCE =
      "db/changelog/changeset/0048_dpa_admin_notice_handoff_provenance/0048-changeSet.xml";
  private static final String INDEX =
      "db/changelog/changeset/0049_dpa_admin_notice_pending_index/0049-changeSet.xml";
  private static final String ADD_PROVENANCE =
      "ALTER TABLE tenant_dpa_signature ADD COLUMN "
          + "admin_notice_handoff_signed_at_epoch_millis BIGINT";
  private static final String ADD_STAGE =
      """
      ALTER TABLE tenant_dpa_signature ADD COLUMN admin_notice_pending_stage TINYINT
      GENERATED ALWAYS AS (CASE WHEN signature_status = 'SIGNED' AND source = 'FORWARDED_EXTERNAL'
        AND admin_notice_pending_since_epoch_millis = signed_at_epoch_millis THEN 1 ELSE 0 END)
      """;
  private static final String ADD_INDEX =
      "CREATE INDEX idx_dpa_admin_notice_pending_stage_id ON tenant_dpa_signature";

  @Test
  void h2ReplaysMatchingObjects() throws Exception {
    try (Connection connection = h2()) {
      verifyReplay(connection);
    }
  }

  @Test
  void h2RejectsConflictingDefinitions() throws Exception {
    try (Connection connection = h2()) {
      verifyConflicts(connection);
    }
  }

  @Test
  void mariaDbReplaysMatchingObjectsAndRejectsConflicts() throws Exception {
    try (var mariaDb = new MariaDBContainer<>("mariadb:10.11.18")) {
      mariaDb.start();
      try (Connection connection =
          DriverManager.getConnection(
              mariaDb.getJdbcUrl(), mariaDb.getUsername(), mariaDb.getPassword())) {
        verifyReplay(connection);
        verifyConflicts(connection);
      }
    }
  }

  private static Connection h2() throws Exception {
    return DriverManager.getConnection(
        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MariaDB", "sa", "");
  }

  private static void verifyReplay(Connection connection) throws Exception {
    var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
    for (int preexistingStageObjects = 0; preexistingStageObjects < 3; preexistingStageObjects++) {
      reset(jdbc);
      jdbc.execute(ADD_PROVENANCE);
      jdbc.update(
          """
          INSERT INTO tenant_dpa_signature (
            id, signature_status, source, admin_notice_pending_since_epoch_millis,
            signed_at_epoch_millis, admin_notice_handoff_signed_at_epoch_millis
          ) VALUES (1, 'SIGNED', 'FORWARDED_EXTERNAL', 10, 10, 42),
            (2, 'SIGNED', 'DIRECT', 10, 10, NULL),
            (3, 'PENDING', 'FORWARDED_EXTERNAL', 10, 10, NULL),
            (4, 'SIGNED', 'FORWARDED_EXTERNAL', 11, 10, NULL)
          """);
      if (preexistingStageObjects >= 1) {
        jdbc.execute(ADD_STAGE);
      }
      if (preexistingStageObjects == 2) {
        jdbc.execute(ADD_INDEX + " (admin_notice_pending_stage, id)");
      }
      apply(connection, PROVENANCE);
      apply(connection, INDEX);
      jdbc.update(
          """
          DELETE FROM DATABASECHANGELOG WHERE ID IN
            ('deriveDpaAdminNoticePendingStage', 'indexDpaAdminNoticePendingStageById')
          """);
      apply(connection, PROVENANCE);
      apply(connection, INDEX);
      liquibase(connection, INDEX).rollback(1, new Contexts(), new LabelExpression());
      liquibase(connection, PROVENANCE).rollback(1, new Contexts(), new LabelExpression());
      assertThat(
              jdbc.queryForObject(
                  """
                  SELECT admin_notice_handoff_signed_at_epoch_millis
                    FROM tenant_dpa_signature WHERE id = 1
                  """,
                  Long.class))
          .isEqualTo(42L);
      assertThat(
              jdbc.queryForList(
                  "SELECT admin_notice_pending_stage FROM tenant_dpa_signature ORDER BY id",
                  Integer.class))
          .containsExactly(1, 0, 0, 0);
    }
  }

  private static void verifyConflicts(Connection connection) throws Exception {
    var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
    reset(jdbc);
    jdbc.execute(ADD_PROVENANCE.replace("BIGINT", "VARCHAR(32)"));
    assertThatThrownBy(() -> apply(connection, PROVENANCE))
        .isInstanceOf(LiquibaseException.class)
        .hasStackTraceContaining("incompatible receipt provenance column");

    reset(jdbc);
    jdbc.execute(
        """
        ALTER TABLE tenant_dpa_signature ADD COLUMN admin_notice_pending_stage TINYINT
        GENERATED ALWAYS AS (1)
        """);
    assertThatThrownBy(() -> apply(connection, INDEX))
        .isInstanceOf(LiquibaseException.class)
        .hasStackTraceContaining("incompatible receipt pending-stage column or index");

    reset(jdbc);
    jdbc.execute(ADD_STAGE);
    jdbc.execute(ADD_INDEX + " (id, admin_notice_pending_stage)");
    assertThatThrownBy(() -> apply(connection, INDEX))
        .isInstanceOf(LiquibaseException.class)
        .hasStackTraceContaining("incompatible receipt pending-stage column or index");
  }

  private static void reset(JdbcTemplate jdbc) {
    jdbc.execute("DROP TABLE IF EXISTS tenant_dpa_signature");
    jdbc.execute("DROP TABLE IF EXISTS DATABASECHANGELOG");
    jdbc.execute("DROP TABLE IF EXISTS DATABASECHANGELOGLOCK");
    jdbc.execute(
        """
        CREATE TABLE tenant_dpa_signature (
          id BIGINT PRIMARY KEY, signature_status VARCHAR(32), source VARCHAR(32),
          admin_notice_pending_since_epoch_millis BIGINT, signed_at_epoch_millis BIGINT
        )
        """);
  }

  private static void apply(Connection connection, String changelog) throws Exception {
    liquibase(connection, changelog).update(new Contexts(), new LabelExpression());
  }

  private static Liquibase liquibase(Connection connection, String changelog) throws Exception {
    var database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    return new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
  }
}
