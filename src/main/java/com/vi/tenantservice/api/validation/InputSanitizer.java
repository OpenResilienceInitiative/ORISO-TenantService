package com.vi.tenantservice.api.validation;

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

  public String sanitize(String input) {
    var sanitizer = new HtmlPolicyBuilder().toFactory();
    return sanitizer.sanitize(input);
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
