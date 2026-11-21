package com.vi.tenantservice.api.validation;

import static com.vi.tenantservice.api.util.JsonConverter.convertMapFromJson;

import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.util.TranslationMetaUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Validates the first-reply content that will be stored after the usual merge rules. */
public record ErstantwortPublication(
    String greeting,
    String whoReadsAlong,
    String emergencyAddition,
    String freeNotice,
    String closing,
    Integer deadline) {

  public static final ErstantwortPublication EMPTY =
      new ErstantwortPublication(null, null, null, null, null, null);

  private static final Pattern NO_BREAK_SPACES = Pattern.compile("[\\u00A0\\u2007\\u202F]");
  private static final Pattern ZERO_WIDTH =
      Pattern.compile("[\\u200B\\u200C\\u200D\\u2060\\uFEFF]");

  public static ErstantwortPublication of(TenantEntity tenant) {
    Integer deadline = tenant.getErstantwortResponseDeadlineDays();
    return new ErstantwortPublication(
        tenant.getContentErstantwortGreeting(),
        tenant.getContentErstantwortWhoReadsAlong(),
        tenant.getContentErstantwortEmergencyAddition(),
        tenant.getContentErstantwortFreeNotice(),
        tenant.getContentErstantwortClosing(),
        deadline != null && deadline > 0 ? deadline : null);
  }

  public void requireDisclosureForChangeFrom(ErstantwortPublication before) {
    if (!sameContent(before) && !hasAuthoredDisclosure()) {
      throw new TenantValidationException(
          HttpStatusExceptionReason.ERSTANTWORT_WHO_READS_ALONG_REQUIRED);
    }
  }

  private boolean sameContent(ErstantwortPublication before) {
    return Objects.equals(deadline, before.deadline)
        && sameTranslations(greeting, before.greeting)
        && sameTranslations(whoReadsAlong, before.whoReadsAlong)
        && sameTranslations(emergencyAddition, before.emergencyAddition)
        && sameTranslations(freeNotice, before.freeNotice)
        && sameTranslations(closing, before.closing);
  }

  private static boolean sameTranslations(String first, String second) {
    return Objects.equals(first, second)
        || retainedTranslations(read(first)).equals(retainedTranslations(read(second)));
  }

  private boolean hasAuthoredDisclosure() {
    var translations = read(whoReadsAlong);
    return TranslationMetaUtil.declaredSourceLanguage(translations)
        .map(source -> hasText(translations.get(source)))
        .orElseGet(
            () -> hasText(translations.get("de")) || hasText(translations.get("de@informal")));
  }

  private static boolean hasText(String html) {
    String visible = InputSanitizer.visibleText(html);
    return !ZERO_WIDTH
        .matcher(NO_BREAK_SPACES.matcher(visible).replaceAll(" "))
        .replaceAll("")
        .isBlank();
  }

  private static Map<String, String> read(String json) {
    if (json == null || json.isBlank() || "null".equals(json) || "{}".equals(json)) {
      return Map.of();
    }
    try {
      return convertMapFromJson(json);
    } catch (RuntimeJsonMappingException malformedStoredContent) {
      // Unreadable legacy content cannot supply a disclosure.
      return Map.of();
    }
  }

  /** Blank translations clear the language and its translation metadata. */
  public static Map<String, String> retainedTranslations(Map<String, String> incoming) {
    var retained = new LinkedHashMap<String, String>();
    incoming.forEach(
        (key, value) -> {
          if (key != null
              && !TranslationMetaUtil.isMetaKey(key)
              && value != null
              && !value.isBlank()) {
            retained.put(key, value);
          }
        });
    incoming.forEach(
        (key, value) -> {
          if (TranslationMetaUtil.isMetaKey(key)
              && value != null
              && !value.isBlank()
              && retained.containsKey(TranslationMetaUtil.languageOf(key))) {
            retained.put(key, value);
          }
        });
    return retained;
  }
}
