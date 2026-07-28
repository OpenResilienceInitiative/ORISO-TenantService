package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end tests for the tenant ID allocation endpoints and the atomic consumption inside the
 * tenant creation path (TEN-INV-U1, ORISO-TenantService#143). Seeds the worked example from the
 * issue: tenant IDs 1-20 and 30-35 are taken.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantIdAllocationControllerIT {

  private static final String AVAILABILITY = "/tenantadmin/tenant-ids/%d/availability";
  private static final String NEXT_FREE = "/tenantadmin/tenant-ids/next-free";
  private static final String RESERVATIONS = "/tenantadmin/tenant-ids/reservations";
  private static final String TENANTADMIN_RESOURCE = "/tenantadmin";

  @Autowired private WebApplicationContext context;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;

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
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
    seedWorkedExample();
  }

  @AfterEach
  void cleanUp() {
    reservationRepository.deleteAll();
    tenantRepository.deleteAll();
  }

  private void seedWorkedExample() {
    cleanUp();
    LongStream.concat(LongStream.rangeClosed(1, 20), LongStream.rangeClosed(30, 35))
        .forEach(
            id -> {
              var now = LocalDateTime.now();
              tenantRepository.save(
                  TenantEntity.builder()
                      .id(id)
                      .name("tenant-" + id)
                      .subdomain("subdomain-" + id)
                      .createDate(now)
                      .updateDate(now)
                      .build());
            });
  }

  private void givenAuthoritiesOfRole(UserRole userRole) {
    when(authorisationService.hasAuthority(Mockito.any()))
        .thenAnswer(
            invocation ->
                Authority.getAuthoritiesByUserRole(userRole).contains(invocation.getArgument(0)));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor platformAdmin() {
    givenAuthoritiesOfRole(TENANT_ADMIN);
    return authentication(
        new AuthenticationMockBuilder().withUserRole(TENANT_ADMIN.getValue()).build());
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor nonPlatformAdminUser() {
    givenAuthoritiesOfRole(SINGLE_TENANT_ADMIN);
    return authentication(
        new AuthenticationMockBuilder().withUserRole(SINGLE_TENANT_ADMIN.getValue()).build());
  }

  @Test
  void getTenantIdAvailability_Should_reportFreeReservedAndAssigned() throws Exception {
    mockMvc
        .perform(get(AVAILABILITY.formatted(21)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id", is(21)))
        .andExpect(jsonPath("status", is("FREE")));

    mockMvc
        .perform(get(AVAILABILITY.formatted(30)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("ASSIGNED")));

    reserveViaApi(25L);
    mockMvc
        .perform(get(AVAILABILITY.formatted(25)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("RESERVED")));
  }

  @Test
  void getNextFreeTenantId_Should_skipTakenIds_InBothDirections() throws Exception {
    mockMvc
        .perform(get(NEXT_FREE).param("from", "29").param("direction", "UP").with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id", is(36)));

    mockMvc
        .perform(
            get(NEXT_FREE).param("from", "36").param("direction", "DOWN").with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id", is(29)));
  }

  @Test
  void getNextFreeTenantId_Should_returnNotFound_When_noFreeIdExistsDownwards() throws Exception {
    mockMvc
        .perform(
            get(NEXT_FREE).param("from", "21").param("direction", "DOWN").with(platformAdmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void reserveTenantId_Should_reserveSmallestFreeId_When_autoMode() throws Exception {
    mockMvc
        .perform(post(RESERVATIONS).with(platformAdmin()).contentType(APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("tenantId", is(21)))
        .andExpect(jsonPath("token", notNullValue()));
  }

  @Test
  void reserveTenantId_Should_returnConflict_When_idIsTaken() throws Exception {
    mockMvc
        .perform(
            post(RESERVATIONS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\": 30}"))
        .andExpect(status().isConflict());
  }

  @Test
  void reserveTenantId_Should_returnConflict_When_idIsAlreadyReserved() throws Exception {
    reserveViaApi(21L);
    mockMvc
        .perform(
            post(RESERVATIONS)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\": 21}"))
        .andExpect(status().isConflict());
  }

  @Test
  void releaseTenantIdReservation_Should_makeIdAssignableAgain() throws Exception {
    reserveViaApi(21L);

    mockMvc
        .perform(delete(RESERVATIONS + "/21").with(platformAdmin()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(AVAILABILITY.formatted(21)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("FREE")));
  }

  @Test
  void releaseTenantIdReservation_Should_returnNotFound_When_noOpenReservationExists()
      throws Exception {
    mockMvc
        .perform(delete(RESERVATIONS + "/21").with(platformAdmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void allocationEndpoints_Should_returnForbidden_When_userLacksPlatformAdminAuthority()
      throws Exception {
    mockMvc
        .perform(get(AVAILABILITY.formatted(21)).with(nonPlatformAdminUser()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get(NEXT_FREE)
                .param("from", "29")
                .param("direction", "UP")
                .with(nonPlatformAdminUser()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post(RESERVATIONS).with(nonPlatformAdminUser()).contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(delete(RESERVATIONS + "/21").with(nonPlatformAdminUser()))
        .andExpect(status().isForbidden());
  }

  @Test
  void createTenant_Should_assignSmallestFreeId_When_noIdInRequest() throws Exception {
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withName("auto tenant")
                        .withSubdomain("auto-subdomain")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id", is(21)));

    mockMvc
        .perform(get(AVAILABILITY.formatted(21)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("ASSIGNED")));
  }

  @Test
  void createTenant_Should_returnConflict_When_manualIdIsTaken() throws Exception {
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(30L)
                        .withName("colliding tenant")
                        .withSubdomain("colliding-subdomain")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isConflict());
  }

  @Test
  void createTenant_Should_returnConflict_When_manualIdIsReservedAndNoTokenPresented()
      throws Exception {
    reserveViaApi(21L);
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(21L)
                        .withName("intruder tenant")
                        .withSubdomain("intruder-subdomain")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isConflict());
  }

  @Test
  void createTenant_Should_consumeReservationAtomically_When_matchingTokenPresented()
      throws Exception {
    String token = reserveViaApi(21L);

    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(21L)
                        .withName("invited tenant")
                        .withSubdomain("invited-subdomain")
                        .withTenantIdReservationToken(token)
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id", is(21)));

    mockMvc
        .perform(get(AVAILABILITY.formatted(21)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("ASSIGNED")));
  }

  @Test
  void createTenant_Should_assignFreeManualId_When_idIsFree() throws Exception {
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(40L)
                        .withName("manual tenant")
                        .withSubdomain("manual-subdomain")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("id", is(40)));

    mockMvc
        .perform(get(AVAILABILITY.formatted(40)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("ASSIGNED")));
  }

  private String reserveViaApi(long tenantId) throws Exception {
    var response =
        mockMvc
            .perform(
                post(RESERVATIONS)
                    .with(platformAdmin())
                    .contentType(APPLICATION_JSON)
                    .content("{\"tenantId\": " + tenantId + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode node = objectMapper.readTree(response);
    return node.get("token").asText();
  }
}
