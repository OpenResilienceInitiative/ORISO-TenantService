package com.vi.tenantservice.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "technical.service.subject=legacy-service-subject"
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class TenantSearchVisibilityIT {

  private static final String SEARCH = "/tenantadmin/search";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtAuthConverter jwtAuthConverter;

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
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(new HttpHeaders());
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders()).thenReturn(new HttpHeaders());
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", " \u2003 ", "HAPPYLIFE owner"})
  void organisationAdminSearchCountsAndLoadsOnlyItsOwnOrganisation(String query) throws Exception {
    when(userAdminService.getTenantAdmins(1)).thenReturn(adminsWithEmail("owner@example.test"));

    mvc.perform(
            get(SEARCH)
                .param("query", query)
                .with(caller("organisation", "1", "tenant-admin", "agency-admin", "user-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$._embedded.length()").value(1))
        .andExpect(jsonPath("$._embedded[0].id").value(1))
        .andExpect(jsonPath("$._embedded[0].adminEmails[0]").value("owner@example.test"));

    verify(userAdminService).getTenantAdmins(1);
    verifyNoMoreInteractions(userAdminService);
  }

  @Test
  void platformAdminSearchCountsAllMatchingOrganisationsBeforePaging() throws Exception {
    when(userAdminService.getTenantAdmins(1)).thenReturn(adminsWithEmail("owner-one@example.test"));
    when(userAdminService.getTenantAdmins(3))
        .thenReturn(adminsWithEmail("owner-three@example.test"));

    mvc.perform(
            get(SEARCH)
                .param("query", "owner")
                .param("page", "2")
                .param("perPage", "1")
                .param("field", "ID")
                .with(caller("platform", "0", "tenant-admin", "agency-admin", "user-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$._embedded.length()").value(1))
        .andExpect(jsonPath("$._embedded[0].id").value(3))
        .andExpect(jsonPath("$._embedded[0].adminEmails[0]").value("owner-three@example.test"));

    verify(userAdminService).getTenantAdmins(1);
    verify(userAdminService).getTenantAdmins(2);
    verify(userAdminService).getTenantAdmins(3);
    verifyNoMoreInteractions(userAdminService);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"invalid", "-1"})
  void missingOrInvalidTenantIdentityCannotReadOrganisationListing(String tenantId)
      throws Exception {
    mvc.perform(get(SEARCH).param("query", "*").with(caller("unscoped", tenantId, "tenant-admin")))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userAdminService);
  }

  @Test
  void statisticsReaderCannotReadOrganisationListingWithPlatformClaim() throws Exception {
    mvc.perform(get(SEARCH).param("query", "*").with(caller("reader", "0", "statistics-reader")))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userAdminService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1"})
  void legacyTechnicalIdentityCannotReadOrganisationListing(String tenantId) throws Exception {
    mvc.perform(
            get(SEARCH)
                .param("query", "*")
                .with(caller("legacy-service", tenantId, "technical", "tenant-admin")))
        .andExpect(status().isForbidden());

    verifyNoInteractions(userAdminService);
  }

  private RequestPostProcessor caller(String name, String tenantId, String... roles) {
    return jwt()
        .jwt(
            claim -> {
              claim
                  .subject(name + "-subject")
                  .claim("username", name)
                  .claim("realm_access", Map.of("roles", List.of(roles)));
              if (tenantId != null) {
                claim.claim("tenantId", tenantId);
              }
            })
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
  }

  private static List<AdminResponseDTO> adminsWithEmail(String email) {
    return List.of(new AdminResponseDTO().embedded(new AdminDTO().email(email)));
  }
}
