package com.vi.tenantservice.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
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
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "ORISO_WIZARD_POLICY_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "ORISO_TENANT_CREATION_CONTEXT_KEY=",
      "TASK_IDENTITY_REQUIRED_TASKS=",
      "IDENTITY_CONFIG_WIZARD_CLIENT_ID=backend-config-wizard",
      "IDENTITY_CONFIG_WIZARD_SERVICE_SUBJECT=wizard-subject",
      "IDENTITY_INVITE_RESERVATIONS_CLIENT_ID=backend-invite-reservations",
      "IDENTITY_INVITE_RESERVATIONS_SERVICE_SUBJECT=reservation-subject",
      "IDENTITY_RUNTIME_POLICY_CLIENT_ID=backend-runtime-policy",
      "IDENTITY_RUNTIME_POLICY_SERVICE_SUBJECT=policy-subject",
      "IDENTITY_NOTIFICATION_DISPATCH_CLIENT_ID=backend-notification-dispatch",
      "IDENTITY_NOTIFICATION_DISPATCH_SERVICE_SUBJECT=dispatch-subject"
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class WizardTenantCreationCompensationIT {
  @Autowired private WebApplicationContext context;
  @Autowired private TenantIdAllocationService tenantIdAllocationService;
  @Autowired private TenantIdReservationRepository reservationRepository;
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

  @Autowired private TenantRepository tenantRepository;
  @Autowired private ConfigurableEnvironment environment;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
  private static final String KEY_PROPERTY = "ORISO_TENANT_CREATION_CONTEXT_KEY";
  private static final String OVERRIDE = "wizardProofFailure";
  private static final String VALID_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    environment.getPropertySources().remove(OVERRIDE);
    tenantRepository.findById(500L).ifPresent(tenantRepository::delete);
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
    environment.getPropertySources().remove(OVERRIDE);
    tenantRepository.findById(500L).ifPresent(tenantRepository::delete);
    reservationRepository.deleteAll();
  }

  @Test
  void missingBootstrapKeyCannotLeaveATenantOrConsumeItsReservation() throws Exception {
    String token = tenantIdAllocationService.reserve(500L, "original-owner").getToken();
    var wizard = task("wizard-subject", "backend-config-wizard", "config-wizard", "tenantservice");
    mvc.perform(createTenant(wizard, 500L, token, "missingproof"))
        .andExpect(status().isForbidden());
    var tenant = tenantRepository.findById(500L);
    var reservation = reservationRepository.findById(500L).orElseThrow();
    org.assertj.core.api.SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(tenant).isEmpty();
          softly.assertThat(reservation.getStatus()).isEqualTo(TenantIdReservationStatus.RESERVED);
          softly.assertThat(reservation.getToken()).isEqualTo(token);
        });
    org.mockito.Mockito.verifyNoInteractions(consultingTypeService);
  }

  @Test
  void signingFailureAfterPersistenceRestoresOnlyTheOriginalReservationAndAllowsRetry()
      throws Exception {
    String token = tenantIdAllocationService.reserve(500L, "original-owner").getToken();
    String unrelatedToken = tenantIdAllocationService.reserve(501L, "other-owner").getToken();
    var persistedAtSigning = new java.util.concurrent.atomic.AtomicBoolean();
    environment
        .getPropertySources()
        .addFirst(
            new org.springframework.core.env.MapPropertySource(
                OVERRIDE, Map.of(KEY_PROPERTY, VALID_KEY)) {
              @Override
              public Object getProperty(String name) {
                if (!KEY_PROPERTY.equals(name)) return null;
                if (jdbc.queryForObject("select count(*) from tenant where id=500", Integer.class)
                    == 0) {
                  return VALID_KEY;
                }
                persistedAtSigning.set(
                    "ASSIGNED"
                            .equals(
                                jdbc.queryForObject(
                                    "select status from tenant_id_reservation where tenant_id=500",
                                    String.class))
                        && token.equals(
                            jdbc.queryForObject(
                                "select token from tenant_id_reservation where tenant_id=500",
                                String.class)));
                return "";
              }
            });
    org.assertj.core.api.Assertions.assertThat(
            VALID_KEY.equals(environment.getProperty(KEY_PROPERTY)))
        .isTrue();
    var wizard = task("wizard-subject", "backend-config-wizard", "config-wizard", "tenantservice");
    mvc.perform(createTenant(wizard, 500L, token, "lateproof")).andExpect(status().isForbidden());
    org.assertj.core.api.Assertions.assertThat(persistedAtSigning).isTrue();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("select count(*) from tenant where id=500", Integer.class))
        .isZero();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select status from tenant_id_reservation where tenant_id=500", String.class))
        .isEqualTo("RESERVED");
    org.assertj.core.api.Assertions.assertThat(
            token.equals(
                jdbc.queryForObject(
                    "select token from tenant_id_reservation where tenant_id=500", String.class)))
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "select status from tenant_id_reservation where tenant_id=501", String.class))
        .isEqualTo("RESERVED");
    org.assertj.core.api.Assertions.assertThat(
            unrelatedToken.equals(
                jdbc.queryForObject(
                    "select token from tenant_id_reservation where tenant_id=501", String.class)))
        .isTrue();
    org.mockito.Mockito.verifyNoInteractions(consultingTypeService);

    environment.getPropertySources().remove(OVERRIDE);
    environment
        .getPropertySources()
        .addFirst(
            new org.springframework.core.env.MapPropertySource(
                OVERRIDE, Map.of(KEY_PROPERTY, VALID_KEY)));
    mvc.perform(createTenant(wizard, 500L, token, "lateproof")).andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(tenantRepository.findById(500L)).isPresent();
    org.assertj.core.api.Assertions.assertThat(
            reservationRepository.findById(500L).orElseThrow().getStatus())
        .isEqualTo(TenantIdReservationStatus.ASSIGNED);
  }

  private RequestPostProcessor task(String subject, String client, String role, String audience) {
    return jwt()
        .jwt(
            token ->
                token
                    .issuer("https://identity.example/realms/test")
                    .subject(subject)
                    .claim("azp", client)
                    .audience(List.of(audience))
                    .issuedAt(java.time.Instant.now().minusSeconds(10))
                    .expiresAt(java.time.Instant.now().plusSeconds(60))
                    .claim("tenantId", 0L)
                    .claim(
                        "realm_access", Map.of("roles", java.util.Arrays.asList(role.split(",")))))
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
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
}
