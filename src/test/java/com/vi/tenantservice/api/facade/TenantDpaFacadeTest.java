package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.validation.InputSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TenantDpaFacadeTest {

  @Mock private TenantDpaService tenantDpaService;
  @Mock private TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  @Mock private TenantService tenantService;
  @Mock private InputSanitizer inputSanitizer;
  @InjectMocks private TenantDpaFacade tenantDpaFacade;

  @Test
  void getSignatures_Should_assertTenantAccess_andMapSignatures() {
    // given
    when(tenantDpaService.getSignatures(5L))
        .thenReturn(
            List.of(
                TenantDpaSignatureEntity.builder()
                    .tenantId(5L)
                    .status(DpaSignatureStatus.SIGNED)
                    .signerName("Erika")
                    .signedAt(LocalDateTime.now())
                    .build()));

    // when
    var result = tenantDpaFacade.getSignatures(5L);

    // then — IDOR guard runs first
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo("SIGNED");
    assertThat(result.get(0).getSignerName()).isEqualTo("Erika");
  }

  @Test
  void getSignatures_Should_throw_andNotQueryService_When_notAuthorizedForTenant() {
    // given
    doThrow(new AccessDeniedException("nope"))
        .when(tenantFacadeAuthorisationService)
        .assertUserIsAuthorizedToAccessTenant(5L);

    // when / then
    assertThatThrownBy(() -> tenantDpaFacade.getSignatures(5L))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(tenantDpaService);
  }

  @Test
  void getGateStatus_Should_reportPublishedAndSigned() {
    // given
    var version = LocalDateTime.now();
    var tenant = new TenantEntity();
    tenant.setContentDataProcessingAgreementActivationDate(version);
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(tenant));
    when(tenantDpaService.isSignedForVersion(5L, version)).thenReturn(true);

    // when
    var status = tenantDpaFacade.getGateStatus(5L);

    // then
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isTrue();
  }

  @Test
  void getGateStatus_Should_reportNotPublished_andSkipSignedCheck_When_noActivationDate() {
    // given a tenant with no DPA activation date (not published)
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));

    // when
    var status = tenantDpaFacade.getGateStatus(5L);

    // then
    assertThat(status.getDpaPublished()).isFalse();
    assertThat(status.getDpaSigned()).isFalse();
    verify(tenantDpaService, never()).isSignedForVersion(any(), any());
  }

  @Test
  void createSignInvite_Should_returnTokenAndLink_When_dpaPublished() {
    // given a tenant with a published DPA
    var tenant = new TenantEntity();
    tenant.setContentDataProcessingAgreementActivationDate(LocalDateTime.now());
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(tenant));
    when(tenantDpaService.createSignInvite(eq(5L), any(), any())).thenReturn("RAWTOKEN");

    // when
    var result = tenantDpaFacade.createSignInvite(5L);

    // then
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(result.getToken()).isEqualTo("RAWTOKEN");
    assertThat(result.getSignLink()).endsWith("/dpa-sign/RAWTOKEN");
    assertThat(result.getExpiresAt()).isNotBlank();
  }

  @Test
  void createSignInvite_Should_throw_When_dpaNotPublished() {
    // given a tenant with no DPA activation date
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));

    // when / then
    assertThatThrownBy(() -> tenantDpaFacade.createSignInvite(5L))
        .isInstanceOf(DpaNotPublishedException.class);
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any());
  }

  @Test
  void publishDpa_Should_sanitize_storeAsJson_stampVersion_andReturnGate() {
    // given
    var tenant = new TenantEntity();
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(tenant));
    when(inputSanitizer.sanitizeAllowingFormattingAndLinks("<p>x</p>")).thenReturn("<p>clean</p>");

    // when
    var status = tenantDpaFacade.publishDpa(5L, Map.of("de", "<p>x</p>"));

    // then
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(tenant.getContentDataProcessingAgreement()).contains("clean");
    assertThat(tenant.getContentDataProcessingAgreementActivationDate()).isNotNull();
    verify(tenantService).update(tenant);
    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isFalse();
  }
}
