package com.vi.tenantservice.api.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Machine-translation metadata convention for multilingual JSON content maps (language -> HTML,
 * e.g. the tenant DPA content): a machine translated language carries a parallel meta key inside
 * the SAME map, e.g. {@code "en__meta": "{\"mt\":true,\"src\":\"de\",\"at\":\"<ISO-timestamp>\"}"}.
 * No schema change is needed - the convention fits the existing LONGTEXT JSON maps. Meta values are
 * strict JSON objects with only the known fields {@code mt} (boolean), {@code src} (string) and
 * {@code at} (string). See documentation/translation-meta.md.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TranslationMetaUtil {

  public static final String META_KEY_SUFFIX = "__meta";

  private static final String FIELD_MT = "mt";
  private static final String FIELD_SRC = "src";
  private static final String FIELD_AT = "at";
  private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_MT, FIELD_SRC, FIELD_AT);

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  /** The meta key for a language, e.g. {@code en -> en__meta}. */
  public static String metaKeyFor(String language) {
    return language + META_KEY_SUFFIX;
  }

  /** Whether the given map key is a meta key ({@code en__meta}) rather than a language key. */
  public static boolean isMetaKey(String key) {
    return key != null && key.endsWith(META_KEY_SUFFIX);
  }

  /** The language a meta key belongs to, e.g. {@code en__meta -> en}. */
  public static String languageOf(String metaKey) {
    return StringUtils.removeEnd(metaKey, META_KEY_SUFFIX);
  }

  /**
   * Builds the strict meta JSON, e.g. {@code {"mt":true,"src":"de","at":"2026-07-03T10:15:30Z"}}.
   */
  public static String buildMeta(String sourceLang, LocalDateTime at) {
    var node = OBJECT_MAPPER.createObjectNode();
    node.put(FIELD_MT, true);
    node.put(FIELD_SRC, sourceLang);
    node.put(FIELD_AT, at.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    return node.toString();
  }

  /**
   * Validates a meta value as a strict JSON object containing only the known fields: {@code mt}
   * (boolean, required), {@code src} (string, optional), {@code at} (string, optional). Unknown
   * fields, wrong types, non-objects and malformed JSON are rejected.
   */
  public static boolean isValidMeta(String metaJson) {
    if (StringUtils.isBlank(metaJson)) {
      return false;
    }
    JsonNode node;
    try {
      node = OBJECT_MAPPER.readTree(metaJson);
    } catch (Exception e) {
      return false;
    }
    if (node == null
        || !node.isObject()
        || !node.has(FIELD_MT)
        || !node.get(FIELD_MT).isBoolean()) {
      return false;
    }
    var fieldNames = node.fieldNames();
    while (fieldNames.hasNext()) {
      var field = fieldNames.next();
      if (!KNOWN_FIELDS.contains(field)) {
        return false;
      }
    }
    return (!node.has(FIELD_SRC) || node.get(FIELD_SRC).isTextual())
        && (!node.has(FIELD_AT) || node.get(FIELD_AT).isTextual());
  }

  /** Whether the given meta value marks the language as machine translated ({@code mt:true}). */
  public static boolean isMachineTranslated(String metaJson) {
    if (!isValidMeta(metaJson)) {
      return false;
    }
    try {
      return OBJECT_MAPPER.readTree(metaJson).get(FIELD_MT).asBoolean();
    } catch (Exception e) {
      return false;
    }
  }

  /** Returns a copy of the map without any meta keys (content-only view). */
  public static Map<String, String> stripMetaKeys(Map<String, String> contentByLanguage) {
    var result = new LinkedHashMap<String, String>();
    if (contentByLanguage != null) {
      contentByLanguage.forEach(
          (key, value) -> {
            if (!isMetaKey(key)) {
              result.put(key, value);
            }
          });
    }
    return result;
  }
}
