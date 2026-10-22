package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vi.tenantservice.config.security.TaskContextProof;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:held-bootstrap-replay;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
      "spring.liquibase.enabled=false",
      "feature.multitenancy.with.single.domain.enabled=true",
      "consulting.type.service.api.url=http://local-bootstrap-precondition",
      "default.consulting.types.json.path=src/test/resources/settings/bootstrap-v2-complete-consulting-type.json",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=CONFIG_WIZARD",
      "ORISO_TASK_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@ActiveProfiles("testing")
@Sql(
    scripts = "/held-bootstrap-operation.sql",
    statements = {
      "ALTER TABLE tenant ADD COLUMN IF NOT EXISTS bootstrap_attempt_id VARCHAR(36)",
      "ALTER TABLE tenant ADD CONSTRAINT IF NOT EXISTS uq_tenant_bootstrap_attempt UNIQUE (bootstrap_attempt_id)",
      "ALTER TABLE tenant DROP CONSTRAINT IF EXISTS UNIQUE_SUBDOMAIN",
      "DELETE FROM tenant_bootstrap_operation",
      "DELETE FROM tenant_id_reservation",
      "DELETE FROM tenant"
    })
class TenantHeldBootstrapIT {
  @Autowired private TenantHeldBootstrap bootstrap;
  @Autowired private TenantAdminControlsService controls;
  @Autowired private TenantIdAllocationService allocation;
  @Autowired private ApplicationContext application;
  @Autowired private JdbcTemplate sql;
  @Autowired private RestTemplate transport;
  @MockitoBean private TaskServiceIdentity identity;

  @MockitoBean
  private com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService settings;

  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private org.springframework.http.client.ClientHttpRequestFactory original;
  private MockRestServiceServer server;

  @BeforeEach
  void caller() {
    when(identity.current("CONFIG_WIZARD")).thenReturn(true);
    when(settings.getApplicationSettings())
        .thenReturn(
            new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                    .ApplicationSettingsDTO()
                .mainTenantSubdomainForSingleDomainMultitenancy(
                    new com.vi.tenantservice.applicationsettingsservice.generated.web.model
                            .SettingDTO()
                        .value("app")));
    var jwt =
        Jwt.withTokenValue("synthetic-held-no-wire-token")
            .header("alg", "RS256")
            .issuer("https://synthetic.identity/realms/context")
            .subject("setup-subject")
            .claim("azp", "backend-setup")
            .expiresAt(Instant.now().plusSeconds(120))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.setUserPrincipal(SecurityContextHolder.getContext().getAuthentication());
    org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
        new org.springframework.web.context.request.ServletRequestAttributes(request));
    original = transport.getRequestFactory();
    server = MockRestServiceServer.bindTo(transport).build();
  }

  @AfterEach
  void clear() {
    transport.setRequestFactory(original);
    SecurityContextHolder.clearContext();
    org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void aLostTenantReplyReplaysTheSameOwnedResultAcrossANewServiceInstance() throws Exception {
    var calls = new AtomicInteger();
    expectCreate(calls, new AtomicReference<>(), false);
    ObjectNode request = request();
    var first = bootstrap.create(request);
    assertThat(first).as("Held creation must return its actual owning result").isNotNull();
    String marker =
        sql.queryForObject("SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class);
    var restarted =
        application.getAutowireCapableBeanFactory().createBean(TenantHeldBootstrap.class);
    var prior = controls.getAccountInactivitySettings();
    var changedPolicy =
        controls.updateAccountInactivitySettings(
            new com.vi.tenantservice.api.model.AccountInactivitySettings(
                7, 11, 18, prior.getRevision()));
    assertThat(changedPolicy.getRevision()).isGreaterThan(prior.getRevision());
    var replay =
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> restarted.create(request.deepCopy()),
            "Unrelated global defaults must not invalidate the owned creation replay");
    assertThat(replay != null && first.receipt().equals(replay.receipt())).isTrue();
    assertThat(
            TaskContextProof.digest(new byte[32], json.valueToTree(first.tenant()))
                .equals(TaskContextProof.digest(new byte[32], json.valueToTree(replay.tenant()))))
        .isTrue();
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM tenant WHERE id=9157", Integer.class))
        .isEqualTo(1);
    assertThat(
            marker.equals(
                sql.queryForObject(
                    "SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class)))
        .isTrue();
    for (String field : new String[] {"claimId", "accountAttemptId", "executionId"}) {
      ObjectNode changed = request.deepCopy();
      changed.put(field, UUID.randomUUID().toString());
      assertThatThrownBy(() -> restarted.create(changed)).isInstanceOf(RuntimeException.class);
    }
    ObjectNode changed = request.deepCopy();
    ((ObjectNode) changed.path("tenant")).put("name", "foreign");
    assertThatThrownBy(() -> restarted.create(changed)).isInstanceOf(RuntimeException.class);
    ObjectNode wrongPair = request.deepCopy();
    ((ObjectNode) wrongPair.path("tenant"))
        .put("tenantIdReservationToken", UUID.randomUUID().toString());
    assertThatThrownBy(() -> restarted.create(wrongPair)).isInstanceOf(RuntimeException.class);
    when(identity.current("CONFIG_WIZARD")).thenReturn(false);
    assertThatThrownBy(() -> restarted.create(request)).isInstanceOf(RuntimeException.class);
    when(identity.current("CONFIG_WIZARD")).thenReturn(true);
    sql.update("UPDATE tenant SET name='changed current row' WHERE id=9157");
    assertThatThrownBy(() -> restarted.create(request)).isInstanceOf(RuntimeException.class);
    assertThat(calls.get()).isEqualTo(1);
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("ASSIGNED");
    server.verify();
  }

  @Test
  void anUnknownCtsReplyRollsForwardOnlyItsOriginalCommandAndNonce() throws Exception {
    var calls = new AtomicInteger();
    var originalWire = new AtomicReference<String>();
    expectCreate(calls, originalWire, true);
    expectCreate(calls, originalWire, false);
    ObjectNode request = request();
    var unknown = org.assertj.core.api.Assertions.catchThrowable(() -> bootstrap.create(request));
    assertThat(unknown).isInstanceOf(RuntimeException.class);
    assertThat(calls.get()).isEqualTo(1);
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM tenant WHERE id=9157", Integer.class))
        .isEqualTo(1);
    String marker =
        sql.queryForObject("SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class);
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("ASSIGNED");
    assertThat(unknown instanceof ResponseStatusException).isTrue();
    assertThat(((ResponseStatusException) unknown).getStatusCode().value()).isEqualTo(503);
    var consulting =
        application.getBean(
            com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        consulting, "defaultConsultingTypesFilePath", "missing-after-original-send");
    try {
      var restarted =
          application.getAutowireCapableBeanFactory().createBean(TenantHeldBootstrap.class);
      var recovered = restarted.create(request.deepCopy());
      assertThat(recovered)
          .as("Unknown original command must reconcile without a fresh INSERT")
          .isNotNull();
      assertThat(recovered.tenant().getId()).isEqualTo(9157L);
      assertThat(
              marker.equals(
                  sql.queryForObject(
                      "SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class)))
          .isTrue();
      assertThat(calls.get()).isEqualTo(2);
      assertThat(
              sql.queryForObject(
                  "SELECT COUNT(*) FROM tenant_bootstrap_operation WHERE state='COMPLETE'",
                  Integer.class))
          .isEqualTo(1);
      server.verify();
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(
          consulting,
          "defaultConsultingTypesFilePath",
          "src/test/resources/settings/bootstrap-v2-complete-consulting-type.json");
    }
  }

  private ObjectNode request() {
    var dto =
        new com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder()
            .withId(9157L)
            .withName("Held tenant")
            .withSubdomain("held-replay")
            .withLicensing()
            .withTenantIdReservationToken(allocation.reserve(9157L, "held-replay").getToken())
            .build();
    var body = json.createObjectNode();
    body.put("claimId", UUID.randomUUID().toString());
    body.put("accountAttemptId", UUID.randomUUID().toString());
    body.put("executionId", UUID.randomUUID().toString());
    body.set("tenant", json.valueToTree(dto));
    return body;
  }

  private void expectCreate(
      AtomicInteger calls, AtomicReference<String> frozen, boolean loseReply) {
    server
        .expect(
            org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                "http://local-bootstrap-precondition/consultingtypes"))
        .andExpect(
            sent -> {
              assertThat(sent.getMethod() == org.springframework.http.HttpMethod.POST).isTrue();
              assertThat("0".equals(sent.getHeaders().getFirst("tenantId"))).isTrue();
              assertThat(
                      org.springframework.transaction.support.TransactionSynchronizationManager
                          .isActualTransactionActive())
                  .isFalse();
              String body =
                  ((org.springframework.mock.http.client.MockClientHttpRequest) sent)
                      .getBodyAsString();
              if (frozen.get() == null) frozen.set(body);
              else assertThat(frozen.get().equals(body)).isTrue();
              try {
                var proof =
                    TaskContextProof.verify(
                        new byte[32],
                        sent.getHeaders()
                            .getFirst(
                                com.vi.tenantservice.config.security.TenantCreationContext.HEADER));
                String marker =
                    sql.queryForObject(
                        "SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class);
                assertThat(proof.path("nonce").asText().equals(marker.replace("-", ""))).isTrue();
              } catch (Exception invalid) {
                throw new AssertionError("Held native wire precondition refused");
              }
              calls.incrementAndGet();
            })
        .andRespond(
            sent -> {
              if (loseReply) throw new java.io.IOException("Synthetic unknown bootstrap reply");
              return org.springframework.test.web.client.response.MockRestResponseCreators
                  .withSuccess(
                      "{\"tenantId\":9157,\"id\":109157}",
                      org.springframework.http.MediaType.APPLICATION_JSON)
                  .createResponse(sent);
            });
  }
}
