package com.vi.tenantservice.api.service.legal;

import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import com.vi.tenantservice.api.repository.PlatformDpiaMasterDataRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fills the platform-owned {@code {{key}}} tokens in public legal content (ORISO-Admin#1067).
 *
 * <p>Only {@code {{Plattform_Datenschutzbeauftragte}}}: the platform DPO from Globale Einstellungen
 * → Dokument-Stammdaten. It is a token of its own on purpose, so the platform DPO can never become
 * the value of a Träger's or Beratungsstelle's {@code {{Datenschutzbeauftragte}}} — that one is
 * AgencyService's to resolve (Beratungsstelle → Träger) and is left standing here.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlatformLegalTextTokens {

  public static final String PLATFORM_DPO = "Plattform_Datenschutzbeauftragte";

  static final String TOKEN = "{{" + PLATFORM_DPO + "}}";

  /** The OWASP sanitiser stores every {@code {{} as {@code {<!-- -->{}. */
  static final String SANITIZED_TOKEN = "{<!-- -->{" + PLATFORM_DPO + "}}";

  private static final String META_KEY_SUFFIX = "__meta";

  private final @NonNull PlatformDpiaMasterDataRepository platformDpiaMasterDataRepository;

  /** Substitutes in place; a missing DPO renders empty (not required anywhere). */
  public Content fill(Content content) {
    if (content == null || !containsToken(content)) {
      return content;
    }
    var value = platformDpoName();
    content.setPrivacy(substitute(content.getPrivacy(), value));
    content.setRenderedPrivacy(substitute(content.getRenderedPrivacy(), value));
    content.setImpressum(substitute(content.getImpressum(), value));
    content.setPrivacyLanguages(substitute(content.getPrivacyLanguages(), value));
    content.setImpressumLanguages(substitute(content.getImpressumLanguages(), value));
    return content;
  }

  static String substitute(String text, String value) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    return text.replace(SANITIZED_TOKEN, value).replace(TOKEN, value);
  }

  private static Map<String, String> substitute(Map<String, String> byLanguage, String value) {
    if (byLanguage == null) {
      return null;
    }
    var result = new LinkedHashMap<String, String>();
    // __meta values are translation metadata JSON, not prose.
    byLanguage.forEach(
        (language, text) ->
            result.put(
                language,
                language != null && language.endsWith(META_KEY_SUFFIX)
                    ? text
                    : substitute(text, value)));
    return result;
  }

  private static boolean containsToken(Content content) {
    return hasToken(content.getPrivacy())
        || hasToken(content.getRenderedPrivacy())
        || hasToken(content.getImpressum())
        || (content.getPrivacyLanguages() != null
            && content.getPrivacyLanguages().values().stream()
                .anyMatch(PlatformLegalTextTokens::hasToken))
        || (content.getImpressumLanguages() != null
            && content.getImpressumLanguages().values().stream()
                .anyMatch(PlatformLegalTextTokens::hasToken));
  }

  private static boolean hasToken(String text) {
    return text != null && (text.contains(TOKEN) || text.contains(SANITIZED_TOKEN));
  }

  /**
   * Stored through {@code InputSanitizer#sanitize}, i.e. already HTML-encoded — inserted as is, as
   * escaping again would show "&amp;#43;" to readers.
   */
  private String platformDpoName() {
    try {
      return platformDpiaMasterDataRepository
          .findById(PlatformDpiaMasterDataEntity.SINGLETON_ID)
          .map(PlatformDpiaMasterDataEntity::getOperatorDpoName)
          .filter(name -> !name.isBlank())
          .orElse("");
    } catch (Exception e) {
      log.warn("Could not read the platform DPO; rendering its placeholder empty", e);
      return "";
    }
  }
}
