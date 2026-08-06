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

  // --- Branding assets (logo, favicon, association logo) ---
  //
  // These fields hold a URL or a base64 data URL, never markup. Running them
  // through the HTML sanitizer HTML-ENCODES the text: every "+" became "&#43;"
  // and the trailing "=" became "&#61;". A base64 payload is full of both, so
  // the stored logo stopped being a decodable data URL — the browser answered
  // ERR_INVALID_URL, the <img> fired onError and the sign-in stage silently
  // dropped the branding. Measured on dev 2026-07-31 for tenant
  // `caritas-berlin`: 3,284 occurrences of "&#43;" in theming.logo alone.

  @Test
  void sanitizeAssetUrl_should_keepBase64DataUrlDecodable() {
    var dataUrl = "data:image/png;base64,iVBORw0KGgo+abc/def+ghi=";

    var result = inputSanitizer.sanitizeAssetUrl(dataUrl);

    assertThat(result).isEqualTo(dataUrl);
    assertThat(result).doesNotContain("&#43;").doesNotContain("&#61;");
  }

  @Test
  void sanitizeAssetUrl_should_keepHttpUrls() {
    assertThat(inputSanitizer.sanitizeAssetUrl("https://assets.example.test/logo.png"))
        .isEqualTo("https://assets.example.test/logo.png");
  }

  @Test
  void sanitizeAssetUrl_should_acceptTheMediaTypesTheAdminUploaderProduces() {
    assertThat(inputSanitizer.sanitizeAssetUrl("data:image/jpeg;base64,AAAA")).isNotNull();
    assertThat(inputSanitizer.sanitizeAssetUrl("data:image/x-icon;base64,AAAA")).isNotNull();
    assertThat(inputSanitizer.sanitizeAssetUrl("data:image/vnd.microsoft.icon;base64,AAAA"))
        .isNotNull();
  }

  @Test
  void sanitizeAssetUrl_should_rejectScriptBearingValues() {
    assertThat(inputSanitizer.sanitizeAssetUrl("javascript:alert(1)")).isNull();
    assertThat(inputSanitizer.sanitizeAssetUrl("data:text/html;base64,PHNjcmlwdD4=")).isNull();
    // SVG can carry script and is not something the admin uploader ever produces.
    assertThat(inputSanitizer.sanitizeAssetUrl("data:image/svg+xml,<svg onload=alert(1)>"))
        .isNull();
    assertThat(inputSanitizer.sanitizeAssetUrl("<img src=x onerror=alert(1)>")).isNull();
  }

  @Test
  void sanitizeAssetUrl_should_rejectNonBase64DataPayloads() {
    assertThat(inputSanitizer.sanitizeAssetUrl("data:image/png,<svg onload=alert(1)>")).isNull();
  }

  @Test
  void sanitizeAssetUrl_should_rejectInvalidBase64Payloads() {
    assertThat(inputSanitizer.sanitizeAssetUrl("data:image/png;base64,A")).isNull();
  }

  @Test
  void sanitizeAssetUrl_should_rejectHttpUrlsWithoutHosts() {
    assertThat(inputSanitizer.sanitizeAssetUrl("https:///logo.png")).isNull();
  }

  @Test
  void sanitizeAssetUrl_should_passThroughEmptiness() {
    assertThat(inputSanitizer.sanitizeAssetUrl(null)).isNull();
    assertThat(inputSanitizer.sanitizeAssetUrl("")).isEqualTo("");
  }
}
