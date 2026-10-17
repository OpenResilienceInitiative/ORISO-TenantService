package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ORISO-Frontend#1737. Each closing-animation column is added on its own, so a database that has
 * only one of them (patched by hand, or half-migrated) ends up with both, and a rollback never
 * drops a column the migration did not add.
 */
class ClosingAnimationMigrationTest {

  private static final String CHANGESET =
      "db/changelog/changeset/0043_tenant_theming_closing_animation/0043-changeSet.xml";

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void partialExistingColumnsAreCompletedAndRollbackPreservesPreexistingColumns(int existing)
      throws Exception {
    try (var connection =
        DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
      var statement = connection.createStatement();
      statement.execute("CREATE TABLE tenant (id BIGINT PRIMARY KEY)");
      if ((existing & 1) != 0) {
        statement.execute("ALTER TABLE tenant ADD theming_closing_animation_desktop VARCHAR(32)");
      }
      if ((existing & 2) != 0) {
        statement.execute("ALTER TABLE tenant ADD theming_closing_animation_mobile VARCHAR(32)");
      }
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      var migration = new Liquibase(CHANGESET, new ClassLoaderResourceAccessor(), database);

      migration.update(new Contexts(), new LabelExpression());
      migration.update(new Contexts(), new LabelExpression());

      assertThat(hasColumn(connection, "THEMING_CLOSING_ANIMATION_DESKTOP")).isTrue();
      assertThat(hasColumn(connection, "THEMING_CLOSING_ANIMATION_MOBILE")).isTrue();

      assertThat(columnType(connection, "THEMING_CLOSING_ANIMATION_DESKTOP"))
          .as("desktop stores the enum name")
          .isEqualTo("VARCHAR(32)");
      assertThat(columnType(connection, "THEMING_CLOSING_ANIMATION_MOBILE"))
          .as("mobile stores the enum name")
          .isEqualTo("VARCHAR(32)");

      migration.rollback(2 - Integer.bitCount(existing), new Contexts(), new LabelExpression());

      assertThat(hasColumn(connection, "THEMING_CLOSING_ANIMATION_DESKTOP"))
          .as("desktop column survives a rollback when it was there before")
          .isEqualTo((existing & 1) != 0);
      assertThat(hasColumn(connection, "THEMING_CLOSING_ANIMATION_MOBILE"))
          .as("mobile column survives a rollback when it was there before")
          .isEqualTo((existing & 2) != 0);
    }
  }

  private String columnType(java.sql.Connection connection, String column) throws Exception {
    try (var columns = connection.getMetaData().getColumns(null, null, "TENANT", column)) {
      assertThat(columns.next()).isTrue();
      return (columns.getInt("DATA_TYPE") == java.sql.Types.VARCHAR ? "VARCHAR" : "OTHER")
          + "("
          + columns.getInt("COLUMN_SIZE")
          + ")";
    }
  }

  private boolean hasColumn(java.sql.Connection connection, String column) throws Exception {
    try (var columns = connection.getMetaData().getColumns(null, null, "TENANT", column)) {
      return columns.next();
    }
  }
}
