package com.vi.tenantservice.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.RoleAuthorizationAuthorityMapper;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * TenantService#345: the platform admin's switch "organisation admins may edit" is enforced on the
 * server for everything the Admin tile promises - name, slogan, languages, form of address and
 * branding - not only for the theming values. Every request goes through the real controller, the
 * real authorisation and the real stored switch (PUT /tenantadmin/controls).
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class OrganisationAdminEditingLockIT {

  private static final long TENANT = 1L;
  private static final String REFUSED = "NOT_ALLOWED_TO_CHANGE_APPEARANCE";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired private WebApplicationContext context;

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
  void setUp() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
    // The Träger's form of address lives in the consulting type: informal today.
    when(consultingTypeService.getConsultingTypesByTenantId((int) TENANT))
        .thenReturn(new FullConsultingTypeResponseDTO().id(7).languageFormal(false));
    setEditingAllowedForOrganisationAdmins(true);
    storeAssetsTheSanitiserAccepts();
  }

  @AfterEach
  void restoreSwitch() throws Exception {
    setEditingAllowedForOrganisationAdmins(true);
  }

  @Test
  void switchOff_Should_beStoredOnTheServerAndReadBackFromThere() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);

    mvc.perform(get("/tenantadmin/controls").with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionPolicies.appearance.value").value(false))
        .andExpect(jsonPath("$.permissionPolicies.appearance.mode").value("ENFORCED"));
  }

  @Test
  void switchOff_Should_refuseAnOrganisationAdminChangingTheName() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);

    refused(save(traeger(), edit(read(), "name", "Renamed by organisation admin")));
  }

  @Test
  void switchOff_Should_refuseAnOrganisationAdminChangingTheSlogan() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode body = read();
    body.withObject("/content/claim").put("de", "A new slogan");

    refused(save(traeger(), body));
  }

  @Test
  void switchOff_Should_refuseAnOrganisationAdminChangingTheLanguages() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode body = read();
    languages(body, "de", "en");

    refused(save(traeger(), body));
  }

  @Test
  void switchOff_Should_refuseAnOrganisationAdminChangingTheFormOfAddress() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode body = read();
    body.withObject("/settings/extendedSettings").put("languageFormal", true);

    refused(save(traeger(), body));
  }

  @Test
  void switchOff_Should_refuseAnOrganisationAdminChangingTheBranding() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode body = read();
    body.withObject("/theming").put("primaryColor", "#123456");

    refused(save(traeger(), body));
  }

  @Test
  void switchOff_Should_acceptTheUnchangedWholeObjectFromAnOrganisationAdmin() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode whole = read();
    // the Admin form echoes name, slogan, languages and form of address as it read them
    languages(whole, "de");
    whole.withObject("/settings/extendedSettings").put("languageFormal", false);

    save(traeger(), whole).andExpect(status().isOk());
  }

  @Test
  void switchOff_Should_stillAllowThePlatformAdminToEditEverything() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode body = read();
    body.put("name", "Renamed by platform admin");
    body.withObject("/content/claim").put("de", "Platform slogan");
    languages(body, "de", "en");
    body.withObject("/settings/extendedSettings").put("languageFormal", true);
    body.withObject("/theming").put("primaryColor", "#123456");

    save(platformAdmin(), body).andExpect(status().isOk());
  }

  @Test
  void switchOn_Should_letTheOrganisationAdminSaveEachOfThem() throws Exception {
    setEditingAllowedForOrganisationAdmins(true);
    ObjectNode body = read();
    body.put("name", "Renamed by organisation admin");
    body.withObject("/content/claim").put("de", "Own slogan");
    languages(body, "de", "en");
    body.withObject("/settings/extendedSettings").put("languageFormal", true);
    body.withObject("/theming").put("primaryColor", "#123456");

    save(traeger(), body).andExpect(status().isOk());
  }

  @Test
  void switchOff_Should_acceptBrandingTheFormMerelyEchoedFromThePlatform() throws Exception {
    // The Admin seeds its cards from the effective tenant, so a Träger without an own logo sends
    // the platform's logo back with every save. That is no change to the Träger's branding.
    String platformLogo = "data:image/png;base64,iVBORw0KGgo=";
    ObjectNode platform = read(0);
    platform.withObject("/theming").put("logo", platformLogo);
    platform.withObject("/theming").put("primaryColor", "#112233");
    save(0, platformAdmin(), platform).andExpect(status().isOk());
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode echo = read(2);
    echo.withObject("/theming").put("logo", platformLogo);
    echo.withObject("/theming").put("primaryColor", "#112233");

    save(2, adminToken("traeger-admin-2", "2"), echo).andExpect(status().isOk());
  }

  @Test
  void switchOff_Should_acceptWhatTheSignedInAdminActuallySends() throws Exception {
    // mapTenantDataToTenantAdminData (ORISO-Admin): seeded from GET /tenant, so the slogan is one
    // string sent as {de: x}, theming is only logo, favicon, colours and association logo, and
    // there is no assistant identity, no login effect and no extendedSettings.
    setEditingAllowedForOrganisationAdmins(false);
    JsonNode tenant =
        MAPPER.readTree(
            mvc.perform(get("/tenant/" + TENANT).with(traeger()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    ObjectNode body = read();
    body.withObject("/settings").remove("extendedSettings");
    ObjectNode theming = MAPPER.createObjectNode();
    for (String field :
        List.of(
            "associationLogo",
            "logo",
            "favicon",
            "primaryColor",
            "secondaryColor",
            "accent",
            "signal")) {
      theming.set(
          field,
          tenant.path("theming").path(field).isMissingNode()
              ? null
              : tenant.path("theming").get(field));
    }
    body.set("theming", theming);
    body.withObject("/content")
        .putObject("claim")
        .put("de", tenant.path("content").path("claim").asText());

    save(traeger(), body).andExpect(status().isOk());

    ObjectNode stored = read();
    org.assertj.core.api.Assertions.assertThat(
            stored.path("theming").path("assistantName").asText())
        .isEqualTo("Helper");
    org.assertj.core.api.Assertions.assertThat(stored.path("theming").path("loginEffect").asText())
        .isEqualTo("CRACKS");
  }

  @Test
  void switchOff_Should_neverLetAFormOfAddressChangeThroughWhenItCannotBeRead() throws Exception {
    setEditingAllowedForOrganisationAdmins(false);
    ObjectNode body = read();
    String nameBefore = body.path("name").asText();
    body.withObject("/settings/extendedSettings").put("languageFormal", true);
    body.put("name", "Renamed while the check was blind");
    when(consultingTypeService.getConsultingTypesByTenantId((int) TENANT))
        .thenThrow(new org.springframework.web.client.RestClientException("consulting type down"));

    save(traeger(), body).andExpect(status().is5xxServerError());

    org.mockito.Mockito.doReturn(new FullConsultingTypeResponseDTO().id(7).languageFormal(false))
        .when(consultingTypeService)
        .getConsultingTypesByTenantId((int) TENANT);

    org.assertj.core.api.Assertions.assertThat(read().path("name").asText()).isEqualTo(nameBefore);
  }

  // --- helpers ----------------------------------------------------------------------------

  private void setEditingAllowedForOrganisationAdmins(boolean allowed) throws Exception {
    mvc.perform(
            put("/tenantadmin/controls")
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionPolicies\":{\"appearance\":{\"value\":"
                        + allowed
                        + ",\"mode\":\""
                        + (allowed ? "SUGGESTED" : "ENFORCED")
                        + "\"}}}"))
        .andExpect(status().isOk());
  }

  /** The seed rows hold placeholder text as logos, which the input sanitiser would drop. */
  private void storeAssetsTheSanitiserAccepts() throws Exception {
    ObjectNode body = read();
    body.withObject("/theming").put("logo", "data:image/png;base64,iVBORw0KGgo=");
    body.withObject("/theming").put("associationLogo", "data:image/png;base64,iVBORw0KGgo=");
    // what a real Träger has stored: assistant identity, login effect, a slogan, awkward characters
    body.withObject("/theming").put("assistantName", "Helper");
    body.withObject("/theming").put("assistantIcon", "robot-7341990");
    body.withObject("/theming").put("loginEffect", "CRACKS");
    body.put("name", "Müller & Söhne <Beratung>");
    body.withObject("/content/claim").put("de", "Ärzte & Söhne für Ü <b>fett</b> 5 < 6");
    save(platformAdmin(), body).andExpect(status().isOk());
  }

  private ObjectNode read() throws Exception {
    return read(TENANT);
  }

  private ObjectNode read(long tenant) throws Exception {
    String json =
        mvc.perform(get("/tenantadmin/" + tenant).with(platformAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return (ObjectNode) MAPPER.readTree(json);
  }

  private static void languages(ObjectNode body, String... languages) {
    var array = body.withObject("/settings").putArray("activeLanguages");
    List.of(languages).forEach(array::add);
  }

  private static ObjectNode edit(ObjectNode body, String field, String value) {
    body.put(field, value);
    return body;
  }

  private ResultActions save(RequestPostProcessor caller, ObjectNode body) throws Exception {
    return save(TENANT, caller, body);
  }

  private ResultActions save(long tenant, RequestPostProcessor caller, ObjectNode body)
      throws Exception {
    return mvc.perform(
        put("/tenantadmin/" + tenant)
            .with(caller)
            .contentType(APPLICATION_JSON)
            .content(MAPPER.writeValueAsString(body)));
  }

  private static void refused(ResultActions result) throws Exception {
    result.andExpect(status().isForbidden()).andExpect(header().string("X-Reason", REFUSED));
  }

  private static RequestPostProcessor traeger() {
    return adminToken("traeger-admin-" + TENANT, String.valueOf(TENANT));
  }

  private static RequestPostProcessor platformAdmin() {
    return adminToken("platform-admin", "0");
  }

  /** Role set and String tenant claim as Keycloak issues them for an admin UserService created. */
  private static RequestPostProcessor adminToken(String username, String tenantIdClaim) {
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
