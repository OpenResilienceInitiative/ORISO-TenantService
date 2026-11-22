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

  @Test
  void strip_shouldNotMaterializeInheritedColoursOnAnUnchangedTenantSave() {
    var submitted =
        new Theming()
            .primaryColor("#111111")
            .secondaryColor("#222222")
            .accent("#333333")
            .signal("#444444");
    var existing = new TenantEntity();
    var platform = new TenantEntity();
    platform.setThemingPrimaryColor("#111111");
    platform.setThemingSecondaryColor("#222222");
    platform.setThemingAccent("#333333");
    platform.setThemingSignal("#444444");

    stripper.strip(submitted, existing, platform);

    assertThat(submitted.getPrimaryColor()).isNull();
    assertThat(submitted.getSecondaryColor()).isNull();
    assertThat(submitted.getAccent()).isNull();
    assertThat(submitted.getSignal()).isNull();
  }

  @Test
  void strip_shouldKeepAColourTheTenantChoseItself() {
    var submitted = new Theming().primaryColor("#111111");
    var existing = new TenantEntity();
    existing.setThemingPrimaryColor("#111111");
    var platform = new TenantEntity();
    platform.setThemingPrimaryColor("#111111");

    stripper.strip(submitted, existing, platform);

    assertThat(submitted.getPrimaryColor()).isEqualTo("#111111");
  }
}
