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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * #319 - the internal group chat recipient scope as the Admin, the UserService and the counselling
 * app read and write it: platform preset, Traeger override, requirement versus recommendation.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class InternalGroupChatRecipientScopeIT {

  private static final String PLATFORM_CONTROLS = "/tenantadmin/controls";
  private static final String TENANT_1_POLICIES = "/tenantadmin/1/permission-policies";
  private static final String TENANT_2_POLICIES = "/tenantadmin/2/permission-policies";
  private static final String PUBLIC_TENANT_1 = "/tenant/public/id/1";
  private static final String PUBLIC_TENANT_2 = "/tenant/public/id/2";
  private static final String SCOPE = "$.internalGroupChat.recipientScope";

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

  private static String scope(String value, String mode) {
    return "{\"recipientScope\":{\"value\":\"" + value + "\",\"mode\":\"" + mode + "\"}}";
  }

  private void platformSets(String value, String mode) throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionsPageEnabled\":true,\"internalGroupChat\":"
                        + scope(value, mode)
                        + "}"))
        .andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.ResultActions traegerSets(
      long tenantId, String value, String mode) throws Exception {
    return mvc.perform(
        put("/tenantadmin/" + tenantId + "/permission-policies")
            .with(traegerAdmin(tenantId))
            .contentType(APPLICATION_JSON)
            .content(
                "{\"tenantId\":"
                    + tenantId
                    + ",\"policies\":{},\"internalGroupChat\":"
                    + scope(value, mode)
                    + "}"));
  }

  @Test
  void scope_should_defaultToCounsellingCentreOnly_whenNobodySetIt() throws Exception {
    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".mode", is("SUGGESTED")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)));

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")));
  }

  @Test
  void scope_should_followThePlatformPreset_untilTheTraegerOverridesIt() throws Exception {
    platformSets("PLATFORM", "SUGGESTED");

    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)));

    traegerSets(1L, "TENANT", "SUGGESTED")
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(false)));

    mvc.perform(get(PUBLIC_TENANT_1)).andExpect(jsonPath(SCOPE + ".value", is("TENANT")));
  }

  @Test
  void scope_should_leaveOtherTraegerUnaffected_whenOneTraegerWidensIt() throws Exception {
    traegerSets(1L, "PLATFORM", "SUGGESTED").andExpect(status().isOk());

    mvc.perform(get(PUBLIC_TENANT_1)).andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")));
    mvc.perform(get(PUBLIC_TENANT_2))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)));
    mvc.perform(get(TENANT_2_POLICIES).with(traegerAdmin(2L)))
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")));
  }

  @Test
  void scope_should_lockTheTraegerAdmin_whenThePlatformRequiresIt() throws Exception {
    platformSets("TENANT", "ENFORCED");

    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(SCOPE + ".mode", is("ENFORCED")))
        .andExpect(jsonPath(SCOPE + ".inherited", is(true)));

    traegerSets(1L, "PLATFORM", "SUGGESTED").andExpect(status().isBadRequest());

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(jsonPath(SCOPE + ".value", is("TENANT")))
        .andExpect(jsonPath(SCOPE + ".mode", is("ENFORCED")));
  }

  @Test
  void scope_should_acceptTheUnchangedEchoOfARequirement_thenFollowTheNextPlatformValue()
      throws Exception {
    platformSets("TENANT", "ENFORCED");

    // the Admin panel PUTs back what it read
    traegerSets(1L, "TENANT", "ENFORCED").andExpect(status().isOk());

    platformSets("PLATFORM", "ENFORCED");
    mvc.perform(get(TENANT_1_POLICIES).with(traegerAdmin(1L)))
        .andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")));
  }

  @Test
  void scope_should_surviveATraegerSaveThatDoesNotMentionIt() throws Exception {
    traegerSets(1L, "PLATFORM", "SUGGESTED").andExpect(status().isOk());

    mvc.perform(
            put(TENANT_1_POLICIES)
                .with(traegerAdmin(1L))
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\":1,\"policies\":{}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")));
  }

  @Test
  void scope_should_surviveAPlatformControlsSaveThatDoesNotMentionIt() throws Exception {
    platformSets("PLATFORM", "SUGGESTED");

    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content("{\"permissionsPageEnabled\":false}"))
        .andExpect(status().isOk());

    mvc.perform(get(PLATFORM_CONTROLS).with(platformAdmin()))
        .andExpect(jsonPath(SCOPE + ".value", is("PLATFORM")))
        .andExpect(jsonPath(SCOPE + ".mode", is("SUGGESTED")));
  }

  @Test
  void scope_should_beReportedAsDefault_onThePlatformControls() throws Exception {
    mvc.perform(get(PLATFORM_CONTROLS).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath(SCOPE + ".value", is("AGENCY")))
        .andExpect(jsonPath(SCOPE + ".mode", is("SUGGESTED")));
  }
}
