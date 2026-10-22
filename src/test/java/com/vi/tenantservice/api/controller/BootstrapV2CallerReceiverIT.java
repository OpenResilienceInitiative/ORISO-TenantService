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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testing")
@EnabledIfEnvironmentVariable(named = "ORISO_TASK_TOKEN_FIXTURE", matches = ".+")
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
@Sql(
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"},
    statements = "ALTER TABLE tenant DROP CONSTRAINT IF EXISTS unique_subdomain")
class BootstrapV2CallerReceiverIT {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private static final AtomicReference<String> SENT_PROOF = new AtomicReference<>();
  private static final AtomicReference<byte[]> SENT_BODY = new AtomicReference<>();
  private static final AtomicInteger RECEIVER_STATUS = new AtomicInteger(-1);
  private static final java.util.concurrent.atomic.AtomicBoolean PAIR_ARMED =
      new java.util.concurrent.atomic.AtomicBoolean();
  private static final AtomicReference<ReplayPacket> REPLAY = new AtomicReference<>();
  private static final java.util.concurrent.CountDownLatch OWNER_RESPONSE_READY =
      new java.util.concurrent.CountDownLatch(1);
  private static final java.util.concurrent.CountDownLatch OWNER_RESPONSE_RELEASE =
      new java.util.concurrent.CountDownLatch(1);
  private static final AtomicInteger COMPENSATION_STATUS = new AtomicInteger(-1);
  private static final AtomicInteger COMPENSATION_ATTEMPTS = new AtomicInteger();
  private static final java.util.concurrent.atomic.AtomicBoolean COMPENSATION_REPLY_LOST =
      new java.util.concurrent.atomic.AtomicBoolean();
  private static final java.util.concurrent.atomic.AtomicBoolean SAME_COMPENSATION =
      new java.util.concurrent.atomic.AtomicBoolean();
  private static final AtomicReference<ReplayPacket> COMPENSATION_PACKET = new AtomicReference<>();
  private static long controlSequence;
  private static HttpServer gateway;
  @LocalServerPort private int port;
  @Autowired private TenantIdAllocationService allocation;
  @Autowired private jakarta.validation.Validator validator;

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
            Path.of("src/test/resources/settings/bootstrap-v2-complete-consulting-type.json")
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
          boolean pairedCreate =
              PAIR_ARMED.get()
                  && "POST".equals(exchange.getRequestMethod())
                  && "/consultingtypes".equals(exchange.getRequestURI().getPath())
                  && JSON.readTree(body).path("tenantId").asInt() == 9158;
          if (pairedCreate)
            REPLAY.set(
                new ReplayPacket(
                    body.clone(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("X-ORISO-Tenant-Creation-Context")));
          try {
            var response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if ("POST".equals(exchange.getRequestMethod())
                && "/consultingtypes".equals(exchange.getRequestURI().getPath())) {
              SENT_BODY.set(body);
              SENT_PROOF.set(
                  exchange.getRequestHeaders().getFirst("X-ORISO-Tenant-Creation-Context"));
              RECEIVER_STATUS.set(response.statusCode());
            }
            if ("/consultingtypes/bootstrap-compensation"
                .equals(exchange.getRequestURI().getPath())) {
              COMPENSATION_STATUS.set(response.statusCode());
              if (PAIR_ARMED.get()
                  && "POST".equals(exchange.getRequestMethod())
                  && JSON.readTree(body).path("tenantId").asInt() == 9158) {
                int number = COMPENSATION_ATTEMPTS.incrementAndGet();
                var packet =
                    new ReplayPacket(
                        body.clone(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("X-ORISO-Tenant-Creation-Context"));
                if (number == 1) COMPENSATION_PACKET.set(packet);
                if (response.statusCode() == 204 && number == 1) {
                  terminalCheckpoint("CAPTURE_TERMINAL");
                  COMPENSATION_REPLY_LOST.set(true);
                  return; // Close the real exchange AFTER actual CTS204, before delivering its
                  // acknowledgement.
                }
                if (response.statusCode() == 204 && number == 2) {
                  ReplayPacket first = COMPENSATION_PACKET.get();
                  SAME_COMPENSATION.set(
                      java.util.Arrays.equals(first.body, packet.body)
                          && proofNonce(first.proof).equals(proofNonce(packet.proof)));
                  terminalCheckpoint("CHECK_TERMINAL");
                }
              }
            }
            if (pairedCreate) {
              OWNER_RESPONSE_READY.countDown();
              if (!OWNER_RESPONSE_RELEASE.await(20, java.util.concurrent.TimeUnit.SECONDS))
                throw fixtureRefusal();
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(
                response.statusCode(), response.statusCode() == 204 ? -1 : response.body().length);
            if (response.body().length > 0) exchange.getResponseBody().write(response.body());
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
  @org.junit.jupiter.api.Order(1)
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

  @Test
  @org.junit.jupiter.api.Order(2)
  void aLaterTenantFailureRetiresOnlyItsIssuedAttemptAndReusesTheExactReservation()
      throws Exception {
    long tenantId = 9158L;
    String reservation = allocation.reserve(tenantId, "native-paired-bootstrap").getToken();
    String base =
        new MultilingualTenantTestDataBuilder()
            .withName("Paired Bootstrap")
            .withSubdomain("pairedbootstrap")
            .withLicensing()
            .withId(tenantId)
            .withTenantIdReservationToken(reservation)
            .jsonify();
    var invalid = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(base);
    invalid.set(
        "onboardingDpaAcceptance",
        JSON.createObjectNode()
            .put("accepted", false)
            .put("signerUserId", "synthetic-paired-signer")
            .put("signerName", "Synthetic Pair Signer"));
    assertThat(
            validator
                .validate(
                    JSON.treeToValue(
                        invalid, com.vi.tenantservice.api.model.MultilingualTenantDTO.class))
                .isEmpty())
        .as("actual required API fields are satisfied before the later domain denial")
        .isTrue();
    command("ARM");
    PAIR_ARMED.set(true);
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    java.util.concurrent.Future<HttpResponse<byte[]>> delayed = null;
    try {
      String bearer = setupBearer();
      var creating = executor.submit(() -> publicCreate(invalid.toString(), bearer));
      JsonNode reached = waitFor("primaryInsertReached");
      ReplayPacket original = REPLAY.get();
      assertThat(original != null).as("actual issued request retained privately").isTrue();
      delayed = executor.submit(() -> replay(original));
      reached = waitFor("delayedInsertReached");
      assertThat(reached.path("primaryInsertReached").asBoolean()).isTrue();
      assertThat(reached.path("delayedInsertReached").asBoolean()).isTrue();
      command("RELEASE_PRIMARY");
      assertThat(OWNER_RESPONSE_READY.await(20, java.util.concurrent.TimeUnit.SECONDS))
          .as("actual successful owner response reached observing proxy")
          .isTrue();
      assertThat(RECEIVER_STATUS.get()).isEqualTo(200);
      JsonNode captured = command("CAPTURE_STALE");
      assertThat(captured.path("staleCaptured").asBoolean()).isTrue();
      OWNER_RESPONSE_RELEASE.countDown();
      var failed = creating.get(30, java.util.concurrent.TimeUnit.SECONDS);
      var paired = JSON.createObjectNode();
      paired.put("tenantFailureStatus", failed.statusCode());
      paired.put("createStatus", RECEIVER_STATUS.get());
      paired.put("compensationStatus", COMPENSATION_STATUS.get());
      paired.put("compensationAttempts", COMPENSATION_ATTEMPTS.get());
      paired.put("compensationReplyLost", COMPENSATION_REPLY_LOST.get());
      paired.put("sameCompensationAttemptAndPayload", SAME_COMPENSATION.get());
      paired.put("allocationRestored", allocation.getStatus(tenantId).name().equals("RESERVED"));
      paired.set("beforeDelayedRelease", command("OBSERVE"));
      savePaired(paired);
      assertThat(failed.statusCode()).isEqualTo(400);
      assertThat(COMPENSATION_REPLY_LOST.get())
          .as("first real CTS204 was lost at the observing transport")
          .isTrue();
      assertThat(COMPENSATION_ATTEMPTS.get())
          .as("same live attempt recovers one lost terminal reply")
          .isEqualTo(2);
      assertThat(SAME_COMPENSATION.get())
          .as("retry retains original nonce and complete raw command")
          .isTrue();
      assertThat(COMPENSATION_STATUS.get()).as("exact terminal acknowledgement").isEqualTo(204);
      assertThat(paired.path("beforeDelayedRelease").path("terminalRetryUnchanged").asBoolean())
          .isTrue();
      assertThat(allocation.getStatus(tenantId).name()).isEqualTo("RESERVED");
      command("RELEASE_DELAYED");
      assertThat(delayed.get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode())
          .isEqualTo(409);
      JsonNode retired = command("OBSERVE");
      assertThat(retired.path("delayedInsertDuplicate").asBoolean())
          .as("validated old request attempted real Mongo INSERT and lost to terminal id fence")
          .isTrue();
      assertThat(retired.path("actualInsertCalls").asInt()).isEqualTo(2);
      assertThat(retired.path("liveTypes").asInt()).isZero();
      assertThat(retired.path("terminalTypes").asInt()).isEqualTo(1);
      assertThat(retired.path("counterUnchangedSinceCapture").asBoolean()).isTrue();
      JsonNode stale = command("STALE_UPDATES");
      assertThat(stale.path("conditionalWriteAttempts").asInt()).isEqualTo(2);
      assertThat(stale.path("conditionalWritesBlocked").asInt()).isEqualTo(2);
      assertThat(stale.path("unconditionalSaveAttempts").asInt()).isZero();
      assertThat(stale.path("staleUpdateRefusals").asInt()).isEqualTo(2);
      for (String field :
          List.of(
              "awareConditionalWriteAttempted",
              "awareConditionalWriteBlocked",
              "unawareConditionalWriteAttempted",
              "unawareConditionalWriteBlocked",
              "retiredReadsHidden")) assertThat(stale.path(field).asBoolean()).as(field).isTrue();
      assertThat(stale.path("retiredReadChecks").asInt()).isEqualTo(20);
      assertThat(stale.path("liveTypes").asInt()).isZero();
      assertThat(stale.path("counterUnchangedSinceCapture").asBoolean()).isTrue();
      assertThat(command("ORDINARY_CONTROL").path("ordinaryPreserved").asBoolean()).isTrue();
      var recreated = publicCreate(base, setupBearer());
      assertThat(recreated.statusCode()).isEqualTo(200);
      assertThat(allocation.getStatus(tenantId).name()).isEqualTo("ASSIGNED");
      JsonNode newAttempt = command("OBSERVE");
      assertThat(newAttempt.path("newAttemptDistinct").asBoolean()).isTrue();
      assertThat(newAttempt.path("newResultDistinct").asBoolean()).isTrue();
      assertThat(newAttempt.path("counterAdvancedExactlyOnce").asBoolean()).isTrue();
      assertThat(newAttempt.path("counterNeverRewound").asBoolean()).isTrue();
      assertThat(newAttempt.path("liveTypes").asInt()).isEqualTo(1);
      assertThat(newAttempt.path("terminalTypes").asInt()).isEqualTo(1);
      assertThat(replay(original).statusCode()).isEqualTo(409);
      JsonNode afterOldReplay = command("OBSERVE");
      for (String field :
          List.of("liveTypes", "terminalTypes", "actualInsertCalls", "counterAdvancedExactlyOnce"))
        assertThat(afterOldReplay.path(field).equals(newAttempt.path(field)))
            .as("retired packet cannot create another result or invoke Mongo insert")
            .isTrue();
      paired.put("delayedReplyStatus", 409);
      paired.put("newTenantStatus", recreated.statusCode());
      paired.put("oldReplayStatus", 409);
      paired.put("allocationRestoredThenReassigned", true);
      paired.set("terminalRace", retired);
      paired.set("staleWrites", stale);
      paired.set("newAttempt", afterOldReplay);
      savePaired(paired);
    } finally {
      OWNER_RESPONSE_RELEASE.countDown();
      try {
        command("RELEASE_PRIMARY");
        command("RELEASE_DELAYED");
      } finally {
        executor.shutdownNow();
        if (!executor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS))
          throw fixtureRefusal();
      }
    }
  }

  private static String proofNonce(String proof) throws java.io.IOException {
    return JSON.readTree(Base64.getUrlDecoder().decode(proof.split("\\.", -1)[0]))
        .path("nonce")
        .asText();
  }

  private static void terminalCheckpoint(String action) {
    try {
      command(action);
    } catch (Exception refused) {
      if (refused instanceof InterruptedException) Thread.currentThread().interrupt();
      throw fixtureRefusal();
    }
  }

  private static synchronized JsonNode command(String action) throws Exception {
    Path directory = Path.of(required("ORISO_BOOTSTRAP_CONTROL_PATH"));
    long sequence = ++controlSequence;
    Path temporary = directory.resolve("request.tmp");
    Files.writeString(
        temporary, JSON.writeValueAsString(Map.of("sequence", sequence, "command", action)));
    Files.move(
        temporary,
        directory.resolve("request.json"),
        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < deadline) {
      Path response = directory.resolve("response.json");
      if (Files.isRegularFile(response)) {
        JsonNode value = JSON.readTree(Files.readString(response));
        if (value.path("state").asText().equals("FIXTURE_FAILURE")) throw fixtureRefusal();
        if (value.path("sequence").asLong() == sequence
            && value.path("state").asText().equals("ACK")) return value;
      }
      Thread.sleep(20);
    }
    throw fixtureRefusal();
  }

  private static JsonNode waitFor(String field) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < deadline) {
      JsonNode value = command("OBSERVE");
      if (value.path(field).asBoolean()) return value;
      Thread.sleep(20);
    }
    throw fixtureRefusal();
  }

  private static HttpResponse<byte[]> replay(ReplayPacket original) {
    try {
      return HTTP.send(
          HttpRequest.newBuilder(
                  URI.create(required("ORISO_CTS_BOOTSTRAP_RECEIVER_URL") + "/consultingtypes"))
              .timeout(Duration.ofSeconds(30))
              .header("Authorization", original.authorization)
              .header("Content-Type", "application/json")
              .header("tenantId", "0")
              .header("csrfHeader", "test")
              .header("Cookie", "csrfCookie=test")
              .header("X-ORISO-Tenant-Creation-Context", original.proof)
              .POST(HttpRequest.BodyPublishers.ofByteArray(original.body))
              .build(),
          HttpResponse.BodyHandlers.ofByteArray());
    } catch (Exception failure) {
      throw fixtureRefusal();
    }
  }

  private HttpResponse<byte[]> publicCreate(String body, String bearer) {
    try {
      return HTTP.send(
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/tenantadmin"))
              .timeout(Duration.ofSeconds(60))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + bearer)
              .header("tenantId", "0")
              .header("csrfHeader", "test")
              .header("Cookie", "csrfCookie=test")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build(),
          HttpResponse.BodyHandlers.ofByteArray());
    } catch (Exception failure) {
      throw fixtureRefusal();
    }
  }

  private static String setupBearer() throws Exception {
    JsonNode setup = null;
    for (JsonNode actor : fixture().path("tasks"))
      if (actor.path("key").asText().equals("SETUP")) setup = actor;
    if (setup == null) throw fixtureRefusal();
    String form =
        "grant_type=client_credentials&client_id="
            + URLEncoder.encode(setup.path("clientId").asText(), StandardCharsets.UTF_8)
            + "&client_secret="
            + URLEncoder.encode(setup.path("secret").asText(), StandardCharsets.UTF_8);
    var response =
        HTTP.send(
            HttpRequest.newBuilder(
                    URI.create(
                        fixture().path("issuer").asText() + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) throw fixtureRefusal();
    return JSON.readTree(response.body()).path("access_token").asText();
  }

  private static void savePaired(JsonNode paired) throws Exception {
    Path path = Path.of(required("ORISO_BOOTSTRAP_OBSERVED"));
    var value =
        (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(path));
    value.set("paired", paired);
    Files.writeString(path, JSON.writeValueAsString(value));
  }

  private static IllegalStateException fixtureRefusal() {
    return new IllegalStateException("Native paired fixture precondition refused");
  }

  private static final class ReplayPacket {
    private final byte[] body;
    private final String authorization, proof;

    private ReplayPacket(byte[] body, String authorization, String proof) {
      this.body = body;
      this.authorization = authorization;
      this.proof = proof;
    }

    @Override
    public String toString() {
      return "<private actually issued request>";
    }
  }
}
