package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.util.JsonConverter.convertFromJson;
import static com.vi.tenantservice.api.util.JsonConverter.convertMapFromJson;

import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.Theming;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
  private static final String NO_LOGIN_EFFECT = "NONE";

  private AppearanceLockedFields() {}

  /**
   * @param storedFormOfAddress the form of address lives in the consulting type service, so it is
   *     only fetched when the request names one
   * @return the locked fields whose submitted value differs from what is stored
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

  /** Content is always present after sanitising; an omitted slogan would clear the stored one. */
  private static boolean sloganChanged(MultilingualTenantDTO submitted, TenantEntity existing) {
    if (submitted.getContent() == null) {
      return false;
    }
    return !withoutBlanks(submitted.getContent().getClaim())
        .equals(withoutBlanks(convertMapFromJson(existing.getContentClaim())));
  }

  private static Map<String, String> withoutBlanks(Map<String, String> translations) {
    Map<String, String> kept = new LinkedHashMap<>();
    if (translations != null) {
      translations.forEach(
          (language, text) -> {
            if (text != null && !text.isBlank()) {
              kept.put(language, text);
            }
          });
    }
    return kept;
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

  /** Assistant identity (name, icon), images and colours; a missing block clears them all. */
  private static List<String> brandingChanged(Theming submitted, TenantEntity existing) {
    Theming theming = submitted == null ? new Theming() : submitted;
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
        loginEffect(theming.getLoginEffect() == null ? null : theming.getLoginEffect().getValue()),
        loginEffect(existing.getThemingLoginEffect()));
    return changed;
  }

  /** Stored as the enum name; never configured reads back as NONE, so both are the plain stage. */
  private static String loginEffect(String value) {
    return value == null ? NO_LOGIN_EFFECT : value;
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
