package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.Theming;
import org.junit.jupiter.api.Test;

class EffectiveThemingApplierTest {

  private final EffectiveThemingApplier applier = new EffectiveThemingApplier();

  @Test
  void applyTo_shouldInheritMissingPlatformFieldsAndKeepTenantOverrides() {
    var platform =
        new Theming()
            .logo("platform-logo")
            .associationLogo("platform-association")
            .favicon("platform-favicon")
            .primaryColor("#111111")
            .accent("#eeeeee")
            .secondaryColor("#ffffff")
            .loginEffect(Theming.LoginEffectEnum.LINES)
            .signal("#ff0000");
    var tenant = new Theming().logo("tenant-logo").primaryColor("#222222");

    applier.applyTo(tenant, platform);

    assertThat(tenant.getLogo()).isEqualTo("tenant-logo");
    assertThat(tenant.getPrimaryColor()).isEqualTo("#222222");
    assertThat(tenant.getAssociationLogo()).isEqualTo("platform-association");
    assertThat(tenant.getFavicon()).isEqualTo("platform-favicon");
    assertThat(tenant.getAccent()).isEqualTo("#eeeeee");
    assertThat(tenant.getSecondaryColor()).isEqualTo("#ffffff");
    assertThat(tenant.getLoginEffect()).isEqualTo(Theming.LoginEffectEnum.LINES);
    assertThat(tenant.getSignal()).isEqualTo("#ff0000");
  }

  @Test
  void applyTo_shouldCreateEffectiveThemingWhenTenantHasNone() {
    var platform = new Theming().logo("platform-logo").favicon("platform-favicon");

    Theming effective = applier.effective(null, platform);

    assertThat(effective.getLogo()).isEqualTo("platform-logo");
    assertThat(effective.getFavicon()).isEqualTo("platform-favicon");
    assertThat(effective).isNotSameAs(platform);
  }

  @Test
  void applyTo_shouldTreatBlankTenantAssetsAsMissing() {
    var platform = new Theming().logo("platform-logo").favicon("platform-favicon");
    var tenant = new Theming().logo(" ").favicon("");

    applier.applyTo(tenant, platform);

    assertThat(tenant.getLogo()).isEqualTo("platform-logo");
    assertThat(tenant.getFavicon()).isEqualTo("platform-favicon");
  }
}
