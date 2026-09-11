package com.vi.tenantservice.api.converter;

import com.vi.tenantservice.api.model.Theming;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Builds the effective tenant branding by filling unset values from platform tenant 0. */
@Component
public class EffectiveThemingApplier {

  public Theming effective(Theming tenant, Theming platform) {
    Theming effective = tenant == null ? new Theming() : tenant;
    applyTo(effective, platform);
    return effective;
  }

  public void applyTo(Theming tenant, Theming platform) {
    if (tenant == null || platform == null) {
      return;
    }
    inheritString(tenant, platform, Theming::getLogo, Theming::setLogo);
    inheritString(tenant, platform, Theming::getAssociationLogo, Theming::setAssociationLogo);
    inheritString(tenant, platform, Theming::getFavicon, Theming::setFavicon);
    inheritString(tenant, platform, Theming::getPrimaryColor, Theming::setPrimaryColor);
    inheritString(tenant, platform, Theming::getAccent, Theming::setAccent);
    inheritString(tenant, platform, Theming::getSecondaryColor, Theming::setSecondaryColor);
    inherit(tenant, platform, Theming::getLoginEffect, Theming::setLoginEffect);
    inheritString(tenant, platform, Theming::getSignal, Theming::setSignal);
  }

  private static void inheritString(
      Theming tenant,
      Theming platform,
      Function<Theming, String> getter,
      BiConsumer<Theming, String> setter) {
    String value = getter.apply(tenant);
    if (value == null || value.isBlank()) {
      setter.accept(tenant, getter.apply(platform));
    }
  }

  private static <T> void inherit(
      Theming tenant,
      Theming platform,
      Function<Theming, T> getter,
      BiConsumer<Theming, T> setter) {
    if (getter.apply(tenant) == null) {
      setter.accept(tenant, getter.apply(platform));
    }
  }
}
