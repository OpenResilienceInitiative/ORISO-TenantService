package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.RoleAuthorizationAuthorityMapper;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.WelcomeMessageDTO;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "consulting.type.service.api.url=http://cts.fixture.invalid"
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class TenantConsultingSettingsOutcomeIT {

  private static final String CTS = "http://cts.fixture.invalid";
  private static final String READ_PATH = CTS + "/consultingtypes/bytenant/1/full";
  private static final String WRITE_PATH = CTS + "/consultingtypes/2";
  private static final String REQUESTED_NAME = "Settings transaction fixture";
  private static final String OUTCOME_UNKNOWN = "CONSULTING_SETTINGS_OUTCOME_UNKNOWN";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate tenantStore;
  @Autowired private RestTemplate restTemplate;

  @MockitoBean private ApplicationSettingsService applicationSettingsService;
  @MockitoBean private UserAdminService userAdminService;

  private enum Outcome {
    PATCH_TIMEOUT(502, true, OUTCOME_UNKNOWN),
    PATCH_UPSTREAM_ERROR(502, true, OUTCOME_UNKNOWN),
    PATCH_UNREADABLE_RESPONSE(502, true, OUTCOME_UNKNOWN),
    PATCH_FORBIDDEN(403, false, null),
    READ_UNAVAILABLE(503, false, null),
    PATCH_ACKNOWLEDGED(200, true, null);

    final int httpStatus;
    final boolean ctsChanged;
    final String reason;

    Outcome(int httpStatus, boolean ctsChanged, String reason) {
      this.httpStatus = httpStatus;
      this.ctsChanged = ctsChanged;
      this.reason = reason;
    }
  }

  @ParameterizedTest
  @EnumSource(Outcome.class)
  void putReportsWhetherTheConsultingSettingsWriteWasConfirmed(Outcome outcome) throws Exception {
    var ctsDatabase =
        new EmbeddedDatabaseBuilder()
            .generateUniqueName(true)
            .setType(EmbeddedDatabaseType.H2)
            .build();
    try {
      var ctsStore = new JdbcTemplate(ctsDatabase);
      ctsStore.execute(
          "CREATE TABLE consulting_type (id INT PRIMARY KEY, language_formal BOOLEAN)");
      ctsStore.update("INSERT INTO consulting_type VALUES (2, false)");
      var originalName = persistedTenantName();
      var server = MockRestServiceServer.bindTo(restTemplate).build();
      server
          .expect(requestTo(READ_PATH))
          .andExpect(method(HttpMethod.GET))
          .andRespond(
              outcome == Outcome.READ_UNAVAILABLE
                  ? withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                  : withSuccess(consultingTypeResponse(false), APPLICATION_JSON));

      if (outcome != Outcome.READ_UNAVAILABLE) {
        server
            .expect(requestTo(WRITE_PATH))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(
                request -> {
                  assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                      .isTrue();
                  assertThat(persistedTenantName()).isEqualTo(REQUESTED_NAME);
                  if (outcome.ctsChanged) {
                    persistRemoteChange(ctsDatabase);
                  }
                  return switch (outcome) {
                    case PATCH_TIMEOUT ->
                        throw new SocketTimeoutException("Synthetic CTS read timeout");
                    case PATCH_UPSTREAM_ERROR ->
                        withStatus(HttpStatus.INTERNAL_SERVER_ERROR).createResponse(request);
                    case PATCH_UNREADABLE_RESPONSE ->
                        withSuccess("{", APPLICATION_JSON).createResponse(request);
                    case PATCH_FORBIDDEN ->
                        withStatus(HttpStatus.FORBIDDEN).createResponse(request);
                    default ->
                        withSuccess(consultingTypeResponse(true), APPLICATION_JSON)
                            .createResponse(request);
                  };
                });
      }
      if (outcome == Outcome.PATCH_ACKNOWLEDGED) {
        server
            .expect(requestTo(READ_PATH))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(consultingTypeResponse(true), APPLICATION_JSON));
      }

      var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
      var response =
          mvc.perform(
                  put("/tenantadmin/1")
                      .with(platformAdmin())
                      .contentType(APPLICATION_JSON)
                      .content(tenantUpdate()))
              .andReturn()
              .getResponse();

      server.verify();
      assertThat(persistedTenantName())
          .isEqualTo(outcome == Outcome.PATCH_ACKNOWLEDGED ? REQUESTED_NAME : originalName);
      assertThat(
              ctsStore.queryForObject(
                  "SELECT language_formal FROM consulting_type WHERE id = 2", Boolean.class))
          .isEqualTo(outcome.ctsChanged);
      assertThat(response.getStatus()).isEqualTo(outcome.httpStatus);
      assertThat(response.getHeader("X-Reason")).isEqualTo(outcome.reason);
    } finally {
      ctsDatabase.shutdown();
    }
  }

  private String persistedTenantName() {
    return tenantStore.queryForObject("SELECT name FROM tenant WHERE id = 1", String.class);
  }

  private static void persistRemoteChange(EmbeddedDatabase ctsDatabase) {
    // The CTS store completes its own transaction before returning any HTTP result.
    try (var connection = ctsDatabase.getConnection();
        var update =
            connection.prepareStatement(
                "UPDATE consulting_type SET language_formal = true WHERE id = 2")) {
      assertThat(connection.getAutoCommit()).isTrue();
      assertThat(update.executeUpdate()).isEqualTo(1);
    } catch (java.sql.SQLException e) {
      throw new AssertionError("Could not write the isolated CTS store", e);
    }
  }

  private static String consultingTypeResponse(boolean languageFormal) {
    return "{\"id\":2,\"languageFormal\":" + languageFormal + "}";
  }

  private static String tenantUpdate() {
    var request =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withId(1L)
            .withName(REQUESTED_NAME)
            .withSubdomain("happylife")
            .withLicensing(5)
            .withSettings();
    request
        .build()
        .getSettings()
        .setExtendedSettings(
            new ConsultingTypePatchDTO()
                .languageFormal(true)
                .welcomeMessage(new WelcomeMessageDTO()));
    return request.jsonify();
  }

  private static RequestPostProcessor platformAdmin() {
    var roles = Set.of("user-admin", "agency-admin", "tenant-admin", "topic-admin");
    return jwt()
        .jwt(
            token ->
                token
                    .subject("settings-fixture")
                    .claim("tenantId", "0")
                    .claim("username", "settings-fixture")
                    .claim("realm_access", Map.of("roles", List.copyOf(roles))))
        .authorities(new RoleAuthorizationAuthorityMapper().mapAuthorities(roles));
  }
}
