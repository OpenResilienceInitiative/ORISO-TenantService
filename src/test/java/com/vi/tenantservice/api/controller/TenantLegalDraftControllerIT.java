package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.repository.TenantLegalDraftRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class TenantLegalDraftControllerIT {

  private static final String TENANT_ONE_PRIVACY = "/tenantadmin/1/legal-drafts/PRIVACY";
  private static final String NEW_PRIVACY =
      "{\"content\":{\"de\":\"<p>Draft</p>\"},"
          + "\"privacyConsent\":{\"de\":\"Ich habe die {{legal_links}} gelesen.\"},"
          + "\"revision\":\"new\"}";

  @Autowired private WebApplicationContext context;
  @Autowired private TenantLegalDraftRepository draftRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
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
    draftRepository.deleteAll();
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
  void tenantAdmin_Should_saveAndReadOwnSanitizedDraft() throws Exception {
    mvc.perform(
            put(TENANT_ONE_PRIVACY)
                .with(tenantAdmin(1L))
                .contentType(APPLICATION_JSON)
                .content(NEW_PRIVACY.replace("</p>", "</p><script>alert(1)</script>")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revision").value(org.hamcrest.Matchers.matchesPattern("[0-9]+:0")))
        .andExpect(jsonPath("$.kind").value("PRIVACY"))
        .andExpect(jsonPath("$.updatedAt").isNotEmpty())
        .andExpect(jsonPath("$.privacyConsent.de").value("Ich habe die {{legal_links}} gelesen."))
        .andExpect(jsonPath("$.content.de").value("<p>Draft</p>"));

    mvc.perform(get(TENANT_ONE_PRIVACY).with(tenantAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kind").value("PRIVACY"))
        .andExpect(jsonPath("$.privacyConsent.de").value("Ich habe die {{legal_links}} gelesen."))
        .andExpect(jsonPath("$.content.de").value("<p>Draft</p>"));
  }

  @Test
  void tenantAdmin_Should_notReadAnotherTenantsDraft() throws Exception {
    mvc.perform(get(TENANT_ONE_PRIVACY).with(tenantAdmin(2L))).andExpect(status().isForbidden());
  }

  @Test
  void legalDraftEndpoints_Should_requireUpdateTenantAuthorityAndAuthentication() throws Exception {
    mvc.perform(get(TENANT_ONE_PRIVACY).with(jwt())).andExpect(status().isForbidden());
    mvc.perform(get(TENANT_ONE_PRIVACY)).andExpect(status().isUnauthorized());
  }

  @Test
  void legalDraftEndpoints_Should_rejectUnknownKindAsBadRequest() throws Exception {
    mvc.perform(get("/tenantadmin/1/legal-drafts/TERMS").with(tenantAdmin(1L)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void platformTenant_Should_keepItsOwnImprintDraft() throws Exception {
    String url = "/tenantadmin/0/legal-drafts/IMPRINT";
    String body = "{\"content\":{\"de\":\"<p>Platform imprint draft</p>\"},\"revision\":\"new\"}";
    jdbcTemplate.update("DELETE FROM tenant WHERE id = 0");

    mvc.perform(put(url).with(platformAdmin()).contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.de").value("<p>Platform imprint draft</p>"));
  }

  @Test
  void nonPlatformTenantAdmin_Should_notAccessPlatformDraft() throws Exception {
    mvc.perform(get("/tenantadmin/0/legal-drafts/PRIVACY").with(tenantAdmin(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  void technicalCallerWithUpdateAuthority_Should_notAccessPlatformDraft() throws Exception {
    mvc.perform(
            get("/tenantadmin/0/legal-drafts/PRIVACY")
                .with(
                    jwt()
                        .jwt(
                            token ->
                                token
                                    .claim("tenantId", 0L)
                                    .claim("username", "technical")
                                    .claim("realm_access", Map.of("roles", List.of())))
                        .authorities(new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void imprintDraft_Should_rejectPrivacyConsent() throws Exception {
    String body =
        "{\"content\":{\"de\":\"<p>Imprint</p>\"},"
            + "\"privacyConsent\":{\"de\":\"not applicable\"},\"revision\":\"new\"}";

    mvc.perform(
            put("/tenantadmin/1/legal-drafts/IMPRINT")
                .with(tenantAdmin(1L))
                .contentType(APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void traegerAdminWithoutLegalRights_Should_notWriteADraft() throws Exception {
    var settings =
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
            .ApplicationSettingsDTO();
    settings.setLegalContentChangesBySingleTenantAdminsAllowed(
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model.FeatureToggleDTO()
            .value(false));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(settings);

    mvc.perform(
            put("/tenantadmin/1/legal-drafts/IMPRINT")
                .with(traegerAdminWithoutLegalRights(1L))
                .contentType(APPLICATION_JSON)
                .content("{\"content\":{\"de\":\"<p>Impressum</p>\"},\"revision\":\"new\"}"))
        .andExpect(status().isForbidden());
    assertThat(draftRepository.count()).isZero();
  }

  private RequestPostProcessor tenantAdmin(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("username", "tenant-admin-" + tenantId)
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_CHANGE_LEGAL_CONTENT"));
  }

  private RequestPostProcessor traegerAdminWithoutLegalRights(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("username", "single-tenant-admin-" + tenantId)
                    .claim("realm_access", Map.of("roles", List.of("single-tenant-admin"))))
        .authorities(new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"));
  }

  private RequestPostProcessor platformAdmin() {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", 0L)
                    .claim("username", "platform-admin")
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS"),
            new SimpleGrantedAuthority("AUTHORIZATION_CHANGE_LEGAL_CONTENT"));
  }
}
