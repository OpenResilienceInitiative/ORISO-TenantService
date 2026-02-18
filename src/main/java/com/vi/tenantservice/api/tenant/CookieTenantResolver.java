package com.vi.tenantservice.api.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

    if (tenantId != null && tenantId.getValue() != null) {
      return Optional.of(Long.valueOf(tenantId.getValue()));
    } else {
      return Optional.empty();
    }
  }

  private Optional<Long> resolveFromCookieValue(Cookie token) {
    String[] chunks = token.getValue().split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();
    String payload = new String(decoder.decode(chunks[1]));
    var objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      Map<String, Object> map = objectMapper.readValue(payload, Map.class);
      Integer tenantIdFromCookie = (Integer) map.get(TENANT_ID);
      return tenantIdFromCookie == null
          ? Optional.empty()
          : Optional.of(Long.valueOf(tenantIdFromCookie));
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
