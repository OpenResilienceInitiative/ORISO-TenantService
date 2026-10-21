package com.vi.tenantservice.config.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class WizardPolicyContext {
  public static final String HEADER = "X-ORISO-Wizard-Policy-Context";
  private final Environment environment;
  private final TaskServiceIdentity identity;

  public WizardPolicyContext(Environment environment, TaskServiceIdentity identity) {
    this.environment = environment;
    this.identity = identity;
    if (TaskContextProof.grouped(environment)) {
      try {
        TaskContextProof.key(environment);
      } catch (IllegalArgumentException invalid) {
        throw new IllegalStateException("Required task context key is not configured");
      }
      return;
    }
    if (java.util.Arrays.asList(
            environment.getProperty("TASK_IDENTITY_REQUIRED_TASKS", "").split(","))
        .stream()
        .map(String::trim)
        .anyMatch("CONFIG_WIZARD"::equals)) {
      try {
        if (Base64.getDecoder()
                .decode(environment.getProperty("ORISO_WIZARD_POLICY_CONTEXT_KEY", ""))
                .length
            < 32) {
          throw new IllegalArgumentException();
        }
      } catch (IllegalArgumentException ex) {
        throw new IllegalStateException("Required wizard policy context key is not configured");
      }
    }
  }

  private byte[] signature(String payload) throws Exception {
    byte[] key =
        Base64.getDecoder().decode(environment.getProperty("ORISO_WIZARD_POLICY_CONTEXT_KEY", ""));
    if (key.length < 32) {
      throw new AccessDeniedException("Wizard policy context key is not configured");
    }
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
  }

  public void require(long tenantId, String proof) {
    if (TaskContextProof.grouped(environment)) {
      requireGrouped(tenantId, proof);
      return;
    }
    if (!identity.current("CONFIG_WIZARD")
        || tenantId < 1
        || proof == null
        || proof.length() > 2048) {
      throw new AccessDeniedException("Verified wizard policy required");
    }
    try {
      String[] parts = proof.split("\\.", -1);
      if (parts.length != 2
          || !MessageDigest.isEqual(signature(parts[0]), Base64.getUrlDecoder().decode(parts[1]))) {
        throw new IllegalArgumentException();
      }
      Map<String, Object> claims =
          new ObjectMapper()
              .readValue(
                  Base64.getUrlDecoder().decode(parts[0]),
                  new TypeReference<Map<String, Object>>() {});
      Jwt caller = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      long now = Instant.now().getEpochSecond();
      long issued = ((Number) claims.get("iat")).longValue();
      long expires = ((Number) claims.get("exp")).longValue();
      if (claims.size() != 11
          || !"oriso-userservice".equals(claims.get("iss"))
          || !"tenantservice".equals(claims.get("aud"))
          || !"wizard.account-policy.read".equals(claims.get("operation"))
          || ((Number) claims.get("v")).intValue() != 1
          || ((Number) claims.get("tenantId")).longValue() != tenantId
          || !caller.getSubject().equals(claims.get("sub"))
          || !caller.getClaimAsString("azp").equals(claims.get("azp"))
          || caller.getIssuer() == null
          || !caller.getIssuer().toString().equals(claims.get("tokenIssuer"))
          || issued > now + 5
          || expires <= now - 5
          || expires <= issued
          || expires - issued > 60
          || !(claims.get("nonce") instanceof String)
          || ((String) claims.get("nonce")).isBlank()) {
        throw new IllegalArgumentException();
      }
    } catch (Exception ex) {
      throw new AccessDeniedException("Invalid wizard policy context");
    }
  }

  private void requireGrouped(long tenantId, String proof) {
    try {
      byte[] key = TaskContextProof.key(environment);
      var claims = TaskContextProof.verify(key, proof);
      var origin = claims.path("origin");
      String kind = origin.path("kind").asText();
      String purpose =
          switch (kind) {
            case "INVITATION" -> "INVITE_RESERVATIONS";
            case "IMPORT" -> "CONSULTANT_IMPORT";
            default -> throw new IllegalArgumentException();
          };
      if (tenantId < 1 || !identity.current(purpose)) throw new IllegalArgumentException();
      Jwt caller = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      long now = Instant.now().getEpochSecond();
      long issued = TaskContextProof.integer(claims.path("iat"));
      long expires = TaskContextProof.integer(claims.path("exp"));
      if (claims.size() != 17
          || TaskContextProof.integer(claims.path("v")) != 2
          || !"userservice".equals(claims.path("iss").asText())
          || !"tenantservice".equals(claims.path("aud").asText())
          || !"SETUP".equals(claims.path("actor").asText())
          || !purpose.equals(claims.path("purpose").asText())
          || !"wizard.account-policy.read".equals(claims.path("operation").asText())
          || !"GET".equals(claims.path("method").asText())
          || TaskContextProof.integer(claims.path("tenantId")) != tenantId
          || !("tenant/" + tenantId + "/account-provisioning-policy")
              .equals(claims.path("target").asText())
          || !caller.getSubject().equals(claims.path("sub").asText())
          || !caller.getClaimAsString("azp").equals(claims.path("azp").asText())
          || caller.getIssuer() == null
          || !caller.getIssuer().toString().equals(claims.path("tokenIssuer").asText())
          || issued > now + 5
          || expires <= now
          || expires <= issued
          || Math.subtractExact(expires, issued) > 60
          || !claims.path("nonce").isTextual()
          || !claims.path("nonce").asText().matches("[a-f0-9]{32}")
          || !claims.path("payloadDigest").isTextual()) throw new IllegalArgumentException();
      requireOrigin(origin, tenantId, kind);
      String digest =
          TaskContextProof.digest(
              key,
              Map.of(
                  "tenantId",
                  tenantId,
                  "origin",
                  origin,
                  "projection",
                  "account-provisioning-policy"));
      if (!MessageDigest.isEqual(
          digest.getBytes(StandardCharsets.US_ASCII),
          claims.path("payloadDigest").asText().getBytes(StandardCharsets.US_ASCII)))
        throw new IllegalArgumentException();
    } catch (Exception invalid) {
      throw new AccessDeniedException("Invalid wizard policy context");
    }
  }

  private static void requireOrigin(
      com.fasterxml.jackson.databind.JsonNode origin, long tenantId, String kind) {
    if (!origin.isObject()
        || origin.size() != ("IMPORT".equals(kind) ? 7 : 6)
        || TaskContextProof.integer(origin.path("tenantId")) != tenantId
        || !java.util.Set.of("CONSULTANT", "CONSULTANT_AGENCY_ADMIN")
            .contains(origin.path("profile").asText())
        || !origin.path("revision").isTextual()
        || !origin.path("revision").asText().matches("[a-f0-9]{64}"))
      throw new IllegalArgumentException();
    var agencies = origin.path("agencyIds");
    if (!agencies.isArray() || agencies.isEmpty() || agencies.size() > 100)
      throw new IllegalArgumentException();
    long previous = 0;
    for (var agency : agencies) {
      long id = TaskContextProof.integer(agency);
      if (id <= previous) throw new IllegalArgumentException();
      previous = id;
    }
    String reference = origin.path("reference").asText();
    if ("INVITATION".equals(kind)) {
      if (!reference.matches("invite:[1-9][0-9]{0,18}")) throw new IllegalArgumentException();
    } else {
      String source = origin.path("sourceDigest").asText();
      if (!source.matches("[a-f0-9]{64}")
          || !reference.matches("import:" + source + ":[1-9][0-9]{0,18}"))
        throw new IllegalArgumentException();
    }
  }
}
