package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for the platform-level DPIA operator master data endpoints (ORISO-Admin#735):
 * super-admin maintained singleton via /tenantadmin/dpia, public read-only view via
 * /tenant/public/dpia.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureMockMvc(addFilters = false)
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class PlatformDpiaMasterDataControllerIT {

  private static final String ADMIN_DPIA_RESOURCE = "/tenantadmin/dpia";
  private static final String PUBLIC_DPIA_RESOURCE = "/tenant/public/dpia";
  private static final String AUTHORITY_WITHOUT_PERMISSIONS = "technical";

  private static final String FULL_PAYLOAD =
      """
      {
        "operator": {
          "legalName": "Deutscher Caritasverband e. V.",
          "shortName": "DCV",
          "address": "Karlstrasse 40, 79104 Freiburg",
          "contactEmail": "datenschutz@example.org",
          "contactPhone": "+49 761 200-0",
          "dpoName": "Jane Doe",
          "department": "Online counselling",
          "responsiblePerson": "John Doe"
        },
        "supervisoryAuthority": {
          "legalFramework": "KDG",
          "name": "Diocesan data protection officer",
          "address": "Some Street 1, 50667 Cologne",
          "email": "supervision@example.org"
        },
        "document": {
          "documentDate": "2026-01-29",
          "nextReviewDate": "2027-01-29"
        },
        "keyFigures": {
          "tenants": {"count": 12, "asOfDate": "2026-08-01"},
          "counsellingCentres": {"count": 340, "asOfDate": "2026-08-01"},
          "activeCounsellors": {"count": 1500, "asOfDate": "2026-08-01"},
          "registeredClients": {"count": 52000, "asOfDate": "2026-08-01"}
        }
      }
      """;

  @Autowired private WebApplicationContext context;

  @Autowired
  private com.vi.tenantservice.api.repository.PlatformDpiaMasterDataRepository
      platformDpiaMasterDataRepository;

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

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    // the singleton survives @Sql re-seeding - reset it so tests are order-independent
    platformDpiaMasterDataRepository.deleteAll();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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

  private void saveFullMasterData() throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            put(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(FULL_PAYLOAD))
        .andExpect(status().isOk());
  }

  @Test
  void updatePlatformDpiaMasterData_Should_persistAndEchoMasterData_When_superAdminAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            put(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(FULL_PAYLOAD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.legalName", is("Deutscher Caritasverband e. V.")))
        .andExpect(jsonPath("$.supervisoryAuthority.legalFramework", is("KDG")))
        .andExpect(jsonPath("$.document.nextReviewDate", is("2027-01-29")))
        .andExpect(jsonPath("$.keyFigures.registeredClients.count", is(52000)));

    mockMvc
        .perform(
            get(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.shortName", is("DCV")))
        .andExpect(jsonPath("$.keyFigures.tenants.asOfDate", is("2026-08-01")));
  }

  @Test
  void updatePlatformDpiaMasterData_Should_overwriteSingleton_When_calledTwice() throws Exception {
    saveFullMasterData();
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            put(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content("{\"operator\": {\"legalName\": \"New operator\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.legalName", is("New operator")));

    mockMvc
        .perform(
            get(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.legalName", is("New operator")))
        .andExpect(jsonPath("$.operator.shortName").doesNotExist());
  }

  @Test
  void updatePlatformDpiaMasterData_Should_sanitizeHtmlInFreeTextFields() throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            put(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"operator\": {\"legalName\": \"<script>alert(1)</script>Legal name\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.legalName", is("Legal name")));
  }

  @Test
  void getPlatformDpiaMasterData_Should_returnForbidden_When_noSuperAdminAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            get(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(AUTHORITY_WITHOUT_PERMISSIONS).build()))
                .with(
                    user("not important")
                        .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void updatePlatformDpiaMasterData_Should_returnForbidden_When_noSuperAdminAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc
        .perform(
            put(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(AUTHORITY_WITHOUT_PERMISSIONS).build()))
                .with(
                    user("not important")
                        .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON)
                .content(FULL_PAYLOAD))
        .andExpect(status().isForbidden());
  }

  @Test
  void getPlatformDpiaMasterData_Should_returnEmptyGroups_When_nothingStoredYet() throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc
        .perform(
            get(ADMIN_DPIA_RESOURCE)
                .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator").exists())
        .andExpect(jsonPath("$.operator.legalName").doesNotExist());
  }

  @Test
  void getPublicDpiaMasterData_Should_returnMasterDataWithBranding_When_tenantContextResolves()
      throws Exception {
    saveFullMasterData();
    when(tenantResolverService.tryResolve()).thenReturn(Optional.of(2L));

    mockMvc
        .perform(get(PUBLIC_DPIA_RESOURCE).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.legalName", is("Deutscher Caritasverband e. V.")))
        .andExpect(jsonPath("$.supervisoryAuthority.legalFramework", is("KDG")))
        .andExpect(jsonPath("$.keyFigures.activeCounsellors.count", is(1500)))
        .andExpect(jsonPath("$.branding.tenantName").exists())
        .andExpect(jsonPath("$.branding.theming").exists())
        // secrets and non-master-data must never appear on the public endpoint
        .andExpect(jsonPath("$.settings").doesNotExist())
        .andExpect(jsonPath("$.branding.settings").doesNotExist())
        .andExpect(jsonPath("$.branding.smtp").doesNotExist())
        .andExpect(jsonPath("$.licensing").doesNotExist());
  }

  @Test
  void getPublicDpiaMasterData_Should_returnMasterDataWithoutBranding_When_noTenantContext()
      throws Exception {
    saveFullMasterData();
    when(tenantResolverService.tryResolve()).thenReturn(Optional.empty());

    mockMvc
        .perform(get(PUBLIC_DPIA_RESOURCE).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operator.legalName", is("Deutscher Caritasverband e. V.")))
        .andExpect(jsonPath("$.branding").doesNotExist());
  }
}
