package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

/** Actual native bearer reaches the receiver through the normal JWT decoder and HTTP server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testing")
@EnabledIfEnvironmentVariable(named = "ORISO_TASK_TOKEN_FIXTURE", matches = ".+")
@TestPropertySource(
    properties = {
      "multitenancy.enabled=true",
      "feature.multitenancy.with.single.domain.enabled=true",
      "csrf.header.property=csrfHeader",
      "csrf.cookie.property=csrfCookie",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "ORISO_WIZARD_POLICY_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "ORISO_TENANT_CREATION_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class RealTaskTokenAuthorizationIT {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  @LocalServerPort private int port;
  @Autowired private TenantIdAllocationService allocation;
  @Autowired private TenantIdReservationRepository reservations;

  @MockitoBean
  private com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService settings;

  @MockitoBean
  private com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory
      settingsFactory;

  @MockitoBean
  private com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory
      typeFactory;

  @MockitoBean
  private com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      typeApi;

  @MockitoBean private com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier headers;
  @MockitoBean private com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService types;
  @MockitoBean private com.vi.tenantservice.api.service.consultingtype.UserAdminService adminUsers;

  private static JsonNode fixture() {
    try {
      return JSON.readTree(Files.readString(Path.of(System.getenv("ORISO_TASK_TOKEN_FIXTURE"))));
    } catch (Exception failure) {
      throw new IllegalStateException("Synthetic native task fixture unavailable");
    }
  }

  @DynamicPropertySource
  static void nativeBindings(DynamicPropertyRegistry properties) {
    JsonNode fixture = fixture();
    String issuer = fixture.path("issuer").asText();
    properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
    properties.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> issuer + "/protocol/openid-connect/certs");
    for (JsonNode task : fixture.path("tasks")) {
      String prefix = "IDENTITY_" + task.path("key").asText();
      properties.add(prefix + "_CLIENT_ID", () -> task.path("clientId").asText());
      properties.add(prefix + "_SERVICE_SUBJECT", () -> task.path("subject").asText());
    }
  }

  private String token(String key) throws Exception {
    JsonNode fixture = fixture();
    JsonNode task = null;
    for (JsonNode candidate : fixture.path("tasks")) {
      if (key.equals(candidate.path("key").asText())) {
        task = candidate;
      }
    }
    if (task == null) {
      throw new IllegalArgumentException("Unknown synthetic task");
    }
    String form =
        "grant_type=client_credentials&client_id="
            + URLEncoder.encode(task.path("clientId").asText(), StandardCharsets.UTF_8)
            + "&client_secret="
            + URLEncoder.encode(task.path("secret").asText(), StandardCharsets.UTF_8);
    var request =
        HttpRequest.newBuilder(
                URI.create(fixture.path("issuer").asText() + "/protocol/openid-connect/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return JSON.readTree(response.body()).path("access_token").asText();
  }

  private HttpResponse<String> call(String method, String path, String bearer, String body)
      throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/json")
            .header("tenantId", "0")
            .header("csrfHeader", "test")
            .header("Cookie", "csrfCookie=test");
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    request.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
    return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  @BeforeEach
  void setup() {
    reservations.deleteAll();
    when(settings.getApplicationSettings())
        .thenReturn(
            new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                    .ApplicationSettingsDTO()
                .mainTenantSubdomainForSingleDomainMultitenancy(
                    new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                            .SettingDTO()
                        .value("app")));
    when(typeFactory.createControllerApi()).thenReturn(typeApi);
    when(typeApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(headers.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(headers.getKeycloakAndCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
  }

  @AfterEach
  void cleanup() {
    reservations.deleteAll();
  }

  @Test
  void actualWizardCreatesOnlyOwnedReservationAndReadsOnlyOperatorDpa() throws Exception {
    String proof = allocation.reserve(9137L, "native-test").getToken();
    String actor = token("CONFIG_WIZARD");
    String body =
        new MultilingualTenantTestDataBuilder()
            .withName("Native Wizard")
            .withSubdomain("nativewizard")
            .withLicensing()
            .withId(9137L)
            .withTenantIdReservationToken(proof)
            .jsonify();
    assertThat(call("POST", "/tenantadmin", actor, body).statusCode()).isEqualTo(200);
    assertThat(reservations.findById(9137L).orElseThrow().getStatus())
        .isEqualTo(com.vi.tenantservice.api.model.TenantIdReservationStatus.ASSIGNED);
    assertThat(call("GET", "/tenant", actor, null).statusCode()).isEqualTo(403);
    assertThat(call("GET", "/tenantadmin/1/dpa/versions", actor, null).statusCode()).isEqualTo(200);
    assertThat(call("GET", "/tenantadmin/2/dpa/versions", actor, null).statusCode()).isEqualTo(403);
    assertThat(
            call("GET", "/internal/tenants/1/account-provisioning-policy", actor, null)
                .statusCode())
        .isEqualTo(403);
  }

  @Test
  void actualReservationActorReleasesOnlyExactUnconsumedProof() throws Exception {
    String proof = allocation.reserve(9137L, "native-test").getToken();
    String actor = token("INVITE_RESERVATIONS");
    String path = "/tenantadmin/tenant-ids/reservations/9137";
    assertThat(call("DELETE", path, actor, null).statusCode()).isEqualTo(403);
    assertThat(call("DELETE", path + "?reservationToken=foreign", actor, null).statusCode())
        .isEqualTo(403);
    assertThat(reservations.existsById(9137L)).isTrue();
    assertThat(
            call(
                    "DELETE",
                    path + "?reservationToken=" + URLEncoder.encode(proof, StandardCharsets.UTF_8),
                    actor,
                    null)
                .statusCode())
        .isEqualTo(204);
    assertThat(reservations.existsById(9137L)).isFalse();
    assertThat(call("POST", "/tenantadmin/tenant-ids/reservations", actor, "{}").statusCode())
        .isEqualTo(403);
  }

  @Test
  void actualPolicyAndDispatchActorsHaveSeparatedMinimalReadProjections() throws Exception {
    String policy = token("RUNTIME_POLICY");
    assertThat(call("GET", "/tenantadmin/1/dpa/gate", policy, null).statusCode()).isEqualTo(200);
    assertThat(call("GET", "/tenantadmin/1/permission-policies", policy, null).statusCode())
        .isEqualTo(200);
    assertThat(call("GET", "/tenant/1", policy, null).statusCode()).isEqualTo(403);
    assertThat(call("GET", "/tenantadmin/1/dpa/signatures", policy, null).statusCode())
        .isEqualTo(403);
    String dispatch = token("NOTIFICATION_DISPATCH");
    var read = call("GET", "/internal/tenants/2/system-email-context", dispatch, null);
    assertThat(read.statusCode()).isEqualTo(200);
    assertThat(JSON.readTree(read.body()).path("id").asLong()).isEqualTo(2);
    JsonNode smtp = JSON.readTree(read.body()).path("settings").path("smtp");
    assertThat(smtp.has("password") || smtp.has("username") || smtp.has("host")).isFalse();
    assertThat(call("GET", "/tenantadmin/2/dpa/signatures", dispatch, null).statusCode())
        .isEqualTo(200);
    assertThat(call("GET", "/tenantadmin/2/dpa/gate", dispatch, null).statusCode()).isEqualTo(403);
  }

  @Test
  void realSignedWrongBindingsOrUnrelatedGrantsCannotUseTaskOrHumanBranch() throws Exception {
    for (String fault : List.of("mixedRoles", "wrongAudience", "wrongSubject")) {
      String actor = fixture().path("variants").path("RUNTIME_POLICY").path(fault).asText();
      assertThat(actor).isNotBlank();
      assertThat(call("GET", "/tenantadmin/1/dpa/gate", actor, null).statusCode()).isEqualTo(403);
      assertThat(call("GET", "/tenant", actor, null).statusCode()).isEqualTo(403);
    }
  }

  @Test
  void invalidNativeSignatureIsRejectedByTheActualDecoder() throws Exception {
    String actor = token("CONFIG_WIZARD");
    int signature = actor.lastIndexOf('.') + 1;
    String changed =
        actor.substring(0, signature)
            + (actor.charAt(signature) == 'A' ? 'B' : 'A')
            + actor.substring(signature + 1);
    assertThat(call("GET", "/tenantadmin/1/dpa/versions", changed, null).statusCode())
        .isEqualTo(401);
  }
}
