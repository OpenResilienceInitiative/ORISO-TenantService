package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.config.security.JwtAuthConverter;
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

/**
 * The Statistik-Leser (realm role {@code statistics-reader}, platform tenant 0) may read the Träger
 * names for the statistics panel and nothing else. Authorities come from the production {@link
 * JwtAuthConverter}, exactly as for a live token (ORISO-UserService#1666).
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class StatisticsReaderTenantNamesIT {

  private static final String NAMES = "/tenantadmin/statistics/names";
  private static final String DEFAULT_ROLES = "default-roles-online-beratung";

  @Autowired private WebApplicationContext context;
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
  void statisticsReader_Should_readEveryTraegerNameExceptTheTechnicalTenantSortedByName()
      throws Exception {
    mvc.perform(get(NAMES).with(statisticsReader("0")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "[{\"id\":2,\"name\":\"Another tenant\"},"
                        + "{\"id\":1,\"name\":\"Happylife Gmbh\"},"
                        + "{\"id\":3,\"name\":\"localhost tenant\"}]",
                    org.springframework.test.json.JsonCompareMode.STRICT));
  }

  @Test
  void statisticsReader_Should_receiveIdAndNameOnly() throws Exception {
    mvc.perform(get(NAMES).with(statisticsReader("0")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].*", hasSize(2)));
  }

  @Test
  void platformAdmin_Should_readTraegerNames() throws Exception {
    mvc.perform(get(NAMES).with(token("platform-admin", "0", "tenant-admin", "agency-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  void statisticsReader_Should_beRefused_When_tokenNamesAnotherTenant() throws Exception {
    mvc.perform(get(NAMES).with(statisticsReader("1"))).andExpect(status().isForbidden());
  }

  @Test
  void statisticsReader_Should_beRefused_When_tokenHasNoTenantClaim() throws Exception {
    mvc.perform(get(NAMES).with(statisticsReader(null))).andExpect(status().isForbidden());
  }

  @Test
  void everyOtherRole_Should_beRefused() throws Exception {
    for (RequestPostProcessor caller :
        List.of(
            token("traeger-admin", "1", "tenant-admin", "agency-admin", "user-admin"),
            token("single-traeger-admin", "1", "single-tenant-admin"),
            token("agency-admin", "1", "agency-admin", "restricted-agency-admin"),
            token("consultant-admin", "1", "restricted-consultant-admin"),
            token("consultant", "1", "consultant"),
            token("asker", "1", "user"),
            token("platform-agency-admin-only", "0", "agency-admin"))) {
      mvc.perform(get(NAMES).with(caller))
          .andExpect(
              result ->
                  assertThat(result.getResponse().getStatus()).as("GET names").isEqualTo(403));
    }
  }

  @Test
  void anonymous_Should_beUnauthorized() throws Exception {
    mvc.perform(get(NAMES)).andExpect(status().isUnauthorized());
  }

  @Test
  void statisticsReader_Should_beRefusedOnEveryOtherTenantEndpoint() throws Exception {
    var reader = statisticsReader("0");
    for (String path :
        List.of(
            "/tenantadmin/search?query=*",
            "/tenantadmin",
            "/tenantadmin/1",
            "/tenant",
            "/tenant/1",
            "/tenantadmin/dpia",
            "/tenantadmin/controls",
            "/tenantadmin/1/permission-policies",
            "/tenantadmin/1/dpa/signatures",
            "/tenantadmin/1/legal-versions?kind=IMPRINT",
            "/tenantadmin/1/legal-drafts/IMPRINT")) {
      mvc.perform(get(path).with(reader))
          .andExpect(
              result ->
                  assertThat(result.getResponse().getStatus())
                      .as("GET %s as statistics reader", path)
                      .isEqualTo(403));
    }
    var validTenant =
        new com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder()
            .withName("reader traeger")
            .withSubdomain("readertraeger")
            .withLicensing()
            .jsonify();
    mvc.perform(
            post("/tenantadmin").with(reader).contentType(APPLICATION_JSON).content(validTenant))
        .andExpect(status().isForbidden());
    String existing =
        mvc.perform(get("/tenantadmin/1").with(token("platform-admin", "0", "tenant-admin")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(put("/tenantadmin/1").with(reader).contentType(APPLICATION_JSON).content(existing))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/tenant/1").with(reader)).andExpect(status().isForbidden());
  }

  private RequestPostProcessor statisticsReader(String tenantIdClaim) {
    return token("statistics-reader", tenantIdClaim, "statistics-reader");
  }

  private RequestPostProcessor token(String username, String tenantIdClaim, String... roles) {
    var realmRoles = new java.util.ArrayList<>(List.of(roles));
    realmRoles.add(DEFAULT_ROLES);
    return jwt()
        .jwt(
            token -> {
              token
                  .subject(username + "-subject")
                  .claim("username", username)
                  .claim("realm_access", Map.of("roles", realmRoles));
              if (tenantIdClaim != null) {
                token.claim("tenantId", tenantIdClaim);
              }
            })
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
  }
}
