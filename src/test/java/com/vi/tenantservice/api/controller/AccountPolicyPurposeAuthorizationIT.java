package com.vi.tenantservice.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.config.security.JwtAuthConverter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The narrow projection authorizes each configured logical purpose without an unrelated task. */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=INVITE_RESERVATIONS,CONSULTANT_IMPORT",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "IDENTITY_SETUP_CLIENT_ID=backend-setup",
      "IDENTITY_SETUP_SERVICE_SUBJECT=setup-subject",
      "ORISO_TASK_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class AccountPolicyPurposeAuthorizationIT {
  @Autowired private WebApplicationContext context;
  @Autowired private JwtAuthConverter converter;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVITATION", "IMPORT"})
  void theConfiguredActualPurposeCanReadTheRealProjectionWithoutConfigWizard(String kind)
      throws Exception {
    mvc.perform(
            get("/internal/tenants/1/account-provisioning-policy")
                .with(actor("setup-subject"))
                .header("X-ORISO-Wizard-Policy-Context", policyProof(1, kind)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.allowedNumberOfUsers").value(5));
  }

  @Test
  void missingProofForeignTenantAndForeignActorStillRefuseTheProjection() throws Exception {
    String path = "/internal/tenants/1/account-provisioning-policy";
    mvc.perform(get(path).with(actor("setup-subject"))).andExpect(status().isForbidden());
    mvc.perform(
            get(path)
                .with(actor("setup-subject"))
                .header("X-ORISO-Wizard-Policy-Context", policyProof(2, "INVITATION")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(path)
                .with(actor("foreign-subject"))
                .header("X-ORISO-Wizard-Policy-Context", policyProof(1, "INVITATION")))
        .andExpect(status().isForbidden());
  }

  private RequestPostProcessor actor(String subject) {
    return jwt()
        .jwt(
            token ->
                token
                    .issuer("https://identity.example/realms/test")
                    .subject(subject)
                    .claim("azp", "backend-setup")
                    .audience(
                        List.of(
                            "tenantservice",
                            "agencyservice",
                            "consultingtypeservice",
                            "userservice",
                            "oriso-task-commands"))
                    .issuedAt(Instant.now().minusSeconds(10))
                    .expiresAt(Instant.now().plusSeconds(60))
                    .claim(
                        "realm_access",
                        Map.of(
                            "roles",
                            List.of(
                                "config-wizard",
                                "invitation-reservations",
                                "consultant-import",
                                "smtp-sync"))))
        .authorities(token -> converter.convert(token).getAuthorities());
  }

  private String policyProof(long tenantId, String kind) throws Exception {
    long now = Instant.now().getEpochSecond();
    String source = "a".repeat(64);
    var origin = new java.util.TreeMap<String, Object>();
    origin.put("kind", kind);
    origin.put("tenantId", tenantId);
    origin.put("agencyIds", List.of(10));
    origin.put("profile", "CONSULTANT");
    origin.put("revision", "b".repeat(64));
    origin.put("reference", "IMPORT".equals(kind) ? "import:" + source + ":1" : "invite:1");
    if ("IMPORT".equals(kind)) origin.put("sourceDigest", source);
    var claims = new java.util.TreeMap<String, Object>();
    claims.put("aud", "tenantservice");
    claims.put("actor", "SETUP");
    claims.put("azp", "backend-setup");
    claims.put("exp", now + 60);
    claims.put("iat", now);
    claims.put("iss", "userservice");
    claims.put("method", "GET");
    claims.put("nonce", java.util.UUID.randomUUID().toString().replace("-", ""));
    claims.put("operation", "wizard.account-policy.read");
    claims.put("origin", origin);
    claims.put("purpose", "IMPORT".equals(kind) ? "CONSULTANT_IMPORT" : "INVITE_RESERVATIONS");
    claims.put("sub", "setup-subject");
    claims.put("target", "tenant/" + tenantId + "/account-provisioning-policy");
    claims.put("tenantId", tenantId);
    claims.put("tokenIssuer", "https://identity.example/realms/test");
    claims.put("v", 2);
    var semantic = new java.util.TreeMap<String, Object>();
    semantic.put("tenantId", tenantId);
    semantic.put("origin", origin);
    semantic.put("projection", "account-provisioning-policy");
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256"));
    claims.put(
        "payloadDigest",
        java.util.HexFormat.of()
            .formatHex(
                mac.doFinal(
                    ("payload\n" + json.writeValueAsString(semantic))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    String payload =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.writeValueAsBytes(claims));
    return payload
        + "."
        + java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                mac.doFinal(
                    ("proof\n" + payload).getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
  }
}
