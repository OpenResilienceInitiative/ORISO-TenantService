package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaAdminSignatureRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Real-flush-semantics proof of the concurrent double-submit guarantee of {@link
 * TenantDpaStatusService#sign} (TEN-INV-U9, ORISO-TenantService#144).
 *
 * <p>Two threads race the same sign call against the real Spring transaction/JPA stack. The unique
 * constraint on (tenant_id, dpa_version) makes exactly one INSERT win; the loser's constraint
 * violation surfaces at flush/commit time of the insert transaction — NOT synchronously from {@code
 * repository.save()} — so this cannot be verified by a mock-based unit test. The losing call must
 * absorb the violation and return the authoritative status instead of throwing.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantDpaStatusServiceConcurrencyIT {

  private static final long TENANT_ID = 301L;
  private static final LocalDateTime VERSION = LocalDateTime.of(2026, 7, 1, 12, 0, 0);
  private static final int ROUNDS = 5;

  @Autowired private TenantDpaStatusService service;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaAdminSignatureRepository adminSignatureRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    // the production repository is append-only by design; tests reset the table directly
    jdbcTemplate.update("DELETE FROM tenant_dpa_admin_signature");
    tenantRepository.deleteAll();
  }

  private void seedTenantWithPublishedDpa() {
    var now = LocalDateTime.now();
    tenantRepository.save(
        TenantEntity.builder()
            .id(TENANT_ID)
            .name("tenant-" + TENANT_ID)
            .subdomain("subdomain-" + TENANT_ID)
            .contentDataProcessingAgreementActivationDate(VERSION)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private TenantDpaStatusService.DpaStatusView signAs(String admin) {
    return service.sign(
        TENANT_ID,
        admin + "-user-id",
        admin,
        new TenantDpaStatusService.AdminSignatureForm(
            admin, "Geschäftsführung", admin + "@example.org", "Träger Nord", "de", "{}"));
  }

  @Test
  void sign_Should_returnAuthoritativeStatusToBothWriters_When_twoAdminsRaceTheSameVersion()
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      for (int round = 0; round < ROUNDS; round++) {
        cleanUp();
        seedTenantWithPublishedDpa();

        var barrier = new CyclicBarrier(2);
        List<Callable<TenantDpaStatusService.DpaStatusView>> racers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
          var admin = "admin-" + i;
          racers.add(
              () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return signAs(admin);
              });
        }

        List<Future<TenantDpaStatusService.DpaStatusView>> results = executor.invokeAll(racers);
        for (Future<TenantDpaStatusService.DpaStatusView> result : results) {
          // the losing writer must not blow up; it reports the authoritative status
          var status = result.get(30, TimeUnit.SECONDS);
          assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
          assertThat(status.signedVersion()).isEqualTo(VERSION);
        }
        assertThat(adminSignatureRepository.countByTenantId(TENANT_ID))
            .as("round %d: the audit trail must hold exactly one row", round)
            .isEqualTo(1);
      }
    } finally {
      executor.shutdownNow();
    }
  }
}
