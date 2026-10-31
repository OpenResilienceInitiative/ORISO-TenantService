package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vi.tenantservice.api.exception.TenantBadRequestException;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** Real DPA publication, signing and SQL state; only caller and CTS transport are fixtures. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:held-dpa-publication;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
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
    scripts = "/db/changelog/changeset/0046_tenant_held_bootstrap/createTenantHeldBootstrap.sql",
    statements = {
      "ALTER TABLE tenant ADD COLUMN IF NOT EXISTS bootstrap_attempt_id VARCHAR(36)",
      "ALTER TABLE tenant ADD CONSTRAINT IF NOT EXISTS uq_tenant_bootstrap_attempt UNIQUE (bootstrap_attempt_id)",
      "ALTER TABLE tenant DROP CONSTRAINT IF EXISTS UNIQUE_SUBDOMAIN",
      "DELETE FROM tenant_bootstrap_operation",
      "DELETE FROM tenant_id_reservation",
      "DELETE FROM tenant_dpa_admin_signature",
      "DELETE FROM tenant_dpa_signature",
      "DELETE FROM tenant_dpa_version",
      "DELETE FROM tenant"
    })
class TenantHeldDpaPublicationIT {
  private static final long TARGET = 9159L;
  private static final LocalDateTime CURRENT = LocalDateTime.of(2026, 7, 1, 12, 0);
  private static final LocalDateTime OLDER = LocalDateTime.of(2026, 6, 1, 12, 0);
  @Autowired private TenantHeldBootstrap bootstrap;
  @Autowired private TenantIdAllocationService allocation;
  @Autowired private TenantDpaStatusService dpaStatus;
  @Autowired private TenantRepository tenants;
  @Autowired private TenantDpaVersionRepository versions;
  @Autowired private JdbcTemplate sql;
  @Autowired private RestTemplate transport;
  @MockitoBean private TaskServiceIdentity identity;

  @MockitoBean
  private com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService settings;

  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final AtomicInteger calls = new AtomicInteger();
  private org.springframework.http.client.ClientHttpRequestFactory original;
  private MockRestServiceServer server;

  @BeforeEach
  void prepare() {
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
        Jwt.withTokenValue("synthetic-held-dpa-no-wire-token")
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
    server
        .expect(
            ExpectedCount.between(0, 1),
            org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                "http://local-bootstrap-precondition/consultingtypes"))
        .andRespond(
            sent -> {
              calls.incrementAndGet();
              return org.springframework.test.web.client.response.MockRestResponseCreators
                  .withSuccess(
                      "{\"tenantId\":9159,\"id\":109159}",
                      org.springframework.http.MediaType.APPLICATION_JSON)
                  .createResponse(sent);
            });
    publish();
  }

  @AfterEach
  void clear() {
    transport.setRequestFactory(original);
    SecurityContextHolder.clearContext();
    org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unpublishedAcceptanceCannotCommitOrPoisonTheOriginalHeldClaim(boolean noPublication) {
    if (noPublication) unpublish();
    ObjectNode request = request();
    acceptance(request).put("dpaVersion", noPublication ? "" : "2026-07-02T12:00:00");

    var failure = org.assertj.core.api.Assertions.catchThrowable(() -> bootstrap.create(request));

    assertThat(
            List.<Object>of(
                sql.queryForObject("SELECT COUNT(*) FROM tenant WHERE id=?", Integer.class, TARGET),
                sql.queryForObject(
                    "SELECT COUNT(*) FROM tenant_bootstrap_operation", Integer.class),
                sql.queryForObject(
                    "SELECT COUNT(*) FROM tenant_dpa_admin_signature WHERE tenant_id=?",
                    Integer.class,
                    TARGET),
                calls.get(),
                allocation.getStatus(TARGET).name()))
        .as("Unpublished DPA must be refused before any held local, signature or CTS effect")
        .containsExactly(0, 0, 0, 0, "RESERVED");
    assertThat(failure).isInstanceOf(TenantBadRequestException.class);

    if (noPublication) publish();
    acceptance(request).put("dpaVersion", CURRENT.toString());
    var result = bootstrap.create(request.deepCopy());
    assertThat(bootstrap.create(request.deepCopy()).receipt()).isEqualTo(result.receipt());
    assertThat(allocation.getStatus(TARGET).name()).isEqualTo("ASSIGNED");
    assertThat(calls.get()).isEqualTo(1);
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " \t ", " 2026-07-01T12:00:00 "})
  void blankCurrentAndTrimmedPublishedVersionsKeepTheirSigningSemantics(String version) {
    ObjectNode request = request();
    acceptance(request).put("dpaVersion", version);

    var result = bootstrap.create(request);

    assertThat(bootstrap.create(request.deepCopy()).receipt()).isEqualTo(result.receipt());
    assertThat(dpaStatus.getStatus(TARGET).status()).isEqualTo(TenantDpaStatus.VALID);
    assertThat(dpaStatus.getStatus(TARGET).signedVersion()).isEqualTo(CURRENT);
    assertThat(calls.get()).isEqualTo(1);
    server.verify();
  }

  @Test
  void anOlderActuallyPublishedOperatorVersionIsRecordedWithoutReplacingWhatWasShown() {
    versions.saveAndFlush(
        TenantDpaVersionEntity.builder()
            .tenantId(1L)
            .activationDate(OLDER)
            .content("{\"de\":\"<p>Earlier published DPA</p>\"}")
            .build());
    ObjectNode request = request();
    acceptance(request).put("dpaVersion", OLDER.toString());

    var result = bootstrap.create(request);

    assertThat(bootstrap.create(request.deepCopy()).receipt()).isEqualTo(result.receipt());
    assertThat(dpaStatus.getStatus(TARGET).status()).isEqualTo(TenantDpaStatus.OUTDATED);
    assertThat(dpaStatus.getStatus(TARGET).signedVersion()).isEqualTo(OLDER);
    assertThat(calls.get()).isEqualTo(1);
    server.verify();
  }

  @Test
  void optionalAcceptanceStaysOptionalWhenNoOperatorDpaIsPublished() {
    unpublish();
    ObjectNode request = request();

    var result = bootstrap.create(request);

    assertThat(bootstrap.create(request.deepCopy()).receipt()).isEqualTo(result.receipt());
    assertThat(
            sql.queryForObject(
                "SELECT COUNT(*) FROM tenant_dpa_admin_signature WHERE tenant_id=?",
                Integer.class,
                TARGET))
        .isZero();
    assertThat(calls.get()).isEqualTo(1);
    server.verify();
  }

  @Test
  void aCompleteOwnedReceiptDoesNotBecomeANewPublicationAttempt() {
    ObjectNode request = request();
    acceptance(request).put("dpaVersion", CURRENT.toString());
    var result = bootstrap.create(request);
    unpublish();

    var replay =
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> bootstrap.create(request.deepCopy()),
            "A completed owned receipt must not start a new DPA publication preflight");

    assertThat(replay.receipt()).isEqualTo(result.receipt());
    assertThat(calls.get()).isEqualTo(1);
    server.verify();
  }

  private ObjectNode request() {
    var dto =
        new com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder()
            .withId(TARGET)
            .withName("Held DPA tenant")
            .withSubdomain("held-dpa")
            .withLicensing()
            .withTenantIdReservationToken(allocation.reserve(TARGET, "held-dpa").getToken())
            .build();
    var body = json.createObjectNode();
    body.put("claimId", UUID.randomUUID().toString());
    body.put("accountAttemptId", UUID.randomUUID().toString());
    body.put("executionId", UUID.randomUUID().toString());
    body.set("tenant", json.valueToTree(dto));
    return body;
  }

  private ObjectNode acceptance(ObjectNode request) {
    var tenant = (ObjectNode) request.path("tenant");
    if (tenant.path("onboardingDpaAcceptance").isObject())
      return (ObjectNode) tenant.path("onboardingDpaAcceptance");
    return tenant
        .putObject("onboardingDpaAcceptance")
        .put("accepted", true)
        .put("signerUserId", "held-admin")
        .put("signerName", "Held DPA Signer");
  }

  private void publish() {
    var now = LocalDateTime.now();
    tenants.saveAndFlush(
        TenantEntity.builder()
            .id(1L)
            .name("Operator")
            .subdomain("operator-dpa")
            .contentDataProcessingAgreementActivationDate(CURRENT)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private void unpublish() {
    versions.deleteAll();
    var operator = tenants.findById(1L).orElseThrow();
    operator.setContentDataProcessingAgreementActivationDate(null);
    tenants.saveAndFlush(operator);
  }
}
