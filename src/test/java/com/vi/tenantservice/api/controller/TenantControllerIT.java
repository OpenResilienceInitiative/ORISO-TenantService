package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.RESTRICTED_AGENCY_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.Lists;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerIT {

  private static final String TENANT_RESOURCE = "/tenant";
  private static final String TENANT_ACCESS = TENANT_RESOURCE + "/access";

  private static final String TENANTADMIN_RESOURCE = "/tenantadmin";
  private static final String TENANT_RESOURCE_SLASH = TENANT_RESOURCE + "/";
  private static final String TENANTADMIN_SEARCH = TENANTADMIN_RESOURCE + "/search";
  private static final String TENANTADMIN_RESOURCE_SLASH = TENANTADMIN_RESOURCE + "/";
  private static final String PUBLIC_TENANT_RESOURCE = "/tenant/public/";
  private static final String PUBLIC_TENANT_RESOURCE_BY_ID = "/tenant/public/id/";
  private static final String PUBLIC_SINGLE_TENANT_RESOURCE = PUBLIC_TENANT_RESOURCE + "single";
  private static final String EXISTING_TENANT = TENANT_RESOURCE_SLASH + "1";

  private static final String EXISTING_TENANT_VIA_ADMIN = TENANTADMIN_RESOURCE_SLASH + "1";
  private static final String EXISTING_PUBLIC_TENANT = PUBLIC_TENANT_RESOURCE_BY_ID + "1";

  private static final String NON_EXISTING_TENANT_VIA_ADMIN = TENANTADMIN_RESOURCE_SLASH + "4";
  private static final String NON_EXISTING_TENANT = TENANT_RESOURCE_SLASH + "4";
  private static final String NON_EXISTING_PUBLIC_TENANT = PUBLIC_TENANT_RESOURCE_BY_ID + "4";
  private static final String AUTHORITY_WITHOUT_PERMISSIONS = "technical";
  private static final String USERNAME = "not important";
  private static final String EXISTING_SUBDOMAIN = "examplesubdomain";
  private static final String SCRIPT_CONTENT = "<script>error</script>";
  private static final int PAGE_SIZE = 3;
  private static final int CONSULTING_TYPE_ID = 2;

  @Autowired private WebApplicationContext context;

  @MockBean AuthorisationService authorisationService;

  @MockBean ApplicationSettingsService applicationSettingsService;

  @MockBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

  @MockBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockBean
  com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  @MockBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockBean TenantResolverService tenantResolverService;

  @MockBean ConsultingTypeService consultingTypeService;

  @MockBean UserAdminService userAdminService;

  @MockBean SubdomainExtractor subdomainExtractor;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    givenSingleTenantAdminCanChangeLegalTexts(true);
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));

    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
  }

  private void giveAuthorisationServiceReturnProperAuthoritiesForRole(UserRole userRole) {
    when(authorisationService.hasAuthority(Mockito.any()))
        .thenAnswer(
            invocation ->
                Authority.getAuthoritiesByUserRole(userRole).contains(invocation.getArgument(0)));
  }

  MultilingualTenantTestDataBuilder multilingualTenantTestDataBuilder =
      new MultilingualTenantTestDataBuilder();

  @Test
  void createTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndValidAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(
                    multilingualTenantTestDataBuilder
                        .withName("tenant")
                        .withSubdomain("subdomain")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id").exists())
        .andExpect(jsonPath("name", is("tenant")))
        .andExpect(jsonPath("subdomain", is("subdomain")))
        .andExpect(jsonPath("settings.featureStatisticsEnabled", is(false)))
        .andExpect(jsonPath("settings.featureTopicsEnabled", is(true)))
        .andExpect(jsonPath("settings.topicsInRegistrationEnabled", is(true)))
        .andExpect(jsonPath("settings.featureDemographicsEnabled", is(false)))
        .andExpect(jsonPath("settings.featureAppointmentsEnabled", is(false)))
        .andExpect(jsonPath("settings.featureGroupChatV2Enabled", is(false)))
        .andExpect(jsonPath("settings.featureAttachmentUploadDisabled", is(true)))
        .andExpect(jsonPath("settings.featureToolsOICDToken", is("token")))
        .andExpect(jsonPath("settings.activeLanguages", is(Lists.newArrayList("de", "en"))));
  }

  @Test
  void createTenant_Should_returnStatusForbidden_When_calledWithoutTenantAdminAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(authentication(builder.withUserRole(AUTHORITY_WITHOUT_PERMISSIONS).build()))
                .with(
                    user("not important")
                        .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON)
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(1L)
                        .withName("tenant")
                        .withSubdomain("subdomain")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void createTenant_Should_notCreateTenant_When_SubdomainIsNotUnique() throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    // given
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .contentType(APPLICATION_JSON)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .content(
                    multilingualTenantTestDataBuilder
                        .withName("tenant")
                        .withSubdomain("sub")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
    // when
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .contentType(APPLICATION_JSON)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(2L)
                        .withName("another tenant")
                        .withSubdomain("sub")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(header().exists("X-Reason"));
  }

  @Test
  void createTenant_Should_notCreateTenant_When_calledWithTenantDataAndTenantId() throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(1L)
                        .withName("tenant")
                        .withSubdomain("subdomain")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isConflict());
  }

  @Test
  void
      getMultilingualTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndValidAuthority()
          throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    Mockito.when(userAdminService.getTenantAdmins(1))
        .thenReturn(
            Lists.newArrayList(
                adminResponseWithMail("admin@admin.com"),
                adminResponseWithMail("admin1@admin.com")));
    when(consultingTypeService.getConsultingTypesByTenantId(1))
        .thenReturn(
            new com.vi.tenantservice.consultingtypeservice.generated.web.model
                    .FullConsultingTypeResponseDTO()
                .languageFormal(true)
                .sendFurtherStepsMessage(true)
                .welcomeMessage(
                    new com.vi.tenantservice.consultingtypeservice.generated.web.model
                            .ExtendedConsultingTypeResponseDTOAllOfWelcomeMessage()
                        .welcomeMessageText("welcome")
                        .sendWelcomeMessage(true))
                .isVideoCallAllowed(true));

    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(TENANTADMIN_RESOURCE + "/1")
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("adminEmails", contains("admin@admin.com", "admin1@admin.com")))
        .andExpect(jsonPath("settings.extendedSettings.isVideoCallAllowed", is(true)))
        .andExpect(jsonPath("settings.extendedSettings.languageFormal", is(true)))
        .andExpect(jsonPath("settings.extendedSettings.sendFurtherStepsMessage", is(true)))
        .andExpect(
            jsonPath("settings.extendedSettings.welcomeMessage.sendWelcomeMessage", is(true)))
        .andExpect(
            jsonPath("settings.extendedSettings.welcomeMessage.welcomeMessageText", is("welcome")))
        .andExpect(
            jsonPath(
                "content.dataProtectionContactTemplate.de.agencyContext.responsibleContact",
                containsString("${name}")))
        .andExpect(
            jsonPath(
                "content.dataProtectionContactTemplate.de.noAgencyContext.responsibleContact",
                notNullValue()))
        .andExpect(
            jsonPath(
                "content.dataProtectionContactTemplate.en.agencyContext.responsibleContact",
                containsString("${name}")))
        .andExpect(
            jsonPath(
                "content.dataProtectionContactTemplate.en.noAgencyContext.responsibleContact",
                notNullValue()));
  }

  private com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO
      adminResponseWithMail(String mail) {
    return new com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO()
        .embedded(
            new com.vi.tenantservice.useradminservice.generated.web.model.AdminDTO().email(mail));
  }

  @Test
  void
      updateTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndTenantAdminAuthority()
          throws Exception {
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    when(consultingTypeService.getConsultingTypesByTenantId(1))
        .thenReturn(
            new com.vi.tenantservice.consultingtypeservice.generated.web.model
                    .FullConsultingTypeResponseDTO()
                .id(2));
    MvcResult mvcResult =
        mockMvc
            .perform(
                put("/tenantadmin/1")
                    .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                    .contentType(APPLICATION_JSON)
                    .content(
                        multilingualTenantTestDataBuilder
                            .withId(1L)
                            .withName("tenant")
                            .withSubdomain("changed subdomain")
                            .withSettingActiveLanguages(Lists.newArrayList("fr", "pl"))
                            .withLicensing()
                            .jsonify())
                    .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subdomain").value("changed subdomain"))
            .andExpect(jsonPath("$.settings.topicsInRegistrationEnabled").value("true"))
            .andExpect(jsonPath("$.settings.activeLanguages").value(Lists.newArrayList("fr", "pl")))
            .andReturn();
  }

  @Test
  void
      updateTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndSingleTenantAdminAuthority()
          throws Exception {

    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
    when(authorisationService.hasRole(SINGLE_TENANT_ADMIN.getValue())).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    when(consultingTypeService.getConsultingTypesByTenantId(1))
        .thenReturn(
            new com.vi.tenantservice.consultingtypeservice.generated.web.model
                    .FullConsultingTypeResponseDTO()
                .id(CONSULTING_TYPE_ID)
                .isVideoCallAllowed(true)
                .languageFormal(true));
    mockMvc
        .perform(
            put(EXISTING_TENANT_VIA_ADMIN)
                .with(authentication(builder.withUserRole(SINGLE_TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(1L)
                        .withName("tenant")
                        .withLicensing(5)
                        .withSubdomain("happylife")
                        .withSettingTopicsInRegistrationEnabled(true)
                        .withTranslatedImpressum("de", "new impressum")
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subdomain").value("happylife"))
        .andExpect(jsonPath("$.settings.topicsInRegistrationEnabled").value("true"))
        .andExpect(jsonPath("$.content.impressum['de']").value("new impressum"))
        .andExpect(jsonPath("$.settings.featureToolsEnabled").value("true"));
  }

  @Test
  void
      updateTenant_Should_returnStatusForbidden_When_attemptToChangeLegalTextBySingleTenantAdminIfItsDisallowed()
          throws Exception {

    givenSingleTenantAdminCanChangeLegalTexts(false);

    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
    when(authorisationService.hasRole(SINGLE_TENANT_ADMIN.getValue())).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            put(EXISTING_TENANT_VIA_ADMIN)
                .with(authentication(builder.withUserRole(SINGLE_TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(1L)
                        .withName("tenant")
                        .withLicensing(5)
                        .withSubdomain("happylife")
                        .withSettingTopicsInRegistrationEnabled(true)
                        .withTranslatedImpressum("de", "new impressum")
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  private void givenSingleTenantAdminCanChangeLegalTexts(boolean value) {
    com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTO
        settingsDTO =
            new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                .ApplicationSettingsDTO();
    settingsDTO.setLegalContentChangesBySingleTenantAdminsAllowed(
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                .ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled()
            .value(value));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(settingsDTO);
  }

  @Test
  void
      updateTenant_Should_returnStatusForbidden_When_calledWithValidTenantUpdateParamsAndNoAuthorityToModifyTenant()
          throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            put(EXISTING_TENANT_VIA_ADMIN)
                .with(authentication(builder.withUserRole("not-a-valid-admin").build()))
                .contentType(APPLICATION_JSON)
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(1L)
                        .withName("tenant")
                        .withSubdomain("changed subdomain")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateTenant_Should_returnStatusNotFound_When_UpdateAttemptForNonExistingTenant()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            put(NON_EXISTING_TENANT_VIA_ADMIN)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .content(
                    multilingualTenantTestDataBuilder
                        .withId(1L)
                        .withName("tenant")
                        .withSubdomain("changed subdomain")
                        .withLicensing()
                        .jsonify())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTenant_Should_returnSettings() throws Exception {
    var builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(EXISTING_TENANT)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("settings.featureStatisticsEnabled", is(true)))
        .andExpect(jsonPath("settings.featureTopicsEnabled", is(true)))
        .andExpect(jsonPath("settings.topicsInRegistrationEnabled", is(true)))
        .andExpect(jsonPath("settings.featureDemographicsEnabled", is(true)))
        .andExpect(jsonPath("settings.featureAppointmentsEnabled", is(true)))
        .andExpect(jsonPath("settings.featureGroupChatV2Enabled", is(true)))
        .andExpect(jsonPath("settings.featureAttachmentUploadDisabled", is(false)))
        .andExpect(jsonPath("settings.activeLanguages", is(Lists.newArrayList("de"))));
  }

  @Test
  void getHealtcheck_Should_returnHealtcheck() throws Exception {
    mockMvc
        .perform(get("/actuator/health").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UP")));
  }

  @Test
  void
      getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForAuthorityThatIsTenantAdmin()
          throws Exception {
    var builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(EXISTING_TENANT)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.subdomain").exists())
        .andExpect(jsonPath("$.licensing").exists())
        .andExpect(jsonPath("$.theming").exists())
        .andExpect(jsonPath("$.theming.logo").exists())
        .andExpect(jsonPath("$.theming.associationLogo").exists())
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content.dataProtectionContactTemplate").exists())
        .andExpect(jsonPath("$.settings").exists());
  }

  @Test
  void getAllTenants_Should_returnForbidden_When_calledForAuthorityThatIsSingleTenantAdmin()
      throws Exception {
    mockMvc
        .perform(
            get(TENANT_RESOURCE)
                .with(
                    user("not important")
                        .authorities((GrantedAuthority) SINGLE_TENANT_ADMIN::getValue))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void
      getAllTenants_Should_returnStatusBasicTenantLicensingData_When_calledForAuthorityThatIsTenantAdmin()
          throws Exception {
    var builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            get(TENANT_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(jsonPath("$[0].id").value(0))
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].subdomain").exists())
        .andExpect(jsonPath("$[0].licensing").exists())
        .andExpect(jsonPath("$[0].theming").doesNotExist())
        .andExpect(jsonPath("$[0].content").doesNotExist());
  }

  @Test
  void
      getRestrictedTenantDataBySubdomain_Should_returnStatusOk_When_calledWithExistingTenantSubdomainAndNoAuthentication()
          throws Exception {
    mockMvc
        .perform(get(PUBLIC_TENANT_RESOURCE + EXISTING_SUBDOMAIN).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.subdomain").exists())
        .andExpect(jsonPath("$.licensing").doesNotExist())
        .andExpect(jsonPath("$.theming").exists())
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.settings").exists());
  }

  @Test
  void getRestrictedTenantData_Should_determineTenantContextFromRequestAndReturnStatusOk()
      throws Exception {

    when(tenantResolverService.tryResolve()).thenReturn(Optional.of(2L));
    mockMvc
        .perform(get(PUBLIC_TENANT_RESOURCE).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.subdomain").exists())
        .andExpect(jsonPath("$.licensing").doesNotExist())
        .andExpect(jsonPath("$.theming").exists())
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.settings").exists());
  }

  @Test
  void
      getRestrictedTenantDataByTenantId_Should_returnStatusOk_When_calledWithExistingTenantIdAndNoAuthentication()
          throws Exception {

    mockMvc
        .perform(get(EXISTING_PUBLIC_TENANT).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.subdomain").exists())
        .andExpect(jsonPath("$.licensing").doesNotExist())
        .andExpect(jsonPath("$.theming").exists())
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content.renderedPrivacy").exists())
        .andExpect(jsonPath("$.settings").exists())
        .andReturn();
  }

  @Test
  void
      getRestrictedTenantDataByTenantId_Should_returnStatusNotFound_When_calledWithNonExistingTenantIdAndNoAuthentication()
          throws Exception {
    mockMvc
        .perform(get(NON_EXISTING_PUBLIC_TENANT).contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForTenantAdminAuthority()
      throws Exception {
    var builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(EXISTING_TENANT)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void
      getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForSingleTenantAdminAuthority()
          throws Exception {
    var builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(SINGLE_TENANT_ADMIN);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
    mockMvc
        .perform(
            get(EXISTING_TENANT)
                .with(authentication(builder.withUserRole(SINGLE_TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void
      getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForRestrictedAgencyAdminAuthority()
          throws Exception {
    var builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(EXISTING_TENANT)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void updateTenant_Should_sanitizeInput_When_calledWithExistingTenantIdAndForTenantAdminAuthority()
      throws Exception {
    String jsonRequest = prepareRequestWithInvalidScriptContent();
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(true);
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            put(EXISTING_TENANT_VIA_ADMIN)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(jsonRequest)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("name"))
        .andExpect(jsonPath("$.subdomain").value("subdomain"))
        .andExpect(jsonPath("$.content.impressum['de']").value("<b>impressum</b>"))
        .andExpect(jsonPath("$.content.claim['de']").value("<b>claim</b>"));
  }

  @Test
  void
      updateTenant_Should_throwValidationException_When_calledWithExistingTenantIdAndForTenantAdminAuthorityButInvalidLanguageCode()
          throws Exception {
    String jsonRequest = prepareRequestWithInvalidLanguageContent();

    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            put(EXISTING_TENANT_VIA_ADMIN)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(jsonRequest)
                .contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  private String prepareRequestWithInvalidScriptContent() {
    return multilingualTenantTestDataBuilder
        .withId(1L)
        .withName(appendMalciousScript("name"))
        .withSubdomain(appendMalciousScript("subdomain"))
        .withContent(appendMalciousScript("<b>impressum</b>"), appendMalciousScript("<b>claim</b>"))
        .jsonify();
  }

  private String prepareRequestWithInvalidLanguageContent() {
    return multilingualTenantTestDataBuilder
        .withId(1L)
        .withName(appendMalciousScript("name"))
        .withSubdomain(appendMalciousScript("subdomain"))
        .withTranslatedImpressum("abc", "impressum")
        .jsonify();
  }

  private String appendMalciousScript(String content) {
    return content + SCRIPT_CONTENT;
  }

  @Test
  void
      getAllTenant_Should_returnStatusForbidden_When_calledWithExistingTenantIdAndForAuthorityWithoutPermissions()
          throws Exception {
    mockMvc
        .perform(
            get(EXISTING_TENANT)
                .with(
                    user(USERNAME)
                        .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void
      getTenant_Should_returnStatusForbidden_When_calledWithExistingTenantIdAndForAuthorityWithoutPermissions()
          throws Exception {
    mockMvc
        .perform(
            get(TENANT_RESOURCE)
                .with(
                    user(USERNAME)
                        .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void getTenant_Should_returnStatusNotFound_When_calledWithNotExistingTenantId() throws Exception {

    var builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(NON_EXISTING_TENANT)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTenant_Should_returnStatusUnauthorized_When_calledWithoutAnyAuthorization()
      throws Exception {
    mockMvc
        .perform(get(EXISTING_TENANT).contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Sql(value = "/database/SingleTenantData.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(value = "/database/MultiTenantData.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  void getRestrictedSingleTenantData_Should_returnOkAndTheRequestedTenantData() throws Exception {
    mockMvc
        .perform(get(PUBLIC_SINGLE_TENANT_RESOURCE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void getRestrictedSingleTenantData_Should_returnConflict_When_notExactlyOneTenantIsFound()
      throws Exception {
    mockMvc.perform(get(PUBLIC_SINGLE_TENANT_RESOURCE)).andExpect(status().isBadRequest());
  }

  @Test
  void canAccessTenant_Should_returnStatusNoContent_When_userIsSuperAdmin() throws Exception {
    var builder = new AuthenticationMockBuilder();
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(true);
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("subdomain"));

    mockMvc
        .perform(
            get(TENANT_ACCESS)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())))
        .andExpect(status().isNoContent());
  }

  @Test
  void canAccessTenant_Should_returnStatusOk_When_tokenHasTenantIdEqualToSubdomainTenantId()
      throws Exception {
    var builder = new AuthenticationMockBuilder();
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(3L));
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("localhost"));

    mockMvc
        .perform(
            get(TENANT_ACCESS)
                .with(
                    authentication(
                        builder.withUserRole(RESTRICTED_AGENCY_ADMIN.getValue()).build())))
        .andExpect(status().isNoContent());
  }

  @Test
  void
      canAccessTenant_Should_returnStatusUnauthorized_When_tokenHasTenantIdDifferentToSubdomainTenantId()
          throws Exception {
    var builder = new AuthenticationMockBuilder();
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));

    mockMvc
        .perform(
            get(TENANT_ACCESS)
                .with(
                    authentication(
                        builder.withUserRole(RESTRICTED_AGENCY_ADMIN.getValue()).build())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void searchTenants_Should_returnOk_When_attemptedToGetTenantWithTenantAdminAuthority()
      throws Exception {
    // given
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);

    // when, then
    this.mockMvc
        .perform(
            get(TENANTADMIN_SEARCH + "?query=*&page=1&perPage=10&field=NAME&order=ASC")
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("_embedded", hasSize(PAGE_SIZE)));
  }

  @Test
  void searchTenants_Should_returnUnauthorized_When_attemptedToGetTenantWithoutTenantAuthority()
      throws Exception {
    // when, then
    this.mockMvc
        .perform(get(TENANTADMIN_SEARCH + "?query=*&page=1&perPage=10&field=NAME&order=ASC"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void searchTenants_Should_returnCorrectTenant_When_idIsProvidedInQuery() throws Exception {
    // given
    final String tenantId = "1";
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    // when, then
    this.mockMvc
        .perform(
            get(TENANTADMIN_SEARCH + "?query=" + tenantId + "&page=1&perPage=10&order=ASC")
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("_embedded", hasSize(1)))
        .andExpect(jsonPath("_embedded[0].id").value(tenantId))
        .andExpect(jsonPath("_embedded[0].name").value("Happylife Gmbh"))
        .andExpect(jsonPath("_embedded[0].subdomain").value("happylife"))
        .andExpect(jsonPath("_embedded[0].beraterCount").value(5));
  }

  @Test
  void searchTenants_Should_returnCorrectTenant_When_nameIsProvidedInQuery() throws Exception {
    // given
    final String name = "host";
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    // when, then
    this.mockMvc
        .perform(
            get(TENANTADMIN_SEARCH + "?query=" + name + "&page=1&perPage=10&order=ASC")
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("_embedded", hasSize(1)))
        .andExpect(jsonPath("_embedded[0].id").value(3))
        .andExpect(jsonPath("_embedded[0].name").value("localhost tenant"))
        .andExpect(jsonPath("_embedded[0].subdomain").value("localhost"))
        .andExpect(jsonPath("_embedded[0].beraterCount").value(12));
  }

  @Test
  void searchTenants_Should_returnEmpty_When_technicalTenantIdIsProvidedInQuery() throws Exception {
    // given
    final String tenantId = "0";
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    // when, then
    this.mockMvc
        .perform(
            get(TENANTADMIN_SEARCH + "?query=" + tenantId + "&page=1&perPage=10&order=ASC")
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("_embedded", hasSize(0)));
  }

  @Test
  void searchTenants_Should_returnEmpty_When_technicalTenantNameIsProvidedInQuery()
      throws Exception {
    // given
    final String name = "notenant";
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    // when, then
    this.mockMvc
        .perform(
            get(TENANTADMIN_SEARCH + "?query=" + name + "&page=1&perPage=10&order=ASC")
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("_embedded", hasSize(0)));
  }

  @Test
  void getAllTenantsWithAdminData_Should_returnForbidden_When_calledWithWrongAuthority()
      throws Exception {

    mockMvc
        .perform(
            get(TENANTADMIN_RESOURCE)
                .with(
                    user("not important")
                        .authorities((GrantedAuthority) SINGLE_TENANT_ADMIN::getValue))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void
      getAllTenantsWithAdminData_Should_returnAdminTenantDTOs_When_calledForAuthorityThatIsTenantAdmin()
          throws Exception {
    var builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            get(TENANTADMIN_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].subdomain").exists())
        .andExpect(jsonPath("$[0].beraterCount").exists())
        .andExpect(jsonPath("$[0].adminEmails").doesNotExist());
  }
}
