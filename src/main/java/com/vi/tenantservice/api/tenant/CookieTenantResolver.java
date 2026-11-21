package com.vi.tenantservice.api.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@AllArgsConstructor
@NoArgsConstructor
@Component
@Slf4j
public class CookieTenantResolver implements TenantResolver {

  private static final String TENANT_ID = "tenantId";

  @Value("${feature.multitenancy.with.single.domain.enabled}")
  private boolean multitenancyWithSingleDomain;

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return resolveTenantFromRequest(request);
  }

  public Optional<Long> resolveTenantFromRequest(HttpServletRequest request) {

    if (!multitenancyWithSingleDomain) {
      return Optional.empty();
    }

    Cookie tokenCookie = WebUtils.getCookie(request, "keycloak");

    if (tokenCookie == null) {
      return tryResolveFromTenantIdCookie(request);
    } else {
      return resolveFromCookieValue(tokenCookie);
    }
  }

  private Optional<Long> tryResolveFromTenantIdCookie(HttpServletRequest request) {
    Cookie tenantId = WebUtils.getCookie(request, TENANT_ID);
    return tenantId == null ? Optional.empty() : parseTenantHint(tenantId.getValue());
  }

  private Optional<Long> resolveFromCookieValue(Cookie token) {
    String value = token.getValue();
    if (value == null) {
      return Optional.empty();
    }
    String[] chunks = value.split("\\.", 3);
    if (chunks.length < 2 || chunks[1].isEmpty()) {
      return Optional.empty();
    }
    try {
      String payload = new String(Base64.getUrlDecoder().decode(chunks[1]), StandardCharsets.UTF_8);
      var objectMapper =
          new ObjectMapper()
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
      Map<?, ?> claims = objectMapper.readValue(payload, Map.class);
      return claims == null ? Optional.empty() : parseTenantHint(claims.get(TENANT_ID));
    } catch (JsonProcessingException | IllegalArgumentException unreadable) {
      return Optional.empty();
    }
  }

  static Optional<Long> parseTenantHint(Object value) {
    try {
      return switch (value) {
        case Integer integer -> Optional.of(integer.longValue());
        case Long integer -> Optional.of(integer);
        case BigInteger integer -> Optional.of(integer.longValueExact());
        case BigDecimal decimal -> Optional.of(decimal.longValueExact());
        case String text -> Optional.of(Long.parseLong(text));
        case null, default -> Optional.empty();
      };
    } catch (ArithmeticException | NumberFormatException unreadable) {
      return Optional.empty();
    }
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
