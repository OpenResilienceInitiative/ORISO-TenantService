package com.vi.tenantservice.api.service.systememail;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("tenantSmtpTestIdentity")
public class TenantSmtpTestIdentity {
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.vi.tenantservice.config.security.TaskServiceIdentity taskIdentity;

  public boolean allows(Authentication authentication, long tenantId) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return false;
    if (com.vi.tenantservice.config.security.TaskServiceIdentity.hasTaskRole(jwt)
        || (taskIdentity != null && taskIdentity.isTaskToken(jwt))) return false;
    Object realmAccess = jwt.getClaims().get("realm_access");
    if (!(realmAccess instanceof Map<?, ?> realm)
        || !(realm.get("roles") instanceof Collection<?> roles)) return false;
    if (!roles.contains("single-tenant-admin") || roles.contains("technical")) return false;
    Object claim = jwt.getClaims().get("tenantId");
    if (claim == null || !Long.toString(tenantId).equals(claim.toString())) return false;
    return Boolean.TRUE.equals(jwt.getClaims().get("email_verified"));
  }
}
