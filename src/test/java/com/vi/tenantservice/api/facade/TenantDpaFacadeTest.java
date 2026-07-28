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
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.api.service.TenantDpaStatusService;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.util.JsonConverter;
import com.vi.tenantservice.api.validation.InputSanitizer;
import com.vi.tenantservice.config.security.AuthorisationService;
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
  @Mock private TenantDpaStatusService tenantDpaStatusService;
  @Mock private TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  @Mock private TenantService tenantService;
  @Mock private InputSanitizer inputSanitizer;
  @Mock private AuthorisationService authorisationService;
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
                    .signerEmail("erika@example.org")
                    .signerOrganisation("Caritas Beispiel")
                    .source("PUBLIC_SIGN_LINK")
                    .signedAt(LocalDateTime.now())
                    .build()));

    // when
    var result = tenantDpaFacade.getSignatures(5L);

    // then — IDOR guard runs first
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo("SIGNED");
    assertThat(result.get(0).getSignerName()).isEqualTo("Erika");
    assertThat(result.get(0).getSignerEmail()).isEqualTo("erika@example.org");
    assertThat(result.get(0).getSignerOrganisation()).isEqualTo("Caritas Beispiel");
    assertThat(result.get(0).getSource()).isEqualTo("PUBLIC_SIGN_LINK");
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
  void getGateStatus_Should_recoverLatestPublishedVersion_When_tenantActivationWasCleared() {
    var version = LocalDateTime.of(2026, 7, 19, 20, 0);
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));
    when(tenantDpaService.getVersions(5L))
        .thenReturn(
            List.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(5L)
                    .activationDate(version)
                    .content("{\"de\":\"published\"}")
                    .build()));
    when(tenantDpaService.isSignedForVersion(5L, version)).thenReturn(true);

    var status = tenantDpaFacade.getGateStatus(5L);

    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isTrue();
  }

  @Test
  void getGateStatus_Should_notRecoverHistory_When_tenantDoesNotExist() {
    when(tenantService.findTenantById(5L)).thenReturn(Optional.empty());

    var status = tenantDpaFacade.getGateStatus(5L);

    assertThat(status.getDpaPublished()).isFalse();
    assertThat(status.getDpaSigned()).isFalse();
    verify(tenantDpaService, never()).getVersions(any());
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
  void createSignInvite_Should_recoverLatestPublishedVersion_When_tenantActivationWasCleared() {
    var version = LocalDateTime.of(2026, 7, 19, 20, 0);
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));
    when(tenantDpaService.getVersions(5L))
        .thenReturn(
            List.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(5L)
                    .activationDate(version)
                    .content("{\"de\":\"published\"}")
                    .build()));
    when(tenantDpaService.createSignInvite(eq(5L), eq(version), any())).thenReturn("RAWTOKEN");

    var result = tenantDpaFacade.createSignInvite(5L);

    assertThat(result.getToken()).isEqualTo("RAWTOKEN");
    verify(tenantDpaService).createSignInvite(eq(5L), eq(version), any());
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
    verify(tenantDpaService).recordPublishedVersion(eq(5L), any(), any());
    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isFalse();
  }

  @Test
  void getVersions_Should_assertTenantAccess_andMapVersions() {
    // given
    when(tenantDpaService.getVersions(5L))
        .thenReturn(
            List.of(
                TenantDpaVersionEntity.builder()
                    .activationDate(LocalDateTime.now())
                    .content("{\"de\":\"x\"}")
                    .build()));

    // when
    var result = tenantDpaFacade.getVersions(5L);

    // then
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("{\"de\":\"x\"}");
    assertThat(result.get(0).getActivationDate()).isNotBlank();
  }

  @Test
  void publishDpa_Should_publishWithoutSanitizing_When_contentMapIsNull() {
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));

    var status = tenantDpaFacade.publishDpa(5L, null);

    verify(inputSanitizer, never()).sanitizeAllowingFormattingAndLinks(any());
    verify(tenantDpaService).recordPublishedVersion(eq(5L), any(), any());
    assertThat(status.getDpaPublished()).isTrue();
  }

  @Test
  void publishDpa_Should_useLinkAllowingSanitiser_forEveryLanguage() {
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));
    when(inputSanitizer.sanitizeAllowingFormattingAndLinks(any())).thenReturn("clean");

    tenantDpaFacade.publishDpa(5L, Map.of("de", "<p>d</p>", "en", "<p>e</p>"));

    verify(inputSanitizer).sanitizeAllowingFormattingAndLinks("<p>d</p>");
    verify(inputSanitizer).sanitizeAllowingFormattingAndLinks("<p>e</p>");
  }

  @Test
  void publishDpa_Should_throwNotFound_When_tenantMissing() {
    when(tenantService.findTenantById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tenantDpaFacade.publishDpa(5L, Map.of("de", "x")))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verify(tenantDpaService, never()).recordPublishedVersion(any(), any(), any());
  }

  // --- machine-translation metadata convention (documentation/translation-meta.md) ---

  private static final String META_EN =
      "{\"mt\":true,\"src\":\"de\",\"at\":\"2026-07-03T10:15:30Z\"}";

  private TenantEntity givenTenantWithStoredDpa(Map<String, String> storedMap) {
    var tenant = new TenantEntity();
    if (storedMap != null) {
      tenant.setContentDataProcessingAgreement(JsonConverter.convertToJson(storedMap));
    }
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(tenant));
    return tenant;
  }

  private void givenIdentitySanitizer() {
    when(inputSanitizer.sanitizeAllowingFormattingAndLinks(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Map<String, String> storedMapOf(TenantEntity tenant) {
    return JsonConverter.convertMapFromJson(tenant.getContentDataProcessingAgreement());
  }

  @Test
  void publishDpa_Should_acceptAndStoreMetaKeys_withoutSanitizingThem() {
    var tenant = givenTenantWithStoredDpa(null);
    givenIdentitySanitizer();

    tenantDpaFacade.publishDpa(5L, Map.of("en", "<p>Hello</p>", "en__meta", META_EN));

    var stored = storedMapOf(tenant);
    assertThat(stored).containsEntry("en", "<p>Hello</p>").containsEntry("en__meta", META_EN);
    // the sanitizer must never see the meta value
    verify(inputSanitizer, never()).sanitizeAllowingFormattingAndLinks(META_EN);
  }

  @Test
  void publishDpa_Should_rejectInvalidMeta_withBadRequest() {
    givenTenantWithStoredDpa(null);

    assertThatThrownBy(
            () ->
                tenantDpaFacade.publishDpa(
                    5L, Map.of("en", "<p>x</p>", "en__meta", "{\"mt\":true,\"evil\":\"field\"}")))
        .isInstanceOfSatisfying(
            org.springframework.web.server.ResponseStatusException.class,
            e ->
                assertThat(e.getStatusCode())
                    .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST));
    verify(tenantDpaService, never()).recordPublishedVersion(any(), any(), any());
  }

  @Test
  void publishDpa_Should_removeMeta_When_manualEditResendsPreviouslyStoredMeta() {
    // given: "en" is machine translated (mt:true meta stored)
    var tenant = givenTenantWithStoredDpa(Map.of("en", "<p>machine</p>", "en__meta", META_EN));
    givenIdentitySanitizer();

    // when: a manual edit changes the HTML but the UI round-trips the old meta unchanged
    tenantDpaFacade.publishDpa(5L, Map.of("en", "<p>manually edited</p>", "en__meta", META_EN));

    // then: the machine-translated tag is cleared
    var stored = storedMapOf(tenant);
    assertThat(stored).containsEntry("en", "<p>manually edited</p>").doesNotContainKey("en__meta");
  }

  @Test
  void publishDpa_Should_keepMeta_When_contentUnchangedOnRepublish() {
    var tenant = givenTenantWithStoredDpa(Map.of("en", "<p>machine</p>", "en__meta", META_EN));
    givenIdentitySanitizer();

    tenantDpaFacade.publishDpa(5L, Map.of("en", "<p>machine</p>", "en__meta", META_EN));

    assertThat(storedMapOf(tenant)).containsEntry("en__meta", META_EN);
  }

  @Test
  void publishDpa_Should_storeNewMeta_When_freshMachineTranslationReplacesContent() {
    var tenant = givenTenantWithStoredDpa(Map.of("en", "<p>old machine</p>", "en__meta", META_EN));
    givenIdentitySanitizer();
    var freshMeta = "{\"mt\":true,\"src\":\"de\",\"at\":\"2026-07-04T08:00:00Z\"}";

    tenantDpaFacade.publishDpa(5L, Map.of("en", "<p>new machine</p>", "en__meta", freshMeta));

    var stored = storedMapOf(tenant);
    assertThat(stored)
        .containsEntry("en", "<p>new machine</p>")
        .containsEntry("en__meta", freshMeta);
  }

  @Test
  void publishDpa_Should_dropOrphanMeta_When_languageHasNoContent() {
    var tenant = givenTenantWithStoredDpa(null);

    tenantDpaFacade.publishDpa(5L, Map.of("en__meta", META_EN));

    assertThat(storedMapOf(tenant)).isEmpty();
  }

  @Test
  void publishDpa_Should_clearMeta_When_manualPublishOmitsMeta() {
    var tenant = givenTenantWithStoredDpa(Map.of("en", "<p>machine</p>", "en__meta", META_EN));
    givenIdentitySanitizer();

    tenantDpaFacade.publishDpa(5L, Map.of("en", "<p>manual rewrite</p>"));

    assertThat(storedMapOf(tenant)).doesNotContainKey("en__meta");
  }
}
