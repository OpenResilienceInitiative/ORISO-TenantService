package com.vi.tenantservice.config.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Recognises the UserService's technical service identity by token claims - its Keycloak subject
 * plus the realm role {@code technical} - never by username (ORISO-Helm#367). The role alone is not
 * enough: the bootstrapped realm admin, a person, carries it as well.
 */
@Component
public class TechnicalServiceIdentity {

  static final String SUBJECT_VARIABLE = "TECHNICAL_SERVICE_SUBJECT";
  private static final String TECHNICAL_ROLE = "technical";

  private final String subject;

  public TechnicalServiceIdentity(@Value("${technical.service.subject:}") String subject) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalStateException(
          SUBJECT_VARIABLE
              + " must be set to the Keycloak subject (sub) of the UserService's technical"
              + " service identity");
    }
    this.subject = subject.trim();
  }

  public boolean isCurrentCaller() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getPrincipal() instanceof Jwt jwt
        && isServiceIdentity(jwt);
  }

  public boolean isServiceIdentity(Jwt jwt) {
    return subject.equals(jwt.getSubject())
        && jwt.getClaims().get("realm_access") instanceof Map<?, ?> access
        && access.get("roles") instanceof Collection<?> roles
        && roles.contains(TECHNICAL_ROLE);
  }
}
