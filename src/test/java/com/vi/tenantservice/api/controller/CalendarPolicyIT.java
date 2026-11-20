package com.vi.tenantservice.api.controller;

import static org.hamcrest.Matchers.is;
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
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * ADR-032 / #365 - the calendar policies (visibility scope, asker usernames) as the Admin, the
 * UserService and the counselling app read and write them: platform preset, Traeger override,
 * requirement versus recommendation.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class CalendarPolicyIT {

  private static final String PLATFORM_CONTROLS = "/tenantadmin/controls";
  private static final String TENANT_1_POLICIES = "/tenantadmin/1/permission-policies";
  private static final String TENANT_2_POLICIES = "/tenantadmin/2/permission-policies";
  private static final String PUBLIC_TENANT_1 = "/tenant/public/id/1";
  private static final String PUBLIC_TENANT_2 = "/tenant/public/id/2";
  private static final String SCOPE = "$.calendar.visibilityScope";
  private static final String USERNAMES = "$.calendar.showAskerUsernames";

  @Autowired WebApplicationContext context;
  @Autowired TenantAdminControlsRepository platformControlsRepository;
  @Autowired TenantPermissionPolicyRepository tenantPolicyRepository;
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
    tenantPolicyRepository.deleteAll();
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
  void cleanUp() {
    // other ITs share this Spring context and database
    platformControlsRepository.deleteAll();
    tenantPolicyRepository.deleteAll();
  }

  private RequestPostProcessor admin(long tokenTenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tokenTenantId)
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS"),
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_GET_TENANT"));
  }

  private RequestPostProcessor platformAdmin() {
    return admin(0L);
  }

  private RequestPostProcessor traegerAdmin(long tenantId) {
    return admin(tenantId);
  }

  private static String calendar(String scope, String scopeMode, boolean usernames, String mode) {
    return "{\"visibilityScope\":{\"value\":\""
        + scope
        + "\",\"mode\":\""
        + scopeMode
        + "\"},\"showAskerUsernames\":{\"value\":"
        + usernames
        + ",\"mode\":\""
        + mode
        + "\"}}";
  }

  private void platformSets(String calendarJson) throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content("{\"permissionsPageEnabled\":true,\"calendar\":" + calendarJson + "}"))
        .andExpect(status().isOk());
  }

  private ResultActions traegerSets(long tenantId, String calendarJson) throws Exception {
    return mvc.perform(
        put("/tenantadmin/" + tenantId + "/permission-policies")
            .with(traegerAdmin(tenantId))
            .contentType(APPLICATION_JSON)
            .content(
                "{\"tenantId\":"
                    + tenantId
                    + ",\"policies\":{},\"calendar\":"
                    + calendarJson
                    + "}"));
  }

  @Test
  void calendar_should_defaultToAgencyAndHiddenUsernames_whenNobodySetIt() throws Exception {
    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".mode", is("SUGGESTED")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)))
        .andExpect(jsonPath(USERNAMES + ".value", is(false)))
        .andExpect(jsonPath(USERNAMES + ".mode", is("SUGGESTED")))
        .andExpect(jsonPath(USERNAMES + ".inherited", is(true)));

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(USERNAMES + ".value", is(false)));
  }

  @Test
  void calendar_should_storeTheTraegerChoice_andServeItOnThePublicEndpoint() throws Exception {
    traegerSets(1L, calendar("TENANT", "SUGGESTED", true, "SUGGESTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(false)))
        .andExpect(jsonPath(USERNAMES + ".value", is(true)))
        .andExpect(jsonPath(USERNAMES + ".inherited", is(false)));

    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(USERNAMES + ".value", is(true)));

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(SCOPE + ".mode", is("SUGGESTED")))
        .andExpect(jsonPath(USERNAMES + ".value", is(true)));

    // another Traeger keeps the defaults
    mvc.perform(get(PUBLIC_TENANT_2))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(USERNAMES + ".value", is(false)));
    mvc.perform(get(TENANT_2_POLICIES).with(traegerAdmin(2L)))
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")));
  }

  @Test
  void calendar_should_followThePlatformPreset_untilTheTraegerOverridesIt() throws Exception {
    platformSets(calendar("PLATFORM", "SUGGESTED", true, "SUGGESTED"));

    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)))
        .andExpect(jsonPath(USERNAMES + ".value", is(true)))
        .andExpect(jsonPath(USERNAMES + ".inherited", is(true)));

    traegerSets(1L, calendar("AGENCY", "SUGGESTED", false, "SUGGESTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(false)))
        .andExpect(jsonPath(USERNAMES + ".value", is(false)));
  }

  @Test
  void calendar_should_lockTheTraegerAdmin_whenThePlatformRequiresIt() throws Exception {
    platformSets(calendar("AGENCY", "ENFORCED", false, "ENFORCED"));

    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".mode", is("ENFORCED")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)));

    traegerSets(1L, calendar("TENANT", "SUGGESTED", false, "ENFORCED"))
        .andExpect(status().isBadRequest());
    traegerSets(1L, calendar("AGENCY", "ENFORCED", true, "SUGGESTED"))
        .andExpect(status().isBadRequest());

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".mode", is("ENFORCED")))
        .andExpect(jsonPath(USERNAMES + ".value", is(false)))
        .andExpect(jsonPath(USERNAMES + ".mode", is("ENFORCED")));

    // the Admin panel PUTs back what it read
    traegerSets(1L, calendar("AGENCY", "ENFORCED", false, "ENFORCED")).andExpect(status().isOk());
  }

  @Test
  void calendar_should_surviveATraegerSaveThatDoesNotMentionIt() throws Exception {
    traegerSets(1L, calendar("PLATFORM", "SUGGESTED", true, "SUGGESTED"))
        .andExpect(status().isOk());

    mvc.perform(
            put(TENANT_1_POLICIES)
                .with(traegerAdmin(1L))
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\":1,\"policies\":{}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")))
        .andExpect(jsonPath(USERNAMES + ".value", is(true)));
  }

  @Test
  void calendar_should_beReportedAsDefault_andSurviveAPlatformSaveThatDoesNotMentionIt()
      throws Exception {
    mvc.perform(get(PLATFORM_CONTROLS).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(USERNAMES + ".value", is(false)));

    platformSets(calendar("TENANT", "SUGGESTED", true, "ENFORCED"));
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content("{\"permissionsPageEnabled\":false}"))
        .andExpect(status().isOk());

    mvc.perform(get(PLATFORM_CONTROLS).with(platformAdmin()))
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(USERNAMES + ".value", is(true)))
        .andExpect(jsonPath(USERNAMES + ".mode", is("ENFORCED")));
  }
}
