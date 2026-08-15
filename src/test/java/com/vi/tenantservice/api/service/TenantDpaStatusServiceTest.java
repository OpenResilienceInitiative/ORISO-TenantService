package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaAdminSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaAdminSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Status derivation and audit-proof admin signing for the tenant DPA (TEN-INV-U9,
 * ORISO-TenantService#144).
 */
@ExtendWith(MockitoExtension.class)
class TenantDpaStatusServiceTest {

  private static final Long TENANT_ID = 7L;
  private static final Long OPERATOR_TENANT_ID = 1L;
  private static final LocalDateTime VERSION_1 = LocalDateTime.of(2026, 5, 1, 10, 0);
  private static final LocalDateTime VERSION_2 = LocalDateTime.of(2026, 7, 1, 12, 0);

  @Mock private TenantDpaAdminSignatureRepository adminSignatureRepository;
  @Mock private TenantDpaSignatureRepository signatureRepository;
  @Mock private TenantDpaVersionRepository versionRepository;
  @Mock private TenantRepository tenantRepository;
  @Mock private PlatformTransactionManager transactionManager;

  /**
   * The governing-document resolver runs for real over the mocked repositories: the version a
   * tenant is measured against and the document it can read and sign must come from the same logic
   * (#569), so stubbing it away here would hide exactly the divergence these tests guard.
   */
  private GoverningDpaResolver governingDpaResolver;

  private TenantDpaStatusService service;

  @BeforeEach
  void setUp() {
    // operatorTenantId stays 0 (fallback disabled) unless a test opts in via givenOperatorDpa
    governingDpaResolver = new GoverningDpaResolver(tenantRepository, versionRepository);
    service =
        new TenantDpaStatusService(
            adminSignatureRepository,
            signatureRepository,
            governingDpaResolver,
            transactionManager);
  }

  private void givenTenantWithEmbeddedVersion(LocalDateTime version) {
    when(tenantRepository.findById(TENANT_ID))
        .thenReturn(
            Optional.of(
                TenantEntity.builder()
                    .id(TENANT_ID)
                    .contentDataProcessingAgreementActivationDate(version)
                    .build()));
  }

  /** The governing operator DPA (#569): tenant {@code app.dpa.operator-tenant-id}. */
  private void givenOperatorDpa(LocalDateTime version) {
    ReflectionTestUtils.setField(governingDpaResolver, "operatorTenantId", OPERATOR_TENANT_ID);
    when(tenantRepository.findById(OPERATOR_TENANT_ID))
        .thenReturn(
            Optional.of(
                TenantEntity.builder()
                    .id(OPERATOR_TENANT_ID)
                    .contentDataProcessingAgreementActivationDate(version)
                    .build()));
  }

  private void givenTenantWithoutOwnDpa() {
    givenTenantWithEmbeddedVersion(null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
  }

  private void givenNoSignatures() {
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());
  }

  private TenantDpaSignatureEntity legacySignedSignature(LocalDateTime version) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(TENANT_ID)
        .dpaVersion(version)
        .signerName("Erika Extern")
        .status(DpaSignatureStatus.SIGNED)
        .signedAt(version == null ? LocalDateTime.of(2026, 6, 1, 9, 0) : version.plusDays(1))
        .build();
  }

  private TenantDpaAdminSignatureEntity adminSignature(LocalDateTime version) {
    return TenantDpaAdminSignatureEntity.builder()
        .tenantId(TENANT_ID)
        .dpaVersion(version)
        .signerUserId("admin-user-id")
        .signerUsername("tenantadmin")
        .signerName("Toni Tenantadmin")
        .signedAt(version.plusHours(2))
        .createDate(version.plusHours(2))
        .formData("{\"signerName\":\"Toni Tenantadmin\"}")
        .build();
  }

  // --- status derivation -----------------------------------------------------------------

  @Test
  void getStatus_Should_returnMissing_When_noDpaWasEverPublishedAndNothingIsSigned() {
    givenTenantWithEmbeddedVersion(null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
    givenNoSignatures();

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.MISSING);
    assertThat(status.currentVersion()).isNull();
    assertThat(status.signedVersion()).isNull();
  }

  @Test
  void getStatus_Should_returnUnsigned_When_dpaIsPublishedButNeverSigned() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    givenNoSignatures();

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.UNSIGNED);
    assertThat(status.currentVersion()).isEqualTo(VERSION_2);
    assertThat(status.signedVersion()).isNull();
  }

  @Test
  void getStatus_Should_returnOutdated_When_onlyAnOlderVersionWasSigned() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(legacySignedSignature(VERSION_1)));

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.OUTDATED);
    assertThat(status.currentVersion()).isEqualTo(VERSION_2);
    assertThat(status.signedVersion()).isEqualTo(VERSION_1);
    assertThat(status.signedBy()).isEqualTo("Erika Extern");
  }

  @Test
  void getStatus_Should_returnValid_When_currentVersionIsSignedByTenantAdmin() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of(adminSignature(VERSION_2)));
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
    assertThat(status.currentVersion()).isEqualTo(VERSION_2);
    assertThat(status.signedVersion()).isEqualTo(VERSION_2);
    assertThat(status.signedBy()).isEqualTo("Toni Tenantadmin");
    assertThat(status.signedAt()).isNotNull();
  }

  @Test
  void getStatus_Should_returnValid_When_currentVersionWasSignedViaLegacyPublicLink() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(legacySignedSignature(VERSION_2)));

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
  }

  @Test
  void getStatus_Should_returnInconsistent_When_signaturesExistButNoDpaWasEverPublished() {
    givenTenantWithEmbeddedVersion(null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(legacySignedSignature(VERSION_1)));

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.INCONSISTENT);
  }

  @Test
  void getStatus_Should_returnInconsistent_When_aSignatureReferencesAVersionNewerThanCurrent() {
    givenTenantWithEmbeddedVersion(VERSION_1);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(legacySignedSignature(VERSION_2)));

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.INCONSISTENT);
  }

  @Test
  void getStatus_Should_returnInconsistent_When_aSignedRowCarriesNoVersion() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(legacySignedSignature(null)));

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.INCONSISTENT);
  }

  @Test
  void getStatus_Should_fallBackToPublishHistory_When_embeddedActivationDateWasCleared() {
    givenTenantWithEmbeddedVersion(null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(
            List.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(TENANT_ID)
                    .activationDate(VERSION_2)
                    .content("{}")
                    .build()));
    givenNoSignatures();

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.UNSIGNED);
    assertThat(status.currentVersion()).isEqualTo(VERSION_2);
  }

  // --- signing ----------------------------------------------------------------------------

  @Test
  void sign_Should_persistAuditProofSignatureRow_When_currentVersionIsNotYetSigned() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    givenNoSignatures();

    service.sign(
        TENANT_ID,
        "admin-user-id",
        "tenantadmin",
        new TenantDpaStatusService.AdminSignatureForm(
            "Toni Tenantadmin",
            "Geschäftsführung",
            "toni@example.org",
            "Träger Nord",
            "de",
            "{\"signerName\":\"Toni Tenantadmin\",\"accepted\":true}"));

    var captor = ArgumentCaptor.forClass(TenantDpaAdminSignatureEntity.class);
    verify(adminSignatureRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(saved.getDpaVersion()).isEqualTo(VERSION_2);
    assertThat(saved.getSignerUserId()).isEqualTo("admin-user-id");
    assertThat(saved.getSignerUsername()).isEqualTo("tenantadmin");
    assertThat(saved.getSignerName()).isEqualTo("Toni Tenantadmin");
    assertThat(saved.getSignerPosition()).isEqualTo("Geschäftsführung");
    assertThat(saved.getSignerEmail()).isEqualTo("toni@example.org");
    assertThat(saved.getSignerOrganisation()).isEqualTo("Träger Nord");
    assertThat(saved.getLanguage()).isEqualTo("de");
    assertThat(saved.getFormData()).contains("\"accepted\":true");
    assertThat(saved.getSignedAt()).isNotNull();
    assertThat(saved.getCreateDate()).isNotNull();
  }

  @Test
  void sign_Should_notPersistASecondRow_When_tenantIsAlreadyValid() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of(adminSignature(VERSION_2)));
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());

    var status =
        service.sign(
            TENANT_ID,
            "another-admin",
            "other",
            new TenantDpaStatusService.AdminSignatureForm("A", null, null, null, "de", "{}"));

    verify(adminSignatureRepository, never()).save(any());
    assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
  }

  @Test
  void sign_Should_throwDpaNotPublished_When_thereIsNoCurrentVersionToSign() {
    givenTenantWithEmbeddedVersion(null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
    givenNoSignatures();

    assertThatThrownBy(
            () ->
                service.sign(
                    TENANT_ID,
                    "admin-user-id",
                    "tenantadmin",
                    new TenantDpaStatusService.AdminSignatureForm(
                        "A", null, null, null, "de", "{}")))
        .isInstanceOf(DpaNotPublishedException.class);

    verify(adminSignatureRepository, never()).save(any());
  }

  /**
   * Guards the catch placement only: a {@link DataIntegrityViolationException} crossing the insert
   * transaction's boundary is absorbed and the authoritative status is re-read. Under real flush
   * semantics the violation surfaces at the inner transaction's commit, not from {@code save()}
   * itself — that timing (and the resulting no-500-for-the-loser guarantee) is proven against the
   * real Spring/JPA stack in {@code TenantDpaStatusServiceConcurrencyIT}.
   */
  @Test
  void sign_Should_absorbTheUniqueConstraintViolation_When_aConcurrentSignatureWinsTheRace() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    givenNoSignatures();
    when(adminSignatureRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate tenant/version"));

    var status =
        service.sign(
            TENANT_ID,
            "admin-user-id",
            "tenantadmin",
            new TenantDpaStatusService.AdminSignatureForm("A", null, null, null, "de", "{}"));

    // the losing writer must not blow up; it reports the (re-computed) authoritative status
    assertThat(status).isNotNull();
    assertThat(status.status()).isEqualTo(TenantDpaStatus.UNSIGNED);
  }

  // --- governing operator DPA (#569) -------------------------------------------------------

  /**
   * Frank's domain rule (2026-07-29): there is one governing operator DPA. A tenant that never
   * published one of its own — every tenant created through the public onboarding, and every tenant
   * a platform admin seeds directly — is measured against the operator's version.
   */
  @Test
  void getStatus_Should_measureAgainstTheOperatorDpa_When_theTenantPublishedNoneOfItsOwn() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    givenNoSignatures();

    var status = service.getStatus(TENANT_ID);

    // blocked, but ACTIONABLE: the admin can sign the operator DPA in-app (was: MISSING)
    assertThat(status.status()).isEqualTo(TenantDpaStatus.UNSIGNED);
    assertThat(status.currentVersion()).isEqualTo(VERSION_2);
  }

  @Test
  void getStatus_Should_returnValid_When_theOnboardingSignatureCoversTheOperatorVersion() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of(adminSignature(VERSION_2)));
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
    assertThat(status.currentVersion()).isEqualTo(VERSION_2);
    assertThat(status.signedVersion()).isEqualTo(VERSION_2);
  }

  /** The fallback is additive: a tenant with its own published DPA keeps being measured by it. */
  @Test
  void getStatus_Should_notFallBackToTheOperator_When_theTenantHasItsOwnPublishedDpa() {
    ReflectionTestUtils.setField(governingDpaResolver, "operatorTenantId", OPERATOR_TENANT_ID);
    givenTenantWithEmbeddedVersion(VERSION_1);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of(adminSignature(VERSION_1)));
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
    assertThat(status.currentVersion()).isEqualTo(VERSION_1);
  }

  @Test
  void getStatus_Should_returnMissing_When_notEvenTheOperatorPublishedADpa() {
    ReflectionTestUtils.setField(governingDpaResolver, "operatorTenantId", OPERATOR_TENANT_ID);
    givenTenantWithoutOwnDpa();
    when(tenantRepository.findById(OPERATOR_TENANT_ID))
        .thenReturn(Optional.of(TenantEntity.builder().id(OPERATOR_TENANT_ID).build()));
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(OPERATOR_TENANT_ID))
        .thenReturn(List.of());
    givenNoSignatures();

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.MISSING);
  }

  /** An absent tenant must never look like "has a contract in force". */
  @Test
  void getStatus_Should_notResolveAnyVersion_When_theTenantDoesNotExist() {
    ReflectionTestUtils.setField(governingDpaResolver, "operatorTenantId", OPERATOR_TENANT_ID);
    when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
    givenNoSignatures();

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.MISSING);
    assertThat(status.currentVersion()).isNull();
  }

  // --- onboarding acceptance (#569) --------------------------------------------------------

  @Test
  void signOnboarding_Should_persistTheAcceptanceWithTheSignerIdentityPassedIn() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    givenNoSignatures();

    var status =
        service.signOnboarding(
            TENANT_ID,
            "onboarded-admin-id",
            "toni@traeger-nord.example",
            VERSION_2,
            new TenantDpaStatusService.AdminSignatureForm(
                "Toni Tenantadmin",
                "Geschäftsführung",
                "toni@traeger-nord.example",
                "Träger Nord e.V.",
                null,
                "{\"accepted\":true}"));

    var captor = ArgumentCaptor.forClass(TenantDpaAdminSignatureEntity.class);
    verify(adminSignatureRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(saved.getDpaVersion()).isEqualTo(VERSION_2);
    assertThat(saved.getSignerUserId()).isEqualTo("onboarded-admin-id");
    assertThat(saved.getSignerUsername()).isEqualTo("toni@traeger-nord.example");
    assertThat(saved.getSignerName()).isEqualTo("Toni Tenantadmin");
    assertThat(saved.getSignerOrganisation()).isEqualTo("Träger Nord e.V.");
    assertThat(saved.getFormData()).contains("\"accepted\":true");
    assertThat(saved.getSignedAt()).isNotNull();
    assertThat(status).isNotNull();
  }

  @Test
  void signOnboarding_Should_useTheVersionInForce_When_noShownVersionIsSubmitted() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    givenNoSignatures();

    service.signOnboarding(
        TENANT_ID,
        "onboarded-admin-id",
        "toni",
        null,
        new TenantDpaStatusService.AdminSignatureForm("Toni", null, null, null, null, "{}"));

    var captor = ArgumentCaptor.forClass(TenantDpaAdminSignatureEntity.class);
    verify(adminSignatureRepository).save(captor.capture());
    assertThat(captor.getValue().getDpaVersion()).isEqualTo(VERSION_2);
  }

  /**
   * The operator republished between rendering the text and submitting the registration: the
   * signature records what was actually shown, so the tenant lands on OUTDATED (blocked but
   * signable in-app) instead of claiming a signature on unseen wording.
   */
  @Test
  void signOnboarding_Should_recordTheShownVersion_When_theOperatorRepublishedInBetween() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    givenNoSignatures();
    when(versionRepository.findFirstByTenantIdAndActivationDate(TENANT_ID, VERSION_1))
        .thenReturn(Optional.empty());
    when(versionRepository.findFirstByTenantIdAndActivationDate(OPERATOR_TENANT_ID, VERSION_1))
        .thenReturn(
            Optional.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(OPERATOR_TENANT_ID)
                    .activationDate(VERSION_1)
                    .content("{}")
                    .build()));

    service.signOnboarding(
        TENANT_ID,
        "onboarded-admin-id",
        "toni",
        VERSION_1,
        new TenantDpaStatusService.AdminSignatureForm("Toni", null, null, null, null, "{}"));

    var captor = ArgumentCaptor.forClass(TenantDpaAdminSignatureEntity.class);
    verify(adminSignatureRepository).save(captor.capture());
    assertThat(captor.getValue().getDpaVersion()).isEqualTo(VERSION_1);
  }

  @Test
  void signOnboarding_Should_reject_When_theSubmittedVersionWasNeverPublished() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    givenNoSignatures();
    when(versionRepository.findFirstByTenantIdAndActivationDate(TENANT_ID, VERSION_1))
        .thenReturn(Optional.empty());
    when(versionRepository.findFirstByTenantIdAndActivationDate(OPERATOR_TENANT_ID, VERSION_1))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.signOnboarding(
                    TENANT_ID,
                    "onboarded-admin-id",
                    "toni",
                    VERSION_1,
                    new TenantDpaStatusService.AdminSignatureForm(
                        "Toni", null, null, null, null, "{}")))
        .isInstanceOf(DpaNotPublishedException.class);

    verify(adminSignatureRepository, never()).save(any());
  }

  @Test
  void signOnboarding_Should_reject_When_noGoverningDpaIsPublishedAtAll() {
    ReflectionTestUtils.setField(governingDpaResolver, "operatorTenantId", OPERATOR_TENANT_ID);
    givenTenantWithoutOwnDpa();
    when(tenantRepository.findById(OPERATOR_TENANT_ID))
        .thenReturn(Optional.of(TenantEntity.builder().id(OPERATOR_TENANT_ID).build()));
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(OPERATOR_TENANT_ID))
        .thenReturn(List.of());
    givenNoSignatures();

    assertThatThrownBy(
            () ->
                service.signOnboarding(
                    TENANT_ID,
                    "onboarded-admin-id",
                    "toni",
                    null,
                    new TenantDpaStatusService.AdminSignatureForm(
                        "Toni", null, null, null, null, "{}")))
        .isInstanceOf(DpaNotPublishedException.class);

    verify(adminSignatureRepository, never()).save(any());
  }

  @Test
  void sign_Should_recordSignature_When_statusIsOutdated() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of());
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(legacySignedSignature(VERSION_1)));

    service.sign(
        TENANT_ID,
        "admin-user-id",
        "tenantadmin",
        new TenantDpaStatusService.AdminSignatureForm("A", null, null, null, "de", "{}"));

    var captor = ArgumentCaptor.forClass(TenantDpaAdminSignatureEntity.class);
    verify(adminSignatureRepository).save(captor.capture());
    assertThat(captor.getValue().getDpaVersion()).isEqualTo(VERSION_2);
  }

  // --- contract on hold + link invalidation (ORISO-TenantService#179) ----------------------

  @Test
  void getStatus_Should_returnPendingForwarded_When_unsignedButALiveSignLinkIsOutstanding() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    givenNoSignatures();
    when(signatureRepository.existsByTenantIdAndStatusAndTokenExpiresAtAfter(
            org.mockito.ArgumentMatchers.eq(TENANT_ID),
            org.mockito.ArgumentMatchers.eq(DpaSignatureStatus.PENDING),
            any(LocalDateTime.class)))
        .thenReturn(true);

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.PENDING_FORWARDED);
  }

  @Test
  void getStatus_Should_stayUnsigned_When_theOnlyOutstandingLinkExpired() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    givenNoSignatures();
    when(signatureRepository.existsByTenantIdAndStatusAndTokenExpiresAtAfter(
            org.mockito.ArgumentMatchers.eq(TENANT_ID),
            org.mockito.ArgumentMatchers.eq(DpaSignatureStatus.PENDING),
            any(LocalDateTime.class)))
        .thenReturn(false);

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.UNSIGNED);
  }

  @Test
  void getStatus_Should_neverMaskValid_When_aStaleLinkIsStillOutstanding() {
    // a VALID tenant stays VALID even if (against the invalidation rule) a link survived
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of(adminSignature(VERSION_2)));
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());

    var status = service.getStatus(TENANT_ID);

    assertThat(status.status()).isEqualTo(TenantDpaStatus.VALID);
    verify(signatureRepository, never())
        .existsByTenantIdAndStatusAndTokenExpiresAtAfter(any(), any(), any());
  }

  @Test
  void sign_Should_invalidateEveryOutstandingSignLink_When_theSignatureIsRecorded() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    givenNoSignatures();

    service.sign(
        TENANT_ID,
        "admin-user-id",
        "tenantadmin",
        new TenantDpaStatusService.AdminSignatureForm("A", null, null, null, "de", "{}"));

    verify(signatureRepository).invalidateOutstandingByTenantId(TENANT_ID);
  }

  @Test
  void signOnboarding_Should_invalidateEveryOutstandingSignLink_When_theAcceptanceIsRecorded() {
    givenOperatorDpa(VERSION_2);
    givenTenantWithoutOwnDpa();
    givenNoSignatures();

    service.signOnboarding(
        TENANT_ID,
        "onboarded-admin-id",
        "toni",
        null,
        new TenantDpaStatusService.AdminSignatureForm("Toni", null, null, null, null, "{}"));

    verify(signatureRepository).invalidateOutstandingByTenantId(TENANT_ID);
  }

  @Test
  void sign_Should_notInvalidateAnything_When_tenantIsAlreadyValid() {
    givenTenantWithEmbeddedVersion(VERSION_2);
    when(adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(TENANT_ID))
        .thenReturn(List.of(adminSignature(VERSION_2)));
    when(signatureRepository.findByTenantIdAndStatus(TENANT_ID, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of());

    service.sign(
        TENANT_ID,
        "admin-user-id",
        "tenantadmin",
        new TenantDpaStatusService.AdminSignatureForm("A", null, null, null, "de", "{}"));

    verify(signatureRepository, never()).invalidateOutstandingByTenantId(any());
  }
}
