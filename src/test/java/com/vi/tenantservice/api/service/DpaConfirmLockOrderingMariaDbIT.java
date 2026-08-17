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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves the confirm-path lock ordering on a REAL MariaDB — the guarantee the ordering exists for
 * cannot be demonstrated anywhere else (ORISO-TenantService#179).
 *
 * <p>Why this is its own gated test rather than part of the normal suite: the integration profile
 * runs H2 in {@code MODE=MySQL}, which has neither InnoDB row locking nor its deadlock detector, so
 * an H2 run would pass whether or not the ordering is correct — the exact "test that cannot fail"
 * shape this PR has been hunting. Gated on an env var like {@code LiquibaseSchemaDriftIT}, so CI
 * skips it and it can be pointed at a real database on demand:
 *
 * <pre>
 * DPA_LOCK_IT_DB_URL=jdbc:mariadb://localhost:3306/tenantservice \
 * DPA_LOCK_IT_DB_USER=root DPA_LOCK_IT_DB_PASSWORD=secret \
 *   ./mvnw verify -Dtest=DpaConfirmLockOrderingMariaDbIT -DfailIfNoTests=false
 * </pre>
 *
 * <p>Without the ordering, two confirmations for one tenant each hold their own row and want the
 * other's, and InnoDB kills one with a deadlock error (SQLState 40001) — which reaches the signer
 * as a 500 even though the surviving transaction committed a valid signature. With it, both take
 * the same rows in the same sequence: one wins, the other finds its row no longer PENDING and gets
 * the defined "already used" answer.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@EnabledIfEnvironmentVariable(named = "DPA_LOCK_IT_DB_URL", matches = ".+")
class DpaConfirmLockOrderingMariaDbIT {

  private static final long TENANT_ID = 777L;
  private static final LocalDateTime VERSION = LocalDateTime.of(2026, 7, 1, 12, 0, 0);
  private static final int ROUNDS = 10;

  @DynamicPropertySource
  static void realMariaDb(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> System.getenv("DPA_LOCK_IT_DB_URL"));
    registry.add("spring.datasource.username", () -> System.getenv("DPA_LOCK_IT_DB_USER"));
    registry.add("spring.datasource.password", () -> System.getenv("DPA_LOCK_IT_DB_PASSWORD"));
    registry.add(
        "spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MariaDBDialect");
  }

  @Autowired private TenantDpaService tenantDpaService;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaSignatureRepository signatureRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
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
    tenant.setName("Lock Ordering");
    tenant.setSubdomain("lock-ordering");
    tenant.setCreateDate(LocalDateTime.now());
    tenant.setContentDataProcessingAgreement("<p>AVV</p>");
    tenant.setContentDataProcessingAgreementActivationDate(VERSION);
    tenantRepository.saveAndFlush(tenant);
  }

  @Test
  void twoConfirmationsForOneTenant_Should_neverDeadlock() throws Exception {
    for (int round = 0; round < ROUNDS; round++) {
      cleanUp();
      givenRegisteredTenantWithReservation();
      // two links for ONE tenant with DISTINCT tokens — the shape that deadlocked
      var tokenA =
          tenantDpaService.createSignInvite(
              TENANT_ID, VERSION, Duration.ofDays(14), null, "reservation-token");
      var tokenB =
          tenantDpaService.createSignInvite(
              TENANT_ID, VERSION, Duration.ofDays(14), null, "reservation-token");

      var barrier = new CyclicBarrier(2);
      ExecutorService pool = Executors.newFixedThreadPool(2);
      List<Future<Outcome>> results = new ArrayList<>();
      try {
        results.add(pool.submit(confirm(barrier, tokenA)));
        results.add(pool.submit(confirm(barrier, tokenB)));

        var outcomes = new ArrayList<Outcome>();
        for (Future<Outcome> result : results) {
          outcomes.add(result.get(30, TimeUnit.SECONDS));
        }

        // exactly one signature, and the loser got the DEFINED answer — never a deadlock
        assertThat(outcomes)
            .as("round %s outcomes", round)
            .filteredOn(outcome -> outcome == Outcome.DEADLOCK)
            .isEmpty();
        assertThat(outcomes).filteredOn(outcome -> outcome == Outcome.SIGNED).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> outcome == Outcome.REJECTED).hasSize(1);
      } finally {
        pool.shutdownNow();
      }

      assertThat(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
          .as("round %s: exactly one signature is recorded", round)
          .hasSize(1);
    }
  }

  private Callable<Outcome> confirm(CyclicBarrier barrier, String rawToken) {
    return () -> {
      barrier.await(10, TimeUnit.SECONDS);
      try {
        tenantDpaService.confirmSignature(
            rawToken, "Erika", "GF", "erika@example.org", "Org", false, "de");
        return Outcome.SIGNED;
      } catch (InvalidDpaSignTokenException alreadyUsed) {
        return Outcome.REJECTED;
      } catch (RuntimeException other) {
        // a deadlock victim surfaces as a Spring dao exception wrapping SQLState 40001; anything
        // else is also a failure of the guarantee, so it is reported rather than swallowed
        return isDeadlock(other) ? Outcome.DEADLOCK : Outcome.OTHER_FAILURE;
      }
    };
  }

  private static boolean isDeadlock(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof java.sql.SQLException sqlException
          && "40001".equals(sqlException.getSQLState())) {
        return true;
      }
    }
    return false;
  }

  private enum Outcome {
    SIGNED,
    REJECTED,
    DEADLOCK,
    OTHER_FAILURE
  }
}
