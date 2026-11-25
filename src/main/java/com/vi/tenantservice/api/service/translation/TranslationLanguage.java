package com.vi.tenantservice.api.service.translation;

import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Turns ORISO language codes into an unambiguous description for the translation prompt. A model
 * does not reliably know what {@code de@informal} means, so the German variants name the form of
 * address explicitly: ORISO's default German addresses the reader formally ("Sie"), the informal
 * variant uses "du".
 */
public final class TranslationLanguage {

  static final String INFORMAL_GERMAN = "de@informal";

  private static final Set<String> KNOWN_LANGUAGES = Set.of(Locale.getISOLanguages());

  private TranslationLanguage() {}

  public static String describe(String code) {
    if (StringUtils.isBlank(code)) {
      return "the source language";
    }
    var trimmed = code.trim();
    if (INFORMAL_GERMAN.equalsIgnoreCase(trimmed)) {
      return "German, informal form of address (use \"du\", never \"Sie\")";
    }
    if ("de".equalsIgnoreCase(trimmed)) {
      return "German, formal form of address (use \"Sie\")";
    }
    var locale = Locale.forLanguageTag(StringUtils.substringBefore(trimmed, "@"));
    if (!KNOWN_LANGUAGES.contains(locale.getLanguage())) {
      return trimmed;
    }
    var displayName = locale.getDisplayName(Locale.ENGLISH);
    return displayName + " (" + trimmed + ")";
  }
}
