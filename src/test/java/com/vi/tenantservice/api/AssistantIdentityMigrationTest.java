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

class AssistantIdentityMigrationTest {
  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void partialExistingColumnsAreCompletedAndRollbackPreservesPreexistingColumns(int existing)
      throws Exception {
    try (var connection =
        DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
      var statement = connection.createStatement();
      statement.execute("CREATE TABLE tenant (id BIGINT PRIMARY KEY)");
      if ((existing & 1) != 0)
        statement.execute("ALTER TABLE tenant ADD theming_assistant_name VARCHAR(80)");
      if ((existing & 2) != 0)
        statement.execute("ALTER TABLE tenant ADD theming_assistant_icon LONGTEXT");
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      var migration =
          new Liquibase(
              "db/changelog/changeset/0041_assistant_identity/0041-changeSet.xml",
              new ClassLoaderResourceAccessor(),
              database);
      migration.update(new Contexts(), new LabelExpression());
      migration.update(new Contexts(), new LabelExpression());
      assertThat(hasColumn(connection, "THEMING_ASSISTANT_NAME")).isTrue();
      assertThat(hasColumn(connection, "THEMING_ASSISTANT_ICON")).isTrue();
      migration.rollback(2 - Integer.bitCount(existing), new Contexts(), new LabelExpression());
      assertThat(hasColumn(connection, "THEMING_ASSISTANT_NAME")).isEqualTo((existing & 1) != 0);
      assertThat(hasColumn(connection, "THEMING_ASSISTANT_ICON")).isEqualTo((existing & 2) != 0);
    }
  }

  private boolean hasColumn(java.sql.Connection connection, String column) throws Exception {
    try (var columns = connection.getMetaData().getColumns(null, null, "TENANT", column)) {
      return columns.next();
    }
  }
}
