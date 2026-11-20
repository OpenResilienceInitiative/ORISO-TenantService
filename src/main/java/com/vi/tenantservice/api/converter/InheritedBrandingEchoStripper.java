package com.vi.tenantservice.api.converter;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.Theming;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Keeps inherited assets unset when an effective public DTO is submitted unchanged. */
@Component
public class InheritedBrandingEchoStripper {

  public void strip(Theming submitted, TenantEntity existing, TenantEntity platform) {
    if (submitted == null || existing == null || platform == null) {
      return;
    }
    stripEcho(
        submitted,
        existing.getThemingAssistantName(),
        platform.getThemingAssistantName(),
        Theming::getAssistantName,
        Theming::setAssistantName);
    stripEcho(
        submitted,
        existing.getThemingAssistantIcon(),
        platform.getThemingAssistantIcon(),
        Theming::getAssistantIcon,
        Theming::setAssistantIcon);
    stripEcho(
        submitted,
        existing.getThemingLogo(),
        platform.getThemingLogo(),
        Theming::getLogo,
        Theming::setLogo);
    stripEcho(
        submitted,
        existing.getThemingFavicon(),
        platform.getThemingFavicon(),
        Theming::getFavicon,
        Theming::setFavicon);
    stripEcho(
        submitted,
        existing.getThemingAssociationLogo(),
        platform.getThemingAssociationLogo(),
        Theming::getAssociationLogo,
        Theming::setAssociationLogo);
    stripEcho(
        submitted,
        existing.getThemingPrimaryColor(),
        platform.getThemingPrimaryColor(),
        Theming::getPrimaryColor,
        Theming::setPrimaryColor);
    stripEcho(
        submitted,
        existing.getThemingSecondaryColor(),
        platform.getThemingSecondaryColor(),
        Theming::getSecondaryColor,
        Theming::setSecondaryColor);
    stripEcho(
        submitted,
        existing.getThemingAccent(),
        platform.getThemingAccent(),
        Theming::getAccent,
        Theming::setAccent);
    stripEcho(
        submitted,
        existing.getThemingSignal(),
        platform.getThemingSignal(),
        Theming::getSignal,
        Theming::setSignal);
  }

  private static void stripEcho(
      Theming submitted,
      String existingValue,
      String platformValue,
      Function<Theming, String> getter,
      BiConsumer<Theming, String> setter) {
    boolean existingIsMissing = existingValue == null || existingValue.isBlank();
    if (existingIsMissing
        && platformValue != null
        && platformValue.equals(getter.apply(submitted))) {
      setter.accept(submitted, null);
    }
  }
}
