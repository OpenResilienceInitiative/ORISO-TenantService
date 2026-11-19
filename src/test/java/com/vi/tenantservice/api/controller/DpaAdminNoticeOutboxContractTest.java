package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withAccepted;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.DpaSignedNoticeHintService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    properties = {
      "spring.profiles.active=testing",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "tenant.dpa.admin-notice-outbox.pull-enabled=true",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=NOTIFICATION_DISPATCH",
      "TASK_IDENTITY_AUDIENCE=tenantservice",
      "IDENTITY_MAIL_CLIENT_ID=backend-mail",
      "IDENTITY_MAIL_SERVICE_SUBJECT=mail-subject"
    })
@AutoConfigureMockMvc
@Transactional
class DpaAdminNoticeOutboxContractTest {
  private static final String OUTBOX = "/internal/dpa-admin-notice-outbox";
  private static final String TENANT_AUDIENCE = "tenantservice";
  private static final long SIGNED_AT = 1_700_000_000_123L;
  private static final List<String> MAIL_ROLES =
      List.of(
          "smtp-sync", "system-email-delivery", "notification-dispatch", "notifications-technical");
  private static final byte[] TEST_KEY =
      "example-test-key-for-outbox-test-only".getBytes(StandardCharsets.UTF_8);

  @Autowired private MockMvc mvc;
  @Autowired private TenantDpaSignatureRepository signatures;
  @Autowired private TenantIdReservationRepository reservations;
  @Autowired private TenantRepository tenants;

  @Test
  void pullRequiresTheExactCryptographicallyVerifiedNotificationActor() throws Exception {
    mvc.perform(get(OUTBOX)).andExpect(status().isUnauthorized());
    mvc.perform(get(OUTBOX).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
    for (var authorization :
        List.of(
            signedToken("mail-subject", "backend-mail", "wrong-service", MAIL_ROLES),
            signedToken("mail-subject", "wrong-client", TENANT_AUDIENCE, MAIL_ROLES),
            signedToken(
                "mail-subject", "backend-mail", TENANT_AUDIENCE, List.of("notification-dispatch")),
            signedToken("wrong-subject", "backend-mail", TENANT_AUDIENCE, MAIL_ROLES),
            signedToken(
                "test-technical-service-subject", "app", TENANT_AUDIENCE, List.of("technical")),
            signedToken("admin-user", "app", TENANT_AUDIENCE, List.of("tenant-admin")))) {
      mvc.perform(get(OUTBOX).header(HttpHeaders.AUTHORIZATION, authorization))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get(OUTBOX).header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk());
  }

  @Test
  void pullReturnsOnlyMetadataForCurrentOwnersAndCanPagePastAStaleOccupant() throws Exception {
    var current = reserve(8971L, TenantIdReservationStatus.RESERVED, "still-current");
    var stale =
        stage(8971L, fingerprint("previous-occupant"), DpaSignatureStatus.SIGNED, SIGNED_AT);
    var queued =
        stage(8971L, fingerprint(current.getToken()), DpaSignatureStatus.SIGNED, SIGNED_AT);
    var tenant =
        tenants.saveAndFlush(
            TenantEntity.builder()
                .id(8972L)
                .name("Example organisation")
                .subdomain("outbox-test")
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build());
    var queuedForTenant = stage(tenant.getId(), null, DpaSignatureStatus.SIGNED, SIGNED_AT);
    stage(tenant.getId(), null, DpaSignatureStatus.SIGNED, null);
    stage(tenant.getId(), null, DpaSignatureStatus.INVALIDATED, SIGNED_AT);
    stage(8973L, null, DpaSignatureStatus.SIGNED, SIGNED_AT);

    mvc.perform(
            get(OUTBOX).param("limit", "1").header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.events").isEmpty())
        .andExpect(jsonPath("$.nextAfterId").value(stale.getId()));
    mvc.perform(
            get(OUTBOX)
                .param("afterId", stale.getId().toString())
                .param("limit", "1")
                .header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events.length()").value(1))
        .andExpect(jsonPath("$.events[0].length()").value(3))
        .andExpect(jsonPath("$.events[0].id").value(queued.getId()))
        .andExpect(jsonPath("$.events[0].tenantId").value(current.getTenantId()))
        .andExpect(jsonPath("$.events[0].signedAtEpochMillis").value(SIGNED_AT))
        .andExpect(jsonPath("$.nextAfterId").value(queued.getId()));
    mvc.perform(
            get(OUTBOX)
                .param("afterId", queued.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events.length()").value(1))
        .andExpect(jsonPath("$.events[0].id").value(queuedForTenant.getId()))
        .andExpect(jsonPath("$.nextAfterId").doesNotExist());
  }

  @Test
  void exactAuditExposesOnlyTheRequestedStagedSignatureToTheTaskActor() throws Exception {
    var owner = reserve(8992L, TenantIdReservationStatus.RESERVED, "exact-owner");
    var binding = fingerprint(owner.getToken());
    var current = stage(owner.getTenantId(), binding, DpaSignatureStatus.SIGNED, SIGNED_AT);
    var historical = stage(owner.getTenantId(), binding, DpaSignatureStatus.SIGNED, null);
    var other =
        stage(
            owner.getTenantId(),
            fingerprint("previous-owner"),
            DpaSignatureStatus.SIGNED,
            SIGNED_AT);
    var inconsistent =
        stage(owner.getTenantId(), binding, DpaSignatureStatus.SIGNED, SIGNED_AT + 1);
    String url = OUTBOX + "/" + current.getId();
    mvc.perform(get(url)).andExpect(status().isUnauthorized());
    mvc.perform(
            get(url)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    signedToken("admin-user", "app", TENANT_AUDIENCE, List.of("tenant-admin"))))
        .andExpect(status().isForbidden());
    mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$.id").value(current.getId()))
        .andExpect(jsonPath("$.tenantId").value(owner.getTenantId()))
        .andExpect(jsonPath("$.signedAtEpochMillis").value(SIGNED_AT))
        .andExpect(jsonPath("$.dpaVersion").value("2026-10-30T12:00"))
        .andExpect(jsonPath("$.signedAt").value("2026-11-19T10:00"))
        .andExpect(jsonPath("$.forwardedByUserId").value("forwarder"))
        .andExpect(jsonPath("$.signerName").value("Synthetic signer"))
        .andExpect(jsonPath("$.signerPosition").value("Director"))
        .andExpect(jsonPath("$.signerEmail").doesNotExist())
        .andExpect(jsonPath("$.reservationBinding").doesNotExist());
    for (var rejected : List.of(historical, other, inconsistent)) {
      mvc.perform(
              get(OUTBOX + "/" + rejected.getId())
                  .header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
          .andExpect(status().isNotFound());
    }
  }

  @Test
  void pullIsDisabledUnlessExplicitlyEnabled() {
    new ApplicationContextRunner()
        .withUserConfiguration(DpaAdminNoticeOutboxController.class)
        .run(context -> assertThat(context).doesNotHaveBean(DpaAdminNoticeOutboxController.class));
  }

  @Test
  void pageSizeIsCappedAndNegativeCursorsAreRejected() throws Exception {
    var owner = reserve(8981L, TenantIdReservationStatus.RESERVED, "paging-owner");
    var binding = fingerprint(owner.getToken());
    for (int i = 0; i < 101; i++) {
      stage(owner.getTenantId(), binding, DpaSignatureStatus.SIGNED, SIGNED_AT);
    }
    mvc.perform(
            get(OUTBOX)
                .param("limit", "10000")
                .header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events.length()").value(100))
        .andExpect(jsonPath("$.nextAfterId").isNumber());
    mvc.perform(
            get(OUTBOX)
                .param("afterId", "-1")
                .header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void legacyHintAcceptedResponseDoesNotAcknowledgeThePendingSignature() throws Exception {
    var owner = reserve(8995L, TenantIdReservationStatus.RESERVED, "hint-owner");
    var event =
        stage(
            owner.getTenantId(),
            fingerprint(owner.getToken()),
            DpaSignatureStatus.SIGNED,
            SIGNED_AT);
    var rest = new RestTemplate();
    var legacyConsumer = MockRestServiceServer.bindTo(rest).build();
    legacyConsumer
        .expect(requestTo("https://users.example/users/tenants/8995/dpa-signed-notices"))
        .andRespond(withAccepted());

    new DpaSignedNoticeHintService(rest, Runnable::run, "https://users.example")
        .notifySignatureRecorded(owner.getTenantId());

    legacyConsumer.verify();
    mvc.perform(get(OUTBOX).header(HttpHeaders.AUTHORIZATION, mailAuthorization()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events[0].id").value(event.getId()));
  }

  @Test
  void thereIsNoPublicAcknowledgementEvenForTheNotificationActor() throws Exception {
    var owner = reserve(8997L, TenantIdReservationStatus.RESERVED, "owner-proof");
    var event =
        stage(
            owner.getTenantId(),
            fingerprint(owner.getToken()),
            DpaSignatureStatus.SIGNED,
            SIGNED_AT);
    mvc.perform(
            post(OUTBOX + "/acknowledgements")
                .header(HttpHeaders.AUTHORIZATION, mailAuthorization())
                .contentType("application/json")
                .content("{\"ids\":[" + event.getId() + "]}"))
        .andExpect(status().isNotFound());
    assertThat(
            signatures
                .findById(event.getId())
                .orElseThrow()
                .getAdminNoticePendingSinceEpochMillis())
        .isEqualTo(SIGNED_AT);
  }

  private TenantIdReservationEntity reserve(
      long id, TenantIdReservationStatus status, String token) {
    var now = LocalDateTime.now();
    return reservations.saveAndFlush(
        TenantIdReservationEntity.builder()
            .tenantId(id)
            .token(token)
            .status(status)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private TenantDpaSignatureEntity stage(
      long tenantId, String binding, DpaSignatureStatus status, Long marker) {
    return signatures.saveAndFlush(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .createDate(LocalDateTime.now())
            .status(status)
            .source("FORWARDED_EXTERNAL")
            .signedAtEpochMillis(SIGNED_AT)
            .signedAt(LocalDateTime.of(2026, 11, 19, 10, 0))
            .dpaVersion(LocalDateTime.of(2026, 10, 30, 12, 0))
            .forwardedByUserId("forwarder")
            .signerName("Synthetic signer")
            .signerPosition("Director")
            .adminNoticePendingSinceEpochMillis(marker)
            .reservationTokenHash(binding)
            .signerEmail("signer@example.org")
            .build());
  }

  private static String fingerprint(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  private static String mailAuthorization() throws JOSEException {
    return signedToken("mail-subject", "backend-mail", TENANT_AUDIENCE, MAIL_ROLES);
  }

  private static String signedToken(
      String subject, String client, String audience, List<String> roles) throws JOSEException {
    var now = Instant.now();
    var jwt =
        new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            new JWTClaimsSet.Builder()
                .subject(subject)
                .audience(audience)
                .claim("azp", client)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(120)))
                .claim("realm_access", Map.of("roles", roles))
                .build());
    jwt.sign(new MACSigner(TEST_KEY));
    return "Bearer " + jwt.serialize();
  }

  @TestConfiguration
  static class FixtureJwtDecoder {
    @Bean
    JwtDecoder jwtDecoder() {
      return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(TEST_KEY, "HmacSHA256")).build();
    }
  }
}
