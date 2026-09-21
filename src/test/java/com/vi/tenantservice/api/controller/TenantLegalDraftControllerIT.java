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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper = new ObjectMapper();
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
    jdbcTemplate.update("DELETE FROM tenant_legal_proposal_delivery");
    jdbcTemplate.update("DELETE FROM tenant_legal_proposal");
    jdbcTemplate.update("DELETE FROM tenant_legal_draft_archive");
    jdbcTemplate.update("DELETE FROM tenant_legal_proposal_distribution");
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
  void technicalCallerWithAPlatformShapedToken_Should_notReadOrWriteThePlatformDraft()
      throws Exception {
    // Tenant 0 plus the tenant-admin role makes isSuperAdmin() true; only the technical-user
    // exclusion keeps this caller out of the platform draft.
    mvc.perform(get("/tenantadmin/0/legal-drafts/PRIVACY").with(technicalPlatformCaller()))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/0/legal-drafts/IMPRINT")
                .with(technicalPlatformCaller())
                .contentType(APPLICATION_JSON)
                .content("{\"content\":{\"de\":\"<p>Impressum</p>\"},\"revision\":\"new\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/tenantadmin/0/legal-drafts/IMPRINT")
                .param("revision", "1:0")
                .with(technicalPlatformCaller()))
        .andExpect(status().isForbidden());
    assertThat(draftRepository.count()).isZero();
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
  void proposalNoticeCanBeDismissedReopenedAndAdoptedWithoutPublishing() throws Exception {
    String publishedBefore =
        jdbcTemplate.queryForObject(
            "SELECT content_privacy FROM tenant WHERE id = 1", String.class);
    String platformDraft =
        mvc.perform(
                put("/tenantadmin/0/legal-drafts/PRIVACY")
                    .with(platformAdmin())
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"content\":{\"de\":\"<p>Neue Plattformfassung</p>\"},"
                            + "\"privacyConsent\":{\"de\":\"Einwilligung\"},\"revision\":\"new\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String sourceRevision = objectMapper.readTree(platformDraft).get("revision").asText();
    String distribution =
        mvc.perform(
                post("/tenantadmin/legal-proposal-distributions")
                    .with(platformAdmin())
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"requestKey\":\"controller-dismiss-adopt\",\"kind\":\"PRIVACY\","
                            + "\"sourceRevision\":\""
                            + sourceRevision
                            + "\",\"audience\":\"SELECTED\",\"tenantIds\":[1]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recipientTenantIds[0]").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode proposal = objectMapper.readTree(distribution).get("proposals").get(0);
    long proposalId = proposal.get("id").asLong();
    String proposalRevision = proposal.get("revision").asText();
    String proposalUrl = "/tenantadmin/1/legal-proposals/" + proposalId;

    mvc.perform(
            post("/tenantadmin/legal-proposal-distributions")
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"requestKey\":\"controller-dismiss-adopt\",\"kind\":\"PRIVACY\","
                        + "\"sourceRevision\":\""
                        + sourceRevision
                        + "\",\"audience\":\"SELECTED\",\"tenantIds\":[1]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.proposals[0].id").value(proposalId));

    mvc.perform(get(proposalUrl).with(platformAdmin())).andExpect(status().isOk());
    mvc.perform(get(proposalUrl).with(tenantAdmin(2L))).andExpect(status().isForbidden());
    mvc.perform(get(proposalUrl).with(technicalLegalCaller(1L))).andExpect(status().isForbidden());

    String dismissed =
        mvc.perform(
                post(proposalUrl + "/dismiss")
                    .with(legalTenantAdmin(1L))
                    .contentType(APPLICATION_JSON)
                    .content("{\"expectedProposalRevision\":\"" + proposalRevision + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISMISSED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String dismissedRevision = objectMapper.readTree(dismissed).get("revision").asText();
    mvc.perform(get(proposalUrl).with(legalTenantAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISMISSED"));

    mvc.perform(
            post(proposalUrl + "/adopt")
                .with(legalTenantAdmin(1L))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"mode\":\"CREATE_IF_EMPTY\",\"expectedProposalRevision\":\""
                        + dismissedRevision
                        + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.de").value("<p>Neue Plattformfassung</p>"))
        .andExpect(jsonPath("$.privacyConsent.de").value("Einwilligung"))
        .andExpect(jsonPath("$.originProposalId").value(proposalId));
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT content_privacy FROM tenant WHERE id = 1", String.class))
        .isEqualTo(publishedBefore);
  }

  @Test
  void templateHistory_Should_listSentSnapshotsNewestFirst_forThePlatformOnly() throws Exception {
    String first = savePlatformImprint("<p>Erste Fassung</p>", "new");
    distributeImprint("history-1", first, "[1]");
    String second = savePlatformImprint("<p>Zweite Fassung</p>", first);
    distributeImprint("history-2", second, "[1,2]");

    mvc.perform(
            get("/tenantadmin/legal-proposal-distributions")
                .param("kind", "IMPRINT")
                .with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].sourceRevision").value(second))
        .andExpect(jsonPath("$[0].recipientCount").value(2))
        .andExpect(jsonPath("$[0].content.de").value("<p>Zweite Fassung</p>"))
        .andExpect(jsonPath("$[1].sourceRevision").value(first))
        .andExpect(jsonPath("$[1].recipientCount").value(1))
        .andExpect(jsonPath("$[1].content.de").value("<p>Erste Fassung</p>"));

    mvc.perform(
            get("/tenantadmin/legal-proposal-distributions")
                .param("kind", "PRIVACY")
                .with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            get("/tenantadmin/legal-proposal-distributions")
                .param("kind", "IMPRINT")
                .with(legalTenantAdmin(1L)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/tenantadmin/legal-proposal-distributions")
                .param("kind", "IMPRINT")
                .with(technicalPlatformCaller()))
        .andExpect(status().isForbidden());
  }

  @Test
  void templateHistory_Should_keepTheSentText_whenAllRecipientProposalsAreGone() throws Exception {
    String revision = savePlatformImprint("<p>Gesendet</p>", "new");
    distributeImprint("history-deleted", revision, "[1]");
    // A deleted recipient tenant cascades its proposals away.
    jdbcTemplate.update("DELETE FROM tenant_legal_proposal_delivery");
    jdbcTemplate.update("DELETE FROM tenant_legal_proposal");

    mvc.perform(
            get("/tenantadmin/legal-proposal-distributions")
                .param("kind", "IMPRINT")
                .with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].content.de").value("<p>Gesendet</p>"));
  }

  @Test
  void templateHistory_Should_putTheLaterRevisionFirst_whenBothWereSentInTheSameSecond()
      throws Exception {
    String first = savePlatformImprint("<p>Erste</p>", "new");
    distributeImprint("same-second-1", first, "[1]");
    String second = savePlatformImprint("<p>Zweite</p>", first);
    distributeImprint("same-second-2", second, "[1]");
    // MariaDB keeps whole seconds; make the tie explicit.
    jdbcTemplate.update(
        "UPDATE tenant_legal_proposal_distribution SET created_at = '2026-09-22 10:00:00'");

    for (int i = 0; i < 5; i++) {
      mvc.perform(
              get("/tenantadmin/legal-proposal-distributions")
                  .param("kind", "IMPRINT")
                  .with(platformAdmin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].sourceRevision").value(second))
          .andExpect(jsonPath("$[1].sourceRevision").value(first));
    }
  }

  private String savePlatformImprint(String html, String revision) throws Exception {
    String response =
        mvc.perform(
                put("/tenantadmin/0/legal-drafts/IMPRINT")
                    .with(platformAdmin())
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{\"content\":{\"de\":\""
                            + html
                            + "\"},\"revision\":\""
                            + revision
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("revision").asText();
  }

  private void distributeImprint(String requestKey, String revision, String tenantIds)
      throws Exception {
    mvc.perform(
            post("/tenantadmin/legal-proposal-distributions")
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"requestKey\":\""
                        + requestKey
                        + "\",\"kind\":\"IMPRINT\",\"sourceRevision\":\""
                        + revision
                        + "\",\"audience\":\"SELECTED\",\"tenantIds\":"
                        + tenantIds
                        + "}"))
        .andExpect(status().isCreated());
  }

  @Test
  void technicalPlatformShapedTokenCannotDistributeOrReadBeforeResourceLookup() throws Exception {
    RequestPostProcessor technical = technicalPlatformCaller();
    mvc.perform(
            post("/tenantadmin/legal-proposal-distributions")
                .with(technical)
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"requestKey\":\"technical-must-not-send\",\"kind\":\"PRIVACY\","
                        + "\"sourceRevision\":\"999:0\",\"audience\":\"SELECTED\","
                        + "\"tenantIds\":[1]}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/legal-proposals/999").with(technicalPlatformCaller()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/legal-draft-archives/999").with(technicalPlatformCaller()))
        .andExpect(status().isForbidden());
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

  private RequestPostProcessor legalTenantAdmin(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("username", "legal-tenant-admin-" + tenantId)
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_CHANGE_LEGAL_CONTENT"));
  }

  private RequestPostProcessor technicalLegalCaller(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("username", "technical")
                    .claim("realm_access", Map.of("roles", List.of())))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_CHANGE_LEGAL_CONTENT"));
  }

  private RequestPostProcessor technicalPlatformCaller() {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", 0L)
                    .claim("username", "technical")
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_CHANGE_LEGAL_CONTENT"));
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
