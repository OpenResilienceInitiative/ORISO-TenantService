package com.vi.tenantservice.api.facade;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Validated public app origin used by DPA sign invitations. */
@Component
public final class DpaSignLinkOrigin {

  private final String origin;

  public DpaSignLinkOrigin(@Value("${app.base.url:}") String configuredOrigin) {
    String value = configuredOrigin == null ? "" : configuredOrigin.trim();
    try {
      URI uri = new URI(value);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || (uri.getRawPath() != null
              && !uri.getRawPath().isEmpty()
              && !"/".equals(uri.getRawPath()))
          || uri.getQuery() != null
          || uri.getFragment() != null) {
        throw invalidOrigin();
      }
      origin = uri.toString().replaceAll("/$", "");
    } catch (URISyntaxException exception) {
      throw invalidOrigin();
    }
  }

  public String build(String rawToken) {
    return origin + "/dpa-sign/" + rawToken;
  }

  private static IllegalArgumentException invalidOrigin() {
    return new IllegalArgumentException(
        "APP_BASE_URL must be configured as an absolute HTTPS app origin without a path");
  }
}
