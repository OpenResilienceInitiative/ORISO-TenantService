package com.vi.tenantservice.api.service;

import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Decodes the data-URI and legacy base64 formats persisted by the branding uploader. */
@Component
public class BrandingAssetDecoder {

  private static final Set<String> SUPPORTED_TYPES =
      Set.of("image/png", "image/jpeg", "image/jpg", "image/x-icon", "image/vnd.microsoft.icon");
  private static final Pattern DATA_URI =
      Pattern.compile("^data:([^;,]+);base64,(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  /**
   * Upper bound for the decoded image, enforced BEFORE allocating: the endpoint is public and the
   * backing column is LONGTEXT (see changeset 0022), so without a cap an oversized stored value
   * would be decoded into memory for every anonymous request. 1 MiB is ~50x the largest branding
   * image observed in production and far beyond any sensible mail logo.
   */
  static final int MAX_DECODED_BYTES = 1024 * 1024;

  public record DecodedAsset(String contentType, byte[] bytes) {}

  public Optional<DecodedAsset> decode(String storedValue) {
    if (storedValue == null || storedValue.isBlank()) {
      return Optional.empty();
    }
    String value = storedValue.trim();
    String contentType = "image/png";
    String payload = value;
    Matcher matcher = DATA_URI.matcher(value);
    if (matcher.matches()) {
      contentType = matcher.group(1).toLowerCase(Locale.ROOT);
      payload = matcher.group(2);
      if (!SUPPORTED_TYPES.contains(contentType)) {
        return Optional.empty();
      }
      if ("image/jpg".equals(contentType)) {
        contentType = "image/jpeg";
      } else if ("image/vnd.microsoft.icon".equals(contentType)) {
        contentType = "image/x-icon";
      }
    } else if (value.regionMatches(true, 0, "http://", 0, 7)
        || value.regionMatches(true, 0, "https://", 0, 8)
        || value.regionMatches(true, 0, "data:", 0, 5)) {
      return Optional.empty();
    }
    if (payload.length() * 3L / 4 > MAX_DECODED_BYTES) {
      return Optional.empty();
    }
    try {
      byte[] bytes = Base64.getMimeDecoder().decode(payload);
      return bytes.length == 0
          ? Optional.empty()
          : Optional.of(new DecodedAsset(contentType, bytes));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
