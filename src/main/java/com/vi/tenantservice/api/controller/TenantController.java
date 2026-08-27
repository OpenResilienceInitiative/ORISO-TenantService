package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.facade.PlatformDpiaMasterDataFacade;
import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.facade.TranslationFacade;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.DpaAdminSignRequestDTO;
import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignPreviewDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaSignatureRequestDTO;
import com.vi.tenantservice.api.model.DpaStatusDTO;
import com.vi.tenantservice.api.model.DpaVersionDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.NextFreeTenantIdDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.PublicDpaForwardRequestDTO;
import com.vi.tenantservice.api.model.PublicDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantIdAvailabilityDTO;
import com.vi.tenantservice.api.model.TenantIdReservationDTO;
import com.vi.tenantservice.api.model.TenantIdReservationRequestDTO;
import com.vi.tenantservice.api.model.TenantMediaResponseDTO;
import com.vi.tenantservice.api.model.TenantsSearchResultDTO;
import com.vi.tenantservice.api.model.TranslationApiKeyUpdateDTO;
import com.vi.tenantservice.api.model.TranslationApiKeysDTO;
import com.vi.tenantservice.api.model.TranslationErrorDTO;
import com.vi.tenantservice.api.model.TranslationRequestDTO;
import com.vi.tenantservice.api.model.TranslationResponseDTO;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.DpaSignedNoticeHintService;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.MediaSizeLimitExceededException;
import com.vi.tenantservice.api.service.PublicBrandingAssetService;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.TenantMediaService;
import com.vi.tenantservice.api.service.UnsupportedMediaContentException;
import com.vi.tenantservice.api.service.translation.TranslationErrorCode;
import com.vi.tenantservice.api.service.translation.TranslationException;
import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.generated.api.controller.TenantApi;
import com.vi.tenantservice.generated.api.controller.TenantadminApi;
import io.swagger.annotations.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

/** Controller for tenant API operations. */
@RestController
@RequiredArgsConstructor
@Api(tags = "tenant-controller")
@Slf4j
@Validated
public class TenantController implements TenantApi, TenantadminApi {

  private final @NonNull TenantServiceFacade tenantServiceFacade;
  private final @NonNull PublicBrandingAssetService publicBrandingAssetService;
  private final @NonNull AuthorisationService authorisationService;
  private final @NonNull TenantDtoMapper tenantDtoMapper;
  private final @NonNull TenantDpaService tenantDpaService;
  private final @NonNull TenantDpaFacade tenantDpaFacade;
  private final @NonNull DpaSignedNoticeHintService dpaSignedNoticeHintService;
  private final @NonNull TranslationFacade translationFacade;
  private final @NonNull TenantMediaService tenantMediaService;
  private final @NonNull TenantIdAllocationService tenantIdAllocationService;
  private final @NonNull PlatformDpiaMasterDataFacade platformDpiaMasterDataFacade;

  /**
   * Public read-only preview of the exact DPA version referenced by a valid sign token. Merely
   * viewing the agreement never consumes the token.
   */
  @Override
  public ResponseEntity<DpaSignPreviewDTO> getDataProcessingAgreementPreview(
      @NotNull String token) {
    var preview = tenantDpaService.getSignPreview(token);
    return ResponseEntity.ok(
        new DpaSignPreviewDTO()
            .tenantName(preview.tenantName())
            .dpaVersion(preview.dpaVersion().toString())
            .content(preview.content())
            .expiresAt(preview.expiresAt().toString()));
  }

  /**
   * Public DPA confirmation via a single-use sign token. No authentication: the token is the
   * authorisation (an external signer who may not hold a platform account confirms via the link).
   */
  @Override
  public ResponseEntity<DpaSignatureDTO> confirmDataProcessingAgreement(
      @NotNull String token, @Valid DpaSignatureRequestDTO request) {
    if (!Boolean.TRUE.equals(request.getAccepted())) {
      return ResponseEntity.badRequest().build();
    }
    // forwardedByUserId/source from the request are deliberately ignored (#179): the forwarder
    // identity was stamped when the sign link was created and is not client-assignable.
    var signature =
        tenantDpaService.confirmSignature(
            token,
            request.getSignerName(),
            request.getSignerPosition(),
            request.getSignerEmail(),
            request.getSignerOrganisation(),
            Boolean.TRUE.equals(request.getSignerIsMember()),
            request.getLanguage());
    // Fire-and-forget: tells the UserService a forwarded signature landed so it can notify the
    // forwarding administrator (ORISO-UserService#1005). Never fails the confirm.
    dpaSignedNoticeHintService.notifySignatureRecorded(signature.getTenantId());
    var dto =
        new DpaSignatureDTO()
            .tenantId(signature.getTenantId())
            .status(signature.getStatus() == null ? null : signature.getStatus().name())
            .dpaVersion(
                signature.getDpaVersion() == null ? null : signature.getDpaVersion().toString())
            .signerName(signature.getSignerName())
            .signerPosition(signature.getSignerPosition())
            .signerEmail(signature.getSignerEmail())
            .signerOrganisation(signature.getSignerOrganisation())
            .forwardedByUserId(signature.getForwardedByUserId())
            .source(signature.getSource())
            .signedAt(signature.getSignedAt() == null ? null : signature.getSignedAt().toString());
    return ResponseEntity.ok(dto);
  }

  /**
   * Public creation of a DPA sign link from the tenant onboarding wizard (#179). No session — the
   * request is authorised by the invite's tenant-ID reservation pair, validated fail-closed by the
   * facade (410 on anything that does not match the ledger).
   */
  @Override
  public ResponseEntity<DpaSignInviteDTO> createPublicDpaForwardInvite(
      @Valid PublicDpaForwardRequestDTO request) {
    return ResponseEntity.ok(tenantDpaFacade.createPublicForwardSignInvite(request));
  }

  /**
   * Lock contention on the confirm path (#179). The confirmation takes every outstanding link of
   * the tenant before consuming one, so a concurrent confirmation for the same tenant makes the
   * second wait; if that wait times out, nothing was written and the caller should simply retry.
   *
   * <p>503 rather than the mint path's 429: 429 says "you are asking too often", which is a
   * statement about the caller and is what the mint path's link cap genuinely means. Here the
   * caller did nothing wrong and has no budget to stay within — the server was briefly busy with
   * another signer. 503 with Retry-After is the honest description, and it keeps the two situations
   * distinguishable in logs and to the client. What matters most is that it is neither a 500 (the
   * signer has committed nothing and there is no fault to report) nor a 410 (the token is still
   * perfectly valid).
   */
  @ExceptionHandler(PessimisticLockingFailureException.class)
  ResponseEntity<Void> handleSignLockContention(PessimisticLockingFailureException e) {
    log.info("DPA confirmation contended for the same tenant; asking the caller to retry");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "2").build();
  }

  @ExceptionHandler(InvalidDpaSignTokenException.class)
  ResponseEntity<Void> handleInvalidDpaSignToken(InvalidDpaSignTokenException e) {
    log.info("Rejected DPA sign attempt: {}", e.getMessage());
    return new ResponseEntity<>(HttpStatus.GONE);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<List<DpaSignatureDTO>> getDataProcessingAgreementSignatures(
      @NotNull Long id) {
    return new ResponseEntity<>(tenantDpaFacade.getSignatures(id), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<DpaGateStatusDTO> getDataProcessingAgreementGate(@NotNull Long id) {
    return new ResponseEntity<>(tenantDpaFacade.getGateStatus(id), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<List<DpaVersionDTO>> getDataProcessingAgreementVersions(@NotNull Long id) {
    return new ResponseEntity<>(tenantDpaFacade.getVersions(id), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<DpaSignInviteDTO> createDataProcessingAgreementSignInvite(
      @NotNull Long id) {
    return new ResponseEntity<>(tenantDpaFacade.createSignInvite(id), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<DpaGateStatusDTO> publishDataProcessingAgreement(
      @NotNull Long id, @Valid Map<String, String> requestBody) {
    return new ResponseEntity<>(tenantDpaFacade.publishDpa(id, requestBody), HttpStatus.OK);
  }

  @ExceptionHandler(DpaNotPublishedException.class)
  ResponseEntity<Void> handleDpaNotPublished(DpaNotPublishedException e) {
    log.info("DPA sign invite rejected: {}", e.getMessage());
    return new ResponseEntity<>(HttpStatus.CONFLICT);
  }

  /**
   * Authoritative DPA state of a tenant for its authenticated tenant admins (TEN-INV-U9). The
   * facade guard restricts single-tenant admins to their own tenant.
   */
  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<DpaStatusDTO> getDataProcessingAgreementStatus(@NotNull Long id) {
    return new ResponseEntity<>(tenantDpaFacade.getDpaStatus(id), HttpStatus.OK);
  }

  /**
   * In-app signing of the currently published DPA version by an authenticated tenant admin. The
   * signer identity is taken from the access token; the submitted form is persisted append-only.
   */
  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<DpaStatusDTO> signDataProcessingAgreement(
      @NotNull Long id, @Valid DpaAdminSignRequestDTO request) {
    if (!Boolean.TRUE.equals(request.getAccepted())) {
      return ResponseEntity.badRequest().build();
    }
    return new ResponseEntity<>(tenantDpaFacade.signDpa(id, request), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<TenantDTO> getTenantById(@NotNull Long id) {

    var tenantById = tenantServiceFacade.findTenantById(id);
    return tenantById.isEmpty()
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(tenantById.get(), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<List<BasicTenantLicensingDTO>> getAllTenants() {
    var tenants = tenantServiceFacade.getAllTenants();
    return !CollectionUtils.isEmpty(tenants)
        ? new ResponseEntity<>(tenants, HttpStatus.OK)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<TenantAdminControls> getTenantAdminControls() {
    return new ResponseEntity<>(tenantServiceFacade.getTenantAdminControls(), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<TenantAdminControls> updateTenantAdminControls(
      @Valid TenantAdminControls tenantAdminControls) {
    return new ResponseEntity<>(
        tenantServiceFacade.updateTenantAdminControls(tenantAdminControls), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<TranslationApiKeysDTO> getTranslationApiKeys() {
    return new ResponseEntity<>(translationFacade.getMaskedApiKeys(), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<TranslationApiKeysDTO> setTranslationApiKey(
      String provider, TranslationApiKeyUpdateDTO translationApiKeyUpdateDTO) {
    log.info(
        "Updating translation API key for provider {} by user {}",
        provider,
        authorisationService.getUsername());
    return new ResponseEntity<>(
        translationFacade.setApiKey(provider, translationApiKeyUpdateDTO), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<TranslationResponseDTO> translate(
      TranslationRequestDTO translationRequestDTO) {
    return new ResponseEntity<>(translationFacade.translate(translationRequestDTO), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_TRANSLATE_GROUP_CHAT_CONTENT')")
  public ResponseEntity<TranslationResponseDTO> translateGroupChatAuthorContent(
      TranslationRequestDTO translationRequestDTO) {
    validateGroupChatAuthorContent(translationRequestDTO);
    return new ResponseEntity<>(translationFacade.translate(translationRequestDTO), HttpStatus.OK);
  }

  private static void validateGroupChatAuthorContent(TranslationRequestDTO request) {
    if (request == null
        || request.getSourceLang() == null
        || request.getSourceLang().isBlank()
        || CollectionUtils.isEmpty(request.getTargetLangs())
        || CollectionUtils.isEmpty(request.getTexts())
        || request.getTexts().size() > 10
        || request.getTargetLangs().size() > 10
        || request.getTargetLangs().stream()
            .anyMatch(language -> language == null || language.isBlank() || language.length() > 10)
        || request.getTexts().entrySet().stream()
            .anyMatch(
                entry ->
                    entry.getKey() == null
                        || entry.getKey().isBlank()
                        || entry.getKey().length() > 64
                        || entry.getValue() == null
                        || entry.getValue().isBlank()
                        || entry.getValue().length() > 120)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid group-chat author content");
    }
  }

  @ExceptionHandler(TranslationException.class)
  ResponseEntity<TranslationErrorDTO> handleTranslationException(TranslationException e) {
    log.warn("Machine translation failed: {} ({})", e.getErrorCode(), e.getMessage());
    var status =
        e.getErrorCode() == TranslationErrorCode.TRANSLATION_NOT_CONFIGURED
            ? HttpStatus.CONFLICT
            : HttpStatus.BAD_GATEWAY;
    var body =
        new TranslationErrorDTO()
            .errorCode(e.getErrorCode().name())
            .provider(e.getProvider())
            .message(e.getMessage());
    return new ResponseEntity<>(body, status);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<MultilingualTenantDTO> getMultilingualTenantById(@NotNull Long id) {
    var tenantById = tenantServiceFacade.findMultilingualTenantById(id);
    return tenantById.isEmpty()
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(tenantById.get(), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_CREATE_TENANT')")
  public ResponseEntity<TenantIdAvailabilityDTO> getTenantIdAvailability(Long id) {
    var status = tenantIdAllocationService.getStatus(id);
    return ResponseEntity.ok(
        new TenantIdAvailabilityDTO()
            .id(id)
            .status(TenantIdAvailabilityDTO.StatusEnum.valueOf(status.name())));
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_CREATE_TENANT')")
  public ResponseEntity<NextFreeTenantIdDTO> getNextFreeTenantId(Long from, String direction) {
    boolean upwards = "UP".equalsIgnoreCase(direction);
    if (!upwards && !"DOWN".equalsIgnoreCase(direction)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be UP or DOWN");
    }
    return tenantIdAllocationService
        .nextFreeId(from, upwards)
        .map(nextFreeId -> ResponseEntity.ok(new NextFreeTenantIdDTO().id(nextFreeId)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_CREATE_TENANT')")
  public ResponseEntity<TenantIdReservationDTO> reserveTenantId(
      TenantIdReservationRequestDTO tenantIdReservationRequestDTO) {
    var requestedId =
        tenantIdReservationRequestDTO == null ? null : tenantIdReservationRequestDTO.getTenantId();
    log.info(
        "Reserving tenant ID {} by user {}",
        requestedId == null ? "AUTO" : requestedId,
        authorisationService.getUsername());
    var reservation =
        tenantIdAllocationService.reserve(requestedId, authorisationService.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new TenantIdReservationDTO()
                .tenantId(reservation.getTenantId())
                .token(reservation.getToken()));
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_CREATE_TENANT')")
  public ResponseEntity<Void> releaseTenantIdReservation(Long id) {
    log.info(
        "Releasing tenant ID reservation {} by user {}", id, authorisationService.getUsername());
    return tenantIdAllocationService.release(id)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_CREATE_TENANT')")
  public ResponseEntity<MultilingualTenantDTO> createTenant(
      @Valid MultilingualTenantDTO tenantMultilingualDTO) {
    log.info("Creating tenant by user {} ", authorisationService.getUsername());
    var tenant = tenantServiceFacade.createTenant(tenantMultilingualDTO);
    return new ResponseEntity<>(tenant, HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<MultilingualTenantDTO> updateTenant(
      @NotNull Long id, @Valid MultilingualTenantDTO tenantDTO) {
    log.info("Updating tenant with id {} by user {} ", id, authorisationService.getUsername());
    var updatedTenantDTO = tenantServiceFacade.updateTenant(id, tenantDTO);
    return new ResponseEntity<>(updatedTenantDTO, HttpStatus.OK);
  }

  @DeleteMapping("/tenant/{id}")
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<Void> deleteTenant(@PathVariable("id") Long id) {
    log.info("Deleting tenant with id {} by user {} ", id, authorisationService.getUsername());
    tenantServiceFacade.deleteTenant(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<RestrictedTenantDTO> getRestrictedTenantDataBySubdomain(
      @NotNull String subdomain, @Valid Long tenantId) {
    var tenantById = tenantServiceFacade.findTenantBySubdomain(subdomain, tenantId);
    return tenantById.isEmpty()
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(tenantById.get(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<RestrictedTenantDTO> getRestrictedTenantDataByTenantId(
      @NotNull Long tenantId) {
    var tenantById = tenantServiceFacade.findRestrictedTenantById(tenantId);
    return tenantById.isEmpty()
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(tenantById.get(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<RestrictedTenantDTO>> getRestrictedTenantDataByTenantIds(
      List<Long> tenantIds) {
    if (tenantIds.contains(null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant ids must not contain null");
    }
    return ResponseEntity.ok(tenantServiceFacade.findRestrictedTenantsByIds(Set.copyOf(tenantIds)));
  }

  @Override
  public ResponseEntity<RestrictedTenantDTO> getRestrictedSingleTenancyTenantData() {
    var singleTenant = tenantServiceFacade.getSingleTenant();
    return singleTenant.isEmpty()
        ? new ResponseEntity<>(HttpStatus.BAD_REQUEST)
        : new ResponseEntity<>(singleTenant.get(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<RestrictedTenantDTO> getRestrictedTenantData() {
    var tenantData = tenantServiceFacade.getRestrictedTenantDataDeterminingTenantContext();
    return new ResponseEntity<>(tenantData, HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<PlatformDpiaMasterDataDTO> getPlatformDpiaMasterData() {
    return new ResponseEntity<>(platformDpiaMasterDataFacade.getMasterData(), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_ALL_TENANTS')")
  public ResponseEntity<PlatformDpiaMasterDataDTO> updatePlatformDpiaMasterData(
      @Valid PlatformDpiaMasterDataDTO platformDpiaMasterDataDTO) {
    return new ResponseEntity<>(
        platformDpiaMasterDataFacade.updateMasterData(platformDpiaMasterDataDTO), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<PublicDpiaMasterDataDTO> getPublicDpiaMasterData() {
    return new ResponseEntity<>(platformDpiaMasterDataFacade.getPublicMasterData(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Resource> getPublicBrandingAsset(String asset) {
    return publicBrandingAssetService
        .find(asset)
        .map(
            image ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.contentType()))
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                    // Strong content ETag: mail clients and image proxies re-fetch the logo for
                    // every open; Spring's conditional-GET processing turns a matching
                    // If-None-Match into a bodyless 304 once the entity carries an ETag.
                    .eTag(contentEtag(image.bytes()))
                    .body((Resource) new ByteArrayResource(image.bytes())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private static String contentEtag(byte[] bytes) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      return HexFormat.of().formatHex(digest, 0, 16);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  @Override
  public Optional<NativeWebRequest> getRequest() {
    return Optional.empty();
  }

  @Override
  public ResponseEntity<Void> canAccessTenant() {
    boolean canAccessTenant = tenantServiceFacade.canAccessTenant();
    if (canAccessTenant) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } else return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_SEARCH_TENANTS')")
  public ResponseEntity<TenantsSearchResultDTO> searchTenants(
      @NotNull @Valid String query,
      @Min(1) @Valid Integer page,
      @Min(1) @Valid Integer perPage,
      @Pattern(regexp = "^(NAME|ID|SUBDOMAIN|BERATERCOUNT)$") @Valid String field,
      @Pattern(regexp = "^(ASC|DESC)$") @Valid String order) {
    var decodedInfix = URLDecoder.decode(query, StandardCharsets.UTF_8).trim();
    var isAscending = order.equalsIgnoreCase("asc");
    var mappedField = tenantDtoMapper.mappedFieldOf(field);
    var resultMap =
        tenantServiceFacade.findTenantsExceptTechnicalByInfix(
            decodedInfix, page - 1, perPage, mappedField, isAscending);

    var result =
        tenantDtoMapper.tenantsSearchResultOf(resultMap, query, page, perPage, field, order);

    return ResponseEntity.ok(result);
  }

  @Override
  @PreAuthorize(
      "hasAuthority('AUTHORIZATION_GET_ALL_TENANTS') AND hasAuthority('AUTHORIZATION_GET_TENANT_ADMIN_DATA')")
  public ResponseEntity<List<AdminTenantDTO>> getAllTenantsWithAdminData() {
    var tenants = tenantServiceFacade.getAllAdminTenantsExceptTechnical();
    return !CollectionUtils.isEmpty(tenants)
        ? new ResponseEntity<>(tenants, HttpStatus.OK)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<TenantMediaResponseDTO> uploadTenantMedia(
      org.springframework.web.multipart.MultipartFile file) {
    var tenantId =
        authorisationService
            .findTenantIdInAccessToken()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "no tenantId claim in access token"));
    try {
      var stored = tenantMediaService.upload(file.getBytes(), file.getOriginalFilename(), tenantId);
      var response =
          new TenantMediaResponseDTO()
              .id(stored.getId())
              .url("/media/" + stored.getId())
              .contentType(stored.getContentType());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (java.io.IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "could not read upload");
    } catch (MediaSizeLimitExceededException | UnsupportedMediaContentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
