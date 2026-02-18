package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantsSearchResultDTO;
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
