package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

/**
 * Real Tenant caller and DTO serializer reach the actual CTS receiver through an observing proxy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testing")
@TestPropertySource(
    properties = {
      "multitenancy.enabled=true",
      "feature.multitenancy.with.single.domain.enabled=true",
      "csrf.header.property=csrfHeader",
      "csrf.cookie.property=csrfCookie",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=CONFIG_WIZARD",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "spring.datasource.url=jdbc:h2:mem:bootstrap-native-tenant;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.datasource.hikari.maximum-pool-size=4",
      "spring.datasource.hikari.minimum-idle=0",
      "spring.liquibase.enabled=false",
      "ORISO_WIZARD_POLICY_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "ORISO_TENANT_CREATION_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class BootstrapV2CallerReceiverIT {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private static final AtomicReference<String> SENT_PROOF = new AtomicReference<>();
  private static final AtomicReference<byte[]> SENT_BODY = new AtomicReference<>();
  private static final AtomicInteger RECEIVER_STATUS = new AtomicInteger(-1);
  private static HttpServer gateway;
  @LocalServerPort private int port;
  @Autowired private TenantIdAllocationService allocation;

  @MockitoBean
  private com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService settings;

  private static JsonNode fixture() {
    try {
      return JSON.readTree(Files.readString(Path.of(required("ORISO_TASK_TOKEN_FIXTURE"))));
    } catch (Exception invalidFixture) {
      throw new IllegalStateException("Synthetic bootstrap fixture is unavailable");
    }
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Bootstrap round-trip fixture configuration missing: " + name);
    }
    return value;
  }

  @DynamicPropertySource
  static void nativeCallerAndReceiver(DynamicPropertyRegistry properties) throws Exception {
    JsonNode fixture = fixture();
    assertThat(fixture.path("identityModel").asText()).isEqualTo("FIVE_ACTORS");
    String issuer = fixture.path("issuer").asText();
    properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
    properties.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> issuer + "/protocol/openid-connect/certs");
    for (JsonNode actor : fixture.path("tasks")) {
      String prefix = "IDENTITY_" + actor.path("key").asText();
      properties.add(prefix + "_CLIENT_ID", () -> actor.path("clientId").asText());
      properties.add(prefix + "_SERVICE_SUBJECT", () -> actor.path("subject").asText());
    }
    properties.add("ORISO_TASK_CONTEXT_KEY", () -> required("ORISO_BOOTSTRAP_SHARED_KEY"));
    properties.add(
        "default.consulting.types.json.path",
        () ->
            Path.of("src/test/resources/settings/bootstrap-v2-consulting-type.json")
                .toAbsolutePath()
                .toString());
    String receiver = required("ORISO_CTS_BOOTSTRAP_RECEIVER_URL");
    gateway = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    gateway.createContext(
        "/",
        exchange -> {
          byte[] body = exchange.getRequestBody().readAllBytes();
          var request =
              HttpRequest.newBuilder(URI.create(receiver + exchange.getRequestURI()))
                  .timeout(Duration.ofSeconds(30));
          for (String header :
              List.of(
                  "Authorization",
                  "Content-Type",
                  "csrfHeader",
                  "Cookie",
                  "tenantId",
                  "X-ORISO-Tenant-Creation-Context")) {
            String value = exchange.getRequestHeaders().getFirst(header);
            if (value != null) request.header(header, value);
          }
          request.method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(body));
          try {
            var response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if ("POST".equals(exchange.getRequestMethod())
                && "/consultingtypes".equals(exchange.getRequestURI().getPath())) {
              SENT_BODY.set(body);
              SENT_PROOF.set(
                  exchange.getRequestHeaders().getFirst("X-ORISO-Tenant-Creation-Context"));
              RECEIVER_STATUS.set(response.statusCode());
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.statusCode(), response.body().length);
            exchange.getResponseBody().write(response.body());
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            exchange.sendResponseHeaders(503, -1);
          } finally {
            exchange.close();
          }
        });
    gateway.start();
    properties.add(
        "consulting.type.service.api.url",
        () -> "http://127.0.0.1:" + gateway.getAddress().getPort());
  }

  @BeforeEach
  void setup() {
    SENT_BODY.set(null);
    SENT_PROOF.set(null);
    RECEIVER_STATUS.set(-1);
    when(settings.getApplicationSettings())
        .thenReturn(
            new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                    .ApplicationSettingsDTO()
                .mainTenantSubdomainForSingleDomainMultitenancy(
                    new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                            .SettingDTO()
                        .value("app")));
  }

  @AfterAll
  static void cleanup() {
    if (gateway != null) gateway.stop(0);
  }

  @Test
  void ownedTenantCreationUsesTheSharedV2ProofAtTheMaterializedDtoSendSeam() throws Exception {
    long tenantId = 9157L;
    String reservation = allocation.reserve(tenantId, "native-bootstrap-v2").getToken();
    String body =
        new MultilingualTenantTestDataBuilder()
            .withName("Bootstrap V2")
            .withSubdomain("nativebootstrapv2")
            .withLicensing()
            .withId(tenantId)
            .withTenantIdReservationToken(reservation)
            .jsonify();
    JsonNode setup = null;
    for (JsonNode actor : fixture().path("tasks")) {
      if ("SETUP".equals(actor.path("key").asText())) setup = actor;
    }
    assertThat(setup).isNotNull();
    String form =
        "grant_type=client_credentials&client_id="
            + URLEncoder.encode(setup.path("clientId").asText(), StandardCharsets.UTF_8)
            + "&client_secret="
            + URLEncoder.encode(setup.path("secret").asText(), StandardCharsets.UTF_8);
    var login =
        HTTP.send(
            HttpRequest.newBuilder(
                    URI.create(
                        fixture().path("issuer").asText() + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(login.statusCode()).isEqualTo(200);
    String bearer = JSON.readTree(login.body()).path("access_token").asText();
    var created =
        HTTP.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/tenantadmin"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearer)
                .header("tenantId", "0")
                .header("csrfHeader", "test")
                .header("Cookie", "csrfCookie=test")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    Files.writeString(
        Path.of(required("ORISO_BOOTSTRAP_OBSERVED")),
        JSON.writeValueAsString(
            Map.of(
                "receiverStatus",
                RECEIVER_STATUS.get(),
                "tenantStatus",
                created.statusCode(),
                "receiverRequestObserved",
                SENT_BODY.get() != null,
                "allocationStatus",
                allocation.getStatus(tenantId).name())));
    // Different legacy signing keys on the two servers cannot grant C acceptance.
    // This first RED must arise after the actual request reaches the CTS HTTP guard.
    assertThat(RECEIVER_STATUS.get()).isEqualTo(200);
    assertThat(created.statusCode()).isEqualTo(200);
    assertThat(JSON.readTree(SENT_BODY.get()).path("tenantId").asLong()).isEqualTo(tenantId);
    String proof = SENT_PROOF.get();
    assertThat(proof).isNotBlank();
    JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(proof.split("\\.", -1)[0]));
    assertThat(claims.size()).isEqualTo(16);
    assertThat(claims.path("v").asInt()).isEqualTo(2);
    assertThat(claims.path("actor").asText()).isEqualTo("SETUP");
    assertThat(claims.path("purpose").asText()).isEqualTo("CONFIG_WIZARD");
    assertThat(claims.path("operation").asText()).isEqualTo("tenant.consulting-type.bootstrap");
    assertThat(claims.path("target").asText()).isEqualTo("tenant/9157/consulting-type/default");
    assertThat(claims.path("payloadDigest").asText()).matches("[a-f0-9]{64}");
  }
}
