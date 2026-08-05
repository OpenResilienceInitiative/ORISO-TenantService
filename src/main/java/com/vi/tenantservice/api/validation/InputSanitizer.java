package com.vi.tenantservice.api.validation;

import java.net.URI;
import java.util.Base64;
import java.util.regex.Pattern;
import org.owasp.html.HtmlPolicyBuilder;
import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

  private static final String[] HEADING_ELEMENTS = {"h1", "h2", "h3", "h4", "h5", "h6"};

  /**
   * Anchor ids on headings power the legal-content anchor navigation. Restricted to a slug charset
   * (no colons, quotes, spaces, ...) so the attribute cannot be abused as an XSS vector.
   */
  private static final Pattern SAFE_ANCHOR_ID = Pattern.compile("[a-zA-Z0-9\\-_]+");

  /** Marker the admin editor sets on headings whose anchor was explicitly removed. */
  private static final Pattern ANCHOR_REMOVED_TRUE = Pattern.compile("true");

  /**
   * Media types the admin's branding uploader can actually produce (see FormFileUploaderField in
   * ORISO-Admin). SVG is deliberately absent: it can carry script and no upload path creates it.
   */
  private static final Pattern ALLOWED_IMAGE_DATA_URL =
      Pattern.compile(
          "^data:image/(?:png|jpe?g|x-icon|vnd\\.microsoft\\.icon);base64,[A-Za-z0-9+/]+={0,2}$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern ALLOWED_HTTP_URL =
      Pattern.compile("^https?://[^\\s\"'<>]+$", Pattern.CASE_INSENSITIVE);

  public String sanitize(String input) {
    var sanitizer = new HtmlPolicyBuilder().toFactory();
    return sanitizer.sanitize(input);
  }

  /**
   * Validate a branding asset (logo, favicon, association logo) as the URL it is, instead of
   * sanitizing it as markup.
   *
   * <p>{@link #sanitize(String)} is an HTML sanitizer: handed plain text it HTML-ENCODES the
   * output. For a base64 data URL that means every {@code +} comes back as {@code &#43;} and the
   * trailing {@code =} as {@code &#61;} — the value is no longer a decodable URL. Browsers answer
   * {@code ERR_INVALID_URL}, the {@code <img>} fires {@code onError}, and the public sign-in stage
   * drops the tenant logo without a trace. The same corruption silently disabled the tenant
   * favicon.
   *
   * <p>These fields are never markup, so the answer is a whitelist, not an escape: an http(s) URL
   * or a base64 data URL in one of the image types the admin uploader produces passes through
   * BYTE-FOR-BYTE; anything else — {@code javascript:}, {@code data:text/html}, SVG, raw markup —
   * is rejected outright rather than mangled into something that neither renders nor fails loudly.
   *
   * @return the value unchanged when it is an acceptable asset URL, {@code null} when it is not
   */
  public String sanitizeAssetUrl(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    var trimmed = input.trim();
    if (isAllowedAssetUrl(trimmed)) {
      return input;
    }

    return null;
  }

  private static boolean isAllowedAssetUrl(String value) {
    if (ALLOWED_HTTP_URL.matcher(value).matches()) {
      try {
        return URI.create(value).getHost() != null;
      } catch (IllegalArgumentException ignored) {
        return false;
      }
    }

    if (ALLOWED_IMAGE_DATA_URL.matcher(value).matches()) {
      try {
        Base64.getDecoder().decode(value.substring(value.indexOf(',') + 1));
        return true;
      } catch (IllegalArgumentException ignored) {
        return false;
      }
    }

    return false;
  }

  public String sanitizeAllowingFormatting(String input) {
    var sanitizer =
        new HtmlPolicyBuilder()
            .allowStyling()
            .allowCommonInlineFormattingElements()
            .allowCommonBlockElements()
            .toFactory();
    return sanitizer.sanitize(input);
  }

  public String sanitizeAllowingFormattingAndLinks(String input) {
    var sanitizer =
        new HtmlPolicyBuilder()
            .allowStyling()
            .allowStandardUrlProtocols()
            .allowCommonInlineFormattingElements()
            .allowCommonBlockElements()
            .allowElements("a")
            .allowAttributes("href", "target")
            .onElements("a")
            .allowElements("img")
            .allowAttributes("src", "width", "height")
            .onElements("img")
            .allowAttributes("id")
            .matching(SAFE_ANCHOR_ID)
            .onElements(HEADING_ELEMENTS)
            .allowAttributes("data-anchor-removed")
            .matching(ANCHOR_REMOVED_TRUE)
            .onElements(HEADING_ELEMENTS)
            .toFactory();
    return sanitizer.sanitize(input);
  }
}
