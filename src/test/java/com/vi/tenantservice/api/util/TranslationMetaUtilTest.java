package com.vi.tenantservice.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TranslationMetaUtilTest {

  @Test
  void metaKeyFor_Should_appendMetaSuffix() {
    assertThat(TranslationMetaUtil.metaKeyFor("en")).isEqualTo("en__meta");
  }

  @Test
  void isMetaKey_Should_detectMetaKeys() {
    assertThat(TranslationMetaUtil.isMetaKey("en__meta")).isTrue();
    assertThat(TranslationMetaUtil.isMetaKey("en")).isFalse();
    assertThat(TranslationMetaUtil.isMetaKey(null)).isFalse();
  }

  @Test
  void languageOf_Should_stripMetaSuffix() {
    assertThat(TranslationMetaUtil.languageOf("en__meta")).isEqualTo("en");
  }

  @Test
  void buildMeta_Should_produceValidStrictJson_withSourceAndTimestamp() {
    var meta = TranslationMetaUtil.buildMeta("de", LocalDateTime.of(2026, 7, 3, 10, 15, 30));

    assertThat(TranslationMetaUtil.isValidMeta(meta)).isTrue();
    assertThat(TranslationMetaUtil.isMachineTranslated(meta)).isTrue();
    assertThat(meta)
        .contains("\"mt\":true")
        .contains("\"src\":\"de\"")
        .contains("2026-07-03T10:15:30");
  }

  @Test
  void isValidMeta_Should_acceptStrictJsonWithKnownFieldsOnly() {
    assertThat(
            TranslationMetaUtil.isValidMeta(
                "{\"mt\":true,\"src\":\"de\",\"at\":\"2026-07-03T10:15:30Z\"}"))
        .isTrue();
    assertThat(TranslationMetaUtil.isValidMeta("{\"mt\":false}")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not json at all",
        "[]",
        "\"string\"",
        "{}",
        "{\"src\":\"de\"}",
        "{\"mt\":\"yes\"}",
        "{\"mt\":true,\"extra\":\"field\"}",
        "{\"mt\":true,\"src\":1}",
        "{\"mt\":true,\"at\":42}",
        "{\"mt\":true,\"src\":\"de\"} trailing",
        ""
      })
  void isValidMeta_Should_rejectMalformedOrUnknownOrWrongTypedFields(String metaJson) {
    assertThat(TranslationMetaUtil.isValidMeta(metaJson)).isFalse();
  }

  @Test
  void isMachineTranslated_Should_beFalse_When_mtFalseOrInvalid() {
    assertThat(TranslationMetaUtil.isMachineTranslated("{\"mt\":false}")).isFalse();
    assertThat(TranslationMetaUtil.isMachineTranslated("garbage")).isFalse();
    assertThat(TranslationMetaUtil.isMachineTranslated(null)).isFalse();
  }

  @Test
  void stripMetaKeys_Should_removeOnlyMetaKeys_andPreserveOrder() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("de", "<p>de</p>");
    map.put("en", "<p>en</p>");
    map.put("en__meta", "{\"mt\":true}");

    var stripped = TranslationMetaUtil.stripMetaKeys(map);

    assertThat(stripped).containsOnlyKeys("de", "en");
    assertThat(stripped.keySet()).containsExactly("de", "en");
  }

  @Test
  void stripMetaKeys_Should_returnEmptyMap_When_inputNull() {
    assertThat(TranslationMetaUtil.stripMetaKeys(null)).isEmpty();
  }
}
