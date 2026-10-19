package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.Theming;
import org.junit.jupiter.api.Test;

class InheritedBrandingEchoStripperTest {

  private final InheritedBrandingEchoStripper stripper = new InheritedBrandingEchoStripper();

  @Test
  void strip_shouldNotMaterializeInheritedAssetsOnAnUnchangedTenantSave() {
    var submitted =
        new Theming()
            .logo("platform-logo")
            .favicon("platform-favicon")
            .associationLogo("platform-association");
    var existing = new TenantEntity();
    var platform = new TenantEntity();
    platform.setThemingLogo("platform-logo");
    platform.setThemingFavicon("platform-favicon");
    platform.setThemingAssociationLogo("platform-association");

    stripper.strip(submitted, existing, platform);

    assertThat(submitted.getLogo()).isNull();
    assertThat(submitted.getFavicon()).isNull();
    assertThat(submitted.getAssociationLogo()).isNull();
  }

  @Test
  void strip_shouldKeepExplicitTenantOverrides() {
    var submitted = new Theming().logo("tenant-logo");
    var existing = new TenantEntity();
    var platform = new TenantEntity();
    platform.setThemingLogo("platform-logo");

    stripper.strip(submitted, existing, platform);

    assertThat(submitted.getLogo()).isEqualTo("tenant-logo");
  }
}
