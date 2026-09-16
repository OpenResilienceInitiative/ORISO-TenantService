package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * #251: a new Träger starts from the platform admin's preset at the moment it is created; Träger
 * that already exist are not touched when the preset changes later.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class NewTenantPlatformPresetIT {

  private static final String PLATFORM_CONTROLS = "/tenantadmin/controls";
  // tenant 1 is seeded with featureGroupChatV2Enabled=true and no team discussion flag
  private static final String PUBLIC_TENANT_1 = "/tenant/public/id/1";

  @Autowired WebApplicationContext context;
  @Autowired TenantAdminControlsRepository platformControlsRepository;
  @MockitoBean AuthorisationService authorisationService;
  @MockitoBean ApplicationSettingsService applicationSettingsService;
  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;
  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean
  com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  @MockitoBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean TenantResolverService tenantResolverService;
  @MockitoBean ConsultingTypeService consultingTypeService;
  @MockitoBean UserAdminService userAdminService;
  @MockitoBean SubdomainExtractor subdomainExtractor;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    platformControlsRepository.deleteAll();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
    when(authorisationService.hasAuthority(Mockito.any()))
        .thenAnswer(
            invocation ->
                Authority.getAuthoritiesByUserRole(TENANT_ADMIN)
                    .contains(invocation.getArgument(0)));
    // platform (super) admin: technical tenant 0 plus the tenant-admin role
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(true);
  }

  @AfterEach
  void removePlatformControls() {
    // other ITs share this Spring context and database
    platformControlsRepository.deleteAll();
  }

  private Authentication platformAdmin() {
    return new AuthenticationMockBuilder().withUserRole(TENANT_ADMIN.getValue()).build();
  }

  private String createTenant(String subdomain) throws Exception {
    String response =
        mvc.perform(
                post("/tenantadmin")
                    .with(authentication(platformAdmin()))
                    .contentType(APPLICATION_JSON)
                    .content(
                        new MultilingualTenantTestDataBuilder()
                            .withName("new traeger")
                            .withSubdomain(subdomain)
                            .withLicensing()
                            .jsonify()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "/tenant/public/id/" + JsonPath.<Integer>read(response, "$.id");
  }

  @Test
  void newTenant_should_startWithAllConversationFeaturesOn_whenPlatformPresetIsUntouched()
      throws Exception {
    String newTenant = createTenant("untouchedpreset");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureGroupChatV2Enabled", is(true)))
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureTeamDiscussionEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureVideoCallsEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureMediaAiScanEnabled", is(false)));
  }

  @Test
  void newTenant_should_startWithSupervisionOff_whenPlatformPresetIsUntouched() throws Exception {
    // decision 2026-09-16: supervision is not tested yet, so a new Träger starts with it off even
    // though the master conversation-feature default is "on" and the seeded test defaults have
    // every supervision flag set to true
    String newTenant = createTenant("supervisionuntouched");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureSupervisionEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureSupervisionOneOnOneChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureSupervisionAnonymousChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureAudioCallsSupervisionChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureVideoCallsSupervisionChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureThreadsSupervisionChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureVoiceMessagesSupervisionChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureMediaUploadSupervisionChatsEnabled", is(false)))
        .andExpect(
            jsonPath("$.settings.featureMediaInlineDisplaySupervisionChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureMediaAiScanSupervisionChatsEnabled", is(false)))
        // unrelated conversation features stay on: supervision-off is not a blanket "off"
        .andExpect(jsonPath("$.settings.featureVideoCallsEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureVideoCallsGroupChatsEnabled", is(true)));
  }

  @Test
  void newTenant_should_startWithSupervisionOn_afterPlatformAdminSetAnExplicitPolicy()
      throws Exception {
    // every policy is already served as "true, SUGGESTED" by default, so requesting true again
    // would not register as explicit (see the echoed-map test below); flip off then on, as the
    // Admin panel does per click, so the change is unambiguously explicit
    savePlatformPolicies(
        "{\"featureSupervisionEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"},"
            + "\"featureSupervisionOneOnOneChatsEnabled\":"
            + "{\"value\":false,\"mode\":\"SUGGESTED\"},"
            + "\"featureVideoCallsSupervisionChatsEnabled\":"
            + "{\"value\":false,\"mode\":\"SUGGESTED\"}}");
    savePlatformPolicies(
        "{\"featureSupervisionEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"},"
            + "\"featureSupervisionOneOnOneChatsEnabled\":"
            + "{\"value\":true,\"mode\":\"SUGGESTED\"},"
            + "\"featureVideoCallsSupervisionChatsEnabled\":"
            + "{\"value\":true,\"mode\":\"SUGGESTED\"}}");

    String newTenant = createTenant("supervisionexplicit");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureSupervisionEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureSupervisionOneOnOneChatsEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureVideoCallsSupervisionChatsEnabled", is(true)))
        // left untouched by the platform admin, so it stays off
        .andExpect(jsonPath("$.settings.featureSupervisionAnonymousChatsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureAudioCallsSupervisionChatsEnabled", is(false)));
  }

  @Test
  void newTenant_should_startWithGroupChatsOff_afterPlatformAdminSwitchedThemOffInThePreset()
      throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(authentication(platformAdmin()))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionPolicies\":{\"featureGroupChatV2Enabled\":"
                        + "{\"value\":false,\"mode\":\"SUGGESTED\"}}}"))
        .andExpect(status().isOk());

    String newTenant = createTenant("presetoff");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureGroupChatV2Enabled", is(false)))
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureTeamDiscussionEnabled", is(true)));
  }

  @Test
  void newTenant_should_followEachFormatOfThePresetSeparately() throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(authentication(platformAdmin()))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionPolicies\":{"
                        + "\"featureGroupChatV2Enabled\":{\"value\":true,\"mode\":\"SUGGESTED\"},"
                        + "\"featureInternalGroupChatEnabled\":"
                        + "{\"value\":true,\"mode\":\"SUGGESTED\"},"
                        + "\"featureSelfHelpGroupsEnabled\":"
                        + "{\"value\":false,\"mode\":\"SUGGESTED\"}}}"))
        .andExpect(status().isOk());

    String newTenant = createTenant("presetformats");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(false)));
  }

  @Test
  void existingTenant_should_keepItsValues_whenPlatformPresetChangesAfterwards() throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(authentication(platformAdmin()))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionPolicies\":{\"featureGroupChatV2Enabled\":"
                        + "{\"value\":false,\"mode\":\"SUGGESTED\"},"
                        + "\"featureCallsEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"}}}"))
        .andExpect(status().isOk());
    createTenant("anothernewone");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureGroupChatV2Enabled", is(true)))
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureTeamDiscussionEnabled", is(false)));
  }

  private void savePlatformPolicies(String policiesJson) throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(authentication(platformAdmin()))
                .contentType(APPLICATION_JSON)
                .content("{\"permissionPolicies\":" + policiesJson + "}"))
        .andExpect(status().isOk());
  }

  @Test
  void newTenant_should_followExplicitPlatformPolicyOff_forAMediaFeature() throws Exception {
    savePlatformPolicies(
        "{\"featureMediaInlineDisplayEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}");

    String newTenant = createTenant("mediaoff");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureMediaInlineDisplayEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureMediaInlineDisplayGroupChatsEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureMediaAiScanEnabled", is(false)));
  }

  @Test
  void newTenant_should_followExplicitPlatformPolicyOn_forAMediaFeature() throws Exception {
    // the platform admin switches AI scan off and on again, as the Admin panel does per click
    savePlatformPolicies(
        "{\"featureMediaAiScanEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}");
    savePlatformPolicies("{\"featureMediaAiScanEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"}}");

    String newTenant = createTenant("aiscanon");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureMediaAiScanEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureMediaAiScanGroupChatsEnabled", is(false)));
  }

  @Test
  void newTenant_should_followAnEnforcedPlatformPolicy_forAMediaFeature() throws Exception {
    savePlatformPolicies("{\"featureMediaAiScanEnabled\":{\"value\":true,\"mode\":\"ENFORCED\"}}");

    String newTenant = createTenant("aiscanenforced");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureMediaAiScanEnabled", is(true)));
  }

  @Test
  void newTenant_should_keepMediaAndAskerDefaults_whenThePlatformAdminNeverTouchedThem()
      throws Exception {
    savePlatformPolicies(
        "{\"featureGroupChatV2Enabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}");

    String newTenant = createTenant("untouchedmedia");

    // media upload is false here because the testing profile's default-tenant-settings.json says
    // so: without an explicit platform policy the configured default stays in charge
    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureMediaUploadEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureMediaInlineDisplayEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureMediaAiScanEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureDisplayNameEditable", is(true)))
        .andExpect(jsonPath("$.settings.featureAskerEmailEnabled", is(true)));
  }

  @Test
  void newTenant_should_notTreatThePolicyMapEchoedBackByTheAdminPanel_asExplicit()
      throws Exception {
    // The Admin panel PUTs the whole map it received from GET, including entries the platform
    // derived from the legacy toggles (AI scan shows up as "on, suggested" there). Only what the
    // platform admin actually changed counts as the preset.
    String controls =
        mvc.perform(get(PLATFORM_CONTROLS).with(authentication(platformAdmin())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissionPolicies.featureMediaAiScanEnabled.value", is(true)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(authentication(platformAdmin()))
                .contentType(APPLICATION_JSON)
                .content(controls))
        .andExpect(status().isOk());

    String newTenant = createTenant("echoedmap");

    mvc.perform(get(newTenant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureMediaAiScanEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureGroupChatV2Enabled", is(true)));
  }
}
