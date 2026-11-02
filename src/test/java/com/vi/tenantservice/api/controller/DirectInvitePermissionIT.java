package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
 * TS#331 / ADR-029 §6: "Direkteinladung erlauben" per chat type as UserService and the counselling
 * app read it from the public tenant endpoint, and as the Admin reads it per Träger.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class DirectInvitePermissionIT {

  private static final String PLATFORM_CONTROLS = "/tenantadmin/controls";
  private static final String TENANT_1_POLICIES = "/tenantadmin/1/permission-policies";
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

  private void saveTraegerPolicies(String policiesJson) throws Exception {
    mvc.perform(
            put(TENANT_1_POLICIES)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\":1,\"policies\":" + policiesJson + "}"))
        .andExpect(status().isOk());
  }

  @Test
  void publicTenant_should_allowAllThreeChatTypes_whenNobodySetAnything() throws Exception {
    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureDirectInviteChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureDirectInviteAudioEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureDirectInviteVideoEnabled", is(true)))
        .andExpect(jsonPath("$.permissionPolicies.featureDirectInviteChatEnabled.value", is(true)))
        .andExpect(
            jsonPath("$.permissionPolicies.featureDirectInviteChatEnabled.inherited", is(true)));
  }

  @Test
  void publicTenant_should_switchOnlyVideoOff_whenPlatformForceDeniesVideo() throws Exception {
    savePlatformControls(
        "{\"permissionPolicies\":{\"featureDirectInviteVideoEnabled\":"
            + "{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureDirectInviteChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureDirectInviteAudioEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureDirectInviteVideoEnabled", is(false)))
        .andExpect(
            jsonPath("$.permissionPolicies.featureDirectInviteVideoEnabled.mode", is("ENFORCED")));
  }

  @Test
  void traeger_should_notReopenADirectInviteThePlatformForceDenied() throws Exception {
    savePlatformControls(
        "{\"permissionPolicies\":{\"featureDirectInviteChatEnabled\":"
            + "{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    mvc.perform(
            put(TENANT_1_POLICIES)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"tenantId\":1,\"policies\":{\"featureDirectInviteChatEnabled\":"
                        + "{\"value\":true,\"mode\":\"SUGGESTED\"}}}"))
        // refused with 500 today for every enforced boolean (follow-up: map it to 400)
        .andExpect(status().isInternalServerError());

    assertThat(tenantPolicyRepository.findAll()).isEmpty();
    mvc.perform(get(TENANT_1_POLICIES).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.policies.featureDirectInviteChatEnabled.value", is(false)))
        .andExpect(jsonPath("$.policies.featureDirectInviteChatEnabled.inherited", is(true)));
    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(jsonPath("$.settings.featureDirectInviteChatEnabled", is(false)));
  }

  @Test
  void publicTenant_should_allowVideo_whenPlatformForceAllowsIt() throws Exception {
    savePlatformControls(
        "{\"permissionPolicies\":{\"featureDirectInviteVideoEnabled\":"
            + "{\"value\":true,\"mode\":\"ENFORCED\"}}}");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureDirectInviteVideoEnabled", is(true)));
  }

  @Test
  void traegerChoice_should_applyToTheTraeger_whenThePlatformLeavesItOpen() throws Exception {
    saveTraegerPolicies(
        "{\"featureDirectInviteAudioEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}");

    mvc.perform(get(PUBLIC_TENANT_1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settings.featureDirectInviteChatEnabled", is(true)))
        .andExpect(jsonPath("$.settings.featureDirectInviteAudioEnabled", is(false)))
        .andExpect(jsonPath("$.settings.featureDirectInviteVideoEnabled", is(true)))
        .andExpect(
            jsonPath("$.permissionPolicies.featureDirectInviteAudioEnabled.inherited", is(false)));

    mvc.perform(get(TENANT_1_POLICIES).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.policies.featureDirectInviteAudioEnabled.value", is(false)))
        .andExpect(jsonPath("$.policies.featureDirectInviteAudioEnabled.inherited", is(false)))
        .andExpect(jsonPath("$.policies.featureDirectInviteChatEnabled.inherited", is(true)))
        .andExpect(jsonPath("$.policies.featureDirectInviteVideoEnabled.inherited", is(true)));
  }
}
