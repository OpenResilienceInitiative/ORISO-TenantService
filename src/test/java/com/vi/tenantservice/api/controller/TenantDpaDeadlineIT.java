package com.vi.tenantservice.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

/** Public publication/status/signing APIs with real authorization, services and persistence. */
@SpringBootTest(classes = TenantServiceApplication.class)
@ActiveProfiles("testing")
@Import(TenantDpaDeadlineIT.TimeFixture.class)
class TenantDpaDeadlineIT {
  @Autowired private WebApplicationContext context;
  @Autowired private JwtAuthConverter jwtAuthConverter;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TenantRepository tenants;
  @Autowired private TenantDpaVersionRepository versions;
  @Autowired private TenantDpaSignatureRepository signatures;
  @Autowired private TenantIdReservationRepository reservations;
  @Autowired private ControlledClock clock;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbcTemplate.update("DELETE FROM tenant_dpa_admin_signature");
    signatures.deleteAll();
    versions.deleteAll();
    tenants.deleteAll();
    reservations.deleteAll();
    clock.set("2026-10-01T10:00:00Z");
    seedTenant(1L);
    seedTenant(2L);
  }

  @Test
  void firstPublicationExposesTheChosenGlobalDeadlineWithoutGrantingInitialAccess()
      throws Exception {
    mvc.perform(
            put("/tenantadmin/1/dpa")
                .with(caller(0L, "tenant-admin"))
                .queryParam("signingDeadlineAt", "2026-10-15T17:00:00+02:00")
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>First AVV</p>\"}"))
        .andExpect(status().isOk());

    mvc.perform(get("/tenantadmin/2/dpa/status").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNSIGNED"))
        .andExpect(jsonPath("$.signingDeadlineAt").value("2026-10-15T15:00:00Z"))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(false))
        .andExpect(jsonPath("$.renewalGraceActive").value(false));
  }

  @Test
  void renewalKeepsNewCounsellingOpenUntilTheExactDeadlineAndSigningRestoresIt() throws Exception {
    publish("2026-10-15T15:00:00Z");
    signCurrent();
    // Two publications in the same clock second must still be different contracts.
    publish("2026-10-03T10:00:00Z");

    mvc.perform(get("/tenantadmin/2/dpa/gate").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("OUTDATED"))
        .andExpect(jsonPath("$.signingDeadlineAt").value("2026-10-03T10:00:00Z"))
        .andExpect(jsonPath("$.renewalGraceActive").value(true))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(true));

    clock.set("2026-10-03T10:00:00Z");
    mvc.perform(get("/tenantadmin/2/dpa/gate").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("OUTDATED"))
        .andExpect(jsonPath("$.renewalGraceActive").value(false))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(false));

    signCurrent();
    mvc.perform(get("/tenantadmin/2/dpa/gate").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("VALID"))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(true));
  }

  @Test
  void firstOwnPublicationCannotReuseTheSignatureOfAnOperatorDocumentInTheSameSecond()
      throws Exception {
    publish("2026-10-15T15:00:00Z");
    signCurrent();
    String operatorVersion = currentVersion();
    mvc.perform(
            put("/tenantadmin/2/dpa")
                .with(caller(2L, "single-tenant-admin"))
                .queryParam("signingDeadlineAt", "2026-10-20T15:00:00Z")
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>Different own contract</p>\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("OUTDATED"))
        .andExpect(jsonPath("$.currentDpaVersion").value("2026-10-01T10:00:01"));
    mvc.perform(
            post("/tenantadmin/2/dpa/sign")
                .with(caller(2L, "single-tenant-admin"))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"signerName\":\"Tenant Signer\",\"accepted\":true,\"dpaVersion\":\""
                        + operatorVersion
                        + "\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void aPreloadedRequestCannotAcceptAContractPublishedByAnotherRequestInTheMeantime()
      throws Exception {
    publish("2026-10-15T15:00:00Z");
    String shownVersion = currentVersion();
    // Keep the request persistence context alive as OSIV does during a long onboarding request.
    var requestContext = entityManagerFactory.createEntityManager();
    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(requestContext));
    try {
      currentVersion();
      CompletableFuture.runAsync(
              () -> {
                try {
                  publish("2026-10-20T15:00:00Z");
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              })
          .get(20, TimeUnit.SECONDS);
      mvc.perform(
              post("/tenantadmin/2/dpa/sign")
                  .with(caller(2L, "single-tenant-admin"))
                  .contentType(APPLICATION_JSON)
                  .content(
                      "{\"signerName\":\"Tenant Signer\",\"accepted\":true,\"dpaVersion\":\""
                          + shownVersion
                          + "\"}"))
          .andExpect(status().isConflict());
    } finally {
      TransactionSynchronizationManager.unbindResource(entityManagerFactory);
      requestContext.close();
    }
    mvc.perform(get("/tenantadmin/2/dpa/gate").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("UNSIGNED"))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void confirmingASupersededForwardedContractCannotCreateInitialGrace(boolean previouslySigned)
      throws Exception {
    publish("2026-10-15T15:00:00Z");
    if (previouslySigned) {
      signCurrent();
    }
    var invite =
        mvc.perform(post("/tenantadmin/2/dpa/invite").with(caller(2L, "single-tenant-admin")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = com.jayway.jsonpath.JsonPath.read(invite, "$.token");
    publish("2026-10-20T15:00:00Z");
    mvc.perform(
            post("/tenant/public/dpa/confirm/{token}", token)
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"signerName\":\"External Signer\",\"signerPosition\":\"CEO\",\"signerEmail\":\"signer@example.org\",\"signerOrganisation\":\"Tenant Two\",\"accepted\":true,\"language\":\"de\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaVersion").value("2026-10-01T10:00"));
    mvc.perform(get("/tenantadmin/2/dpa/gate").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("OUTDATED"))
        .andExpect(jsonPath("$.renewalGraceActive").value(previouslySigned))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(previouslySigned));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "not-a-date",
        "2026-10-02T10:00:00",
        "2026-10-01T10:00:00Z",
        "2026-09-30T10:00:00Z"
      })
  void invalidOrNonFutureDeadlineDoesNotReplaceThePublishedContract(String deadline)
      throws Exception {
    publish("2026-10-15T15:00:00Z");

    mvc.perform(
            put("/tenantadmin/1/dpa")
                .with(caller(0L, "tenant-admin"))
                .queryParam("signingDeadlineAt", deadline)
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>Must not publish</p>\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/tenantadmin/2/dpa/versions").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].signingDeadlineAt").value("2026-10-15T15:00:00Z"))
        .andExpect(jsonPath("$[0].content").value("{\"de\":\"<p>AVV publication</p>\"}"));
  }

  @Test
  void onlyTheConfiguredTechnicalIdentityCanReadAnotherTenantsCounsellingGate() throws Exception {
    publish("2026-10-15T15:00:00Z");
    mvc.perform(
            get("/tenantadmin/2/dpa/gate")
                .with(caller(0L, "technical", "test-technical-service-subject")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dpaStatus").value("UNSIGNED"))
        .andExpect(jsonPath("$.newCounsellingAllowed").value(false))
        .andExpect(jsonPath("$.signerEmail").doesNotExist());
    mvc.perform(
            get("/tenantadmin/2/dpa/gate")
                .with(caller(0L, "technical", "foreign-technical-role-holder")))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/tenantadmin/1/dpa")
                .with(caller(0L, "technical", "test-technical-service-subject"))
                .queryParam("signingDeadlineAt", "2026-10-15T15:00:00Z")
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>Must not publish</p>\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void legacyOwnDocumentDoesNotInheritAnOperatorsDeadlineWithTheSameTimestamp() throws Exception {
    publish("2026-10-15T15:00:00Z");
    var legacy =
        TenantEntity.builder()
            .id(3L)
            .name("Legacy own AVV")
            .subdomain("legacy-own-avv")
            .createDate(LocalDateTime.of(2026, 9, 1, 0, 0))
            .updateDate(LocalDateTime.of(2026, 9, 1, 0, 0))
            .contentDataProcessingAgreement("{\"de\":\"<p>Legacy own contract</p>\"}")
            .contentDataProcessingAgreementActivationDate(LocalDateTime.of(2026, 10, 1, 10, 0))
            .build();
    tenants.save(legacy);
    mvc.perform(get("/tenantadmin/3/dpa/status").with(caller(3L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentDpaVersion").value("2026-10-01T10:00"))
        .andExpect(jsonPath("$.signingDeadlineAt").doesNotExist())
        .andExpect(jsonPath("$.renewalGraceActive").value(false));
  }

  @Test
  void newPublicationIsNewerThanRetainedHistoryWhenTheEmbeddedDateWasCleared() throws Exception {
    versions.save(
        TenantDpaVersionEntity.builder()
            .tenantId(2L)
            .activationDate(LocalDateTime.of(2026, 10, 2, 10, 0))
            .content("{\"de\":\"<p>Retained history</p>\"}")
            .build());
    mvc.perform(
            put("/tenantadmin/2/dpa")
                .with(caller(2L, "single-tenant-admin"))
                .queryParam("signingDeadlineAt", "2026-10-15T15:00:00Z")
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>New own publication</p>\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentDpaVersion").value("2026-10-02T10:00:01"));
    mvc.perform(get("/tenantadmin/2/dpa/versions").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].activationDate").value("2026-10-02T10:00:01"))
        .andExpect(jsonPath("$[0].signingDeadlineAt").value("2026-10-15T15:00:00Z"));
  }

  @Test
  void signingAnOlderDisplayedVersionDoesNotAcceptAnUnseenRenewal() throws Exception {
    publish("2026-10-15T15:00:00Z");
    String shownVersion = currentVersion();
    publish("2026-10-20T15:00:00Z");
    mvc.perform(
            post("/tenantadmin/2/dpa/sign")
                .with(caller(2L, "single-tenant-admin"))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"signerName\":\"Tenant Signer\",\"accepted\":true,\"dpaVersion\":\""
                        + shownVersion
                        + "\"}"))
        .andExpect(status().isConflict());
    mvc.perform(get("/tenantadmin/2/dpa/status").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNSIGNED"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not-a-version", "2026-10-01T10:00:00Z"})
  void malformedDisplayedVersionCannotCreateASignature(String shownVersion) throws Exception {
    publish("2026-10-15T15:00:00Z");
    mvc.perform(
            post("/tenantadmin/2/dpa/sign")
                .with(caller(2L, "single-tenant-admin"))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"signerName\":\"Tenant Signer\",\"accepted\":true,\"dpaVersion\":\""
                        + shownVersion
                        + "\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/tenantadmin/2/dpa/status").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNSIGNED"));
  }

  @Test
  void missingDeadlineAndMissingDisplayedVersionAreRejectedWithoutDefaults() throws Exception {
    mvc.perform(
            put("/tenantadmin/1/dpa")
                .with(caller(0L, "tenant-admin"))
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>Must not publish</p>\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/tenantadmin/2/dpa/status").with(caller(2L, "single-tenant-admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MISSING"));
    publish("2026-10-15T15:00:00Z");
    mvc.perform(
            post("/tenantadmin/2/dpa/sign")
                .with(caller(2L, "single-tenant-admin"))
                .contentType(APPLICATION_JSON)
                .content("{\"signerName\":\"Tenant Signer\",\"accepted\":true}"))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(strings = {"restricted-agency-admin", "restricted-consultant-admin"})
  void restrictedAgencyRolesCanReadTheirOwnDeadlineButCannotReadForeignTenantsOrSign(String role)
      throws Exception {
    publish("2026-10-15T15:00:00Z");
    mvc.perform(get("/tenantadmin/2/dpa/gate").with(caller(2L, role)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.signingDeadlineAt").value("2026-10-15T15:00:00Z"));
    mvc.perform(get("/tenantadmin/1/dpa/gate").with(caller(2L, role)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/tenantadmin/2/dpa/sign")
                .with(caller(2L, role))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"signerName\":\"Tenant Signer\",\"accepted\":true,\"dpaVersion\":\"2026-10-01T10:00\"}"))
        .andExpect(status().isForbidden());
  }

  private String currentVersion() throws Exception {
    var response =
        mvc.perform(get("/tenantadmin/2/dpa/status").with(caller(2L, "single-tenant-admin")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(response, "$.currentDpaVersion");
  }

  private void publish(String deadline) throws Exception {
    mvc.perform(
            put("/tenantadmin/1/dpa")
                .with(caller(0L, "tenant-admin"))
                .queryParam("signingDeadlineAt", deadline)
                .contentType(APPLICATION_JSON)
                .content("{\"de\":\"<p>AVV publication</p>\"}"))
        .andExpect(status().isOk());
  }

  private void signCurrent() throws Exception {
    String shownVersion = currentVersion();
    mvc.perform(
            post("/tenantadmin/2/dpa/sign")
                .with(caller(2L, "single-tenant-admin"))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"signerName\":\"Tenant Signer\",\"accepted\":true,\"language\":\"de\",\"dpaVersion\":\""
                        + shownVersion
                        + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALID"));
  }

  private void seedTenant(long id) {
    var now = LocalDateTime.of(2026, 9, 1, 0, 0);
    tenants.save(
        TenantEntity.builder()
            .id(id)
            .name("deadline-tenant-" + id)
            .subdomain("deadline-tenant-" + id)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private RequestPostProcessor caller(long tenantId, String role) {
    return caller(tenantId, role, "deadline-admin-" + tenantId);
  }

  private RequestPostProcessor caller(long tenantId, String role, String subject) {
    return jwt()
        .jwt(
            token ->
                token
                    .subject(subject)
                    .claim("azp", "app")
                    .claim("tenantId", tenantId)
                    .claim("username", "deadline-admin-" + tenantId)
                    .claim("realm_access", Map.of("roles", List.of(role))))
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
  }

  @TestConfiguration
  static class TimeFixture {
    @Bean
    @Primary
    ControlledClock controlledDpaClock() {
      return new ControlledClock();
    }
  }

  static class ControlledClock extends Clock {
    private final AtomicReference<Instant> now =
        new AtomicReference<>(Instant.parse("2026-10-01T10:00:00Z"));

    void set(String instant) {
      now.set(Instant.parse(instant));
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant(), zone);
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  }
}
