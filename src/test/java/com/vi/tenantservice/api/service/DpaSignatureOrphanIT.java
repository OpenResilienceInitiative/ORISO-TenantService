package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * The regression that covers the whole orphaned-signature family (ORISO-TenantService#179).
 *
 * <p>Dropping the {@code tenant_dpa_signature} foreign key made orphan rows possible: a tenant ID
 * is a reusable slot, and a sign link can outlive the reservation it was minted from. Four separate
 * review findings were all the same defect reached by different orderings — release-then-confirm,
 * release-then-re-reserve, a confirmation racing the release. Guarding each write ordering could
 * never be complete, so the guarantee is asserted here on the READ side: whatever produced the
 * orphan, it must not count for the organisation that holds the ID afterwards.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class DpaSignatureOrphanIT {

  private static final long TENANT_ID = 402L;
  private static final LocalDateTime VERSION = LocalDateTime.of(2026, 7, 1, 12, 0, 0);

  @Autowired private TenantDpaService tenantDpaService;
  @Autowired private TenantDpaStatusService tenantDpaStatusService;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaSignatureRepository signatureRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM tenant_dpa_signature");
    jdbcTemplate.update("DELETE FROM tenant_id_reservation");
    tenantRepository.deleteAll();
  }

  private TenantIdReservationEntity reserve(String token, TenantIdReservationStatus status) {
    return reservationRepository.saveAndFlush(
        TenantIdReservationEntity.builder()
            .tenantId(TENANT_ID)
            .token(token)
            .status(status)
            .createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now())
            .build());
  }

  private TenantEntity registerTenant(String name) {
    var tenant = new TenantEntity();
    tenant.setId(TENANT_ID);
    tenant.setName(name);
    tenant.setSubdomain("occupant-" + TENANT_ID);
    tenant.setCreateDate(LocalDateTime.now());
    tenant.setContentDataProcessingAgreement("<p>AVV</p>");
    tenant.setContentDataProcessingAgreementActivationDate(VERSION);
    return tenantRepository.saveAndFlush(tenant);
  }

  @Test
  void aSignatureFromAReleasedReservation_Should_notMakeTheNextOccupantValid() {
    // organisation A reserves the id and forwards its DPA
    reserve("reservation-token-A", TenantIdReservationStatus.RESERVED);
    registerTenant("A");
    var rawToken =
        tenantDpaService.createSignInvite(
            TENANT_ID, VERSION, java.time.Duration.ofDays(14), null, "reservation-token-A");
    tenantDpaService.confirmSignature(
        rawToken, "Erika A", "GF", "erika@a.example", "Organisation A", false, "de");
    assertThat(tenantDpaStatusService.getStatus(TENANT_ID).status())
        .isEqualTo(TenantDpaStatus.VALID);

    // the reservation is released and the id handed to an unrelated organisation B
    reservationRepository.deleteAll();
    tenantRepository.deleteAll();
    reserve("reservation-token-B", TenantIdReservationStatus.ASSIGNED);
    registerTenant("B");

    // A's signature row still physically references the id — the foreign key is gone
    assertThat(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .hasSize(1);

    // but it must be inert for B: unsigned gate, and none of A's signer PII in B's audit list
    assertThat(tenantDpaStatusService.getStatus(TENANT_ID).status())
        .isEqualTo(TenantDpaStatus.UNSIGNED);
    assertThat(tenantDpaService.getSignatures(TENANT_ID)).isEmpty();
  }

  @Test
  void aConfirmationRacingTheRelease_Should_neverCountForTheNextOccupant() throws Exception {
    // drives the ordering the write-path guards kept missing: the release lands while the
    // confirmation is in flight, so the SIGNED row can still be written for a released id
    var rounds = 5;
    for (int round = 0; round < rounds; round++) {
      cleanUp();
      reserve("reservation-token-A", TenantIdReservationStatus.RESERVED);
      var rawToken =
          tenantDpaService.createSignInvite(
              TENANT_ID, VERSION, java.time.Duration.ofDays(14), null, "reservation-token-A");

      var barrier = new CyclicBarrier(2);
      ExecutorService pool = Executors.newFixedThreadPool(2);
      try {
        var confirming =
            pool.submit(
                () -> {
                  barrier.await(5, TimeUnit.SECONDS);
                  try {
                    tenantDpaService.confirmSignature(
                        rawToken,
                        "Erika A",
                        "GF",
                        "erika@a.example",
                        "Organisation A",
                        false,
                        "de");
                  } catch (InvalidDpaSignTokenException expectedWhenTheGuardWins) {
                    // ONLY the guard rejecting the token is an acceptable alternative outcome; any
                    // other failure means the confirmation never reached the signature write, and
                    // the UNSIGNED assertion below would then pass for the wrong reason
                  }
                  return null;
                });
        var releasing =
            pool.submit(
                () -> {
                  barrier.await(5, TimeUnit.SECONDS);
                  reservationRepository.deleteAll();
                  return null;
                });
        confirming.get(15, TimeUnit.SECONDS);
        releasing.get(15, TimeUnit.SECONDS);
      } finally {
        pool.shutdownNow();
      }

      // whatever the interleaving produced, the id is now handed to organisation B
      reservationRepository.deleteAll();
      reserve("reservation-token-B", TenantIdReservationStatus.ASSIGNED);
      registerTenant("B");

      assertThat(tenantDpaStatusService.getStatus(TENANT_ID).status())
          .as("round %s: an orphan from the released reservation must not sign for B", round)
          .isEqualTo(TenantDpaStatus.UNSIGNED);
      assertThat(tenantDpaService.getSignatures(TENANT_ID))
          .as("round %s: B's audit list must not expose A's signer", round)
          .isEmpty();
    }
  }

  @Test
  void aLegitimateSignature_Should_surviveTheRegistrationThatConsumesItsReservation() {
    // the case the guard must NOT break: consuming a reservation keeps its token, so a link minted
    // before registration still counts afterwards
    reserve("reservation-token", TenantIdReservationStatus.RESERVED);
    var rawToken =
        tenantDpaService.createSignInvite(
            TENANT_ID, VERSION, java.time.Duration.ofDays(14), null, "reservation-token");

    // registration consumes the reservation (RESERVED -> ASSIGNED, same token) and creates the
    // tenant; the forwarded signature only lands afterwards
    reservationRepository.deleteAll();
    reserve("reservation-token", TenantIdReservationStatus.ASSIGNED);
    registerTenant("Träger Nord");
    tenantDpaService.confirmSignature(
        rawToken, "Erika", "GF", "erika@example.org", "Träger Nord", false, "de");

    assertThat(tenantDpaStatusService.getStatus(TENANT_ID).status())
        .isEqualTo(TenantDpaStatus.VALID);
    assertThat(tenantDpaService.getSignatures(TENANT_ID)).hasSize(1);
  }

  @Test
  void anAdminCreatedInvite_Should_stayUnaffectedByTheOwnershipCheck() {
    // links an authenticated admin mints carry no reservation binding and must keep counting
    registerTenant("Träger Nord");
    var rawToken =
        tenantDpaService.createSignInvite(
            TENANT_ID, VERSION, java.time.Duration.ofDays(14), "kc-1");
    tenantDpaService.confirmSignature(
        rawToken, "Erika", "GF", "erika@example.org", "Träger Nord", false, "de");

    assertThat(tenantDpaStatusService.getStatus(TENANT_ID).status())
        .isEqualTo(TenantDpaStatus.VALID);
    assertThat(tenantDpaService.getSignatures(TENANT_ID))
        .extracting(TenantDpaSignatureEntity::getSignerName)
        .isEqualTo(List.of("Erika"));
  }
}
