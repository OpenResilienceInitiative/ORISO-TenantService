package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TenantDpaRetentionServiceTest {

  @Mock private TenantDpaSignatureRepository signatureRepository;
  @InjectMocks private TenantDpaRetentionService retentionService;

  @Test
  void purgeExpiredDeniedSignatures_Should_deleteDeniedOlderThanRetentionWindow() {
    // given a 365-day window
    ReflectionTestUtils.setField(retentionService, "deniedRetentionDays", 365L);
    when(signatureRepository.deleteByStatusAndCreateDateBefore(
            eq(DpaSignatureStatus.DENIED), any(LocalDateTime.class)))
        .thenReturn(3L);

    // when
    long removed = retentionService.purgeExpiredDeniedSignatures();

    // then only DENIED are targeted, with a cutoff ~ now - 365 days
    assertThat(removed).isEqualTo(3);
    var cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(signatureRepository)
        .deleteByStatusAndCreateDateBefore(eq(DpaSignatureStatus.DENIED), cutoff.capture());
    assertThat(cutoff.getValue())
        .isBefore(LocalDateTime.now().minusDays(364))
        .isAfter(LocalDateTime.now().minusDays(366));
  }
}
