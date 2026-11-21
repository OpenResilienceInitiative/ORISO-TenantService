package com.vi.tenantservice.api.validation;

import static com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason.ERSTANTWORT_WHO_READS_ALONG_REQUIRED;
import static com.vi.tenantservice.api.util.JsonConverter.convertToJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.model.TenantEntity;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ErstantwortPublicationTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<p><br></p>",
        "<p>&nbsp;&#x2007;&#x202f;&#x200b;&#xfeff;</p>",
        " \u200C\u200D\u2060 "
      })
  void sourceWithoutVisibleTextCannotBePublished(String blankSource) {
    var disclosure =
        Map.of("de", blankSource, "en", "Translation", "en__meta", "{\"mt\":true,\"src\":\"de\"}");

    assertThatThrownBy(
            () ->
                publication(disclosure)
                    .requireDisclosureForChangeFrom(ErstantwortPublication.EMPTY))
        .isInstanceOfSatisfying(
            TenantValidationException.class,
            failure ->
                assertThat(failure.getStatusExceptionReason())
                    .isEqualTo(ERSTANTWORT_WHO_READS_ALONG_REQUIRED));
  }

  @Test
  void unreadableMetadataFallsBackToTheAuthoredGermanVariant() {
    var disclosure =
        Map.of(
            "de@informal",
            "<p>Das Beratungsteam liest mit.</p>",
            "en",
            "Translation",
            "en__meta",
            "{unreadable");

    assertThatCode(
            () ->
                publication(disclosure)
                    .requireDisclosureForChangeFrom(ErstantwortPublication.EMPTY))
        .doesNotThrowAnyException();
  }

  @Test
  void aLegacyEchoIgnoresJsonOrderAndEmptyValues() {
    var before =
        ErstantwortPublication.of(
            TenantEntity.builder()
                .contentErstantwortGreeting("{\"en\":\"Hi\",\"de\":\"Hallo\",\"fr\":\"\"}")
                .erstantwortResponseDeadlineDays(0)
                .build());
    var after =
        ErstantwortPublication.of(
            TenantEntity.builder()
                .contentErstantwortGreeting("{\"de\":\"Hallo\",\"en\":\"Hi\"}")
                .build());

    assertThatCode(() -> after.requireDisclosureForChangeFrom(before)).doesNotThrowAnyException();
  }

  private static ErstantwortPublication publication(Map<String, String> disclosure) {
    return ErstantwortPublication.of(
        TenantEntity.builder()
            .contentErstantwortGreeting("{\"de\":\"Hallo\"}")
            .contentErstantwortWhoReadsAlong(convertToJson(disclosure))
            .build());
  }
}
