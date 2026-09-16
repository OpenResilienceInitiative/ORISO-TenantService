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
 * Group chat formats (#250) as the counselling app reads them from the public tenant endpoint,
 * while the platform still carries rules written before the two format flags existed.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class GroupChatFormatFlagsIT {

  private static final String PLATFORM_CONTROLS = "/tenantadmin/controls";
  // tenant 1 is seeded with featureGroupChatV2Enabled=true and no format flags
  private static final String PUBLIC_TENANT_1 = "/tenant/public/id/1";

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
  void removePlatformControls() {
    // other ITs share this Spring context and database
    platformControlsRepository.deleteAll();
    tenantPolicyRepository.deleteAll();
  }

  private RequestPostProcessor platformAdmin() {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", 0L)
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS"),
            new SimpleGrantedAuthority("AUTHORIZATION_UPDATE_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_GET_TENANT"));
  }

  private void savePlatformControls(String json) throws Exception {
    mvc.perform(
            put(PLATFORM_CONTROLS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk());
  }

  @Test
  void publicTenant_should_switchBothFormatsOff_whenPlatformForbadeGroupChatsBeforeFormatsExisted()
      throws Exception {
    savePlatformControls("{\"allowedPermissionToggles\":{\"groupChat\":false}}");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureGroupChatV2Enabled", is(false)))
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(false)));
  }

  @Test
  void publicTenant_should_switchBothFormatsOff_whenPlatformPolicyLockedGroupChatsOff()
      throws Exception {
    savePlatformControls(
        "{\"permissionPolicies\":{\"featureGroupChatV2Enabled\":"
            + "{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureGroupChatV2Enabled", is(false)))
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(false)));
  }

  @Test
  void publicTenant_should_switchOnlySelfHelpGroupsOff_whenPlatformLocksOnlyThatFormat()
      throws Exception {
    savePlatformControls(
        "{\"permissionPolicies\":{"
            + "\"featureGroupChatV2Enabled\":{\"value\":true,\"mode\":\"SUGGESTED\"},"
            + "\"featureInternalGroupChatEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"},"
            + "\"featureSelfHelpGroupsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(false)));
  }

  @Test
  void platformControls_should_showFormatRulesInheritedFromGroupChat_whenNeverStored()
      throws Exception {
    savePlatformControls(
        "{\"allowedPermissionToggles\":{\"groupChat\":false},"
            + "\"permissionPolicies\":{\"featureGroupChatV2Enabled\":"
            + "{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    mvc.perform(get(PLATFORM_CONTROLS).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowedPermissionToggles.internalGroupChat", is(false)))
        .andExpect(jsonPath("$.allowedPermissionToggles.selfHelpGroups", is(false)))
        .andExpect(
            jsonPath("$.permissionPolicies.featureInternalGroupChatEnabled.mode", is("ENFORCED")))
        .andExpect(jsonPath("$.permissionPolicies.featureSelfHelpGroupsEnabled.value", is(false)));
  }

  @Test
  void publicTenant_should_switchBothFormatsOff_whenTenantLockedGroupChatsOffBeforeFormatsExisted()
      throws Exception {
    mvc.perform(
            put("/tenantadmin/1/permission-policies")
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"tenantId\":1,\"policies\":{\"featureGroupChatV2Enabled\":"
                        + "{\"value\":false,\"mode\":\"ENFORCED\"}}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.policies.featureSelfHelpGroupsEnabled.mode", is("ENFORCED")));

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureInternalGroupChatEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureSelfHelpGroupsEnabled", is(false)));
  }
}
