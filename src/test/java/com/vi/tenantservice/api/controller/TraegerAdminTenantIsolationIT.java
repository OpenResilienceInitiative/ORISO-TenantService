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
import com.vi.tenantservice.api.authorisation.RoleAuthorizationAuthorityMapper;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Every Träger admin holds the realm role {@code tenant-admin} (UserService {@code
 * CreateAdminService#getUserRolesForTenantAdmin}), exactly like the platform admin. Only the {@code
 * tenantId} claim tells them apart: {@code "0"} for the platform, the Träger's own id otherwise.
 * The tokens here carry the real role set and a String claim, and authorities come from the
 * production {@link RoleAuthorizationAuthorityMapper}, so {@code GET_ALL_TENANTS} is present
 * exactly as in a live token.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class TraegerAdminTenantIsolationIT {

  private static final long OWN_TENANT = 1L;
  private static final long FOREIGN_TENANT = 2L;

  @Autowired private WebApplicationContext context;
  @Autowired private TenantRepository tenantRepository;

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
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
  }

  @Test
  void traegerAdmin_Should_notReadAnotherTraegersData() throws Exception {
    var caller = traegerAdminOf(OWN_TENANT);
    for (String path :
        List.of(
            "/tenantadmin/" + FOREIGN_TENANT,
            "/tenant/" + FOREIGN_TENANT,
            "/tenantadmin/" + FOREIGN_TENANT + "/legal-drafts/IMPRINT",
            "/tenantadmin/" + FOREIGN_TENANT + "/dpa/signatures",
            "/tenantadmin/" + FOREIGN_TENANT + "/dpa/versions",
            "/tenantadmin/" + FOREIGN_TENANT + "/dpa/status",
            "/tenantadmin/" + FOREIGN_TENANT + "/dpa/gate",
            "/tenantadmin/" + FOREIGN_TENANT + "/permission-policies")) {
      mvc.perform(get(path).with(caller))
          .andExpect(
              result ->
                  assertThat(result.getResponse().getStatus())
                      .as("GET %s as Träger admin of tenant %d", path, OWN_TENANT)
                      .isEqualTo(403));
    }
  }

  @Test
  void traegerAdmin_Should_notChangeOrDeleteAnotherTraeger() throws Exception {
    var caller = traegerAdminOf(OWN_TENANT);

    mvc.perform(
            put("/tenantadmin/" + FOREIGN_TENANT + "/permission-policies")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\":" + FOREIGN_TENANT + ",\"policies\":{}}"))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/tenant/" + FOREIGN_TENANT).with(caller)).andExpect(status().isForbidden());

    assertThat(tenantRepository.findById(FOREIGN_TENANT)).isPresent();
  }

  @Test
  void traegerAdmin_Should_notListEveryTraeger() throws Exception {
    var caller = traegerAdminOf(OWN_TENANT);

    mvc.perform(get("/tenantadmin").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenant").with(caller)).andExpect(status().isForbidden());
  }

  @Test
  void traegerAdmin_Should_notReadOrChangePlatformDpiaMasterData() throws Exception {
    var caller = traegerAdminOf(OWN_TENANT);

    mvc.perform(get("/tenantadmin/dpia").with(caller)).andExpect(status().isForbidden());
    mvc.perform(put("/tenantadmin/dpia").with(caller).contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void traegerAdmin_Should_keepReadingItsOwnTraeger() throws Exception {
    var caller = traegerAdminOf(OWN_TENANT);

    mvc.perform(get("/tenantadmin/" + OWN_TENANT).with(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(OWN_TENANT));
    mvc.perform(get("/tenant/" + OWN_TENANT).with(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(OWN_TENANT));
    mvc.perform(get("/tenantadmin/" + OWN_TENANT + "/permission-policies").with(caller))
        .andExpect(status().isOk());
  }

  @Test
  void platformAdmin_Should_keepReadingEveryTraeger() throws Exception {
    var caller = platformAdmin();

    mvc.perform(get("/tenantadmin/" + FOREIGN_TENANT).with(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(FOREIGN_TENANT));
    mvc.perform(get("/tenant/" + FOREIGN_TENANT).with(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(FOREIGN_TENANT));
    mvc.perform(get("/tenantadmin/" + FOREIGN_TENANT + "/permission-policies").with(caller))
        .andExpect(status().isOk());
    mvc.perform(get("/tenantadmin").with(caller)).andExpect(status().isOk());
    mvc.perform(get("/tenant").with(caller)).andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/dpia").with(caller)).andExpect(status().isOk());
  }

  @Test
  void legacyTenant_Should_beActiveInManagementAndPublicReads() throws Exception {
    mvc.perform(get("/tenantadmin/" + OWN_TENANT).with(traegerAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(get("/tenant/" + OWN_TENANT).with(traegerAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void traegerAdmin_Should_pauseAndReactivateOwnTenantWithoutLosingManagementAccess()
      throws Exception {
    var caller = traegerAdminOf(OWN_TENANT);
    String original = readTenant(OWN_TENANT, caller);
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT)
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content(withActive(original, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(get("/tenantadmin/" + OWN_TENANT).with(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT)
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content(withActive(original, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void platformAdmin_Should_notPauseTenantItDoesNotAdminister() throws Exception {
    var caller = platformAdmin();
    String original = readTenant(OWN_TENANT, caller);
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT)
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content(withActive(original, false)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/" + OWN_TENANT).with(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void legacyUpdate_Should_keepPausedStatusWhenActiveIsOmittedOrNull() throws Exception {
    var owner = traegerAdminOf(OWN_TENANT);
    String original = readTenant(OWN_TENANT, owner);
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT)
                .with(owner)
                .contentType(APPLICATION_JSON)
                .content(withActive(original, false)))
        .andExpect(status().isOk());
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(original);
    json.remove("active");
    for (boolean explicitNull : List.of(false, true)) {
      if (explicitNull) json.putNull("active");
      mvc.perform(
              put("/tenantadmin/" + OWN_TENANT)
                  .with(platformAdmin())
                  .contentType(APPLICATION_JSON)
                  .content(mapper.writeValueAsString(json)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(false));
      mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(false));
    }
    mvc.perform(get("/tenantadmin").with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              var listed = mapper.readTree(result.getResponse().getContentAsString());
              var ownTenant =
                  java.util.stream.StreamSupport.stream(listed.spliterator(), false)
                      .filter(tenant -> tenant.path("id").asLong() == OWN_TENANT)
                      .findFirst()
                      .orElseThrow();
              assertThat(ownTenant.get("active").booleanValue()).isFalse();
            });
    mvc.perform(get("/tenantadmin/search").param("query", "Happylife").with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded[0].active").value(false));
  }

  @Test
  void foreignTenantAdmin_Should_notChangeOperationalStatus() throws Exception {
    String original = readTenant(OWN_TENANT, platformAdmin());
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT)
                .with(traegerAdminOf(FOREIGN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(withActive(original, false)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void matchingTenantWithoutAdminRole_Should_notChangeOperationalStatus() throws Exception {
    String original = readTenant(OWN_TENANT, platformAdmin());
    var caller =
        jwt()
            .jwt(
                token ->
                    token
                        .claim("tenantId", "1")
                        .claim("realm_access", Map.of("roles", List.of("consultant"))))
            .authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "AUTHORIZATION_UPDATE_TENANT"));
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT)
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content(withActive(original, false)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void createTenant_Should_rejectPausedInitialStatusInsteadOfIgnoringIt() throws Exception {
    mvc.perform(
            post("/tenantadmin")
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"name\":\"Paused tenant\",\"subdomain\":\"paused-tenant\",\"active\":false}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/tenant/public/paused-tenant")).andExpect(status().isNotFound());
  }

  @Test
  void operationalStatusCommand_Should_changeOnlyStatusAndPreserveTenantMetadata()
      throws Exception {
    var owner = traegerAdminOf(OWN_TENANT);
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var before =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(readTenant(OWN_TENANT, owner));
    mvc.perform(
            put("/tenantadmin/" + OWN_TENANT + "/status")
                .with(owner)
                .contentType(APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(OWN_TENANT))
        .andExpect(jsonPath("$.active").value(false));
    var after =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(readTenant(OWN_TENANT, owner));
    assertThat(after.get("active").booleanValue()).isFalse();
    before.remove(List.of("active", "updateDate"));
    after.remove(List.of("active", "updateDate"));
    assertThat(after).isEqualTo(before);
    mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void operationalStatusCommand_Should_rejectUnscopedActorsWithoutChangingSavedStatus()
      throws Exception {
    for (var caller : List.of(platformAdmin(), traegerAdminOf(FOREIGN_TENANT))) {
      mvc.perform(
              put("/tenantadmin/" + OWN_TENANT + "/status")
                  .with(caller)
                  .contentType(APPLICATION_JSON)
                  .content("{\"active\":false}"))
          .andExpect(status().isForbidden());
      mvc.perform(get("/tenant/public/id/" + OWN_TENANT))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(true));
    }
  }

  @Test
  void operationalStatusCommand_Should_requireBooleanAndPermitOwnTenantReactivation()
      throws Exception {
    var owner = traegerAdminOf(OWN_TENANT);
    for (String invalid : List.of("{}", "{\"active\":null}")) {
      mvc.perform(
              put("/tenantadmin/" + OWN_TENANT + "/status")
                  .with(owner)
                  .contentType(APPLICATION_JSON)
                  .content(invalid))
          .andExpect(status().isBadRequest());
    }
    for (boolean active : List.of(false, true)) {
      mvc.perform(
              put("/tenantadmin/" + OWN_TENANT + "/status")
                  .with(owner)
                  .contentType(APPLICATION_JSON)
                  .content("{\"active\":" + active + "}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(active));
      mvc.perform(get("/tenantadmin/" + OWN_TENANT).with(owner))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(active));
    }
  }

  private String readTenant(long tenantId, RequestPostProcessor caller) throws Exception {
    return mvc.perform(get("/tenantadmin/" + tenantId).with(caller))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String withActive(String body, boolean active) throws Exception {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(body);
    json.put("active", active);
    return mapper.writeValueAsString(json);
  }

  private static RequestPostProcessor traegerAdminOf(long tenantId) {
    return tenantAdminToken("traeger-admin-" + tenantId, String.valueOf(tenantId));
  }

  private static RequestPostProcessor platformAdmin() {
    return tenantAdminToken("platform-admin", "0");
  }

  /**
   * Role set and String tenant claim as Keycloak issues them for an admin created by UserService.
   */
  private static RequestPostProcessor tenantAdminToken(String username, String tenantIdClaim) {
    Set<String> realmRoles =
        Set.of(
            "default-roles-online-beratung",
            "user-admin",
            "agency-admin",
            "tenant-admin",
            "topic-admin");
    return jwt()
        .jwt(
            token ->
                token
                    .subject(username + "-subject")
                    .claim("tenantId", tenantIdClaim)
                    .claim("username", username)
                    .claim("realm_access", Map.of("roles", List.copyOf(realmRoles))))
        .authorities(new RoleAuthorizationAuthorityMapper().mapAuthorities(realmRoles));
  }
}
