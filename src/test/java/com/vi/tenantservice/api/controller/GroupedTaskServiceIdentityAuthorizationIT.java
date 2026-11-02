package com.vi.tenantservice.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Grouping credentials does not remove receiver operation, reservation or target guards. */
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=CONFIG_WIZARD,INVITE_RESERVATIONS,NOTIFICATION_DISPATCH,RUNTIME_POLICY",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "IDENTITY_SETUP_CLIENT_ID=backend-setup",
      "IDENTITY_SETUP_SERVICE_SUBJECT=setup-subject",
      "IDENTITY_MAIL_CLIENT_ID=backend-mail",
      "IDENTITY_MAIL_SERVICE_SUBJECT=mail-subject",
      "IDENTITY_RUNTIME_CLIENT_ID=backend-runtime",
      "IDENTITY_RUNTIME_SERVICE_SUBJECT=runtime-subject",
      "IDENTITY_ACCOUNT_ADMIN_CLIENT_ID=backend-account-admin",
      "IDENTITY_ACCOUNT_ADMIN_SERVICE_SUBJECT=account-admin-subject",
      "ORISO_WIZARD_POLICY_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "ORISO_TENANT_CREATION_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class GroupedTaskServiceIdentityAuthorizationIT {
  private static final List<String> SETUP_ROLES =
      List.of("config-wizard", "invitation-reservations", "consultant-import");
  private static final List<String> MAIL_ROLES =
      List.of(
          "smtp-sync", "system-email-delivery", "notification-dispatch", "notifications-technical");
  private static final List<String> RUNTIME_ROLES =
      List.of(
          "runtime-policy",
          "matrix-agency",
          "matrix-agency-provision",
          "appointment-sync",
          "appointment-participant-cleanup",
          "deletion-audit-read");

  @Autowired private WebApplicationContext context;
  @Autowired private JwtAuthConverter converter;
  @Autowired private TenantIdAllocationService allocation;
  @Autowired private TenantIdReservationRepository reservations;

  @MockitoBean
  private com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService settings;

  @MockitoBean
  private com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory
      settingsFactory;

  @MockitoBean
  private com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory
      typeFactory;

  @MockitoBean
  private com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      typeApi;

  @MockitoBean private com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier headers;
  @MockitoBean private com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService types;
  @MockitoBean private com.vi.tenantservice.api.service.consultingtype.UserAdminService admins;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    reservations.deleteAll();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(typeFactory.createControllerApi()).thenReturn(typeApi);
    when(typeApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(headers.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(headers.getKeycloakAndCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
  }

  @Test
  void oneSetupActorCreatesAndReleasesOnlyReservationsWithTheMatchingProof() throws Exception {
    var setup = actor("setup-subject", "backend-setup", SETUP_ROLES, "tenantservice");
    String own = allocation.reserve(9500L, "grouped-test").getToken();
    String foreign = allocation.reserve(9501L, "grouped-test").getToken();
    mvc.perform(
            delete("/tenantadmin/tenant-ids/reservations/9500")
                .with(setup)
                .param("reservationToken", foreign))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/tenantadmin/tenant-ids/reservations/9500")
                .with(setup)
                .param("reservationToken", own))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/tenantadmin")
                .with(setup)
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withName("Grouped setup")
                        .withSubdomain("groupedsetup")
                        .withLicensing()
                        .withId(9501L)
                        .withTenantIdReservationToken(foreign)
                        .jsonify()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9501));
    mvc.perform(get("/tenant").with(setup)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/dpa/versions").with(setup)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/2/dpa/versions").with(setup)).andExpect(status().isForbidden());
  }

  @Test
  void setupPolicyStillRequiresTheSignedExactTenantContext() throws Exception {
    var setup = actor("setup-subject", "backend-setup", SETUP_ROLES, "tenantservice");
    String path = "/internal/tenants/1/account-provisioning-policy";
    mvc.perform(get(path).with(setup)).andExpect(status().isForbidden());
    mvc.perform(get(path).with(setup).header("X-ORISO-Wizard-Policy-Context", policyProof(2)))
        .andExpect(status().isForbidden());
    mvc.perform(get(path).with(setup).header("X-ORISO-Wizard-Policy-Context", policyProof(1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void groupedMailAndRuntimeKeepTheirDifferentReadProjections() throws Exception {
    var mail = actor("mail-subject", "backend-mail", MAIL_ROLES, "tenantservice");
    var runtime = actor("runtime-subject", "backend-runtime", RUNTIME_ROLES, "tenantservice");
    mvc.perform(get("/internal/tenants/2/system-email-context").with(mail))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.smtp.password").doesNotExist())
        .andExpect(jsonPath("$.settings.smtp.username").doesNotExist())
        .andExpect(jsonPath("$.settings.smtp.host").doesNotExist());
    mvc.perform(get("/tenantadmin/1/dpa/gate").with(runtime)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/1/permission-policies").with(runtime)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/1/dpa/gate").with(mail)).andExpect(status().isForbidden());
    mvc.perform(get("/internal/tenants/2/system-email-context").with(runtime))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenant/2").with(mail)).andExpect(status().isForbidden());
  }

  @Test
  void wrongActorAudienceAndInheritedHumanGrantsCannotUseTaskOrHumanRoutes() throws Exception {
    for (var caller :
        List.of(
            actor("mail-subject", "backend-mail", SETUP_ROLES, "tenantservice"),
            actor("foreign", "backend-setup", SETUP_ROLES, "tenantservice"),
            actor("setup-subject", "backend-setup", SETUP_ROLES, "agencyservice"),
            actor(
                "setup-subject",
                "backend-setup",
                List.of(
                    "config-wizard",
                    "invitation-reservations",
                    "consultant-import",
                    "tenant-admin"),
                "tenantservice"),
            actor(
                "account-admin-subject",
                "backend-account-admin",
                List.of("tenant-admin"),
                "tenantservice"))) {
      mvc.perform(get("/tenantadmin/1/dpa/versions").with(caller))
          .andExpect(status().isForbidden());
      mvc.perform(get("/tenant").with(caller)).andExpect(status().isForbidden());
    }
  }

  @Test
  void groupedOptInPreservesTheBoundLegacyTechnicalReadContract() throws Exception {
    var legacy =
        actor("test-technical-service-subject", "app", List.of("technical"), "tenantservice");
    mvc.perform(get("/tenant/1").with(legacy)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/1/dpa/gate").with(legacy)).andExpect(status().isOk());
  }

  private RequestPostProcessor actor(
      String subject, String client, List<String> roles, String audience) {
    return jwt()
        .jwt(
            token ->
                token
                    .issuer("https://identity.example/realms/test")
                    .subject(subject)
                    .claim("azp", client)
                    .audience(List.of(audience))
                    .issuedAt(Instant.now().minusSeconds(10))
                    .expiresAt(Instant.now().plusSeconds(60))
                    .claim("tenantId", 0L)
                    .claim("realm_access", Map.of("roles", roles)))
        .authorities(token -> converter.convert(token).getAuthorities());
  }

  private String policyProof(long tenantId) throws Exception {
    long now = Instant.now().getEpochSecond();
    var claims = new java.util.TreeMap<String, Object>();
    claims.put("aud", "tenantservice");
    claims.put("azp", "backend-setup");
    claims.put("exp", now + 60);
    claims.put("iat", now);
    claims.put("iss", "oriso-userservice");
    claims.put("nonce", java.util.UUID.randomUUID().toString());
    claims.put("operation", "wizard.account-policy.read");
    claims.put("sub", "setup-subject");
    claims.put("tenantId", tenantId);
    claims.put("tokenIssuer", "https://identity.example/realms/test");
    claims.put("v", 1);
    String payload =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(claims));
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256"));
    return payload
        + "."
        + java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
  }
}
