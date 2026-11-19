package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Self-contained persistence test for the AVV / Data Processing Agreement model. The module's other
 * {@code @DataJpaTest} repository tests assume an externally provisioned schema (the configured
 * MariaDB dialect produces DDL that an empty embedded H2 cannot build), so this test overrides the
 * dialect to H2 and lets Hibernate create the schema from the entities — making it runnable and
 * meaningful in a bare local build.
 */
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ExtendWith(SpringExtension.class)
@DataJpaTest
class TenantDpaSignatureRepositoryTest {

  private static final LocalDateTime VERSION = LocalDateTime.of(2026, 7, 1, 12, 0);

  @Autowired private TenantDpaSignatureRepository signatureRepository;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  void tenantEntity_Should_persistDataProcessingAgreementFields() {
    // given
    TenantEntity entity = new TenantEntity();
    entity.setName("avv tenant");
    entity.setSubdomain("avv-subdomain");
    entity.setCreateDate(LocalDateTime.now());
    entity.setContentDataProcessingAgreement("<p>Auftragsverarbeitungsvertrag</p>");
    entity.setContentDataProcessingAgreementActivationDate(LocalDateTime.now());

    // when
    TenantEntity saved = tenantRepository.save(entity);
    tenantRepository.flush();

    // then
    Optional<TenantEntity> reloaded = tenantRepository.findById(saved.getId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getContentDataProcessingAgreement())
        .isEqualTo("<p>Auftragsverarbeitungsvertrag</p>");
    assertThat(reloaded.get().getContentDataProcessingAgreementActivationDate()).isNotNull();
  }

  @Test
  void save_Should_persistSignatureAndFindByTenantId() {
    // given
    var now = LocalDateTime.now();
    TenantDpaSignatureEntity signature =
        TenantDpaSignatureEntity.builder()
            .tenantId(1L)
            .dpaVersion(now.minusDays(1))
            .signerName("Erika Mustermann")
            .signerPosition("Geschäftsführerin")
            .signerIsMember(false)
            .language("de")
            .status(DpaSignatureStatus.SIGNED)
            .signedAt(now)
            .createDate(now)
            .build();

    // when
    signatureRepository.save(signature);
    signatureRepository.flush();

    // then
    List<TenantDpaSignatureEntity> found = signatureRepository.findByTenantId(1L);
    assertThat(found).hasSize(1);
    var saved = found.get(0);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getSignerName()).isEqualTo("Erika Mustermann");
    assertThat(saved.getSignerPosition()).isEqualTo("Geschäftsführerin");
    assertThat(saved.getSignerIsMember()).isFalse();
    assertThat(saved.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(saved.getLanguage()).isEqualTo("de");
  }

  @Test
  void findByTenantIdAndStatus_Should_filterByStatus() {
    // given
    var now = LocalDateTime.now();
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(2L)
            .status(DpaSignatureStatus.SIGNED)
            .signerName("Signed Person")
            .dpaVersion(now)
            .createDate(now)
            .build());
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(2L)
            .status(DpaSignatureStatus.DENIED)
            .signerName("Denied Person")
            .dpaVersion(now)
            .createDate(now)
            .build());
    signatureRepository.flush();

    // when
    var signed = signatureRepository.findByTenantIdAndStatus(2L, DpaSignatureStatus.SIGNED);

    // then
    assertThat(signed).hasSize(1);
    assertThat(signed.get(0).getSignerName()).isEqualTo("Signed Person");
  }

  @Test
  void consumeSignToken_Should_signExactlyOnce_andRejectReuse() {
    // given a PENDING row carrying a token and the forwarder identity stamped at creation (#179)
    var now = LocalDateTime.now();
    var pending =
        signatureRepository.saveAndFlush(
            TenantDpaSignatureEntity.builder()
                .tenantId(3L)
                .status(DpaSignatureStatus.PENDING)
                .forwardedByUserId("admin-1")
                .source("FORWARDED_EXTERNAL")
                .tokenHash("HASH")
                .tokenExpiresAt(now.plusDays(1))
                .createDate(now)
                .build());

    // when the first consume wins
    int first =
        signatureRepository.consumeSignToken(
            "HASH",
            "Erika",
            "GF",
            "e@example.org",
            "Caritas",
            false,
            "de",
            "FORWARDED_EXTERNAL",
            now,
            1_700_000_000_000L,
            true);
    // and a second consume of the same token affects nothing (single-use)
    int second =
        signatureRepository.consumeSignToken(
            "HASH",
            "Mallory",
            "X",
            "m@example.org",
            "Bad Org",
            true,
            "en",
            "FORWARDED_EXTERNAL",
            now,
            1_800_000_000_000L,
            false);
    signatureRepository.flush();

    // then
    assertThat(first).isEqualTo(1);
    assertThat(second).isZero();
    var reloaded = signatureRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(reloaded.getCurrentVersionWhenSigned()).isTrue();
    assertThat(reloaded.getSignedAtEpochMillis()).isEqualTo(1_700_000_000_000L);
    assertThat(adminNoticePendingSince(reloaded.getId())).isEqualTo(1_700_000_000_000L);
    assertThat(reloaded.getSignerName()).isEqualTo("Erika"); // not overwritten by the 2nd attempt
    assertThat(reloaded.getSignerEmail()).isEqualTo("e@example.org");
    assertThat(reloaded.getSignerOrganisation()).isEqualTo("Caritas");
    // the stamped forwarder identity survives the consume untouched (#179)
    assertThat(reloaded.getForwardedByUserId()).isEqualTo("admin-1");
    assertThat(reloaded.getSource()).isEqualTo("FORWARDED_EXTERNAL");
    assertThat(reloaded.getTokenHash()).isNull();
    assertThat(signatureRepository.findByTokenHashAndStatus("HASH", DpaSignatureStatus.PENDING))
        .isEmpty();
  }

  @Test
  void adminNoticeStaging_Should_leaveHistoricalAndOtherRowsUnmarked() {
    var now = LocalDateTime.now();
    var historical =
        signatureRepository.saveAndFlush(
            TenantDpaSignatureEntity.builder()
                .tenantId(94L)
                .status(DpaSignatureStatus.SIGNED)
                .source("FORWARDED_EXTERNAL")
                .signedAt(now.minusDays(1))
                .signedAtEpochMillis(1_699_999_999_999L)
                .createDate(now.minusDays(1))
                .build());
    var direct =
        signatureRepository.saveAndFlush(
            TenantDpaSignatureEntity.builder()
                .tenantId(94L)
                .status(DpaSignatureStatus.SIGNED)
                .source("WIZARD_SELF")
                .signedAt(now)
                .signedAtEpochMillis(1_700_000_000_000L)
                .createDate(now)
                .build());
    var outstanding = pendingLink(94L, "HASH-NOT-CONFIRMED", now);
    signatureRepository.invalidateOutstandingByTenantId(94L);

    assertThat(adminNoticePendingSince(historical.getId())).isNull();
    assertThat(adminNoticePendingSince(direct.getId())).isNull();
    assertThat(adminNoticePendingSince(outstanding.getId())).isNull();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void adminNoticeStaging_Should_rollBackWithConfirmation() {
    var now = LocalDateTime.now();
    var pending =
        signatureRepository.saveAndFlush(boundPendingLink(95L, "HASH-ROLLBACK", "OWNER", now));
    var transaction = new TransactionTemplate(transactionManager);
    try {
      transaction.executeWithoutResult(
          status -> {
            assertThat(consumeToken("HASH-ROLLBACK", now, 1_700_000_000_000L)).isEqualTo(1);
            assertThat(adminNoticePendingSince(pending.getId())).isEqualTo(1_700_000_000_000L);
            status.setRollbackOnly();
          });
      assertThat(signatureRepository.findById(pending.getId()).orElseThrow().getStatus())
          .isEqualTo(DpaSignatureStatus.PENDING);
      assertThat(adminNoticePendingSince(pending.getId())).isNull();
    } finally {
      signatureRepository.deleteById(pending.getId());
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void adminNoticeStaging_Should_useTheWinningConcurrentConfirmation() throws Exception {
    var now = LocalDateTime.now();
    var pending =
        signatureRepository.saveAndFlush(boundPendingLink(96L, "HASH-RACE", "OWNER", now));
    var transaction = new TransactionTemplate(transactionManager);
    var ready = new CyclicBarrier(2);
    long firstSigningInstant = 1_700_000_000_000L;
    long secondSigningInstant = 1_700_000_000_001L;
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> {
                ready.await(10, TimeUnit.SECONDS);
                return transaction.execute(
                    status -> consumeToken("HASH-RACE", now, firstSigningInstant));
              });
      var second =
          executor.submit(
              () -> {
                ready.await(10, TimeUnit.SECONDS);
                return transaction.execute(
                    status -> consumeToken("HASH-RACE", now, secondSigningInstant));
              });
      int firstCount = first.get(15, TimeUnit.SECONDS);
      assertThat(List.of(firstCount, second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(0, 1);
      long winningInstant = firstCount == 1 ? firstSigningInstant : secondSigningInstant;
      var signed = signatureRepository.findById(pending.getId()).orElseThrow();
      assertThat(signed.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
      assertThat(signed.getSignedAtEpochMillis()).isEqualTo(winningInstant);
      assertThat(signed.getReservationTokenHash()).isEqualTo("OWNER");
      assertThat(adminNoticePendingSince(pending.getId())).isEqualTo(winningInstant);
    } finally {
      signatureRepository.deleteById(pending.getId());
    }
  }

  private int consumeToken(String tokenHash, LocalDateTime now, long signingInstant) {
    return signatureRepository.consumeSignToken(
        tokenHash,
        "Signer",
        "Director",
        "signer@example.org",
        "Test organisation",
        false,
        "de",
        "FORWARDED_EXTERNAL",
        now,
        signingInstant,
        true);
  }

  private Long adminNoticePendingSince(Long signatureId) {
    return jdbcTemplate.queryForObject(
        "select admin_notice_pending_since_epoch_millis from tenant_dpa_signature where id = ?",
        Long.class,
        signatureId);
  }

  @Test
  void invalidateOutstandingByTenantId_Should_killOnlyPendingRowsOfThatTenant() {
    // given: two outstanding links for tenant 4, a signed row for tenant 4, a link for tenant 5
    var now = LocalDateTime.now();
    var outstanding1 = pendingLink(4L, "HASH-A", now);
    var outstanding2 = pendingLink(4L, "HASH-B", now);
    var signedRow =
        signatureRepository.save(
            TenantDpaSignatureEntity.builder()
                .tenantId(4L)
                .status(DpaSignatureStatus.SIGNED)
                .createDate(now)
                .build());
    var otherTenantLink = pendingLink(5L, "HASH-C", now);
    signatureRepository.flush();

    // when
    int invalidated = signatureRepository.invalidateOutstandingByTenantId(4L);
    signatureRepository.flush();

    // then
    assertThat(invalidated).isEqualTo(2);
    assertThat(signatureRepository.findById(outstanding1.getId()).orElseThrow().getStatus())
        .isEqualTo(DpaSignatureStatus.INVALIDATED);
    assertThat(signatureRepository.findById(outstanding2.getId()).orElseThrow().getTokenHash())
        .isNull();
    assertThat(signatureRepository.findById(signedRow.getId()).orElseThrow().getStatus())
        .isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(signatureRepository.findById(otherTenantLink.getId()).orElseThrow().getStatus())
        .isEqualTo(DpaSignatureStatus.PENDING);
    // an invalidated link no longer resolves as PENDING -> public endpoints answer 410
    assertThat(signatureRepository.findByTokenHashAndStatus("HASH-A", DpaSignatureStatus.PENDING))
        .isEmpty();
  }

  @Test
  void outstandingLinkPredicate_Should_seeOnlyUnexpiredPendingLinks() {
    // given one expired and one live link for tenant 6
    var now = LocalDateTime.now();
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(6L)
            .dpaVersion(VERSION)
            .status(DpaSignatureStatus.PENDING)
            .tokenHash("HASH-EXPIRED")
            .tokenExpiresAt(now.minusMinutes(1))
            .createDate(now)
            .build());
    signatureRepository.flush();

    assertThat(
            signatureRepository.findByTenantIdAndDpaVersionAndStatusAndTokenExpiresAtAfter(
                6L, VERSION, DpaSignatureStatus.PENDING, now))
        .isEmpty();

    pendingLink(6L, "HASH-LIVE", now);
    signatureRepository.flush();

    assertThat(
            signatureRepository.findByTenantIdAndDpaVersionAndStatusAndTokenExpiresAtAfter(
                6L, VERSION, DpaSignatureStatus.PENDING, now))
        .hasSize(1);
  }

  @Test
  void outstandingLinkPredicate_Should_ignoreLinksForASupersededVersion() {
    // given a live link minted for the PREVIOUS version, then a republish (#179)
    var now = LocalDateTime.now();
    var supersededVersion = VERSION.minusDays(10);
    pendingLink(10L, "HASH-OLD-VERSION", supersededVersion, now);
    signatureRepository.flush();

    // the stale link can only ever produce an OUTDATED signature, so the CURRENT contract is not
    // "awaiting a signer" because of it
    assertThat(
            signatureRepository.findByTenantIdAndDpaVersionAndStatusAndTokenExpiresAtAfter(
                10L, VERSION, DpaSignatureStatus.PENDING, now))
        .isEmpty();
    assertThat(
            signatureRepository.findByTenantIdAndDpaVersionAndStatusAndTokenExpiresAtAfter(
                10L, supersededVersion, DpaSignatureStatus.PENDING, now))
        .hasSize(1);
  }

  @Test
  void outstandingCount_Should_beScopedToTheReservationThatMintedTheLinks() {
    // tenant 11 was reserved once (A), released, then reserved again (B). A's links are already
    // unusable, so they must not count against B's budget.
    var now = LocalDateTime.now();
    var hashA = "HASH-RESERVATION-A";
    var hashB = "HASH-RESERVATION-B";
    signatureRepository.save(boundPendingLink(11L, "L1", hashA, now));
    signatureRepository.save(boundPendingLink(11L, "L2", hashA, now));
    signatureRepository.save(boundPendingLink(11L, "L3", hashB, now));
    signatureRepository.flush();

    assertThat(
            signatureRepository
                .countByTenantIdAndReservationTokenHashAndStatusAndTokenExpiresAtAfter(
                    11L, hashB, DpaSignatureStatus.PENDING, now))
        .isEqualTo(1);
    assertThat(
            signatureRepository
                .countByTenantIdAndReservationTokenHashAndStatusAndTokenExpiresAtAfter(
                    11L, hashA, DpaSignatureStatus.PENDING, now))
        .isEqualTo(2);
  }

  private TenantDpaSignatureEntity boundPendingLink(
      Long tenantId, String tokenHash, String reservationTokenHash, LocalDateTime now) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(tenantId)
        .dpaVersion(VERSION)
        .status(DpaSignatureStatus.PENDING)
        .tokenHash(tokenHash)
        .reservationTokenHash(reservationTokenHash)
        .tokenExpiresAt(now.plusDays(1))
        .createDate(now)
        .build();
  }

  @Test
  void deleteByTenantId_Should_removeAllRowsOfTheTenant() {
    // given
    var now = LocalDateTime.now();
    pendingLink(7L, "HASH-D", now);
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(7L)
            .status(DpaSignatureStatus.SIGNED)
            .createDate(now)
            .build());
    pendingLink(8L, "HASH-E", now);
    signatureRepository.flush();

    // when
    long removed = signatureRepository.deleteByTenantId(7L);
    signatureRepository.flush();

    // then
    assertThat(removed).isEqualTo(2);
    assertThat(signatureRepository.findByTenantId(7L)).isEmpty();
    assertThat(signatureRepository.findByTenantId(8L)).hasSize(1);
  }

  private TenantDpaSignatureEntity pendingLink(Long tenantId, String tokenHash, LocalDateTime now) {
    return pendingLink(tenantId, tokenHash, VERSION, now);
  }

  private TenantDpaSignatureEntity pendingLink(
      Long tenantId, String tokenHash, LocalDateTime dpaVersion, LocalDateTime now) {
    return signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(dpaVersion)
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(tokenHash)
            .tokenExpiresAt(now.plusDays(1))
            .createDate(now)
            .build());
  }

  @Test
  void deleteByStatusAndCreateDateBefore_Should_removeOnlyDeniedBeforeCutoff() {
    var now = LocalDateTime.now();
    var cutoff = now.minusDays(365);
    signatureRepository.save(denied(9L, now.minusDays(400))); // old denied -> purge
    signatureRepository.save(denied(9L, now.minusDays(10))); // recent denied -> keep
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(9L)
            .status(DpaSignatureStatus.SIGNED)
            .createDate(now.minusDays(400))
            .build()); // old but SIGNED -> keep
    signatureRepository.flush();

    long removed =
        signatureRepository.deleteByStatusAndCreateDateBefore(DpaSignatureStatus.DENIED, cutoff);

    assertThat(removed).isEqualTo(1);
    assertThat(signatureRepository.findByTenantId(9L)).hasSize(2);
  }

  @Test
  void deleteByStatusAndCreateDateBefore_Should_keepRowExactlyAtCutoff() {
    // strict "before" window: a DENIED row created exactly at the cutoff must be kept, not purged
    var cutoff = LocalDateTime.now().minusDays(365).truncatedTo(ChronoUnit.SECONDS);
    signatureRepository.save(denied(11L, cutoff.minusSeconds(1))); // just before -> purge
    signatureRepository.save(denied(11L, cutoff)); // exactly at cutoff -> keep
    signatureRepository.flush();

    long removed =
        signatureRepository.deleteByStatusAndCreateDateBefore(DpaSignatureStatus.DENIED, cutoff);

    assertThat(removed).isEqualTo(1);
    var survivors = signatureRepository.findByTenantId(11L);
    assertThat(survivors).hasSize(1);
    assertThat(survivors.get(0).getCreateDate()).isEqualTo(cutoff);
  }

  private TenantDpaSignatureEntity denied(Long tenantId, java.time.LocalDateTime createDate) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(tenantId)
        .status(DpaSignatureStatus.DENIED)
        .createDate(createDate)
        .build();
  }
}
