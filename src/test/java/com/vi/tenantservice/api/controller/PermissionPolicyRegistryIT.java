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
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
 * Permission policies (#254) as the Admin and the counselling app read them while the platform row
 * carries a single policy entry, the shape dev has been running since the case handover rollout.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class PermissionPolicyRegistryIT {

  private static final String TENANT_1_POLICIES = "/tenantadmin/1/permission-policies";
  private static final String PUBLIC_TENANT_1 = "/tenant/public/id/1";
  private static final int REGISTRY_SIZE = PermissionFeature.values().length;
  // the platform row exactly as stored on dev: one policy entry, no legacy toggles
  private static final String PLATFORM_ROW_WITH_ONLY_CASE_HANDOVER =
      "{\"permissionsPageEnabled\":true,"
          + "\"permissionPolicies\":{\"caseHandoverEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"}}}";

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
    platformControlsRepository.saveAndFlush(
        TenantAdminControlsEntity.builder()
            .controls(PLATFORM_ROW_WITH_ONLY_CASE_HANDOVER)
            .updateDate(LocalDateTime.now(ZoneOffset.UTC))
            .build());
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

  private void tenantEnforcesAudioCalls() throws Exception {
    mvc.perform(
            put(TENANT_1_POLICIES)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"tenantId\":1,\"policies\":{\"featureAudioCallsEnabled\":"
                        + "{\"value\":true,\"mode\":\"ENFORCED\"}}}"))
        .andExpect(status().isOk());
  }

  @Test
  void permissionPolicies_should_keepATenantRuleForAFeatureThePlatformNeverListed_afterReload()
      throws Exception {
    tenantEnforcesAudioCalls();

    mvc.perform(get(TENANT_1_POLICIES).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.policies.featureAudioCallsEnabled.mode", is("ENFORCED")))
        .andExpect(jsonPath("$.policies.featureAudioCallsEnabled.value", is(true)))
        .andExpect(jsonPath("$.policies.featureAudioCallsEnabled.inherited", is(false)))
        .andExpect(jsonPath("$.policies.caseHandoverEnabled.mode", is("SUGGESTED")))
        .andExpect(jsonPath("$.policies.caseHandoverEnabled.inherited", is(true)))
        .andExpect(jsonPath("$.policies.length()", is(REGISTRY_SIZE)));
  }

  @Test
  void publicTenant_should_listEveryRegistryFeature_whenThePlatformRowHoldsASingleEntry()
      throws Exception {
    tenantEnforcesAudioCalls();

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionPolicies.length()", is(REGISTRY_SIZE)))
        .andExpect(jsonPath("$.permissionPolicies.featureAudioCallsEnabled.mode", is("ENFORCED")))
        .andExpect(jsonPath("$.permissionPolicies.featureAudioCallsEnabled.inherited", is(false)))
        .andExpect(jsonPath("$.permissionPolicies.featureVideoCallsEnabled.mode", is("SUGGESTED")))
        .andExpect(jsonPath("$.permissionPolicies.featureVideoCallsEnabled.value", is(true)))
        .andExpect(jsonPath("$.permissionPolicies.caseHandoverEnabled.value", is(true)));
  }
}
