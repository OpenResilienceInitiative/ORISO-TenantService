package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaSignatureRequestDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantsSearchResultDTO;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.generated.api.controller.TenantApi;
import com.vi.tenantservice.generated.api.controller.TenantadminApi;
import io.swagger.annotations.Api;
import jakarta.validation.Valid;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

/** Controller for tenant API operations. */
@RestController
@RequiredArgsConstructor
@Api(tags = "tenant-controller")
@Slf4j
public class TenantController implements TenantApi, TenantadminApi {

  private final @NonNull TenantServiceFacade tenantServiceFacade;
  private final @NonNull AuthorisationService authorisationService;
  private final @NonNull TenantDtoMapper tenantDtoMapper;
  private final @NonNull TenantDpaService tenantDpaService;
  private final @NonNull TenantDpaFacade tenantDpaFacade;

  /**
   * Public DPA confirmation via a single-use sign token. No authentication: the token is the
   * authorisation (an external signer who may not hold a platform account confirms via the link).
   */
  @Override
  public ResponseEntity<DpaSignatureDTO> confirmDataProcessingAgreement(
      String token, DpaSignatureRequestDTO request) {
    var signature =
        tenantDpaService.confirmSignature(
            token,
            request.getSignerName(),
            request.getSignerPosition(),
            Boolean.TRUE.equals(request.getSignerIsMember()),
            request.getLanguage());
    var dto =
        new DpaSignatureDTO()
            .tenantId(signature.getTenantId())
            .status(signature.getStatus() == null ? null : signature.getStatus().name())
            .signerName(signature.getSignerName())
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
  public ResponseEntity<List<DpaSignatureDTO>> getDataProcessingAgreementSignatures(Long id) {
    return new ResponseEntity<>(tenantDpaFacade.getSignatures(id), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<DpaGateStatusDTO> getDataProcessingAgreementGate(Long id) {
    return new ResponseEntity<>(tenantDpaFacade.getGateStatus(id), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<DpaSignInviteDTO> createDataProcessingAgreementSignInvite(Long id) {
    return new ResponseEntity<>(tenantDpaFacade.createSignInvite(id), HttpStatus.OK);
  }

  @ExceptionHandler(DpaNotPublishedException.class)
  ResponseEntity<Void> handleDpaNotPublished(DpaNotPublishedException e) {
    log.info("DPA sign invite rejected: {}", e.getMessage());
    return new ResponseEntity<>(HttpStatus.CONFLICT);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<TenantDTO> getTenantById(Long id) {

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
  @PreAuthorize("hasAuthority('AUTHORIZATION_GET_TENANT')")
  public ResponseEntity<MultilingualTenantDTO> getMultilingualTenantById(Long id) {
    var tenantById = tenantServiceFacade.findMultilingualTenantById(id);
    return tenantById.isEmpty()
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(tenantById.get(), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_CREATE_TENANT')")
  public ResponseEntity<MultilingualTenantDTO> createTenant(
      @Valid MultilingualTenantDTO tenantMultilingualDTO) {
    log.info("Creating tenant with by user {} ", authorisationService.getUsername());
    var tenant = tenantServiceFacade.createTenant(tenantMultilingualDTO);
    return new ResponseEntity<>(tenant, HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasAuthority('AUTHORIZATION_UPDATE_TENANT')")
  public ResponseEntity<MultilingualTenantDTO> updateTenant(
      Long id, @Valid MultilingualTenantDTO tenantDTO) {
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
      String subdomain, Long tenantId) {
    var tenantById = tenantServiceFacade.findTenantBySubdomain(subdomain, tenantId);
    return tenantById.isEmpty()
        ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(tenantById.get(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<RestrictedTenantDTO> getRestrictedTenantDataByTenantId(Long tenantId) {
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
      String query, Integer page, Integer perPage, String field, String order) {
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
