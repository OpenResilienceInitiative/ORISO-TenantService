package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaSignatureRequestDTO;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TenantControllerDpaConfirmTest {

  @Mock private TenantServiceFacade tenantServiceFacade;
  @Mock private AuthorisationService authorisationService;
  @Mock private TenantDtoMapper tenantDtoMapper;
  @Mock private TenantDpaService tenantDpaService;
  @Mock private TenantDpaFacade tenantDpaFacade;
  @InjectMocks private TenantController controller;

  @Test
  void confirmDataProcessingAgreement_Should_returnOkWithMappedDto() {
    // given
    var request =
        new DpaSignatureRequestDTO()
            .signerName("Erika M")
            .signerPosition("Geschäftsführerin")
            .signerIsMember(false)
            .language("de");
    var signedAt = LocalDateTime.now();
    var entity =
        TenantDpaSignatureEntity.builder()
            .tenantId(7L)
            .status(DpaSignatureStatus.SIGNED)
            .signerName("Erika M")
            .signedAt(signedAt)
            .build();
    when(tenantDpaService.confirmSignature("tok", "Erika M", "Geschäftsführerin", false, "de"))
        .thenReturn(entity);

    // when
    var response = controller.confirmDataProcessingAgreement("tok", request);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTenantId()).isEqualTo(7L);
    assertThat(response.getBody().getStatus()).isEqualTo("SIGNED");
    assertThat(response.getBody().getSignerName()).isEqualTo("Erika M");
  }

  @Test
  void handleInvalidDpaSignToken_Should_return410Gone() {
    // when
    var response =
        controller.handleInvalidDpaSignToken(new InvalidDpaSignTokenException("expired"));

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
  }

  @Test
  void getDataProcessingAgreementSignatures_Should_returnOkWithFacadeList() {
    // given
    when(tenantDpaFacade.getSignatures(7L))
        .thenReturn(
            List.of(new DpaSignatureDTO().tenantId(7L).status("SIGNED").signerName("Erika")));

    // when
    var response = controller.getDataProcessingAgreementSignatures(7L);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void getDataProcessingAgreementGate_Should_returnOkWithFacadeStatus() {
    // given
    when(tenantDpaFacade.getGateStatus(7L))
        .thenReturn(new DpaGateStatusDTO().dpaPublished(true).dpaSigned(false));

    // when
    var response = controller.getDataProcessingAgreementGate(7L);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getDpaPublished()).isTrue();
    assertThat(response.getBody().getDpaSigned()).isFalse();
  }

  @Test
  void createDataProcessingAgreementSignInvite_Should_returnOkWithInvite() {
    // given
    when(tenantDpaFacade.createSignInvite(7L))
        .thenReturn(
            new DpaSignInviteDTO()
                .token("T")
                .signLink("/dpa-sign/T")
                .expiresAt("2026-07-15T00:00"));

    // when
    var response = controller.createDataProcessingAgreementSignInvite(7L);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getToken()).isEqualTo("T");
    assertThat(response.getBody().getSignLink()).isEqualTo("/dpa-sign/T");
  }

  @Test
  void handleDpaNotPublished_Should_return409Conflict() {
    // when
    var response = controller.handleDpaNotPublished(new DpaNotPublishedException("not published"));

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }
}
