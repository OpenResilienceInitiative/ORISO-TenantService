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
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.GoverningDpaResolver;
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
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TenantDpaFacadeTest {

  @Mock private TenantDpaService tenantDpaService;
  @Mock private TenantDpaStatusService tenantDpaStatusService;
  @Mock private GoverningDpaResolver governingDpaResolver;
  @Mock private TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  @Mock private TenantService tenantService;

  @Mock
  private com.vi.tenantservice.api.repository.TenantIdReservationRepository
      tenantIdReservationRepository;

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
  void getGateStatus_Should_reportPublishedAndSigned_When_theAuthoritativeStatusIsValid() {
    // given
    var version = LocalDateTime.of(2026, 7, 19, 20, 0);
    givenStatus(5L, TenantDpaStatus.VALID, version);

    // when
    var status = tenantDpaFacade.getGateStatus(5L);

    // then
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isTrue();
  }

  @Test
  void getGateStatus_Should_reportNotPublished_When_noDpaIsInForce() {
    // given a tenant with no DPA of its own and no governing operator DPA
    givenStatus(5L, TenantDpaStatus.MISSING, null);

    // when
    var status = tenantDpaFacade.getGateStatus(5L);

    // then
    assertThat(status.getDpaPublished()).isFalse();
    assertThat(status.getDpaSigned()).isFalse();
  }

  /**
   * The gate must agree with the U9 status: a tenant measured against the governing operator DPA is
   * "published" for the gate too, otherwise signing it in-app leaves agency work blocked forever.
   */
  @Test
  void getGateStatus_Should_reportPublishedButUnsigned_When_theGoverningDpaIsNotSignedYet() {
    givenStatus(5L, TenantDpaStatus.UNSIGNED, LocalDateTime.of(2026, 7, 19, 20, 0));

    var status = tenantDpaFacade.getGateStatus(5L);

    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isFalse();
  }

  private void givenStatus(Long tenantId, TenantDpaStatus status, LocalDateTime currentVersion) {
    givenStatus(tenantId, status, currentVersion, false);
  }

  private void givenStatus(
      Long tenantId, TenantDpaStatus status, LocalDateTime currentVersion, boolean forwardPending) {
    when(tenantDpaStatusService.getStatus(tenantId))
        .thenReturn(
            new TenantDpaStatusService.DpaStatusView(
                tenantId, status, currentVersion, null, null, null, forwardPending));
  }

  @Test
  void createSignInvite_Should_returnTokenAndLink_When_aDpaIsInForce() {
    // given a tenant governed by its own published DPA
    var version = LocalDateTime.of(2026, 7, 19, 20, 0);
    when(governingDpaResolver.resolve(5L))
        .thenReturn(new GoverningDpaResolver.GoverningDpa(5L, version));
    when(authorisationService.getUserId()).thenReturn("tenant-admin-1");
    when(tenantDpaService.createSignInvite(eq(5L), eq(version), any(), eq("tenant-admin-1")))
        .thenReturn("RAWTOKEN");

    // when
    var result = tenantDpaFacade.createSignInvite(5L);

    // then
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    assertThat(result.getToken()).isEqualTo("RAWTOKEN");
    assertThat(result.getSignLink()).endsWith("/dpa-sign/RAWTOKEN");
    assertThat(result.getExpiresAt()).isNotBlank();
    // the authenticated forwarder is stamped on the invite (#179)
    verify(tenantDpaService).createSignInvite(eq(5L), eq(version), any(), eq("tenant-admin-1"));
  }

  @Test
  void createSignInvite_Should_throw_When_noDpaIsInForce() {
    // given a tenant with no DPA of its own and no governing operator DPA
    when(governingDpaResolver.resolve(5L)).thenReturn(null);

    // when / then
    assertThatThrownBy(() -> tenantDpaFacade.createSignInvite(5L))
        .isInstanceOf(DpaNotPublishedException.class);
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  /**
   * The forwarding path covers the same governing document as the in-app path (#569): the invite
   * carries the OPERATOR's version but is recorded under the TENANT, so the resulting signature
   * counts for that tenant's gate.
   */
  @Test
  void createSignInvite_Should_forwardTheOperatorVersion_UnderTheTenantId() {
    var operatorVersion = LocalDateTime.of(2026, 7, 19, 20, 0);
    when(governingDpaResolver.resolve(5L))
        .thenReturn(new GoverningDpaResolver.GoverningDpa(1L, operatorVersion));
    when(tenantDpaService.createSignInvite(eq(5L), eq(operatorVersion), any(), any()))
        .thenReturn("RAWTOKEN");

    var result = tenantDpaFacade.createSignInvite(5L);

    assertThat(result.getToken()).isEqualTo("RAWTOKEN");
    verify(tenantDpaService).createSignInvite(eq(5L), eq(operatorVersion), any(), any());
  }

  // --- public forward from the onboarding context (#179) ---

  private static com.vi.tenantservice.api.model.PublicDpaForwardRequestDTO forwardRequest() {
    return new com.vi.tenantservice.api.model.PublicDpaForwardRequestDTO()
        .reservedTenantId(42L)
        .tenantIdReservationToken("reservation-token");
  }

  private void givenReservation(long tenantId, String token) {
    givenReservation(
        tenantId, token, com.vi.tenantservice.api.model.TenantIdReservationStatus.RESERVED);
  }

  private void givenReservation(
      long tenantId,
      String token,
      com.vi.tenantservice.api.model.TenantIdReservationStatus status) {
    when(tenantIdReservationRepository.findByTenantIdForUpdate(tenantId))
        .thenReturn(
            Optional.of(
                com.vi.tenantservice.api.model.TenantIdReservationEntity.builder()
                    .tenantId(tenantId)
                    .token(token)
                    .status(status)
                    .build()));
  }

  @Test
  void
      createPublicForwardSignInvite_Should_bindOperatorVersionToReservedTenant_BeforeRegistration() {
    // given a valid reservation whose tenant does not exist yet
    givenReservation(42L, "reservation-token");
    var operatorVersion = LocalDateTime.of(2026, 7, 19, 20, 0);
    when(governingDpaResolver.resolveForUnregisteredTenant())
        .thenReturn(new GoverningDpaResolver.GoverningDpa(1L, operatorVersion));
    when(tenantDpaService.createSignInvite(
            eq(42L), eq(operatorVersion), any(), eq(null), eq("reservation-token")))
        .thenReturn("RAWTOKEN");

    // when
    var result = tenantDpaFacade.createPublicForwardSignInvite(forwardRequest());

    // then: bound to the RESERVED tenant id, no forwarder account, no session assertion
    assertThat(result.getSignLink()).endsWith("/dpa-sign/RAWTOKEN");
    verify(tenantDpaService)
        .createSignInvite(eq(42L), eq(operatorVersion), any(), eq(null), eq("reservation-token"));
    verifyNoInteractions(tenantFacadeAuthorisationService);
  }

  @Test
  void createPublicForwardSignInvite_Should_failClosed_When_reservationUnknown() {
    // given
    when(tenantIdReservationRepository.findByTenantIdForUpdate(42L)).thenReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> tenantDpaFacade.createPublicForwardSignInvite(forwardRequest()))
        .isInstanceOf(com.vi.tenantservice.api.service.InvalidDpaSignTokenException.class);
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  @Test
  void createPublicForwardSignInvite_Should_failClosed_When_reservationTokenMismatches() {
    // given
    givenReservation(42L, "the-real-token");

    // when / then
    assertThatThrownBy(() -> tenantDpaFacade.createPublicForwardSignInvite(forwardRequest()))
        .isInstanceOf(com.vi.tenantservice.api.service.InvalidDpaSignTokenException.class);
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  @Test
  void
      createPublicForwardSignInvite_Should_failClosed_When_registrationAlreadyConsumedTheReservation() {
    // registration flips the ledger row to ASSIGNED but keeps the same token — the onboarding
    // credential must stop minting links at that point (#179)
    givenReservation(
        42L,
        "reservation-token",
        com.vi.tenantservice.api.model.TenantIdReservationStatus.ASSIGNED);

    assertThatThrownBy(() -> tenantDpaFacade.createPublicForwardSignInvite(forwardRequest()))
        .isInstanceOf(com.vi.tenantservice.api.service.InvalidDpaSignTokenException.class);
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  @Test
  void createPublicForwardSignInvite_Should_answerRetry_When_theReservationRowIsLocked() {
    // a concurrent forward for the same onboarding holds the row; the caller must get a retryable
    // answer rather than a generic 500 once the bounded lock wait elapses
    when(tenantIdReservationRepository.findByTenantIdForUpdate(42L))
        .thenThrow(new org.springframework.dao.CannotAcquireLockException("lock wait timeout"));

    assertThatThrownBy(() -> tenantDpaFacade.createPublicForwardSignInvite(forwardRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("429");
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  @Test
  void createPublicForwardSignInvite_Should_throttle_When_tooManyLinksAreAlreadyOutstanding() {
    // the endpoint needs no session, so a leaked onboarding token could otherwise mint links (and
    // signature rows) without limit. Existing links are capped, never replaced — the product rule
    // is that every issued link keeps working until a signature lands.
    givenReservation(42L, "reservation-token");
    when(tenantDpaService.countOutstandingSignInvites(42L, "reservation-token"))
        .thenReturn((long) TenantDpaFacade.MAX_OUTSTANDING_INVITES);

    assertThatThrownBy(() -> tenantDpaFacade.createPublicForwardSignInvite(forwardRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("429");
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  @Test
  void createPublicForwardSignInvite_Should_countOnlyLinksOfThePresentedReservation() {
    // an id released and reserved again keeps its number; the previous reservation's links are
    // already dead (binding mismatch), so they must not spend the new onboarding's budget
    givenReservation(42L, "reservation-token");
    var operatorVersion = LocalDateTime.of(2026, 7, 19, 20, 0);
    when(governingDpaResolver.resolveForUnregisteredTenant())
        .thenReturn(new GoverningDpaResolver.GoverningDpa(1L, operatorVersion));
    when(tenantDpaService.createSignInvite(
            eq(42L), eq(operatorVersion), any(), eq(null), eq("reservation-token")))
        .thenReturn("RAWTOKEN");

    tenantDpaFacade.createPublicForwardSignInvite(forwardRequest());

    // the budget question is asked about THIS reservation, never about the tenant id alone
    verify(tenantDpaService).countOutstandingSignInvites(42L, "reservation-token");
  }

  @Test
  void createPublicForwardSignInvite_Should_throw_When_nothingIsPublishedToSign() {
    // given
    givenReservation(42L, "reservation-token");
    when(governingDpaResolver.resolveForUnregisteredTenant()).thenReturn(null);

    // when / then
    assertThatThrownBy(() -> tenantDpaFacade.createPublicForwardSignInvite(forwardRequest()))
        .isInstanceOf(DpaNotPublishedException.class);
    verify(tenantDpaService, never()).createSignInvite(any(), any(), any(), any(), any());
  }

  @Test
  void createPublicForwardSignInvite_Should_rejectIncompleteRequests() {
    assertThatThrownBy(
            () ->
                tenantDpaFacade.createPublicForwardSignInvite(
                    new com.vi.tenantservice.api.model.PublicDpaForwardRequestDTO()
                        .reservedTenantId(42L)))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verifyNoInteractions(tenantIdReservationRepository);
  }

  @Test
  void getGateStatus_Should_reportForwardPending_When_contractIsOnHold() {
    // the signature status stays the UNSIGNED existing consumers already handle; the waiting
    // state travels as the additive flag (#179)
    givenStatus(5L, TenantDpaStatus.UNSIGNED, LocalDateTime.of(2026, 7, 19, 20, 0), true);

    var status = tenantDpaFacade.getGateStatus(5L);

    assertThat(status.getDpaPublished()).isTrue();
    assertThat(status.getDpaSigned()).isFalse();
    assertThat(status.getDpaForwardPending()).isTrue();
  }

  @Test
  void getDpaStatus_Should_exposeForwardPending_OnTheStatusDto() {
    givenStatus(5L, TenantDpaStatus.UNSIGNED, LocalDateTime.of(2026, 7, 19, 20, 0), true);
    when(tenantService.findTenantById(5L)).thenReturn(Optional.of(new TenantEntity()));

    var status = tenantDpaFacade.getDpaStatus(5L);

    assertThat(status.getStatus())
        .isEqualTo(com.vi.tenantservice.api.model.DpaStatusDTO.StatusEnum.UNSIGNED);
    assertThat(status.getForwardPending()).isTrue();
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
    // given a tenant governed by its own published DPA
    when(governingDpaResolver.documentTenantIdFor(5L)).thenReturn(5L);
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

  /**
   * The document a tenant is MEASURED against must be the document it can READ (#569): otherwise
   * the U10 blocker renders with nothing to sign and cannot be resolved.
   */
  @Test
  void getVersions_Should_serveTheGoverningOperatorDocument_When_theTenantHasNoOwnDpa() {
    // given a tenant governed by the operator DPA (tenant 1)
    when(governingDpaResolver.documentTenantIdFor(5L)).thenReturn(1L);
    when(tenantDpaService.getVersions(1L))
        .thenReturn(
            List.of(
                TenantDpaVersionEntity.builder()
                    .tenantId(1L)
                    .activationDate(LocalDateTime.of(2026, 7, 1, 12, 0))
                    .content("{\"de\":\"operator\",\"en\":\"operator-en\"}")
                    .build()));

    // when
    var result = tenantDpaFacade.getVersions(5L);

    // then the full multilingual map is passed through — the caller picks the signer's language
    verify(tenantFacadeAuthorisationService).assertUserIsAuthorizedToAccessTenant(5L);
    verify(tenantDpaService, never()).getVersions(5L);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent())
        .isEqualTo("{\"de\":\"operator\",\"en\":\"operator-en\"}");
    assertThat(result.get(0).getActivationDate()).isEqualTo("2026-07-01T12:00");
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
