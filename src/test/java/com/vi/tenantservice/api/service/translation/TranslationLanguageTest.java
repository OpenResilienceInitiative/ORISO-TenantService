package com.vi.tenantservice.api.service.translation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TranslationLanguageTest {

  @Test
  void informalGermanNamesTheDuFormInsteadOfTheRawCode() {
    var description = TranslationLanguage.describe("de@informal");
    assertThat(description).startsWith("German, informal").contains("\"du\"");
    assertThat(description).doesNotContain("@");
  }

  @Test
  void defaultGermanNamesTheFormalSieForm() {
    assertThat(TranslationLanguage.describe("de"))
        .isEqualTo("German, formal form of address (use \"Sie\")");
  }

  @Test
  void otherCodesUseTheEnglishLanguageName() {
    assertThat(TranslationLanguage.describe("fr")).isEqualTo("French (fr)");
    assertThat(TranslationLanguage.describe("uk")).isEqualTo("Ukrainian (uk)");
  }

  @Test
  void unknownCodesStayAsTheyAre() {
    assertThat(TranslationLanguage.describe("xx-unknown")).isEqualTo("xx-unknown");
  }
}
