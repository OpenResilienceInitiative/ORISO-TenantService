package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import java.time.LocalDateTime;
import java.util.List;
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

  private TenantDpaSignatureEntity signature(LocalDateTime version) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(5L)
        .dpaVersion(version)
        .status(DpaSignatureStatus.SIGNED)
        .build();
  }
}
