package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Theming;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicBrandingAssetServiceTest {
  @Mock private TenantServiceFacade tenants;

  @Test
  void explicitTenantImagesDoNotDependOnMailClientCookies() {
    var service = new PublicBrandingAssetService(tenants, new BrandingAssetDecoder());
    when(tenants.findRestrictedTenantById(7L)).thenReturn(Optional.of(tenant("first-logo")));
    when(tenants.findRestrictedTenantById(8L)).thenReturn(Optional.of(tenant("second-logo")));

    assertThat(service.find(7L, "logo").orElseThrow().bytes())
        .isEqualTo("first-logo".getBytes(StandardCharsets.UTF_8));
    assertThat(service.find(8L, "logo").orElseThrow().bytes())
        .isEqualTo("second-logo".getBytes(StandardCharsets.UTF_8));
    verifyNoMoreInteractions(tenants);
  }

  @Test
  void unknownTenantDoesNotSilentlyDisplayAnotherTenantsLogo() {
    var service = new PublicBrandingAssetService(tenants, new BrandingAssetDecoder());
    when(tenants.findRestrictedTenantById(99L)).thenReturn(Optional.empty());
    assertThat(service.find(99L, "logo")).isEmpty();
    verifyNoMoreInteractions(tenants);
  }

  @Test
  void unknownAssetAndNegativeTenantAreRejectedBeforeLookup() {
    var service = new PublicBrandingAssetService(tenants, new BrandingAssetDecoder());
    assertThat(service.find(7L, "smtpPassword")).isEmpty();
    assertThat(service.find(-1L, "logo")).isEmpty();
    verifyNoMoreInteractions(tenants);
  }

  private RestrictedTenantDTO tenant(String logo) {
    var theming = new Theming();
    theming.setLogo(
        "data:image/png;base64,"
            + Base64.getEncoder().encodeToString(logo.getBytes(StandardCharsets.UTF_8)));
    return new RestrictedTenantDTO().theming(theming);
  }
}
