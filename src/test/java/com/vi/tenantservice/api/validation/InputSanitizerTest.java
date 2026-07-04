package com.vi.tenantservice.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InputSanitizerTest {

  private final InputSanitizer inputSanitizer = new InputSanitizer();

  // --- Anchor navigation support (Admin anchor feature): id + data-anchor-removed on headings ---

  @Test
  void sanitizeAllowingFormattingAndLinks_should_keepIdOnHeadings() {
    var input = "<h2 id=\"intro\">Intro</h2><h3 id=\"data-usage_2\">Data usage</h3>";

    var result = inputSanitizer.sanitizeAllowingFormattingAndLinks(input);

    assertThat(result).contains("<h2 id=\"intro\">");
    assertThat(result).contains("<h3 id=\"data-usage_2\">");
  }

  @Test
  void sanitizeAllowingFormattingAndLinks_should_keepDataAnchorRemovedTrueOnHeadings() {
    var input = "<h2 data-anchor-removed=\"true\">Old chapter</h2>";

    var result = inputSanitizer.sanitizeAllowingFormattingAndLinks(input);

    assertThat(result).contains("data-anchor-removed=\"true\"");
  }

  @Test
  void sanitizeAllowingFormattingAndLinks_should_dropIdWithUnsafeCharacters() {
    var javascriptLike = "<h2 id=\"javascript:alert(1)\">x</h2>";
    var withQuotes = "<h2 id=\"intro&quot;onmouseover=alert(1)\">x</h2>";
    var withSpaces = "<h2 id=\"intro chapter\">x</h2>";

    assertThat(inputSanitizer.sanitizeAllowingFormattingAndLinks(javascriptLike))
        .doesNotContain("id=");
    assertThat(inputSanitizer.sanitizeAllowingFormattingAndLinks(withQuotes)).doesNotContain("id=");
    assertThat(inputSanitizer.sanitizeAllowingFormattingAndLinks(withSpaces)).doesNotContain("id=");
  }

  @Test
  void sanitizeAllowingFormattingAndLinks_should_dropDataAnchorRemovedWithOtherValues() {
    var input = "<h2 data-anchor-removed=\"false\">x</h2>";

    var result = inputSanitizer.sanitizeAllowingFormattingAndLinks(input);

    assertThat(result).doesNotContain("data-anchor-removed");
  }

  @Test
  void sanitizeAllowingFormattingAndLinks_should_notAllowIdOnNonHeadingElements() {
    var input = "<p id=\"intro\">text</p><a id=\"intro\" href=\"http://oriso.org\">link</a>";

    var result = inputSanitizer.sanitizeAllowingFormattingAndLinks(input);

    assertThat(result).doesNotContain("id=");
  }

  @Test
  void sanitizeAllowingFormattingAndLinks_should_stillStripScriptInjection() {
    var input =
        "<h2 id=\"intro\">Intro</h2><script>alert(1)</script>"
            + "<img src=\"x\" onerror=\"alert(1)\" /><h2 onclick=\"alert(1)\">x</h2>";

    var result = inputSanitizer.sanitizeAllowingFormattingAndLinks(input);

    assertThat(result).doesNotContain("script");
    assertThat(result).doesNotContain("onerror");
    assertThat(result).doesNotContain("onclick");
    assertThat(result).contains("<h2 id=\"intro\">");
  }
}
