package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.facade.TranslationFacade;
import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignPreviewDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaSignatureRequestDTO;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.DpaVersionDTO;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

  @Mock
  private com.vi.tenantservice.api.service.DpaSignedNoticeHintService dpaSignedNoticeHintService;

  @Mock private TranslationFacade translationFacade;
  @Mock com.vi.tenantservice.api.service.TenantMediaService tenantMediaService;
  @Mock com.vi.tenantservice.api.service.TenantIdAllocationService tenantIdAllocationService;

  @Mock com.vi.tenantservice.api.facade.PlatformDpiaMasterDataFacade platformDpiaMasterDataFacade;

  @InjectMocks private TenantController controller;

  @Test
  void getDataProcessingAgreementPreview_Should_returnContractBoundToToken() {
    // given
    var version = LocalDateTime.of(2026, 7, 20, 12, 30);
    var expiresAt = LocalDateTime.of(2026, 8, 3, 12, 30);
    when(tenantDpaService.getSignPreview("tok"))
        .thenReturn(
            new TenantDpaService.DpaSignPreview(
                7L, "Träger Nord", version, "{\"de\":\"<p>Vertragstext</p>\"}", expiresAt));

    // when
    var response = controller.getDataProcessingAgreementPreview("tok");

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isInstanceOf(DpaSignPreviewDTO.class);
    assertThat(response.getBody().getTenantName()).isEqualTo("Träger Nord");
    assertThat(response.getBody().getDpaVersion()).isEqualTo("2026-07-20T12:30");
    assertThat(response.getBody().getContent()).contains("Vertragstext");
    assertThat(response.getBody().getExpiresAt()).isEqualTo("2026-08-03T12:30");
    verifyNoInteractions(tenantServiceFacade);
  }

  @Test
  void confirmDataProcessingAgreement_Should_returnOkWithMappedDto() {
    // given: forwardedByUserId/source in the request are legacy fields the server must IGNORE —
    // the stamped row values win (#179)
    var request =
        new DpaSignatureRequestDTO()
            .signerName("Erika M")
            .signerPosition("Geschäftsführerin")
            .signerEmail("erika@example.org")
            .signerOrganisation("Caritas Beispiel")
            .forwardedByUserId("spoofed-admin")
            .source("SPOOFED_SOURCE")
            .signerIsMember(false)
            .accepted(true)
            .language("de");
    var signedAt = LocalDateTime.now();
    var entity =
        TenantDpaSignatureEntity.builder()
            .tenantId(7L)
            .status(DpaSignatureStatus.SIGNED)
            .signerName("Erika M")
            .signerEmail("erika@example.org")
            .signerOrganisation("Caritas Beispiel")
            .forwardedByUserId("tenant-admin-1")
            .source("FORWARDED_EXTERNAL")
            .signedAt(signedAt)
            .build();
    when(tenantDpaService.confirmSignature(
            "tok",
            "Erika M",
            "Geschäftsführerin",
            "erika@example.org",
            "Caritas Beispiel",
            false,
            "de"))
        .thenReturn(entity);

    // when
    var response = controller.confirmDataProcessingAgreement("tok", request);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTenantId()).isEqualTo(7L);
    assertThat(response.getBody().getStatus()).isEqualTo("SIGNED");
    assertThat(response.getBody().getSignerName()).isEqualTo("Erika M");
    assertThat(response.getBody().getSignerEmail()).isEqualTo("erika@example.org");
    assertThat(response.getBody().getSignerOrganisation()).isEqualTo("Caritas Beispiel");
    // the response reflects the STAMPED forwarder identity, not the spoofed request fields
    assertThat(response.getBody().getForwardedByUserId()).isEqualTo("tenant-admin-1");
    assertThat(response.getBody().getSource()).isEqualTo("FORWARDED_EXTERNAL");
    // the UserService is hinted so it can notify the forwarding admin (ORISO-UserService#1005)
    org.mockito.Mockito.verify(dpaSignedNoticeHintService).notifySignatureRecorded(7L);
  }

  @Test
  void createPublicDpaForwardInvite_Should_delegateToFacade() {
    // given
    var request =
        new com.vi.tenantservice.api.model.PublicDpaForwardRequestDTO()
            .reservedTenantId(42L)
            .tenantIdReservationToken("reservation-token");
    when(tenantDpaFacade.createPublicForwardSignInvite(request))
        .thenReturn(
            new DpaSignInviteDTO().token("raw").signLink("https://app.example.org/dpa-sign/raw"));

    // when
    var response = controller.createPublicDpaForwardInvite(request);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSignLink()).contains("/dpa-sign/");
  }

  @Test
  void confirmDataProcessingAgreement_Should_rejectMissingAcceptConfirmation() {
    // given
    var request =
        new DpaSignatureRequestDTO()
            .signerName("Erika M")
            .signerPosition("Geschäftsführerin")
            .signerEmail("erika@example.org")
            .signerOrganisation("Caritas Beispiel")
            .language("de");

    // when
    var response = controller.confirmDataProcessingAgreement("tok", request);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(tenantDpaService);
  }

  @Test
  void handleSignLockContention_Should_return503WithRetryAfter_NotAServerError() {
    // a contended confirmation wrote nothing and the token is still valid, so neither 500 nor 410
    // is the truth — the caller should simply come back
    var response =
        controller.handleSignLockContention(
            new org.springframework.dao.CannotAcquireLockException("lock wait timeout"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("2");
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

  @Test
  void publishDataProcessingAgreement_Should_returnOkWithGate() {
    // given
    when(tenantDpaFacade.publishDpa(eq(7L), any()))
        .thenReturn(new DpaGateStatusDTO().dpaPublished(true).dpaSigned(false));

    // when
    var response = controller.publishDataProcessingAgreement(7L, Map.of("de", "<p>x</p>"));

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getDpaPublished()).isTrue();
  }

  @Test
  void getDataProcessingAgreementVersions_Should_returnOkWithFacadeList() {
    // given
    when(tenantDpaFacade.getVersions(7L))
        .thenReturn(
            List.of(
                new DpaVersionDTO().activationDate("2026-07-13T10:22").content("{\"de\":\"x\"}")));

    // when
    var response = controller.getDataProcessingAgreementVersions(7L);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }
}
