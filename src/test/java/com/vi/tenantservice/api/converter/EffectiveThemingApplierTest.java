package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.ClosingAnimation;
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

  // --- closing animation (ORISO-Frontend#1737): desktop and mobile inherit independently

  private static Theming platformClosingAnimations() {
    return new Theming()
        .closingAnimationDesktop(ClosingAnimation.CIRCLE_ZOOM)
        .closingAnimationMobile(ClosingAnimation.SLIDE_REVEAL);
  }

  @Test
  void applyTo_shouldInheritBothClosingAnimationsWhenTheTenantChoseNone() {
    var tenant = new Theming();

    applier.applyTo(tenant, platformClosingAnimations());

    assertThat(tenant.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.CIRCLE_ZOOM);
    assertThat(tenant.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.SLIDE_REVEAL);
  }

  @Test
  void applyTo_shouldInheritTheDesktopAnimationAndKeepTheTenantsOwnMobileOne() {
    var tenant = new Theming().closingAnimationMobile(ClosingAnimation.CURTAIN);

    applier.applyTo(tenant, platformClosingAnimations());

    assertThat(tenant.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.CIRCLE_ZOOM);
    assertThat(tenant.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.CURTAIN);
  }

  @Test
  void applyTo_shouldInheritTheMobileAnimationAndKeepTheTenantsOwnDesktopOne() {
    var tenant = new Theming().closingAnimationDesktop(ClosingAnimation.CURTAIN);

    applier.applyTo(tenant, platformClosingAnimations());

    assertThat(tenant.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.CURTAIN);
    assertThat(tenant.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.SLIDE_REVEAL);
  }

  @Test
  void applyTo_shouldLetTheTenantsOwnClosingAnimationsWinOverThePlatformsOnBothSides() {
    var tenant =
        new Theming()
            .closingAnimationDesktop(ClosingAnimation.SLIDE_REVEAL)
            .closingAnimationMobile(ClosingAnimation.CIRCLE_ZOOM);

    applier.applyTo(tenant, platformClosingAnimations());

    assertThat(tenant.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.SLIDE_REVEAL);
    assertThat(tenant.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.CIRCLE_ZOOM);
  }

  @Test
  void applyTo_shouldLeaveTheClosingAnimationsUnsetWhenThePlatformHasNoneEither() {
    var tenant = new Theming();

    applier.applyTo(tenant, new Theming().logo("platform-logo"));

    // null is the signal for the frontend to use its own default; nothing is invented here
    assertThat(tenant.getClosingAnimationDesktop()).isNull();
    assertThat(tenant.getClosingAnimationMobile()).isNull();
  }

  @Test
  void effective_shouldCarryThePlatformClosingAnimationsIntoANewTheming() {
    Theming effective = applier.effective(null, platformClosingAnimations());

    assertThat(effective.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.CIRCLE_ZOOM);
    assertThat(effective.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.SLIDE_REVEAL);
  }
}
