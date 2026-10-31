package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Independent wire builder exercises the real receiving identity and proof guard. */
class WizardPolicyV2ContextTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final byte[] SHARED_KEY = new byte[32];
  private static final String ISSUER = "https://synthetic.identity/realms/context";

  @AfterEach
  void clearIdentity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void invitationPolicyUsesItsActualPurposeAndTheSoleSharedKey() throws Exception {
    var environment =
        new MockEnvironment()
            .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
            .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD,INVITE_RESERVATIONS")
            .withProperty("TASK_IDENTITY_AUDIENCE", "tenantservice")
            .withProperty("IDENTITY_SETUP_CLIENT_ID", "backend-setup")
            .withProperty("IDENTITY_SETUP_SERVICE_SUBJECT", "setup-subject")
            .withProperty("ORISO_TASK_CONTEXT_KEY", Base64.getEncoder().encodeToString(SHARED_KEY))
            .withProperty(
                "ORISO_WIZARD_POLICY_CONTEXT_KEY",
                Base64.getEncoder().encodeToString(new byte[33]));
    var jwt =
        Jwt.withTokenValue("synthetic-no-wire-token")
            .header("alg", "RS256")
            .issuer(ISSUER)
            .subject("setup-subject")
            .claim("azp", "backend-setup")
            .audience(
                List.of(
                    "tenantservice",
                    "agencyservice",
                    "consultingtypeservice",
                    "userservice",
                    "oriso-task-commands"))
            .expiresAt(Instant.now().plusSeconds(120))
            .claim(
                "realm_access",
                Map.of(
                    "roles",
                    List.of(
                        "config-wizard",
                        "invitation-reservations",
                        "consultant-import",
                        "smtp-sync")))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    assertThat(new TaskServiceIdentity(environment).current("INVITE_RESERVATIONS")).isTrue();
    var receiver = new WizardPolicyContext(environment, new TaskServiceIdentity(environment));
    assertThatCode(() -> receiver.require(42, proof(claims("INVITATION"))))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "v",
        "iss",
        "aud",
        "actor",
        "purpose",
        "operation",
        "method",
        "azp",
        "sub",
        "tokenIssuer",
        "tenantId",
        "target",
        "payloadDigest",
        "nonce",
        "numericNonce",
        "iat",
        "fractionalIat",
        "exp",
        "ttl",
        "extra",
        "missing",
        "originKind",
        "originTenant",
        "originProfile",
        "originReference",
        "originRevision",
        "numericRevision",
        "emptyAgencies",
        "zeroAgency",
        "duplicateAgency",
        "unsortedAgencies",
        "originExtra"
      })
  void independentlySignedMutationsCannotAuthorizeThePolicyRead(String field) throws Exception {
    var receiver = groupedReceiver();
    Map<String, Object> value = claims("INVITATION");
    @SuppressWarnings("unchecked")
    Map<String, Object> origin = (Map<String, Object>) value.get("origin");
    switch (field) {
      case "v" -> value.put("v", 1);
      case "iss",
              "aud",
              "actor",
              "purpose",
              "operation",
              "method",
              "azp",
              "sub",
              "tokenIssuer",
              "target",
              "payloadDigest",
              "nonce" ->
          value.put(field, "wrong");
      case "numericNonce" -> value.put("nonce", new java.math.BigInteger("1".repeat(32)));
      case "tenantId" -> value.put(field, 43);
      case "iat" -> value.put(field, Instant.now().getEpochSecond() + 30);
      case "fractionalIat" -> value.put("iat", Instant.now().getEpochSecond() + 0.5);
      case "exp" -> value.put(field, Instant.now().getEpochSecond() - 1);
      case "ttl" -> value.put("exp", Instant.now().getEpochSecond() + 120);
      case "extra" -> value.put("unknown", "unexpected");
      case "missing" -> value.remove("purpose");
      case "originKind" -> origin.put("kind", "HUMAN_ADMIN");
      case "originTenant" -> origin.put("tenantId", 43);
      case "originProfile" -> origin.put("profile", "TENANT_ADMIN");
      case "originReference" -> origin.put("reference", "arbitrary-tenant");
      case "originRevision" -> origin.put("revision", "wrong");
      case "numericRevision" -> origin.put("revision", new java.math.BigInteger("1".repeat(64)));
      case "emptyAgencies" -> origin.put("agencyIds", List.of());
      case "zeroAgency" -> origin.put("agencyIds", List.of(0));
      case "duplicateAgency" -> origin.put("agencyIds", List.of(7, 7));
      case "unsortedAgencies" -> origin.put("agencyIds", List.of(8, 7));
      case "originExtra" -> origin.put("unknown", "unexpected");
      default -> throw new IllegalArgumentException();
    }
    if (field.startsWith("origin")
        || field.endsWith("Agencies")
        || field.endsWith("Agency")
        || "numericRevision".equals(field)) {
      value.put(
          "payloadDigest",
          HexFormat.of()
              .formatHex(
                  hmac(
                      "payload\n"
                          + JSON.writeValueAsString(
                              new TreeMap<>(
                                  Map.of(
                                      "tenantId",
                                      42,
                                      "origin",
                                      origin,
                                      "projection",
                                      "account-provisioning-policy"))))));
    }
    String signed = proof(value);
    assertThatThrownBy(() -> receiver.require(42, signed))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void configuredImportHasItsOwnActualLogicalPurpose() throws Exception {
    var receiver = groupedReceiver();
    assertThatCode(() -> receiver.require(42, proof(claims("IMPORT")))).doesNotThrowAnyException();
    var changed = claims("IMPORT");
    changed.put("purpose", "INVITE_RESERVATIONS");
    assertThatThrownBy(() -> receiver.require(42, proof(changed)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void duplicateJsonClaimsAreRejectedEvenWithAValidSignature() throws Exception {
    var receiver = groupedReceiver();
    String json = JSON.writeValueAsString(claims("INVITATION"));
    String duplicate = "{\"v\":2," + json.substring(1);
    String signed = rawProof(duplicate);
    assertThatThrownBy(() -> receiver.require(42, signed))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static WizardPolicyContext groupedReceiver() {
    var environment =
        new MockEnvironment()
            .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
            .withProperty(
                "TASK_IDENTITY_REQUIRED_TASKS",
                "CONFIG_WIZARD,INVITE_RESERVATIONS,CONSULTANT_IMPORT")
            .withProperty("TASK_IDENTITY_AUDIENCE", "tenantservice")
            .withProperty("IDENTITY_SETUP_CLIENT_ID", "backend-setup")
            .withProperty("IDENTITY_SETUP_SERVICE_SUBJECT", "setup-subject")
            .withProperty("ORISO_TASK_CONTEXT_KEY", Base64.getEncoder().encodeToString(SHARED_KEY))
            .withProperty(
                "ORISO_WIZARD_POLICY_CONTEXT_KEY",
                Base64.getEncoder().encodeToString(new byte[33]));
    var jwt =
        Jwt.withTokenValue("synthetic-no-wire-token")
            .header("alg", "RS256")
            .issuer(ISSUER)
            .subject("setup-subject")
            .claim("azp", "backend-setup")
            .audience(
                List.of(
                    "tenantservice",
                    "agencyservice",
                    "consultingtypeservice",
                    "userservice",
                    "oriso-task-commands"))
            .expiresAt(Instant.now().plusSeconds(120))
            .claim(
                "realm_access",
                Map.of(
                    "roles",
                    List.of(
                        "config-wizard",
                        "invitation-reservations",
                        "consultant-import",
                        "smtp-sync")))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    var identity = new TaskServiceIdentity(environment);
    assertThat(identity.current("INVITE_RESERVATIONS")).isTrue();
    assertThat(identity.current("CONSULTANT_IMPORT")).isTrue();
    return new WizardPolicyContext(environment, identity);
  }

  private static Map<String, Object> claims(String kind) throws Exception {
    Map<String, Object> origin =
        new TreeMap<>(
            Map.of(
                "kind",
                kind,
                "reference",
                "INVITATION".equals(kind) ? "invite:9" : "import:" + "c".repeat(64) + ":3",
                "tenantId",
                42,
                "agencyIds",
                List.of(7),
                "profile",
                "CONSULTANT",
                "revision",
                "a".repeat(64)));
    if ("IMPORT".equals(kind)) origin.put("sourceDigest", "c".repeat(64));
    Map<String, Object> payload =
        new TreeMap<>(
            Map.of("tenantId", 42, "origin", origin, "projection", "account-provisioning-policy"));
    long now = Instant.now().getEpochSecond();
    Map<String, Object> claims =
        new TreeMap<>(
            Map.of(
                "v",
                2,
                "iss",
                "userservice",
                "aud",
                "tenantservice",
                "actor",
                "SETUP",
                "purpose",
                "INVITATION".equals(kind) ? "INVITE_RESERVATIONS" : "CONSULTANT_IMPORT",
                "operation",
                "wizard.account-policy.read",
                "method",
                "GET",
                "azp",
                "backend-setup",
                "sub",
                "setup-subject",
                "tokenIssuer",
                ISSUER));
    claims.putAll(
        Map.of(
            "tenantId",
            42,
            "target",
            "tenant/42/account-provisioning-policy",
            "origin",
            origin,
            "payloadDigest",
            HexFormat.of().formatHex(hmac("payload\n" + JSON.writeValueAsString(payload))),
            "nonce",
            "b".repeat(32),
            "iat",
            now,
            "exp",
            now + 60));
    return claims;
  }

  private static String proof(Map<String, Object> claims) throws Exception {
    return rawProof(JSON.writeValueAsString(claims));
  }

  private static String rawProof(String json) throws Exception {
    String encoded =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    return encoded
        + "."
        + Base64.getUrlEncoder().withoutPadding().encodeToString(hmac("proof\n" + encoded));
  }

  private static byte[] hmac(String text) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SHARED_KEY, "HmacSHA256"));
    return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
  }
}
