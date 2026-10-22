package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Only the current platform account creation defaults, without administrative controls. */
@RestController
public class PlatformAccountInactivityPolicyController {
  public static final String HEADER = "X-ORISO-Platform-Inactivity-Context";
  private final Environment environment;
  private final TaskServiceIdentity identity;
  private final TenantAdminControlsService controls;

  public PlatformAccountInactivityPolicyController(
      Environment environment, TaskServiceIdentity identity, TenantAdminControlsService controls) {
    this.environment = environment;
    this.identity = identity;
    this.controls = controls;
  }

  @GetMapping("/internal/platform/account-inactivity-policy")
  @org.springframework.security.access.prepost.PreAuthorize(
      "@taskServiceIdentity.allows(authentication, 'RUNTIME_POLICY')")
  public ResponseEntity<Policy> get(@RequestHeader(value = HEADER, required = false) String proof) {
    require(proof);
    var source = controls.getAccountInactivitySettings();
    if (source == null || source.getOtherMonths() == null || source.getOtherMonths() < 1
        || source.getRevision() == null || source.getRevision() < 0)
      throw new IllegalStateException("Platform account policy unavailable");
    return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
        .body(new Policy(source.getOtherMonths(), source.getRevision()));
  }

  private void require(String proof) {
    try {
      if (!com.vi.tenantservice.config.security.TaskContextProof.grouped(environment)
          || !identity.current("RUNTIME_POLICY")) throw new IllegalArgumentException();
      var claims = com.vi.tenantservice.config.security.TaskContextProof.verify(
          com.vi.tenantservice.config.security.TaskContextProof.key(environment), proof);
      var fields = new java.util.HashSet<String>();
      claims.fieldNames().forEachRemaining(fields::add);
      if (!fields.equals(java.util.Set.of("v", "iss", "aud", "actor", "purpose", "operation", "method", "target", "tenantId", "sub", "azp", "tokenIssuer", "iat", "exp", "nonce")))
        throw new IllegalArgumentException();
      var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
      if (!(authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt caller))
        throw new IllegalArgumentException();
      long now = java.time.Instant.now().getEpochSecond();
      long issued = com.vi.tenantservice.config.security.TaskContextProof.integer(claims.path("iat"));
      long expires = com.vi.tenantservice.config.security.TaskContextProof.integer(claims.path("exp"));
      String nonce = claims.path("nonce").textValue();
      if (com.vi.tenantservice.config.security.TaskContextProof.integer(claims.path("v")) != 2
          || com.vi.tenantservice.config.security.TaskContextProof.integer(claims.path("tenantId")) != 0
          || !"userservice".equals(claims.path("iss").textValue())
          || !"tenantservice".equals(claims.path("aud").textValue())
          || !"RUNTIME".equals(claims.path("actor").textValue())
          || !"RUNTIME_POLICY".equals(claims.path("purpose").textValue())
          || !"platform.account-inactivity.read".equals(claims.path("operation").textValue())
          || !"GET".equals(claims.path("method").textValue())
          || !"platform/0/account-inactivity".equals(claims.path("target").textValue())
          || !caller.getSubject().equals(claims.path("sub").textValue())
          || !caller.getClaimAsString("azp").equals(claims.path("azp").textValue())
          || caller.getIssuer() == null
          || !caller.getIssuer().toString().equals(claims.path("tokenIssuer").textValue())
          || caller.getExpiresAt() == null || expires > caller.getExpiresAt().getEpochSecond()
          || issued > now + 5 || issued < now - 60 || expires <= now || expires <= issued || expires - issued > 60
          || nonce == null || !java.util.UUID.fromString(nonce).toString().equals(nonce))
        throw new IllegalArgumentException();
    } catch (Exception invalid) {
      throw new org.springframework.security.access.AccessDeniedException("Checked platform policy required");
    }
  }

  public record Policy(int otherMonths, long revision) {}
}
