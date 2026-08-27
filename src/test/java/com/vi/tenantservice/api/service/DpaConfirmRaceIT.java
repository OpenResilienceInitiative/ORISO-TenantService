package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Two confirmations racing for one tenant must serialise, and exactly one signature may result
 * (ORISO-TenantService#179).
 *
 * <p>This is the demonstration behind {@code confirmSignature}'s lock ordering. The unit test on
 * the same behaviour pins only the call sequence — it cannot show that the lock is real, that two
 * confirmations actually serialise, or what the loser is told. H2 can show all three: it takes row
 * locks and blocks the second writer. What H2 cannot show is InnoDB's deadlock detector or the
 * {@code for update wait} clause, which is why the MariaDB-only case lives in {@link
 * DpaConfirmLockOrderingMariaDbIT}.
 *
 * <p>The database lock timeout is raised for the duration so the loser genuinely waits for the
 * winner to commit and then reports the DEFINED outcome ("already used", HTTP 410) rather than
 * timing out. A timeout is an acceptable production outcome — it is mapped to a retryable 503 — but
 * it would make this test's central assertion nondeterministic.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class DpaConfirmRaceIT {

  private static final long TENANT_ID = 909L;
  private static final LocalDateTime VERSION = LocalDateTime.of(2026, 7, 1, 12, 0, 0);
  private static final int ROUNDS = 5;

  @Autowired private TenantDpaService tenantDpaService;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaSignatureRepository signatureRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    // let the loser wait for the winner instead of timing out, so the assertion is deterministic
    jdbcTemplate.execute("SET DEFAULT_LOCK_TIMEOUT 20000");
    cleanUp();
  }

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM tenant_dpa_signature WHERE tenant_id = ?", TENANT_ID);
    jdbcTemplate.update("DELETE FROM tenant_id_reservation WHERE tenant_id = ?", TENANT_ID);
    tenantRepository.findById(TENANT_ID).ifPresent(tenantRepository::delete);
  }

  private void givenRegisteredTenantWithReservation() {
    reservationRepository.saveAndFlush(
        TenantIdReservationEntity.builder()
            .tenantId(TENANT_ID)
            .token("reservation-token")
            .status(TenantIdReservationStatus.ASSIGNED)
            .createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now())
            .build());
    var tenant = new TenantEntity();
    tenant.setId(TENANT_ID);
    tenant.setName("Race");
    tenant.setSubdomain("race-" + TENANT_ID);
    tenant.setCreateDate(LocalDateTime.now());
    tenant.setContentDataProcessingAgreement("<p>AVV</p>");
    tenant.setContentDataProcessingAgreementActivationDate(VERSION);
    tenantRepository.saveAndFlush(tenant);
  }

  @Test
  void twoConfirmationsForOneTenant_Should_serialiseAndProduceExactlyOneSignature()
      throws Exception {
    for (int round = 0; round < ROUNDS; round++) {
      cleanUp();
      givenRegisteredTenantWithReservation();
      // two DISTINCT tokens for ONE tenant — the shape that used to deadlock, because each
      // confirmation locks its own row and then wants every other pending row of the tenant
      var tokenA =
          tenantDpaService.createSignInvite(
              TENANT_ID, VERSION, Duration.ofDays(14), null, "reservation-token");
      var tokenB =
          tenantDpaService.createSignInvite(
              TENANT_ID, VERSION, Duration.ofDays(14), null, "reservation-token");

      var barrier = new CyclicBarrier(2);
      ExecutorService pool = Executors.newFixedThreadPool(2);
      List<Outcome> outcomes = new ArrayList<>();
      try {
        List<Future<Outcome>> running =
            List.of(pool.submit(confirm(barrier, tokenA)), pool.submit(confirm(barrier, tokenB)));
        for (Future<Outcome> result : running) {
          outcomes.add(result.get(60, TimeUnit.SECONDS));
        }
      } finally {
        pool.shutdownNow();
      }

      // the loser is told the defined thing, never handed a raw failure
      assertThat(outcomes)
          .as("round %s: one signs, the other is told the link is already used", round)
          .containsExactlyInAnyOrder(Outcome.SIGNED, Outcome.REJECTED_AS_ALREADY_USED);

      // and the database agrees: one signature, and no link left alive
      assertThat(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
          .as("round %s: exactly one signature recorded", round)
          .hasSize(1);
      assertThat(
              signatureRepository.findByTenantIdAndDpaVersionAndStatusAndTokenExpiresAtAfter(
                  TENANT_ID, VERSION, DpaSignatureStatus.PENDING, LocalDateTime.now()))
          .as("round %s: the signature invalidated every outstanding link", round)
          .isEmpty();
    }
  }

  private Callable<Outcome> confirm(CyclicBarrier barrier, String rawToken) {
    return () -> {
      barrier.await(20, TimeUnit.SECONDS);
      try {
        tenantDpaService.confirmSignature(
            rawToken, "Erika", "GF", "erika@example.org", "Org", false, "de");
        return Outcome.SIGNED;
      } catch (InvalidDpaSignTokenException alreadyUsed) {
        return Outcome.REJECTED_AS_ALREADY_USED;
      } catch (PessimisticLockingFailureException contended) {
        // production-acceptable (mapped to a retryable 503) but not what this test is pinning;
        // reported distinctly so a timeout can never be mistaken for the defined rejection
        return Outcome.LOCK_TIMEOUT;
      }
    };
  }

  private enum Outcome {
    SIGNED,
    REJECTED_AS_ALREADY_USED,
    LOCK_TIMEOUT
  }
}
