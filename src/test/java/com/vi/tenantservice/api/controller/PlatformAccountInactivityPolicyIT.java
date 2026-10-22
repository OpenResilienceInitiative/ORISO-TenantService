package com.vi.tenantservice.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import com.vi.tenantservice.config.security.TaskContextProof;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=RUNTIME_POLICY",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "IDENTITY_RUNTIME_CLIENT_ID=backend-runtime",
      "IDENTITY_RUNTIME_SERVICE_SUBJECT=runtime-subject",
      "ORISO_TASK_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class PlatformAccountInactivityPolicyIT {
  @Autowired private WebApplicationContext context;
  @Autowired private JwtAuthConverter converter;
  @Autowired private TenantAdminControlsService controls;
  private MockMvc mvc;
  private static final String PATH = "/internal/platform/account-inactivity-policy";

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void actualRuntimeReadsOnlyTheCurrentAuthoritativePlatformDefaultsWithoutStore()
      throws Exception {
    var prior = controls.getAccountInactivitySettings();
    controls.updateAccountInactivitySettings(new com.vi.tenantservice.api.model.AccountInactivitySettings(7, 11, 18, prior.getRevision()));
    var authoritative = controls.getAccountInactivitySettings();
    org.assertj.core.api.Assertions.assertThat(authoritative.getOtherMonths()).isEqualTo(18);
    org.assertj.core.api.Assertions.assertThat(authoritative.getRevision()).isGreaterThan(prior.getRevision());
    var result =
        mvc.perform(
                get(PATH)
                    .with(actor("runtime-subject", false))
                    .header(PlatformAccountInactivityPolicyController.HEADER, proof(null)))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.otherMonths").value(authoritative.getOtherMonths()))
            .andExpect(jsonPath("$.revision").value(authoritative.getRevision()))
            .andReturn();
    org.assertj.core.api.Assertions.assertThat(
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsByteArray())
                .size())
        .isEqualTo(2);
  }

  @Test
  void independentlyResignedWrongOperationTargetOwnerFreshnessAndExtraFieldsRefuse()
      throws Exception {
    for (String mutation :
        List.of(
            "operation",
            "target",
            "tenantId",
            "sub",
            "azp",
            "tokenIssuer",
            "aud",
            "iss",
            "actor",
            "purpose",
            "method",
            "iat",
            "exp",
            "nonce",
            "extra",
            "v")) {
      mvc.perform(
              get(PATH)
                  .with(actor("runtime-subject", false))
                  .header(PlatformAccountInactivityPolicyController.HEADER, proof(mutation)))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get(PATH).with(actor("runtime-subject", false))).andExpect(status().isForbidden());
  }

  @Test
  void foreignSubjectOrNonCanonicalAudienceCannotUseTheProjectionOrAdminControls()
      throws Exception {
    mvc.perform(
            get(PATH)
                .with(actor("foreign", false))
                .header(PlatformAccountInactivityPolicyController.HEADER, proof(null)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(PATH)
                .with(actor("runtime-subject", true))
                .header(PlatformAccountInactivityPolicyController.HEADER, proof(null)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/controls").with(actor("runtime-subject", false)))
        .andExpect(status().isForbidden());
  }

  private RequestPostProcessor actor(String subject, boolean extraAudience) {
    return jwt()
        .jwt(
            token ->
                token
                    .issuer("https://identity.example/realms/test")
                    .subject(subject)
                    .claim("azp", "backend-runtime")
                    .audience(
                        extraAudience
                            ? List.of(
                                "tenantservice", "agencyservice", "appointmentservice", "foreign")
                            : List.of("tenantservice", "agencyservice", "appointmentservice"))
                    .issuedAt(Instant.now().minusSeconds(10))
                    .expiresAt(Instant.now().plusSeconds(60))
                    .claim(
                        "realm_access",
                        Map.of(
                            "roles",
                            List.of(
                                "runtime-policy",
                                "matrix-agency",
                                "matrix-agency-provision",
                                "appointment-sync",
                                "appointment-participant-cleanup"))))
        .authorities(token -> converter.convert(token).getAuthorities());
  }

  private String proof(String mutation) throws Exception {
    long now = Instant.now().getEpochSecond();
    var claims = new TreeMap<String, Object>();
    claims.put("v", 2);
    claims.put("iss", "userservice");
    claims.put("aud", "tenantservice");
    claims.put("actor", "RUNTIME");
    claims.put("purpose", "RUNTIME_POLICY");
    claims.put("operation", "platform.account-inactivity.read");
    claims.put("method", "GET");
    claims.put("target", "platform/0/account-inactivity");
    claims.put("tenantId", 0);
    claims.put("sub", "runtime-subject");
    claims.put("azp", "backend-runtime");
    claims.put("tokenIssuer", "https://identity.example/realms/test");
    claims.put("iat", now);
    claims.put("exp", now + 30);
    claims.put("nonce", java.util.UUID.randomUUID().toString());
    if (mutation != null) {
      switch (mutation) {
        case "iat" -> claims.put("iat", now + 100);
        case "exp" -> claims.put("exp", now - 100);
        case "tenantId" -> claims.put("tenantId", 1);
        case "v" -> claims.put("v", "2");
        case "nonce" -> claims.put("nonce", "");
        case "extra" -> claims.put("extra", false);
        default -> claims.put(mutation, "foreign");
      }
    }
    return TaskContextProof.sign(new byte[32], claims);
  }
}
