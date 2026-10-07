package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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

/**
 * The UserService calls TenantService as the service identity with realm role {@code technical}
 * (ORISO-Helm#367). That identity gets exactly the narrow machine authorities its calls need, so it
 * no longer depends on the broad {@code tenant-admin} role. A token that carries {@code
 * tenant-admin} in addition (current Staging grant) must keep working exactly as before.
 *
 * <p>Authorities come from the production {@link JwtAuthConverter}, so the technical-only
 * authorities are granted only to the configured service subject, exactly as for a real token.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "ORISO_WIZARD_POLICY_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "ORISO_TENANT_CREATION_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "IDENTITY_CONFIG_WIZARD_CLIENT_ID=backend-config-wizard",
      "IDENTITY_CONFIG_WIZARD_SERVICE_SUBJECT=wizard-subject",
      "IDENTITY_INVITE_RESERVATIONS_CLIENT_ID=backend-invite-reservations",
      "IDENTITY_INVITE_RESERVATIONS_SERVICE_SUBJECT=reservation-subject",
      "IDENTITY_RUNTIME_POLICY_CLIENT_ID=backend-runtime-policy",
      "IDENTITY_RUNTIME_POLICY_SERVICE_SUBJECT=policy-subject",
      "IDENTITY_NOTIFICATION_DISPATCH_CLIENT_ID=backend-notification-dispatch",
      "IDENTITY_NOTIFICATION_DISPATCH_SERVICE_SUBJECT=dispatch-subject"
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class TechnicalServiceIdentityAuthorizationIT {

  private static final long RESERVED_ID = 500L;
  private static final long FREE_ID = 501L;
  // technical.service.subject in application-testing.properties
  private static final String SERVICE_SUBJECT = "test-technical-service-subject";
  private static final String FOREIGN_SUBJECT = "someone-else-with-the-technical-role";

  @Autowired private WebApplicationContext context;
  @Autowired private TenantIdAllocationService tenantIdAllocationService;
  @Autowired private TenantIdReservationRepository reservationRepository;
  @Autowired private JwtAuthConverter jwtAuthConverter;

  @MockitoBean private ApplicationSettingsService applicationSettingsService;

  @MockitoBean
  private ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

  @MockitoBean
  private ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean
  private com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  @MockitoBean private SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean private ConsultingTypeService consultingTypeService;
  @MockitoBean private UserAdminService userAdminService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    reservationRepository.deleteAll();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
  }

  @AfterEach
  void tearDown() {
    reservationRepository.deleteAll();
  }

  // ---- (a) reads the UserService makes as technical ----

  @Test
  void technicalOnly_Should_readAnySingleTenantsDataDpaAndPolicies() throws Exception {
    assertOnboardingReadsSucceed(technicalOnly());
  }

  @Test
  void technicalWithTenantAdmin_Should_keepReadingExactlyAsBefore() throws Exception {
    assertOnboardingReadsSucceed(technicalWithTenantAdmin());
  }

  private void assertOnboardingReadsSucceed(RequestPostProcessor caller) throws Exception {
    for (long tenantId : List.of(1L, 2L)) {
      mvc.perform(get("/tenant/" + tenantId).with(caller))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(tenantId));
      mvc.perform(get("/tenantadmin/" + tenantId + "/dpa/versions").with(caller))
          .andExpect(status().isOk());
      mvc.perform(get("/tenantadmin/" + tenantId + "/dpa/signatures").with(caller))
          .andExpect(status().isOk());
      mvc.perform(get("/tenantadmin/" + tenantId + "/permission-policies").with(caller))
          .andExpect(status().isOk());
    }
  }

  // ---- (b) createTenant during public onboarding ----

  @Test
  void technicalOnly_Should_createTenantWithAValidReservationToken() throws Exception {
    String token = reserve(RESERVED_ID);

    mvc.perform(createTenant(technicalOnly(), RESERVED_ID, token, "reserved"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(RESERVED_ID));

    assertThat(reservationRepository.findById(RESERVED_ID))
        .get()
        .extracting(reservation -> reservation.getStatus())
        .isEqualTo(TenantIdReservationStatus.ASSIGNED);
  }

  @Test
  void technicalWithTenantAdmin_Should_createTenantWithAValidReservationTokenAsBefore()
      throws Exception {
    String token = reserve(RESERVED_ID);

    mvc.perform(createTenant(technicalWithTenantAdmin(), RESERVED_ID, token, "reserved"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(RESERVED_ID));
  }

  @Test
  void technicalWithTenantAdmin_Should_stillCreateWithoutReservationAsBefore() throws Exception {
    mvc.perform(createTenant(technicalWithTenantAdmin(), null, null, "automode"))
        .andExpect(status().isOk());
  }

  @Test
  void technicalOnly_Should_beForbiddenToCreateWithoutAnyReservation() throws Exception {
    mvc.perform(createTenant(technicalOnly(), null, null, "automode"))
        .andExpect(status().isForbidden());
  }

  @Test
  void technicalOnly_Should_beForbiddenToCreateAFreeIdWithoutReservation() throws Exception {
    mvc.perform(createTenant(technicalOnly(), FREE_ID, null, "freeid"))
        .andExpect(status().isForbidden());
    mvc.perform(createTenant(technicalOnly(), FREE_ID, "made-up-token", "freeid"))
        .andExpect(status().isForbidden());
  }

  @Test
  void technicalOnly_Should_notConsumeAReservationWithoutItsToken() throws Exception {
    reserve(RESERVED_ID);

    mvc.perform(createTenant(technicalOnly(), RESERVED_ID, null, "notoken"))
        .andExpect(status().isForbidden());
    mvc.perform(createTenant(technicalOnly(), RESERVED_ID, "wrong-token", "wrongtoken"))
        .andExpect(status().isConflict());

    assertThat(reservationRepository.findById(RESERVED_ID))
        .get()
        .extracting(reservation -> reservation.getStatus())
        .isEqualTo(TenantIdReservationStatus.RESERVED);
  }

  // ---- (c) reservation cleanup ----

  @Test
  void technicalOnly_Should_releaseAnUnconsumedReservation() throws Exception {
    reserve(RESERVED_ID);

    mvc.perform(delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID).with(technicalOnly()))
        .andExpect(status().isNoContent());

    assertThat(reservationRepository.findById(RESERVED_ID)).isEmpty();
  }

  @Test
  void technicalWithTenantAdmin_Should_releaseAnUnconsumedReservationAsBefore() throws Exception {
    reserve(RESERVED_ID);

    mvc.perform(
            delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID)
                .with(technicalWithTenantAdmin()))
        .andExpect(status().isNoContent());
  }

  // ---- the role alone is not the service identity ----

  @Test
  void foreignSubjectWithTechnicalRole_Should_beForbiddenToRead() throws Exception {
    var caller = foreignSubjectWithTechnicalRole();

    mvc.perform(get("/tenant/1").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/dpa/versions").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/permission-policies").with(caller))
        .andExpect(status().isForbidden());
  }

  @Test
  void foreignSubjectWithTechnicalRole_Should_beForbiddenToReleaseOrConsumeAReservation()
      throws Exception {
    String token = reserve(RESERVED_ID);
    var caller = foreignSubjectWithTechnicalRole();

    mvc.perform(delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID).with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(createTenant(caller, RESERVED_ID, token, "foreign"))
        .andExpect(status().isForbidden());

    assertThat(reservationRepository.findById(RESERVED_ID))
        .get()
        .extracting(reservation -> reservation.getStatus())
        .isEqualTo(TenantIdReservationStatus.RESERVED);
  }

  // ---- everything else stays closed to the technical-only identity ----

  @Test
  void technicalOnly_Should_beForbiddenEverywhereElse() throws Exception {
    var caller = technicalOnly();

    mvc.perform(get("/tenant").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/search").param("query", "*").with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1").with(caller)).andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withName("changed")
                        .withSubdomain("changed")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1/permission-policies")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\":1,\"policies\":{}}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/dpia").with(caller)).andExpect(status().isForbidden());
    mvc.perform(put("/tenantadmin/dpia").with(caller).contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/legal-drafts/PRIVACY").with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1/legal-drafts/PRIVACY")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{\"content\":{\"de\":\"<p>x</p>\"},\"revision\":\"new\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1/dpa")
                .with(caller)
                .queryParam("signingDeadlineAt", "2099-10-15T15:00:00Z")
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/tenantadmin/tenant-ids/reservations")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/controls").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/translation/keys").with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/tenant/1").with(caller)).andExpect(status().isForbidden());
  }

  @Test
  void wizardCreatesOnlyItsReservedTenantAndCannotReleaseOrList() throws Exception {
    var wizard = task("wizard-subject", "backend-config-wizard", "config-wizard", "tenantservice");
    String token = reserve(RESERVED_ID);
    mvc.perform(createTenant(wizard, RESERVED_ID, token, "wizardtenant"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(RESERVED_ID));
    mvc.perform(createTenant(wizard, FREE_ID, null, "unreserved"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenant").with(wizard)).andExpect(status().isForbidden());
    mvc.perform(delete("/tenantadmin/tenant-ids/reservations/501").with(wizard))
        .andExpect(status().isForbidden());
  }

  @Test
  void runtimePolicyReadsOnlyGateAndPolicies() throws Exception {
    var policy =
        task("policy-subject", "backend-runtime-policy", "runtime-policy", "tenantservice");
    mvc.perform(get("/tenantadmin/1/dpa/gate").with(policy)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/1/permission-policies").with(policy)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/1/dpa/signatures").with(policy))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenant/1").with(policy)).andExpect(status().isForbidden());
    mvc.perform(
            get("/tenantadmin/1/dpa/gate")
                .with(task("foreign", "backend-runtime-policy", "runtime-policy", "tenantservice")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/tenantadmin/1/dpa/gate")
                .with(task("policy-subject", "wrong-client", "runtime-policy", "tenantservice")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/tenantadmin/1/dpa/gate")
                .with(
                    task(
                        "policy-subject",
                        "backend-runtime-policy",
                        "runtime-policy",
                        "agencyservice")))
        .andExpect(status().isForbidden());
  }

  @Test
  void reservationWorkerReleasesOnlyMatchingProofAndCannotReserveOrCreate() throws Exception {
    String own = reserve(RESERVED_ID);
    reserve(FREE_ID);
    var caller =
        task(
            "reservation-subject",
            "backend-invite-reservations",
            "invitation-reservations",
            "tenantservice");
    mvc.perform(get("/tenantadmin/tenant-ids/" + RESERVED_ID + "/availability").with(caller))
        .andExpect(status().isOk());
    mvc.perform(
            delete("/tenantadmin/tenant-ids/reservations/" + FREE_ID)
                .param("reservationToken", own)
                .with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID).with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID)
                .param("reservationToken", own)
                .with(caller))
        .andExpect(status().isNoContent());
    assertThat(reservationRepository.findById(FREE_ID)).isPresent();
    mvc.perform(
            post("/tenantadmin/tenant-ids/reservations")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(createTenant(caller, FREE_ID, own, "invalidworker"))
        .andExpect(status().isForbidden());
  }

  @Test
  void wizardReadsOnlyOperatorDpaAndDispatcherReceivesLimitedMailContext() throws Exception {
    var wizard = task("wizard-subject", "backend-config-wizard", "config-wizard", "tenantservice");
    mvc.perform(get("/tenantadmin/1/dpa/versions").with(wizard)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/2/dpa/versions").with(wizard)).andExpect(status().isForbidden());
    var dispatch =
        task(
            "dispatch-subject",
            "backend-notification-dispatch",
            "notification-dispatch",
            "tenantservice");
    mvc.perform(get("/internal/tenants/2/system-email-context").with(dispatch))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.settings.featureSystemNotificationEmailsEnabled").exists())
        .andExpect(jsonPath("$.settings.smtpMode").exists())
        .andExpect(jsonPath("$.settings.smtp.password").doesNotExist())
        .andExpect(jsonPath("$.settings.smtp.username").doesNotExist())
        .andExpect(jsonPath("$.settings.smtp.host").doesNotExist())
        .andExpect(jsonPath("$.settings.featureAppointmentsEnabled").doesNotExist());
    mvc.perform(get("/tenant/2").with(dispatch)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/2/dpa/signatures").with(dispatch)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/2/dpa/gate").with(dispatch)).andExpect(status().isForbidden());
    mvc.perform(get("/internal/tenants/2/system-email-context").with(wizard))
        .andExpect(status().isForbidden());
  }

  @Test
  void taskTokenWithInheritedHumanOrTechnicalRoleCannotUseLegacyBranches() throws Exception {
    for (String role : List.of("config-wizard,tenant-admin", "config-wizard,technical")) {
      var caller = task("wizard-subject", "backend-config-wizard", role, "tenantservice");
      mvc.perform(get("/tenant/2").with(caller)).andExpect(status().isForbidden());
      mvc.perform(createTenant(caller, null, null, "inherited")).andExpect(status().isForbidden());
    }
  }

  private RequestPostProcessor task(String subject, String client, String role, String audience) {
    return jwt()
        .jwt(
            token ->
                token
                    .issuer("https://identity.example/realms/test")
                    .subject(subject)
                    .claim("azp", client)
                    .audience(List.of(audience))
                    .issuedAt(java.time.Instant.now().minusSeconds(10))
                    .expiresAt(java.time.Instant.now().plusSeconds(60))
                    .claim("tenantId", 0L)
                    .claim(
                        "realm_access", Map.of("roles", java.util.Arrays.asList(role.split(",")))))
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
  }

  @Test
  void wizardPolicyRequiresSignedOwnedTargetAndExcludesOtherTenantData() throws Exception {
    var wizard = task("wizard-subject", "backend-config-wizard", "config-wizard", "tenantservice");
    String path = "/internal/tenants/1/account-provisioning-policy";
    mvc.perform(get(path).with(wizard).header("X-ORISO-Wizard-Policy-Context", policyProof(1, 0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").doesNotExist())
        .andExpect(jsonPath("$.settings").doesNotExist());
    mvc.perform(get(path).with(wizard).header("tenantId", "1")).andExpect(status().isForbidden());
    mvc.perform(get(path).with(wizard).header("X-ORISO-Wizard-Policy-Context", policyProof(2, 0)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(path).with(wizard).header("X-ORISO-Wizard-Policy-Context", policyProof(1, -120)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(path).with(wizard).header("X-ORISO-Wizard-Policy-Context", policyProof(1, 0) + "x"))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(path)
                .with(
                    task(
                        "dispatch-subject",
                        "backend-notification-dispatch",
                        "notification-dispatch",
                        "tenantservice"))
                .header("X-ORISO-Wizard-Policy-Context", policyProof(1, 0)))
        .andExpect(status().isForbidden());
  }

  private String policyProof(long tenantId, long timeOffset) throws Exception {
    long issued = java.time.Instant.now().getEpochSecond() + timeOffset;
    var claims = new java.util.TreeMap<String, Object>();
    claims.put("aud", "tenantservice");
    claims.put("azp", "backend-config-wizard");
    claims.put("exp", issued + 60);
    claims.put("iat", issued);
    claims.put("iss", "oriso-userservice");
    claims.put("nonce", java.util.UUID.randomUUID().toString());
    claims.put("operation", "wizard.account-policy.read");
    claims.put("sub", "wizard-subject");
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

  private String reserve(long tenantId) {
    return tenantIdAllocationService.reserve(tenantId, "test").getToken();
  }

  private org.springframework.test.web.servlet.RequestBuilder createTenant(
      RequestPostProcessor caller, Long id, String reservationToken, String subdomain) {
    var builder =
        new MultilingualTenantTestDataBuilder()
            .withName("onboarded " + subdomain)
            .withSubdomain(subdomain)
            .withLicensing();
    if (id != null) {
      builder.withId(id);
    }
    if (reservationToken != null) {
      builder.withTenantIdReservationToken(reservationToken);
    }
    return post("/tenantadmin")
        .with(caller)
        .contentType(APPLICATION_JSON)
        .content(builder.jsonify());
  }

  private RequestPostProcessor technicalOnly() {
    return caller(SERVICE_SUBJECT, Set.of("default-roles-online-beratung", "technical"));
  }

  private RequestPostProcessor technicalWithTenantAdmin() {
    return caller(
        SERVICE_SUBJECT, Set.of("default-roles-online-beratung", "technical", "tenant-admin"));
  }

  private RequestPostProcessor foreignSubjectWithTechnicalRole() {
    return caller(FOREIGN_SUBJECT, Set.of("default-roles-online-beratung", "technical"));
  }

  private RequestPostProcessor caller(String subject, Set<String> realmRoles) {
    return jwt()
        .jwt(
            token ->
                token
                    .issuer("https://identity.example/realms/test")
                    .subject(subject)
                    .claim("azp", "app")
                    .claim("tenantId", 0L)
                    .claim("username", "technical")
                    .claim("realm_access", Map.of("roles", List.copyOf(realmRoles))))
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
  }
}
