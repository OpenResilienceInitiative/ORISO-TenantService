package com.vi.tenantservice.api.service.systememail;

import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("systemEmailServiceIdentity")
public class SystemEmailServiceIdentity {
  private final String subject;
  private final String client;

  public SystemEmailServiceIdentity(
      @Value("${system.email.delivery.service-subject:}") String subject,
      @Value("${system.email.delivery.service-client:}") String client) {
    this.subject = subject;
    this.client = client;
  }

  public boolean allows(Authentication authentication) {
    if (subject.isBlank()
        || client.isBlank()
        || authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt jwt)) return false;
    if (!subject.equals(jwt.getSubject()) || !client.equals(jwt.getClaimAsString("azp")))
      return false;
    Object realm = jwt.getClaims().get("realm_access");
    return realm instanceof Map<?, ?> access
        && access.get("roles") instanceof Collection<?> roles
        && roles.contains("technical");
  }
}
