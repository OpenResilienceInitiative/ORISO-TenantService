package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantDpaServiceTest {

  @Mock private TenantDpaSignatureRepository signatureRepository;
  @Mock private TenantDpaVersionRepository versionRepository;
  @Mock private TenantRepository tenantRepository;

  @Mock
  private com.vi.tenantservice.api.repository.TenantIdReservationRepository
      tenantIdReservationRepository;

  private GoverningDpaResolver governingDpaResolver;
  private TenantDpaService tenantDpaService;

  @BeforeEach
  void setUp() {
    // the governing-document resolver runs for real over the mocked repositories: the preview must
    // resolve the exact snapshot the invite was issued for (#569)
    governingDpaResolver = new GoverningDpaResolver(tenantRepository, versionRepository);
    tenantDpaService =
        new TenantDpaService(
            signatureRepository,
            versionRepository,
            tenantRepository,
            tenantIdReservationRepository,
            governingDpaResolver);
  }

  @Test
  void recordSignature_Should_persistSignedRecordWithVersionAndSigner() {
    // given
    var version = LocalDateTime.now().minusDays(1);
    when(signatureRepository.save(any(TenantDpaSignatureEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    var result =
        tenantDpaService.recordSignature(5L, version, "Erika M", "Geschäftsführerin", false, "de");

    // then
    var captor = ArgumentCaptor.forClass(TenantDpaSignatureEntity.class);
    verify(signatureRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getTenantId()).isEqualTo(5L);
    assertThat(saved.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(saved.getDpaVersion()).isEqualTo(version);
    assertThat(saved.getSignerName()).isEqualTo("Erika M");
    assertThat(saved.getSignerPosition()).isEqualTo("Geschäftsführerin");
    assertThat(saved.getSignerIsMember()).isFalse();
    assertThat(saved.getLanguage()).isEqualTo("de");
    assertThat(saved.getSignedAt()).isNotNull();
    assertThat(saved.getCreateDate()).isNotNull();
    assertThat(result).isSameAs(saved);
  }

  @Test
  void isSignedForVersion_Should_returnTrue_When_signedSignatureForThatVersionExists() {
    // given
    var version = LocalDateTime.now();
    when(signatureRepository.findByTenantIdAndStatus(5L, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(signature(version)));

    // when / then
    assertThat(tenantDpaService.isSignedForVersion(5L, version)).isTrue();
  }

  @Test
  void isSignedForVersion_Should_returnFalse_When_onlyOtherVersionsSigned() {
    // given
    var version = LocalDateTime.now();
    when(signatureRepository.findByTenantIdAndStatus(5L, DpaSignatureStatus.SIGNED))
        .thenReturn(List.of(signature(version.minusDays(2))));

    // when / then
    assertThat(tenantDpaService.isSignedForVersion(5L, version)).isFalse();
  }

  @Test
  void getSignatures_Should_returnAllForTenant() {
    // given
    when(signatureRepository.findByTenantId(5L))
        .thenReturn(List.of(signature(LocalDateTime.now())));

    // when / then
    assertThat(tenantDpaService.getSignatures(5L)).hasSize(1);
  }

  @Test
  void recordPublishedVersion_Should_saveSnapshot() {
    // given
    var activationDate = LocalDateTime.now();

    // when
    tenantDpaService.recordPublishedVersion(5L, "{\"de\":\"x\"}", activationDate);

    // then
    var captor = ArgumentCaptor.forClass(TenantDpaVersionEntity.class);
    verify(versionRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getTenantId()).isEqualTo(5L);
    assertThat(saved.getContent()).isEqualTo("{\"de\":\"x\"}");
    assertThat(saved.getActivationDate()).isEqualTo(activationDate);
    assertThat(saved.getCreateDate()).isNotNull();
  }

  @Test
  void getVersions_Should_returnFromRepoNewestFirst() {
    // given
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(5L))
        .thenReturn(List.of(TenantDpaVersionEntity.builder().tenantId(5L).build()));

    // when / then
    assertThat(tenantDpaService.getVersions(5L)).hasSize(1);
  }

  @Test
  void createSignInvite_Should_persistPendingRowWithHashedTokenAndExpiry_andReturnRawToken() {
    // given
    var version = LocalDateTime.now();
    when(signatureRepository.save(any(TenantDpaSignatureEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    var rawToken = tenantDpaService.createSignInvite(7L, version, Duration.ofDays(14), null);

    // then
    assertThat(rawToken).isNotBlank();
    var captor = ArgumentCaptor.forClass(TenantDpaSignatureEntity.class);
    verify(signatureRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getTenantId()).isEqualTo(7L);
    assertThat(saved.getStatus()).isEqualTo(DpaSignatureStatus.PENDING);
    assertThat(saved.getDpaVersion()).isEqualTo(version);
    // the raw token is never stored — only its SHA-256 hash
    assertThat(saved.getTokenHash()).isEqualTo(DpaSignToken.hash(rawToken)).isNotEqualTo(rawToken);
    assertThat(saved.getTokenExpiresAt()).isAfter(LocalDateTime.now());
    // pre-account wizard forward: no forwarder account, but the source is stamped at creation
    assertThat(saved.getForwardedByUserId()).isNull();
    assertThat(saved.getSource()).isEqualTo(TenantDpaService.SOURCE_FORWARDED_EXTERNAL);
  }

  @Test
  void createSignInvite_Should_stampForwardingAdmin_When_createdAuthenticated() {
    // given
    when(signatureRepository.save(any(TenantDpaSignatureEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    tenantDpaService.createSignInvite(
        7L, LocalDateTime.now(), Duration.ofDays(14), "tenant-admin-1");

    // then
    var captor = ArgumentCaptor.forClass(TenantDpaSignatureEntity.class);
    verify(signatureRepository).save(captor.capture());
    assertThat(captor.getValue().getForwardedByUserId()).isEqualTo("tenant-admin-1");
    assertThat(captor.getValue().getSource()).isEqualTo(TenantDpaService.SOURCE_FORWARDED_EXTERNAL);
  }

  @Test
  void getSignPreview_Should_returnExactPublishedVersion_withoutConsumingToken() {
    // given
    var rawToken = "preview-token";
    var version = LocalDateTime.of(2026, 7, 20, 12, 30);
    var pending =
        TenantDpaSignatureEntity.builder()
            .tenantId(7L)
            .dpaVersion(version)
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .build();
    var published =
        TenantDpaVersionEntity.builder()
            .tenantId(7L)
            .activationDate(version)
            .content("{\"de\":\"<h2>AVV</h2><p>Vertragstext</p>\"}")
            .build();
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(Optional.of(pending));
    when(versionRepository.findFirstByTenantIdAndActivationDate(7L, version))
        .thenReturn(Optional.of(published));
    when(tenantRepository.findById(7L))
        .thenReturn(Optional.of(TenantEntity.builder().id(7L).name("Träger Nord").build()));

    // when
    var preview = tenantDpaService.getSignPreview(rawToken);

    // then
    assertThat(preview.tenantId()).isEqualTo(7L);
    assertThat(preview.tenantName()).isEqualTo("Träger Nord");
    assertThat(preview.dpaVersion()).isEqualTo(version);
    assertThat(preview.content()).contains("Vertragstext");
    assertThat(preview.expiresAt()).isEqualTo(pending.getTokenExpiresAt());
    verify(signatureRepository, org.mockito.Mockito.never())
        .consumeSignToken(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void getSignPreview_Should_fallBackToReservation_When_tenantNotRegisteredYet() {
    // given a sign link created from the public onboarding wizard: the tenant id is only RESERVED
    // and the governing operator DPA fallback is active (tenant 1)
    org.springframework.test.util.ReflectionTestUtils.setField(
        governingDpaResolver, "operatorTenantId", 1L);
    var rawToken = "reserved-preview-token";
    var version = LocalDateTime.of(2026, 7, 20, 12, 30);
    var pending =
        TenantDpaSignatureEntity.builder()
            .tenantId(42L)
            .dpaVersion(version)
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .build();
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(Optional.of(pending));
    // own history empty -> the resolver falls back to the operator snapshot (tenant 1)
    when(versionRepository.findFirstByTenantIdAndActivationDate(42L, version))
        .thenReturn(Optional.empty());
    when(versionRepository.findFirstByTenantIdAndActivationDate(1L, version))
        .thenReturn(
            Optional.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(1L)
                    .activationDate(version)
                    .content("{\"de\":\"<p>Operator AVV</p>\"}")
                    .build()));
    when(tenantRepository.findById(42L)).thenReturn(Optional.empty());
    when(tenantIdReservationRepository.existsById(42L)).thenReturn(true);

    // when
    var preview = tenantDpaService.getSignPreview(rawToken);

    // then: the link works BEFORE the registration completes; only the name is not known yet
    assertThat(preview.tenantId()).isEqualTo(42L);
    assertThat(preview.tenantName()).isNull();
    assertThat(preview.content()).contains("Operator AVV");
  }

  @Test
  void getSignPreview_Should_failClosed_When_tenantNeitherExistsNorReserved() {
    // given
    var rawToken = "orphan-preview-token";
    var version = LocalDateTime.of(2026, 7, 20, 12, 30);
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(
            Optional.of(
                TenantDpaSignatureEntity.builder()
                    .tenantId(42L)
                    .dpaVersion(version)
                    .status(DpaSignatureStatus.PENDING)
                    .tokenHash(DpaSignToken.hash(rawToken))
                    .tokenExpiresAt(LocalDateTime.now().plusDays(1))
                    .build()));
    when(versionRepository.findFirstByTenantIdAndActivationDate(42L, version))
        .thenReturn(
            Optional.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(42L)
                    .activationDate(version)
                    .content("{}")
                    .build()));
    when(tenantRepository.findById(42L)).thenReturn(Optional.empty());
    when(tenantIdReservationRepository.existsById(42L)).thenReturn(false);

    // when / then
    assertThatThrownBy(() -> tenantDpaService.getSignPreview(rawToken))
        .isInstanceOf(InvalidDpaSignTokenException.class);
  }

  @Test
  void getSignPreview_Should_rejectExpiredToken() {
    // given
    var rawToken = "expired-preview-token";
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(
            Optional.of(
                TenantDpaSignatureEntity.builder()
                    .status(DpaSignatureStatus.PENDING)
                    .tokenExpiresAt(LocalDateTime.now().minusMinutes(1))
                    .build()));

    // when / then
    assertThatThrownBy(() -> tenantDpaService.getSignPreview(rawToken))
        .isInstanceOf(InvalidDpaSignTokenException.class);
    verifyNoInteractions(versionRepository);
  }

  @Test
  void confirmSignature_Should_markSigned_andConsumeToken_When_tokenValid() {
    // given a link an authenticated admin forwarded: identity was stamped at creation
    var rawToken = "raw-token-value";
    var pending =
        TenantDpaSignatureEntity.builder()
            .tenantId(7L)
            .status(DpaSignatureStatus.PENDING)
            .forwardedByUserId("tenant-admin-1")
            .source(TenantDpaService.SOURCE_FORWARDED_EXTERNAL)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .build();
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(Optional.of(pending));
    when(signatureRepository.consumeSignToken(
            eq(DpaSignToken.hash(rawToken)),
            eq("Erika M"),
            eq("Geschäftsführerin"),
            eq("erika@example.org"),
            eq("Caritas Beispiel"),
            eq(false),
            eq("de"),
            any(LocalDateTime.class)))
        .thenReturn(1);

    // when
    var result =
        tenantDpaService.confirmSignature(
            rawToken,
            "Erika M",
            "Geschäftsführerin",
            "erika@example.org",
            "Caritas Beispiel",
            false,
            "de");

    // then
    verify(signatureRepository)
        .consumeSignToken(
            any(), any(), any(), any(), any(), any(), any(), any(LocalDateTime.class));
    // any successful signature kills every other outstanding link of the tenant (#179)
    verify(signatureRepository).invalidateOutstandingByTenantId(7L);
    assertThat(result.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(result.getSignerName()).isEqualTo("Erika M");
    assertThat(result.getSignerEmail()).isEqualTo("erika@example.org");
    assertThat(result.getSignerOrganisation()).isEqualTo("Caritas Beispiel");
    // the stamped forwarder identity survives; the request cannot override it
    assertThat(result.getForwardedByUserId()).isEqualTo("tenant-admin-1");
    assertThat(result.getSource()).isEqualTo(TenantDpaService.SOURCE_FORWARDED_EXTERNAL);
    assertThat(result.getSignedAt()).isNotNull();
    assertThat(result.getTokenHash()).isNull(); // consumed -> single use
  }

  @Test
  void confirmSignature_Should_normalizeLegacyRowsToForwardedExternal() {
    // given a legacy PENDING row created before source stamping existed
    var rawToken = "legacy-token";
    var pending =
        TenantDpaSignatureEntity.builder()
            .tenantId(7L)
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .build();
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(Optional.of(pending));
    when(signatureRepository.consumeSignToken(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // when
    var result = tenantDpaService.confirmSignature(rawToken, "n", "p", null, null, false, "de");

    // then: a token-based signature is by definition the forwarded path
    assertThat(result.getSource()).isEqualTo(TenantDpaService.SOURCE_FORWARDED_EXTERNAL);
    assertThat(result.getForwardedByUserId()).isNull();
  }

  @Test
  void confirmSignature_Should_throw_When_tokenNullOrBlank() {
    // when / then
    assertThatThrownBy(
            () -> tenantDpaService.confirmSignature(" ", "n", "p", null, null, false, "de"))
        .isInstanceOf(InvalidDpaSignTokenException.class);
    verifyNoInteractions(signatureRepository);
  }

  @Test
  void confirmSignature_Should_throw_When_lostConcurrentRace() {
    // given a valid, non-expired PENDING row, but the atomic consume affects 0 rows (someone won)
    var rawToken = "race-token";
    var pending =
        TenantDpaSignatureEntity.builder()
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .build();
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(Optional.of(pending));
    when(signatureRepository.consumeSignToken(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(0);

    // when / then
    assertThatThrownBy(
            () -> tenantDpaService.confirmSignature(rawToken, "n", "p", null, null, false, "de"))
        .isInstanceOf(InvalidDpaSignTokenException.class);
  }

  @Test
  void confirmSignature_Should_throw_When_tokenUnknownOrAlreadyUsed() {
    // given
    when(signatureRepository.findByTokenHashAndStatus(any(), any())).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(
            () -> tenantDpaService.confirmSignature("bad", "n", "p", null, null, false, "de"))
        .isInstanceOf(InvalidDpaSignTokenException.class);
  }

  @Test
  void confirmSignature_Should_throw_When_tokenExpired() {
    // given
    var rawToken = "expired-token";
    var pending =
        TenantDpaSignatureEntity.builder()
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(LocalDateTime.now().minusMinutes(1))
            .build();
    when(signatureRepository.findByTokenHashAndStatus(
            DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING))
        .thenReturn(Optional.of(pending));

    // when / then
    assertThatThrownBy(
            () -> tenantDpaService.confirmSignature(rawToken, "n", "p", null, null, false, "de"))
        .isInstanceOf(InvalidDpaSignTokenException.class);
  }

  @Test
  void hasOutstandingSignInvite_Should_delegateToUnexpiredPendingPredicate() {
    // given
    when(signatureRepository.existsByTenantIdAndStatusAndTokenExpiresAtAfter(
            eq(7L), eq(DpaSignatureStatus.PENDING), any(LocalDateTime.class)))
        .thenReturn(true);

    // when / then
    assertThat(tenantDpaService.hasOutstandingSignInvite(7L)).isTrue();
  }

  @Test
  void invalidateOutstandingSignInvites_Should_delegateToBulkInvalidation() {
    // when
    tenantDpaService.invalidateOutstandingSignInvites(7L);

    // then
    verify(signatureRepository).invalidateOutstandingByTenantId(7L);
  }

  private TenantDpaSignatureEntity signature(LocalDateTime version) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(5L)
        .dpaVersion(version)
        .status(DpaSignatureStatus.SIGNED)
        .build();
  }
}
