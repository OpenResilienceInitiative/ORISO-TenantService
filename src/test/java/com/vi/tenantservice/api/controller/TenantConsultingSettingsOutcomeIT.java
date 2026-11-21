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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.RoleAuthorizationAuthorityMapper;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.WelcomeMessageDTO;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(OutputCaptureExtension.class)
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

  private static final String SAVES = "oriso.tenant.settings.save";
  private static final String PATCH = "oriso.tenant.consulting.settings.patch";
  private static final String REQUEST_ID = "X-Consulting-Settings-Request-ID";
  private static final String MEASUREMENT_LOG = "Consulting settings PATCH requestId=";

  @Autowired private WebApplicationContext context;
  @Autowired private MeterRegistry registry;
  @Autowired private JdbcTemplate tenantStore;
  @Autowired private RestTemplate restTemplate;

  @MockitoBean private ApplicationSettingsService applicationSettingsService;
  @MockitoBean private UserAdminService userAdminService;

  private enum Outcome {
    PATCH_SOCKET_TIMEOUT(502, true, OUTCOME_UNKNOWN, "timeout"),
    PATCH_JDK_TIMEOUT(502, true, OUTCOME_UNKNOWN, "timeout"),
    PATCH_TRANSPORT_ERROR(502, true, OUTCOME_UNKNOWN, "transport_error"),
    PATCH_UPSTREAM_ERROR(502, true, OUTCOME_UNKNOWN, "upstream_error"),
    PATCH_UNREADABLE_RESPONSE(502, true, OUTCOME_UNKNOWN, "invalid_response"),
    PATCH_FORBIDDEN(403, false, null, "refused"),
    PATCH_REDIRECTED(500, false, null, "other"),
    READ_UNAVAILABLE(503, false, null, null),
    READ_WITHOUT_PATCH_ID(500, false, null, null),
    PATCH_ACKNOWLEDGED(200, true, null, "confirmed");

    final int httpStatus;
    final boolean ctsChanged;
    final String reason;
    final String metricOutcome;

    Outcome(int httpStatus, boolean ctsChanged, String reason, String metricOutcome) {
      this.httpStatus = httpStatus;
      this.ctsChanged = ctsChanged;
      this.reason = reason;
      this.metricOutcome = metricOutcome;
    }
  }

  @ParameterizedTest
  @EnumSource(Outcome.class)
  void putReportsWhetherTheConsultingSettingsWriteWasConfirmed(
      Outcome outcome, CapturedOutput output) throws Exception {
    var savesBefore = saveCount();
    var patchesBefore = patchCount();
    var outcomeBefore = patchCount(outcome.metricOutcome);
    var durationBefore = patchDuration(outcome.metricOutcome);
    var requestId = new AtomicReference<String>();
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
          .andExpect(request -> assertThat(request.getHeaders().getFirst(REQUEST_ID)).isNull())
          .andRespond(
              switch (outcome) {
                case READ_UNAVAILABLE -> withStatus(HttpStatus.SERVICE_UNAVAILABLE);
                case READ_WITHOUT_PATCH_ID ->
                    withSuccess("{\"languageFormal\":false}", APPLICATION_JSON);
                default -> withSuccess(consultingTypeResponse(false), APPLICATION_JSON);
              });

      if (outcome.metricOutcome != null) {
        server
            .expect(requestTo(WRITE_PATH))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(
                request ->
                    requestId.set(canonicalRequestId(request.getHeaders().getFirst(REQUEST_ID))))
            .andRespond(
                request -> {
                  assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                      .isTrue();
                  assertThat(persistedTenantName()).isEqualTo(REQUESTED_NAME);
                  if (outcome.ctsChanged) {
                    persistRemoteChange(ctsDatabase);
                  }
                  return switch (outcome) {
                    case PATCH_SOCKET_TIMEOUT ->
                        throw new SocketTimeoutException("Synthetic CTS read timeout");
                    case PATCH_JDK_TIMEOUT ->
                        throw new HttpTimeoutException("Synthetic CTS JDK read timeout");
                    case PATCH_TRANSPORT_ERROR ->
                        throw new SocketException("Synthetic CTS socket failure");
                    case PATCH_UPSTREAM_ERROR ->
                        withStatus(HttpStatus.INTERNAL_SERVER_ERROR).createResponse(request);
                    case PATCH_UNREADABLE_RESPONSE ->
                        withSuccess("{", APPLICATION_JSON).createResponse(request);
                    case PATCH_FORBIDDEN ->
                        withStatus(HttpStatus.FORBIDDEN).createResponse(request);
                    case PATCH_REDIRECTED -> withStatus(HttpStatus.FOUND).createResponse(request);
                    default ->
                        withSuccess(consultingTypeResponse(true), APPLICATION_JSON)
                            .createResponse(request);
                  };
                });
      }

      var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
      var response =
          mvc.perform(
                  put("/tenantadmin/1")
                      .with(adminForTenant("0"))
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
      assertThat(saveCount()).isEqualTo(savesBefore + 1);
      assertThat(registry.get(SAVES).counter().getId().getTags()).isEmpty();

      var measurements =
          output.getAll().lines().filter(line -> line.contains(MEASUREMENT_LOG)).toList();
      if (outcome.metricOutcome == null) {
        assertThat(patchCount()).isEqualTo(patchesBefore);
        assertThat(measurements).isEmpty();
      } else {
        assertThat(patchCount()).isEqualTo(patchesBefore + 1);
        assertThat(patchCount(outcome.metricOutcome)).isEqualTo(outcomeBefore + 1);
        assertThat(patchDuration(outcome.metricOutcome)).isGreaterThan(durationBefore);
        assertThat(
                registry.get(PATCH).tag("outcome", outcome.metricOutcome).timer().getId().getTags())
            .hasSize(1);
        assertThat(measurements)
            .singleElement()
            .satisfies(
                line ->
                    assertThat(line.substring(line.indexOf(MEASUREMENT_LOG)).stripTrailing())
                        .matches(
                            MEASUREMENT_LOG
                                + requestId.get()
                                + " outcome="
                                + outcome.metricOutcome
                                + " elapsedMs=[0-9]+"));
      }
    } finally {
      ctsDatabase.shutdown();
    }
  }

  @Test
  void rejectsAnOutOfScopeTenantSaveBeforeCountingIt() throws Exception {
    var savesBefore = saveCount();
    var attemptsBefore = patchCount();
    var server = MockRestServiceServer.bindTo(restTemplate).build();
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    mvc.perform(
            put("/tenantadmin/1")
                .with(adminForTenant("2"))
                .contentType(APPLICATION_JSON)
                .content(tenantUpdate()))
        .andExpect(status().isForbidden());

    server.verify();
    assertThat(saveCount()).isEqualTo(savesBefore);
    assertThat(patchCount()).isEqualTo(attemptsBefore);
  }

  @Test
  void createsDistinctIdsAndDoesNotForwardTheBrowserSuppliedId() throws Exception {
    var requestIds = new ArrayList<String>();
    var browserId = UUID.randomUUID().toString();
    var server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    server
        .expect(ExpectedCount.twice(), requestTo(READ_PATH))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(consultingTypeResponse(false), APPLICATION_JSON));
    server
        .expect(ExpectedCount.twice(), requestTo(WRITE_PATH))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(
            request ->
                requestIds.add(canonicalRequestId(request.getHeaders().getFirst(REQUEST_ID))))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(
              put("/tenantadmin/1")
                  .with(adminForTenant("0"))
                  .header(REQUEST_ID, browserId)
                  .contentType(APPLICATION_JSON)
                  .content(tenantUpdate()))
          .andExpect(status().isForbidden());
    }

    server.verify();
    assertThat(requestIds).hasSize(2).doesNotHaveDuplicates().doesNotContain(browserId);
  }

  private double saveCount() {
    return registry.find(SAVES).counters().stream().mapToDouble(counter -> counter.count()).sum();
  }

  private long patchCount() {
    return registry.find(PATCH).timers().stream().mapToLong(Timer::count).sum();
  }

  private long patchCount(String outcome) {
    if (outcome == null) return 0;
    return registry.find(PATCH).tag("outcome", outcome).timers().stream()
        .mapToLong(Timer::count)
        .sum();
  }

  private double patchDuration(String outcome) {
    if (outcome == null) return 0;
    return registry.find(PATCH).tag("outcome", outcome).timers().stream()
        .mapToDouble(timer -> timer.totalTime(TimeUnit.NANOSECONDS))
        .sum();
  }

  private static String canonicalRequestId(String value) {
    assertThat(value).isNotNull();
    var id = UUID.fromString(value);
    assertThat(id.toString()).isEqualTo(value);
    assertThat(id.version()).isEqualTo(4);
    return value;
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

  private static RequestPostProcessor adminForTenant(String tenantId) {
    var roles = Set.of("user-admin", "agency-admin", "tenant-admin", "topic-admin");
    return jwt()
        .jwt(
            token ->
                token
                    .subject("settings-fixture")
                    .claim("tenantId", tenantId)
                    .claim("username", "settings-fixture")
                    .claim("realm_access", Map.of("roles", List.copyOf(roles))))
        .authorities(new RoleAuthorizationAuthorityMapper().mapAuthorities(roles));
  }
}
