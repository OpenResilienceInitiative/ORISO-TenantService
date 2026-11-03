package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.util.JsonConverter.convertFromJson;
import static com.vi.tenantservice.api.util.JsonConverter.convertMapFromJson;

import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.Theming;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Everything the platform admin's switch "organisation admins may edit" locks (TenantService#345):
 * name, slogan, languages, form of address and the branding. The Admin greys these cards out; this
 * is the same list on the server, in one place, so a new locked field is added here and nowhere
 * else.
 *
 * <p>Only a real difference counts. The admin panel sends whole tenant payloads, so a restricted
 * admin's save of an unrelated card carries all of these fields too, and reading the tenant back
 * fills in defaults the row never stored (an unset login effect reads as {@code NONE}, no languages
 * read as German). Each value is compared in the form the API serves it.
 */
final class AppearanceLockedFields {

  private static final String GERMAN = "de";

  private AppearanceLockedFields() {}

  /**
   * @param storedFormOfAddress the form of address lives in the consulting type service, so it is
   *     only fetched when the request names one
   * @return the locked fields whose submitted value differs from what is stored. Side effect: an
   *     assistant identity or login effect the request leaves out is filled in from the stored
   *     value, so a save that passes this check does not clear them.
   */
  static List<String> changed(
      MultilingualTenantDTO submitted,
      TenantEntity existing,
      Supplier<Boolean> storedFormOfAddress) {
    List<String> changed = new ArrayList<>();
    if (submitted.getName() != null && !submitted.getName().equals(existing.getName())) {
      changed.add("name");
    }
    if (sloganChanged(submitted, existing)) {
      changed.add("slogan");
    }
    if (languagesChanged(submitted, existing)) {
      changed.add("activeLanguages");
    }
    if (formOfAddressChanged(submitted, storedFormOfAddress)) {
      changed.add("languageFormal");
    }
    changed.addAll(brandingChanged(submitted.getTheming(), existing));
    return changed;
  }

  /**
   * The claim is stored as sent, blank entries included, and a language with a blank entry shows
   * blank instead of falling back to German. So the maps are compared as they are, not trimmed of
   * blanks. A claim with fewer languages than stored (the Admin sends only {@code de}) or none at
   * all would drop the others silently, so it counts as a change too.
   */
  private static boolean sloganChanged(MultilingualTenantDTO submitted, TenantEntity existing) {
    if (submitted.getContent() == null) {
      return false;
    }
    Map<String, String> sent = submitted.getContent().getClaim();
    return !(sent == null ? Map.of() : sent).equals(convertMapFromJson(existing.getContentClaim()));
  }

  /** Absent settings keep what is stored; German is always active, so none reads as {@code de}. */
  private static boolean languagesChanged(MultilingualTenantDTO submitted, TenantEntity existing) {
    if (submitted.getSettings() == null) {
      return false;
    }
    TenantSettings stored =
        existing.getSettings() == null
            ? new TenantSettings()
            : convertFromJson(existing.getSettings());
    return !activeLanguages(submitted.getSettings().getActiveLanguages())
        .equals(activeLanguages(stored.getActiveLanguages()));
  }

  private static Set<String> activeLanguages(List<String> languages) {
    return languages == null || languages.isEmpty() ? Set.of(GERMAN) : new HashSet<>(languages);
  }

  private static boolean formOfAddressChanged(
      MultilingualTenantDTO submitted, Supplier<Boolean> storedFormOfAddress) {
    if (submitted.getSettings() == null || submitted.getSettings().getExtendedSettings() == null) {
      return false;
    }
    Boolean requested = submitted.getSettings().getExtendedSettings().getLanguageFormal();
    return requested != null && !requested.equals(storedFormOfAddress.get());
  }

  /**
   * Assistant identity (name, icon), images, colours and login effect. A missing block clears the
   * images and colours, so it counts as a change. The signed-in Admin never sends the assistant
   * identity or the login effect at all, though; leaving those out would clear a stored value, so
   * the stored value is kept for the save instead of refusing every save of that Träger. Blank is
   * different from missing: it is an explicit clear and counts as a change.
   */
  private static List<String> brandingChanged(Theming submitted, TenantEntity existing) {
    Theming theming = submitted == null ? new Theming() : submitted;
    keepUnsent(theming, existing);
    List<String> changed = new ArrayList<>();
    differs(
        changed,
        "theming.assistantName",
        theming.getAssistantName(),
        existing.getThemingAssistantName());
    differs(
        changed,
        "theming.assistantIcon",
        theming.getAssistantIcon(),
        existing.getThemingAssistantIcon());
    differs(changed, "theming.logo", theming.getLogo(), existing.getThemingLogo());
    differs(changed, "theming.favicon", theming.getFavicon(), existing.getThemingFavicon());
    differs(
        changed,
        "theming.associationLogo",
        theming.getAssociationLogo(),
        existing.getThemingAssociationLogo());
    differs(
        changed,
        "theming.primaryColor",
        theming.getPrimaryColor(),
        existing.getThemingPrimaryColor());
    differs(
        changed,
        "theming.secondaryColor",
        theming.getSecondaryColor(),
        existing.getThemingSecondaryColor());
    differs(changed, "theming.accent", theming.getAccent(), existing.getThemingAccent());
    differs(changed, "theming.signal", theming.getSignal(), existing.getThemingSignal());
    differs(
        changed,
        "theming.loginEffect",
        theming.getLoginEffect().getValue(),
        effectiveLoginEffect(existing.getThemingLoginEffect()).getValue());
    return changed;
  }

  private static void keepUnsent(Theming theming, TenantEntity existing) {
    if (theming.getAssistantName() == null) {
      theming.setAssistantName(existing.getThemingAssistantName());
    }
    if (theming.getAssistantIcon() == null) {
      theming.setAssistantIcon(existing.getThemingAssistantIcon());
    }
    if (theming.getLoginEffect() == null) {
      theming.setLoginEffect(effectiveLoginEffect(existing.getThemingLoginEffect()));
    }
  }

  /** As {@code TenantConverter#toLoginEffect} reads it: never configured or unknown is NONE. */
  private static Theming.LoginEffectEnum effectiveLoginEffect(String stored) {
    if (stored == null) {
      return Theming.LoginEffectEnum.NONE;
    }
    try {
      return Theming.LoginEffectEnum.fromValue(stored);
    } catch (IllegalArgumentException e) {
      return Theming.LoginEffectEnum.NONE;
    }
  }

  private static void differs(List<String> changed, String field, String submitted, String stored) {
    if (!Objects.equals(blankToNull(submitted), blankToNull(stored))) {
      changed.add(field);
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
