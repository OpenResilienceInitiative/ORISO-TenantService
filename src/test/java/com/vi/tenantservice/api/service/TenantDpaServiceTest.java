package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantDpaServiceTest {

  @Mock private TenantDpaSignatureRepository signatureRepository;
  @InjectMocks private TenantDpaService tenantDpaService;

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
  void createSignInvite_Should_persistPendingRowWithHashedTokenAndExpiry_andReturnRawToken() {
    // given
    var version = LocalDateTime.now();
    when(signatureRepository.save(any(TenantDpaSignatureEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    var rawToken = tenantDpaService.createSignInvite(7L, version, Duration.ofDays(14));

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
  }

  @Test
  void confirmSignature_Should_markSigned_andConsumeToken_When_tokenValid() {
    // given
    var rawToken = "raw-token-value";
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
    when(signatureRepository.save(any(TenantDpaSignatureEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    var result =
        tenantDpaService.confirmSignature(rawToken, "Erika M", "Geschäftsführerin", false, "de");

    // then
    assertThat(result.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(result.getSignerName()).isEqualTo("Erika M");
    assertThat(result.getSignedAt()).isNotNull();
    assertThat(result.getTokenHash()).isNull(); // consumed -> single use
  }

  @Test
  void confirmSignature_Should_throw_When_tokenUnknownOrAlreadyUsed() {
    // given
    when(signatureRepository.findByTokenHashAndStatus(any(), any())).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> tenantDpaService.confirmSignature("bad", "n", "p", false, "de"))
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
    assertThatThrownBy(() -> tenantDpaService.confirmSignature(rawToken, "n", "p", false, "de"))
        .isInstanceOf(InvalidDpaSignTokenException.class);
  }

  private TenantDpaSignatureEntity signature(LocalDateTime version) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(5L)
        .dpaVersion(version)
        .status(DpaSignatureStatus.SIGNED)
        .build();
  }
}
