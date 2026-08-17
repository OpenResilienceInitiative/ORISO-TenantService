package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Regression tests for ORISO-TenantService#197: the facade's input-validation rejections must reach
 * the client as HTTP 400, not HTTP 500.
 *
 * <p>These are deliberately end-to-end through the real {@code TenantServiceFacade} rather than a
 * stubbed one. The defect was that {@code jakarta.ws.rs.BadRequestException} could not even be
 * CONSTRUCTED at runtime — only the JAX-RS API jar was on the classpath, no implementation, so
 * {@code RuntimeDelegate.getInstance()} blew up inside the exception constructor. A test that stubs
 * the facade to throw would therefore never reproduce the bug; only running the production throw
 * site does. Asserting on the status code (and not on the exception type) is the point: with the
 * old code every one of these paths answered 500.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantBadRequestStatusIT {

  private static final String TENANTADMIN_RESOURCE = "/tenantadmin";
  private static final String TENANT_RESOURCE_SLASH = "/tenant/";

  /** The technical tenant, which {@code deleteTenant} must refuse to delete. */
  private static final long TECHNICAL_TENANT = 0L;

  private static final long ONBOARDED_TENANT = 42L;

  @Autowired private WebApplicationContext context;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaVersionRepository versionRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;

  @MockitoBean AuthorisationService authorisationService;
  @MockitoBean ApplicationSettingsService applicationSettingsService;
  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;
  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;
  @MockitoBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean TenantResolverService tenantResolverService;
  @MockitoBean ConsultingTypeService consultingTypeService;
  @MockitoBean UserAdminService userAdminService;
  @MockitoBean SubdomainExtractor subdomainExtractor;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    cleanUp();
    seedTenant(TECHNICAL_TENANT);
  }

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM tenant_dpa_admin_signature");
    versionRepository.deleteAll();
    tenantRepository.deleteAll();
    reservationRepository.deleteAll();
  }

  private void seedTenant(long id) {
    var now = LocalDateTime.now();
    tenantRepository.save(
        TenantEntity.builder()
            .id(id)
            .name("tenant-" + id)
            .subdomain("subdomain-" + id)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private void givenAuthoritiesOfRole(UserRole userRole) {
    when(authorisationService.hasAuthority(Mockito.any()))
        .thenAnswer(
            invocation ->
                Authority.getAuthoritiesByUserRole(userRole).contains(invocation.getArgument(0)));
  }

  private RequestPostProcessor platformAdmin() {
    givenAuthoritiesOfRole(TENANT_ADMIN);
    when(authorisationService.getUserId()).thenReturn("platform-admin-user");
    when(authorisationService.getUsername()).thenReturn("platformadmin");
    return authentication(
        new AuthenticationMockBuilder().withUserRole(TENANT_ADMIN.getValue()).build());
  }

  /** {@code TenantServiceFacade.deleteTenant} — "Technical tenant cannot be deleted." */
  @Test
  void deleteTenant_Should_returnBadRequest_When_theTechnicalTenantIsTargeted() throws Exception {
    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TECHNICAL_TENANT).with(platformAdmin()))
        .andExpect(status().isBadRequest());
  }

  /** {@code TenantServiceFacade.recordOnboardingDpaAcceptance} — incomplete signer identity. */
  @Test
  void createTenant_Should_returnBadRequest_When_onboardingDpaAcceptanceHasNoSignerUserId()
      throws Exception {
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(ONBOARDED_TENANT)
                        .withName("Traeger Nord")
                        .withSubdomain("traeger-nord")
                        .withLicensing()
                        .withOnboardingDpaAcceptance("  ", "Toni Tenantadmin", null)
                        .jsonify()))
        .andExpect(status().isBadRequest());
  }

  /** {@code TenantServiceFacade.parseDpaVersion} — dpaVersion is not an ISO-8601 timestamp. */
  @Test
  void createTenant_Should_returnBadRequest_When_onboardingDpaVersionIsNotATimestamp()
      throws Exception {
    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(ONBOARDED_TENANT)
                        .withName("Traeger Sued")
                        .withSubdomain("traeger-sued")
                        .withLicensing()
                        .withOnboardingDpaAcceptance(
                            "kc-onboarded-admin", "Toni Tenantadmin", "not-a-timestamp")
                        .jsonify()))
        .andExpect(status().isBadRequest());
  }
}
