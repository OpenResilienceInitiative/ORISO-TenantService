package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.RoleAuthorizationAuthorityMapper;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * The UserService calls TenantService as the service identity with realm role {@code technical}
 * (ORISO-Helm#367). That identity gets exactly the narrow machine authorities its calls need, so it
 * no longer depends on the broad {@code tenant-admin} role. A token that carries {@code
 * tenant-admin} in addition (current Staging grant) must keep working exactly as before.
 *
 * <p>Authorities are derived from the realm roles with the production {@link
 * RoleAuthorizationAuthorityMapper}, as {@code JwtAuthConverter} does for a real token.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class TechnicalServiceIdentityAuthorizationIT {

  private static final long RESERVED_ID = 500L;
  private static final long FREE_ID = 501L;

  @Autowired private WebApplicationContext context;
  @Autowired private TenantIdAllocationService tenantIdAllocationService;
  @Autowired private TenantIdReservationRepository reservationRepository;

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
    reservationRepository.deleteAll();
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
  void tearDown() {
    reservationRepository.deleteAll();
  }

  // ---- (a) reads the UserService makes as technical ----

  @Test
  void technicalOnly_Should_readAnySingleTenantsDataDpaAndPolicies() throws Exception {
    assertOnboardingReadsSucceed(technicalOnly());
  }

  @Test
  void technicalWithTenantAdmin_Should_keepReadingExactlyAsBefore() throws Exception {
    assertOnboardingReadsSucceed(technicalWithTenantAdmin());
  }

  private void assertOnboardingReadsSucceed(RequestPostProcessor caller) throws Exception {
    for (long tenantId : List.of(1L, 2L)) {
      mvc.perform(get("/tenant/" + tenantId).with(caller))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(tenantId));
      mvc.perform(get("/tenantadmin/" + tenantId + "/dpa/versions").with(caller))
          .andExpect(status().isOk());
      mvc.perform(get("/tenantadmin/" + tenantId + "/dpa/signatures").with(caller))
          .andExpect(status().isOk());
      mvc.perform(get("/tenantadmin/" + tenantId + "/permission-policies").with(caller))
          .andExpect(status().isOk());
    }
  }

  // ---- (b) createTenant during public onboarding ----

  @Test
  void technicalOnly_Should_createTenantWithAValidReservationToken() throws Exception {
    String token = reserve(RESERVED_ID);

    mvc.perform(createTenant(technicalOnly(), RESERVED_ID, token, "reserved"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(RESERVED_ID));

    assertThat(reservationRepository.findById(RESERVED_ID))
        .get()
        .extracting(reservation -> reservation.getStatus())
        .isEqualTo(TenantIdReservationStatus.ASSIGNED);
  }

  @Test
  void technicalWithTenantAdmin_Should_createTenantWithAValidReservationTokenAsBefore()
      throws Exception {
    String token = reserve(RESERVED_ID);

    mvc.perform(createTenant(technicalWithTenantAdmin(), RESERVED_ID, token, "reserved"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(RESERVED_ID));
  }

  @Test
  void technicalWithTenantAdmin_Should_stillCreateWithoutReservationAsBefore() throws Exception {
    mvc.perform(createTenant(technicalWithTenantAdmin(), null, null, "automode"))
        .andExpect(status().isOk());
  }

  @Test
  void technicalOnly_Should_beForbiddenToCreateWithoutAnyReservation() throws Exception {
    mvc.perform(createTenant(technicalOnly(), null, null, "automode"))
        .andExpect(status().isForbidden());
  }

  @Test
  void technicalOnly_Should_beForbiddenToCreateAFreeIdWithoutReservation() throws Exception {
    mvc.perform(createTenant(technicalOnly(), FREE_ID, null, "freeid"))
        .andExpect(status().isForbidden());
    mvc.perform(createTenant(technicalOnly(), FREE_ID, "made-up-token", "freeid"))
        .andExpect(status().isForbidden());
  }

  @Test
  void technicalOnly_Should_notConsumeAReservationWithoutItsToken() throws Exception {
    reserve(RESERVED_ID);

    mvc.perform(createTenant(technicalOnly(), RESERVED_ID, null, "notoken"))
        .andExpect(status().isForbidden());
    mvc.perform(createTenant(technicalOnly(), RESERVED_ID, "wrong-token", "wrongtoken"))
        .andExpect(status().isConflict());

    assertThat(reservationRepository.findById(RESERVED_ID))
        .get()
        .extracting(reservation -> reservation.getStatus())
        .isEqualTo(TenantIdReservationStatus.RESERVED);
  }

  // ---- (c) reservation cleanup ----

  @Test
  void technicalOnly_Should_releaseAnUnconsumedReservation() throws Exception {
    reserve(RESERVED_ID);

    mvc.perform(delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID).with(technicalOnly()))
        .andExpect(status().isNoContent());

    assertThat(reservationRepository.findById(RESERVED_ID)).isEmpty();
  }

  @Test
  void technicalWithTenantAdmin_Should_releaseAnUnconsumedReservationAsBefore() throws Exception {
    reserve(RESERVED_ID);

    mvc.perform(
            delete("/tenantadmin/tenant-ids/reservations/" + RESERVED_ID)
                .with(technicalWithTenantAdmin()))
        .andExpect(status().isNoContent());
  }

  // ---- everything else stays closed to the technical-only identity ----

  @Test
  void technicalOnly_Should_beForbiddenEverywhereElse() throws Exception {
    var caller = technicalOnly();

    mvc.perform(get("/tenant").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/search").param("query", "*").with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1").with(caller)).andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withName("changed")
                        .withSubdomain("changed")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1/permission-policies")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{\"tenantId\":1,\"policies\":{}}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/dpia").with(caller)).andExpect(status().isForbidden());
    mvc.perform(put("/tenantadmin/dpia").with(caller).contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/legal-drafts/PRIVACY").with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1/legal-drafts/PRIVACY")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{\"content\":{\"de\":\"<p>x</p>\"},\"revision\":\"new\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(put("/tenantadmin/1/dpa").with(caller).contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/tenantadmin/tenant-ids/reservations")
                .with(caller)
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/controls").with(caller)).andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/translation/keys").with(caller))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/tenant/1").with(caller)).andExpect(status().isForbidden());
  }

  private String reserve(long tenantId) {
    return tenantIdAllocationService.reserve(tenantId, "test").getToken();
  }

  private org.springframework.test.web.servlet.RequestBuilder createTenant(
      RequestPostProcessor caller, Long id, String reservationToken, String subdomain) {
    var builder =
        new MultilingualTenantTestDataBuilder()
            .withName("onboarded " + subdomain)
            .withSubdomain(subdomain)
            .withLicensing();
    if (id != null) {
      builder.withId(id);
    }
    if (reservationToken != null) {
      builder.withTenantIdReservationToken(reservationToken);
    }
    return post("/tenantadmin")
        .with(caller)
        .contentType(APPLICATION_JSON)
        .content(builder.jsonify());
  }

  private static RequestPostProcessor technicalOnly() {
    return serviceIdentity(Set.of("default-roles-online-beratung", "technical"));
  }

  private static RequestPostProcessor technicalWithTenantAdmin() {
    return serviceIdentity(Set.of("default-roles-online-beratung", "technical", "tenant-admin"));
  }

  private static RequestPostProcessor serviceIdentity(Set<String> realmRoles) {
    return jwt()
        .jwt(
            token ->
                token
                    .subject("technical-service-subject")
                    .claim("azp", "app")
                    .claim("tenantId", 0L)
                    .claim("username", "technical")
                    .claim("realm_access", Map.of("roles", List.copyOf(realmRoles))))
        .authorities(new RoleAuthorizationAuthorityMapper().mapAuthorities(realmRoles));
  }
}
