package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.model.DpiaBrandingDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.PublicDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.service.PlatformDpiaMasterDataService;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Composes the public DPIA master data view (ORISO-Admin#735): the platform-level operator master
 * data plus the existing public branding (logo/appearance tokens) of the resolved tenant context.
 *
 * <p>Branding intentionally reuses the restricted public tenant data — the same pruned view served
 * by /tenant/public/** — so no new exposure surface is created. If no tenant context can be
 * resolved (e.g. the DPIA renderer calls the API host directly), the master data is served without
 * branding instead of failing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformDpiaMasterDataFacade {

  private final @NonNull PlatformDpiaMasterDataService platformDpiaMasterDataService;
  private final @NonNull TenantServiceFacade tenantServiceFacade;

  public PlatformDpiaMasterDataDTO getMasterData() {
    return platformDpiaMasterDataService.getMasterData();
  }

  public PlatformDpiaMasterDataDTO updateMasterData(PlatformDpiaMasterDataDTO masterData) {
    return platformDpiaMasterDataService.updateMasterData(masterData);
  }

  public PublicDpiaMasterDataDTO getPublicMasterData() {
    PlatformDpiaMasterDataDTO masterData = platformDpiaMasterDataService.getMasterData();
    PublicDpiaMasterDataDTO publicMasterData =
        new PublicDpiaMasterDataDTO()
            .operator(masterData.getOperator())
            .supervisoryAuthority(masterData.getSupervisoryAuthority())
            .document(masterData.getDocument())
            .keyFigures(masterData.getKeyFigures());
    resolveBrandingTenant()
        .ifPresent(
            tenant ->
                publicMasterData.branding(
                    new DpiaBrandingDTO()
                        .tenantName(tenant.getName())
                        .theming(tenant.getTheming())));
    return publicMasterData;
  }

  private Optional<RestrictedTenantDTO> resolveBrandingTenant() {
    try {
      return Optional.of(tenantServiceFacade.getRestrictedTenantDataDeterminingTenantContext());
    } catch (NoSuchElementException exception) {
      log.debug("No tenant context for DPIA branding, trying single-tenant fallback", exception);
    }
    try {
      return tenantServiceFacade.getSingleTenant();
    } catch (IllegalStateException exception) {
      log.debug("No single tenant either - serving DPIA master data without branding", exception);
      return Optional.empty();
    }
  }
}
