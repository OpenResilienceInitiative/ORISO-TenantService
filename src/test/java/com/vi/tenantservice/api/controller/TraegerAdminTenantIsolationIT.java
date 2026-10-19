package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
