package com.vi.tenantservice.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TenantCreationContext {
  public static final String HEADER = "X-ORISO-Tenant-Creation-Context";
  private final Environment environment;
  private final TaskServiceIdentity identity;

  public TenantCreationContext(Environment environment, TaskServiceIdentity identity) {
    this.environment = environment;
    this.identity = identity;
    if (java.util.Arrays.asList(
            environment.getProperty("TASK_IDENTITY_REQUIRED_TASKS", "").split(","))
        .stream()
        .map(String::trim)
        .anyMatch("CONFIG_WIZARD"::equals)) {
      try {
        if (Base64.getDecoder()
                .decode(environment.getProperty("ORISO_TENANT_CREATION_CONTEXT_KEY", ""))
                .length
            < 32) {
          throw new IllegalArgumentException();
        }
      } catch (IllegalArgumentException ex) {
        throw new IllegalStateException("Required tenant creation context key is not configured");
      }
    }
  }

  /** Checks the signing prerequisite before a Wizard tenant/reservation transaction begins. */
  public void validateConfiguration() {
    try {
      signingKey();
    } catch (IllegalArgumentException ex) {
      throw new AccessDeniedException("Tenant creation context key is not configured", ex);
    }
  }

  private byte[] signingKey() {
    byte[] key =
        Base64.getDecoder()
            .decode(environment.getProperty("ORISO_TENANT_CREATION_CONTEXT_KEY", ""));
    if (key.length < 32) throw new IllegalArgumentException("Invalid tenant creation context key");
    return key;
  }

  private byte[] signature(String payload) throws Exception {
    byte[] key = signingKey();
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
  }

  public String issue(long tenantId) {
    if (tenantId < 1 || !identity.current("CONFIG_WIZARD")) {
      throw new AccessDeniedException("Verified tenant bootstrap required");
    }
    Jwt caller = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (caller.getIssuer() == null) {
      throw new AccessDeniedException("Verified issuer required");
    }
    long now = Instant.now().getEpochSecond();
    Map<String, Object> claims = new TreeMap<>();
    claims.put("aud", "consultingtypeservice");
    claims.put("azp", caller.getClaimAsString("azp"));
    claims.put("exp", now + 60);
    claims.put("iat", now);
    claims.put("iss", "tenantservice");
    claims.put("nonce", UUID.randomUUID().toString());
    claims.put("sub", caller.getSubject());
    claims.put("tenantId", tenantId);
    claims.put("tokenIssuer", caller.getIssuer().toString());
    claims.put("v", 1);
    try {
      String payload =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(new ObjectMapper().writeValueAsBytes(claims));
      return payload
          + "."
          + Base64.getUrlEncoder().withoutPadding().encodeToString(signature(payload));
    } catch (Exception ex) {
      throw new IssuanceException(ex);
    }
  }

  /** A signing/configuration failure after creation, distinct from caller authorization denial. */
  public static class IssuanceException extends AccessDeniedException {
    public IssuanceException(Exception cause) {
      super("Tenant bootstrap proof could not be issued", cause);
    }
  }
}
