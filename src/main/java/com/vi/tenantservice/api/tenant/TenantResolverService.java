package com.vi.tenantservice.api.tenant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantResolverService {

  @NonNull AccessTokenTenantResolver accessTokenTenantResolver;

  @NonNull CookieTenantResolver cookieTenantResolver;

  @NonNull SubdomainTenantResolver subdomainTenantResolver;

  public Optional<Long> tryResolve() {
    HttpServletRequest request = getHttpServletRequest();
    if (userIsAuthenticated(request)) {
      return accessTokenTenantResolver.resolve(request);
    } else {
      return tryResolveForNonAuthUsers(request);
    }
  }

  private static HttpServletRequest getHttpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
        .getRequest();
  }

  public Optional<Long> tryResolveForNonAuthUsers() {
    return tryResolveForNonAuthUsers(getHttpServletRequest());
  }

  private Optional<Long> tryResolveForNonAuthUsers(HttpServletRequest request) {
    var resolvedFromCookie = cookieTenantResolver.resolveTenantFromRequest(request);

    if (resolvedFromCookie.isEmpty()) {
      return subdomainTenantResolver.resolve(request);
    } else {
      return resolvedFromCookie;
    }
  }

  private boolean userIsAuthenticated(HttpServletRequest request) {
    return request.getUserPrincipal() != null;
  }
}
