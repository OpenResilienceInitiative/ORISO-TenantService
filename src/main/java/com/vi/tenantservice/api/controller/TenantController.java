package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.facade.TranslationFacade;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaSignatureRequestDTO;
import com.vi.tenantservice.api.model.DpaVersionDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantsSearchResultDTO;
import com.vi.tenantservice.api.model.TranslationApiKeyUpdateDTO;
import com.vi.tenantservice.api.model.TranslationApiKeysDTO;
import com.vi.tenantservice.api.model.TranslationErrorDTO;
import com.vi.tenantservice.api.model.TranslationRequestDTO;
import com.vi.tenantservice.api.model.TranslationResponseDTO;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.TenantDpaService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
  private final @NonNull AuthorisationService authorisationService;
  private final @NonNull TenantDtoMapper tenantDtoMapper;
  private final @NonNull TenantDpaService tenantDpaService;
  private final @NonNull TenantDpaFacade tenantDpaFacade;
  private final @NonNull TranslationFacade translationFacade;

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
    var signature =
        tenantDpaService.confirmSignature(
            token,
            request.getSignerName(),
            request.getSignerPosition(),
            request.getSignerEmail(),
            request.getSignerOrganisation(),
            request.getForwardedByUserId(),
            request.getSource(),
            Boolean.TRUE.equals(request.getSignerIsMember()),
            request.getLanguage());
    var dto =
        new DpaSignatureDTO()
            .tenantId(signature.getTenantId())
            .status(signature.getStatus() == null ? null : signature.getStatus().name())
            .signerName(signature.getSignerName())
            .signerPosition(signature.getSignerPosition())
            .signerEmail(signature.getSignerEmail())
            .signerOrganisation(signature.getSignerOrganisation())
            .forwardedByUserId(signature.getForwardedByUserId())
            .source(signature.getSource())
            .signedAt(signature.getSignedAt() == null ? null : signature.getSignedAt().toString());
    return ResponseEntity.ok(dto);
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
}
