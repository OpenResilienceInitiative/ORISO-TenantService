package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalProposalAdoptionMode;
import com.vi.tenantservice.api.model.TenantLegalProposalAudience;
import com.vi.tenantservice.api.repository.TenantLegalProposalRepository;
import com.vi.tenantservice.api.service.TenantLegalProposalService;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
  @Autowired private TenantLegalProposalService legalProposalService;
  @MockitoSpyBean private TenantLegalProposalRepository legalProposalRepository;

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
          "sequence_tenant_dpa_admin_signature",
          "sequence_tenant_legal_draft",
          "sequence_tenant_legal_proposal",
          "sequence_tenant_legal_proposal_delivery",
          "sequence_tenant_legal_draft_archive"
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
          "tenant_dpa_admin_signature",
          "tenant_legal_draft",
          "tenant_legal_proposal_distribution",
          "tenant_legal_proposal",
          "tenant_legal_proposal_delivery",
          "tenant_legal_draft_archive"
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
  void tenantLegalDraftForeignKey_shouldCascadeTenantDeletion() {
    String deleteRule =
        jdbcTemplate.queryForObject(
            "SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS"
                + " WHERE CONSTRAINT_SCHEMA = DATABASE()"
                + " AND CONSTRAINT_NAME = 'fk_tenant_legal_draft_tenant'",
            String.class);

    assertThat(deleteRule).as("tenant legal-draft delete rule").isEqualTo("CASCADE");
  }

  @Test
  void tenantLegalDraft_shouldSupportPlatformOwnerWithoutTenantZeroAndKeepRealTenantCascade() {
    assertThat(count("tenant", "id", 0L)).as("technical tenant sentinel is not a row").isZero();

    insertDraft(0L, null, "PRIVACY", "platform-privacy");
    insertDraft(0L, null, "IMPRINT", "platform-imprint");
    assertThat(count("tenant_legal_draft", "owner_key", 0L)).isEqualTo(2);
    assertThatThrownBy(() -> insertDraft(0L, null, "PRIVACY", "duplicate-platform-privacy"))
        .as("one platform draft per legal kind")
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

    insertTenant(9_900_001L, "Purged draft owner", "purged-draft-owner");
    insertTenant(9_900_002L, "Retained draft owner", "retained-draft-owner");
    assertThatThrownBy(() -> insertDraft(9_900_099L, 9_900_001L, "IMPRINT", "mismatched-owner"))
        .as("owner key must match its real tenant foreign key")
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    insertDraft(9_900_001L, 9_900_001L, "PRIVACY", "purged-tenant-draft");
    insertDraft(9_900_002L, 9_900_002L, "PRIVACY", "retained-tenant-draft");

    jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", 9_900_001L);

    assertThat(count("tenant_legal_draft", "owner_key", 9_900_001L)).isZero();
    assertThat(count("tenant_legal_draft", "owner_key", 9_900_002L)).isOne();
    assertThat(count("tenant_legal_draft", "owner_key", 0L)).isEqualTo(2);

    jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", 9_900_002L);
    jdbcTemplate.update("DELETE FROM tenant_legal_draft WHERE owner_key = 0");
  }

  private void insertTenant(long id, String name, String subdomain) {
    jdbcTemplate.update(
        "INSERT INTO tenant (id, name, subdomain) VALUES (?, ?, ?)", id, name, subdomain);
  }

  private void insertDraft(long ownerKey, Long tenantId, String kind, String content) {
    jdbcTemplate.update(
        "INSERT INTO tenant_legal_draft "
            + "(id, version, owner_key, tenant_id, kind, content, update_date) "
            + "VALUES (NEXT VALUE FOR sequence_tenant_legal_draft, 0, ?, ?, ?, ?, UTC_TIMESTAMP)",
        ownerKey,
        tenantId,
        kind,
        content);
  }

  private long count(String table, String column, long value) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, value);
  }

  @Test
  void tenantLegalDraft_shouldHoldAPolicyAsLargeAsThePublishedOne() {
    // The published texts live in LONGTEXT columns. A draft column of TEXT (65,535
    // bytes) would reject a multilingual policy that publishes without trouble, and
    // save() reports the failure as a revision conflict, so the admin is told to
    // reload and retry forever instead of learning the real reason.
    String oversized = "<p>" + "a".repeat(100_000) + "</p>";

    insertDraft(0L, null, "PRIVACY", oversized);

    Long stored =
        jdbcTemplate.queryForObject(
            "SELECT CHAR_LENGTH(content) FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'PRIVACY'",
            Long.class);
    assertThat(stored).as("stored draft length").isEqualTo(oversized.length());

    jdbcTemplate.update("DELETE FROM tenant_legal_draft WHERE owner_key = 0");
  }

  @Test
  void legalSnapshots_shouldHoldADraftAsLargeAsTheDraftItself() {
    // Delivery copies the platform draft into a proposal, and replacing a draft copies it into
    // the archive. With TEXT (65,535 bytes) there, a draft that saves fine would fail on
    // distribution or adoption and roll back.
    for (String table : List.of("tenant_legal_proposal", "tenant_legal_draft_archive")) {
      for (String column : List.of("content", "privacy_consent")) {
        String type =
            jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.columns"
                    + " WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                String.class,
                table,
                column);
        assertThat(type).as(table + "." + column).isEqualToIgnoringCase("longtext");
      }
    }
    // The fixed audience of an ALL delivery grows with the number of Träger; TEXT would cap it
    // at a few thousand ids and roll the whole delivery back.
    String audience =
        jdbcTemplate.queryForObject(
            "SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE()"
                + " AND table_name = 'tenant_legal_proposal_distribution'"
                + " AND column_name = 'recipient_ids'",
            String.class);
    assertThat(audience).as("distribution recipient_ids").isEqualToIgnoringCase("longtext");
  }

  @Test
  void tenantLegalDraft_shouldCarryOptionalPrivacyConsent() {
    String nullable =
        jdbcTemplate.queryForObject(
            "SELECT IS_NULLABLE FROM information_schema.columns"
                + " WHERE table_schema = DATABASE() AND table_name = 'tenant_legal_draft'"
                + " AND column_name = 'privacy_consent'",
            String.class);

    assertThat(nullable).as("privacy consent nullability").isEqualTo("YES");
  }

  @Test
  void legalProposalHistoryIndexes_shouldMatchRecipientQueries() {
    for (String index :
        new String[] {
          "idx_tenant_legal_proposal_recipient_history",
          "idx_tenant_legal_proposal_recipient_state",
          "idx_tenant_legal_draft_archive_history"
        }) {
      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.statistics"
                  + " WHERE table_schema = DATABASE() AND index_name = ?",
              Integer.class,
              index);
      assertThat(count).as("legal history index %s", index).isGreaterThan(0);
    }
  }

  @Test
  void tenantDeletion_shouldCascadeOnlyItsProposalRowsAndPreserveDistributionFingerprint() {
    long deletedTenant = 9_900_011L;
    long retainedTenant = 9_900_012L;
    String distribution = "00000000-0000-0000-0000-000000000035";
    insertTenant(deletedTenant, "Deleted proposal recipient", "deleted-proposal-recipient");
    insertTenant(retainedTenant, "Retained proposal recipient", "retained-proposal-recipient");
    jdbcTemplate.update(
        "INSERT INTO tenant_legal_proposal_distribution"
            + " (id, request_key, kind, audience, source_draft_id, source_draft_version,"
            + " source_updated_at, request_fingerprint, recipient_ids, created_by, created_at)"
            + " VALUES (?, ?, 'PRIVACY', 'ALL', 1, 0, UTC_TIMESTAMP, ?, ?, 'platform', UTC_TIMESTAMP)",
        distribution,
        "schema-purge-distribution",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        deletedTenant + "," + retainedTenant);
    insertProposal(distribution, deletedTenant);
    insertProposal(distribution, retainedTenant);
    List<Long> proposalIds =
        jdbcTemplate.queryForList(
            "SELECT id FROM tenant_legal_proposal WHERE distribution_id = ? ORDER BY recipient_tenant_id",
            Long.class,
            distribution);
    for (Long proposalId : proposalIds) {
      jdbcTemplate.update(
          "INSERT INTO tenant_legal_proposal_delivery"
              + " (id, distribution_id, proposal_id)"
              + " VALUES (NEXT VALUE FOR sequence_tenant_legal_proposal_delivery, ?, ?)",
          distribution,
          proposalId);
    }
    insertArchive(deletedTenant);
    insertArchive(retainedTenant);

    jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", deletedTenant);

    assertThat(count("tenant_legal_proposal", "recipient_tenant_id", deletedTenant)).isZero();
    assertThat(count("tenant_legal_proposal", "recipient_tenant_id", retainedTenant)).isOne();
    assertThat(count("tenant_legal_draft_archive", "tenant_id", deletedTenant)).isZero();
    assertThat(count("tenant_legal_draft_archive", "tenant_id", retainedTenant)).isOne();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT recipient_ids FROM tenant_legal_proposal_distribution WHERE id = ?",
                String.class,
                distribution))
        .isEqualTo(deletedTenant + "," + retainedTenant);

    jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", retainedTenant);
    jdbcTemplate.update(
        "DELETE FROM tenant_legal_proposal_distribution WHERE id = ?", distribution);
  }

  @Test
  void concurrentIdenticalDelivery_shouldReturnOneFixedReceiptWithoutConflict() throws Exception {
    long recipient = 9_900_021L;
    insertTenant(recipient, "Concurrent proposal recipient", "concurrent-proposal-recipient");
    jdbcTemplate.update("DELETE FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'PRIVACY'");
    insertDraft(0L, null, "PRIVACY", "{\"de\":\"concurrent source\"}");
    String revision =
        jdbcTemplate.queryForObject(
            "SELECT CONCAT(id, ':', version) FROM tenant_legal_draft"
                + " WHERE owner_key = 0 AND kind = 'PRIVACY'",
            String.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<List<Long>> delivery =
        () -> {
          start.await();
          return legalProposalService
              .deliver(
                  "concurrent-identical-request",
                  TenantLegalDraftKind.PRIVACY,
                  revision,
                  TenantLegalProposalAudience.SELECTED,
                  Set.of(recipient),
                  "platform")
              .stream()
              .map(p -> p.getId())
              .toList();
        };
    Future<List<Long>> first = executor.submit(delivery);
    Future<List<Long>> second = executor.submit(delivery);
    start.countDown();
    try {
      assertThat(first.get(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS))
          .isEqualTo(second.get(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS));
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM tenant_legal_proposal_distribution"
                      + " WHERE request_key = 'concurrent-identical-request'",
                  Long.class))
          .isOne();
      assertThat(count("tenant_legal_proposal", "recipient_tenant_id", recipient)).isOne();
    } finally {
      executor.shutdownNow();
      jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", recipient);
      jdbcTemplate.update(
          "DELETE FROM tenant_legal_proposal_distribution"
              + " WHERE request_key = 'concurrent-identical-request'");
      jdbcTemplate.update(
          "DELETE FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'PRIVACY'");
    }
  }

  @Test
  void concurrentOverlappingDeliveries_shouldReuseCanonicalSnapshotWithoutPartialAudience()
      throws Exception {
    long firstTenant = 9_900_041L;
    long overlapTenant = 9_900_042L;
    long thirdTenant = 9_900_043L;
    insertTenant(firstTenant, "First overlap recipient", "first-overlap-recipient");
    insertTenant(overlapTenant, "Shared overlap recipient", "shared-overlap-recipient");
    insertTenant(thirdTenant, "Third overlap recipient", "third-overlap-recipient");
    jdbcTemplate.update("DELETE FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'IMPRINT'");
    insertDraft(0L, null, "IMPRINT", "{\"de\":\"overlap source\"}");
    String revision =
        jdbcTemplate.queryForObject(
            "SELECT CONCAT(id, ':', version) FROM tenant_legal_draft"
                + " WHERE owner_key = 0 AND kind = 'IMPRINT'",
            String.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Set<Long>> first =
        () -> {
          start.await();
          return legalProposalService
              .deliver(
                  "overlap-request-a",
                  TenantLegalDraftKind.IMPRINT,
                  revision,
                  TenantLegalProposalAudience.SELECTED,
                  Set.of(firstTenant, overlapTenant),
                  "platform")
              .stream()
              .map(p -> p.getRecipientTenantId())
              .collect(java.util.stream.Collectors.toSet());
        };
    Callable<Set<Long>> second =
        () -> {
          start.await();
          return legalProposalService
              .deliver(
                  "overlap-request-b",
                  TenantLegalDraftKind.IMPRINT,
                  revision,
                  TenantLegalProposalAudience.SELECTED,
                  Set.of(overlapTenant, thirdTenant),
                  "platform")
              .stream()
              .map(p -> p.getRecipientTenantId())
              .collect(java.util.stream.Collectors.toSet());
        };
    Future<Set<Long>> firstResult = executor.submit(first);
    Future<Set<Long>> secondResult = executor.submit(second);
    start.countDown();
    try {
      assertThat(firstResult.get(20, TimeUnit.SECONDS))
          .containsExactlyInAnyOrder(firstTenant, overlapTenant);
      assertThat(secondResult.get(20, TimeUnit.SECONDS))
          .containsExactlyInAnyOrder(overlapTenant, thirdTenant);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM tenant_legal_proposal_distribution"
                      + " WHERE request_key IN ('overlap-request-a', 'overlap-request-b')",
                  Long.class))
          .isEqualTo(2);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM tenant_legal_proposal"
                      + " WHERE recipient_tenant_id IN (?, ?, ?)",
                  Long.class,
                  firstTenant,
                  overlapTenant,
                  thirdTenant))
          .isEqualTo(3);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM tenant_legal_proposal_delivery d"
                      + " JOIN tenant_legal_proposal_distribution r ON r.id = d.distribution_id"
                      + " WHERE r.request_key IN ('overlap-request-a', 'overlap-request-b')",
                  Long.class))
          .isEqualTo(4);
    } finally {
      executor.shutdownNow();
      jdbcTemplate.update(
          "DELETE FROM tenant WHERE id IN (?, ?, ?)", firstTenant, overlapTenant, thirdTenant);
      jdbcTemplate.update(
          "DELETE FROM tenant_legal_proposal_distribution"
              + " WHERE request_key IN ('overlap-request-a', 'overlap-request-b')");
      jdbcTemplate.update(
          "DELETE FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'IMPRINT'");
    }
  }

  @Test
  void adoptionFailureAfterDraftWrite_shouldRollBackDraftArchiveAndProposalOnMariaDb() {
    long recipient = 9_900_031L;
    insertTenant(recipient, "Rollback proposal recipient", "rollback-proposal-recipient");
    jdbcTemplate.update("DELETE FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'PRIVACY'");
    insertDraft(0L, null, "PRIVACY", "{\"de\":\"new platform text\"}");
    insertDraft(recipient, recipient, "PRIVACY", "{\"de\":\"recipient edit\"}");
    String sourceRevision =
        jdbcTemplate.queryForObject(
            "SELECT CONCAT(id, ':', version) FROM tenant_legal_draft"
                + " WHERE owner_key = 0 AND kind = 'PRIVACY'",
            String.class);
    String draftRevision =
        jdbcTemplate.queryForObject(
            "SELECT CONCAT(id, ':', version) FROM tenant_legal_draft"
                + " WHERE owner_key = ? AND kind = 'PRIVACY'",
            String.class,
            recipient);
    var proposal =
        legalProposalService
            .deliver(
                "rollback-after-draft-write",
                TenantLegalDraftKind.PRIVACY,
                sourceRevision,
                TenantLegalProposalAudience.SELECTED,
                Set.of(recipient),
                "platform")
            .getFirst();
    doThrow(new org.springframework.dao.DataIntegrityViolationException("injected proposal write"))
        .when(legalProposalRepository)
        .saveAndFlush(any());

    try {
      assertThatThrownBy(
              () ->
                  legalProposalService.adopt(
                      recipient,
                      proposal.getId(),
                      TenantLegalProposalAdoptionMode.ARCHIVE_AND_REPLACE,
                      legalProposalService.revision(proposal),
                      draftRevision,
                      "recipient"))
          .isInstanceOf(SettingsUpdateConflictException.class);
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT content FROM tenant_legal_draft"
                      + " WHERE owner_key = ? AND kind = 'PRIVACY'",
                  String.class,
                  recipient))
          .isEqualTo("{\"de\":\"recipient edit\"}");
      assertThat(count("tenant_legal_draft_archive", "tenant_id", recipient)).isZero();
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT status FROM tenant_legal_proposal WHERE id = ?",
                  String.class,
                  proposal.getId()))
          .isEqualTo("PENDING");
    } finally {
      reset(legalProposalRepository);
      jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", recipient);
      jdbcTemplate.update(
          "DELETE FROM tenant_legal_proposal_distribution"
              + " WHERE request_key = 'rollback-after-draft-write'");
      jdbcTemplate.update(
          "DELETE FROM tenant_legal_draft WHERE owner_key = 0 AND kind = 'PRIVACY'");
    }
  }

  private void insertProposal(String distribution, long recipient) {
    jdbcTemplate.update(
        "INSERT INTO tenant_legal_proposal"
            + " (id, version, recipient_tenant_id, kind, distribution_id, audience,"
            + " source_draft_id, source_draft_version, source_updated_at, content, status,"
            + " created_by, created_at) VALUES"
            + " (NEXT VALUE FOR sequence_tenant_legal_proposal, 0, ?, 'PRIVACY', ?, 'ALL',"
            + " 1, 0, UTC_TIMESTAMP, '{}', 'PENDING', 'platform', UTC_TIMESTAMP)",
        recipient,
        distribution);
  }

  private void insertArchive(long tenantId) {
    jdbcTemplate.update(
        "INSERT INTO tenant_legal_draft_archive"
            + " (id, tenant_id, kind, draft_row_id, draft_revision, content, draft_saved_at,"
            + " archived_by, archived_at) VALUES"
            + " (NEXT VALUE FOR sequence_tenant_legal_draft_archive, ?, 'PRIVACY', 1, '1:0', '{}',"
            + " UTC_TIMESTAMP, 'recipient', UTC_TIMESTAMP)",
        tenantId);
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
