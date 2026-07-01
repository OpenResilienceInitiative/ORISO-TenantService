package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.DpaSignatureRequestDTO;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
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
}
