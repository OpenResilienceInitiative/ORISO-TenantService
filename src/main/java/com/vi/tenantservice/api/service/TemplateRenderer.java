package com.vi.tenantservice.api.service;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TemplateRenderer {

  private static final Pattern LEGACY_PLACEHOLDER =
      Pattern.compile("\\$\\{\\s*([a-zA-Z_][a-zA-Z_0-9]*)\\s*}");

  /** Substitute legacy privacy contact placeholders in the current request's text. */
  public String renderTemplate(String templateContent, Map<String, String> contacts) {
    if (templateContent == null) {
      return null;
    }
    return LEGACY_PLACEHOLDER
        .matcher(templateContent)
        .replaceAll(
            match -> Matcher.quoteReplacement(Objects.toString(contacts.get(match.group(1)), "")));
  }
}
