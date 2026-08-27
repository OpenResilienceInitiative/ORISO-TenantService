package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.service.BrandingAssetDecoder.DecodedAsset;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves a tenant's effective branding and exposes its stored image bytes. */
@Service
@RequiredArgsConstructor
public class PublicBrandingAssetService {

  private final @NonNull TenantServiceFacade tenantServiceFacade;
  private final @NonNull BrandingAssetDecoder brandingAssetDecoder;

  public Optional<DecodedAsset> find(String asset) {
    return resolveTenant()
        .map(RestrictedTenantDTO::getTheming)
        .map(theming -> select(theming, asset))
        .flatMap(brandingAssetDecoder::decode);
  }

  private Optional<RestrictedTenantDTO> resolveTenant() {
    try {
      return Optional.of(tenantServiceFacade.getRestrictedTenantDataDeterminingTenantContext());
    } catch (NoSuchElementException exception) {
      return tenantServiceFacade.getPlatformTenant();
    }
  }

  private String select(Theming theming, String asset) {
    if (theming == null) {
      return null;
    }
    if ("favicon".equals(asset)) {
      return theming.getFavicon();
    }
    if ("logo".equals(asset)) {
      return theming.getLogo() != null ? theming.getLogo() : theming.getAssociationLogo();
    }
    return null;
  }
}
