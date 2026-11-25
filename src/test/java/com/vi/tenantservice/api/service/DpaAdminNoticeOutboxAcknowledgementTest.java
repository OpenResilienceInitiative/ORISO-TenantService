package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@Import(DpaAdminNoticeOutboxService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DpaAdminNoticeOutboxAcknowledgementTest {

  private static final long SIGNED_AT = 1_700_000_000_123L;

  @Autowired private DpaAdminNoticeOutboxService outbox;
  @Autowired private TenantDpaSignatureRepository signatures;
  @Autowired private TenantIdReservationRepository reservations;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("delete from tenant_dpa_signature where tenant_id in (9781, 9782, 9783)");
    jdbcTemplate.update("delete from tenant_id_reservation where tenant_id in (9781, 9782, 9783)");
  }

  @Test
  void acknowledgementIsExactAndSafeToRepeat() {
    var owner = reserve(9781L, TenantIdReservationStatus.RESERVED);
    var confirmed = signature(owner.getTenantId(), owner.getToken(), SIGNED_AT, SIGNED_AT);
    var other = signature(owner.getTenantId(), owner.getToken(), SIGNED_AT, SIGNED_AT);
    var old = signature(owner.getTenantId(), owner.getToken(), SIGNED_AT, null);
    var inconsistent = signature(owner.getTenantId(), owner.getToken(), SIGNED_AT, SIGNED_AT + 1);

    assertThat(outbox.readExact(confirmed.getId())).isPresent();
    assertThat(outbox.readExact(old.getId())).isEmpty();
    assertThat(outbox.readExact(inconsistent.getId())).isEmpty();
    assertThat(outbox.acknowledge(confirmed.getId(), SIGNED_AT - 1)).isFalse();
    assertThat(outbox.acknowledge(old.getId(), SIGNED_AT)).isFalse();
    assertThat(outbox.acknowledge(inconsistent.getId(), SIGNED_AT)).isFalse();
    assertThat(outbox.acknowledge(confirmed.getId(), SIGNED_AT)).isTrue();
    assertThat(outbox.acknowledge(confirmed.getId(), SIGNED_AT)).isTrue();

    assertThat(outbox.readExact(confirmed.getId()))
        .get()
        .extracting(DpaAdminNoticeOutboxService.ExactSignature::signedAtEpochMillis)
        .isEqualTo(SIGNED_AT);
    assertThat(reload(confirmed).getAdminNoticePendingSinceEpochMillis()).isNull();
    assertThat(reload(confirmed).getAdminNoticeHandoffSignedAtEpochMillis()).isEqualTo(SIGNED_AT);
    assertThat(reload(confirmed).getSignedAtEpochMillis()).isEqualTo(SIGNED_AT);
    assertThat(reload(confirmed).getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(reload(other).getAdminNoticePendingSinceEpochMillis()).isEqualTo(SIGNED_AT);
    assertThat(reload(inconsistent).getAdminNoticePendingSinceEpochMillis())
        .isEqualTo(SIGNED_AT + 1);

    var replacement = reservations.findById(owner.getTenantId()).orElseThrow();
    replacement.setToken("replacement-owner");
    reservations.saveAndFlush(replacement);
    assertThat(outbox.readExact(confirmed.getId())).isEmpty();
    assertThat(outbox.readExact(other.getId())).isEmpty();
    assertThat(reload(confirmed).getAdminNoticeHandoffSignedAtEpochMillis()).isEqualTo(SIGNED_AT);
  }

  @Test
  void acknowledgementPreservesEventsFromAReplacedOrDeletedOccupant() {
    var replacement = reserve(9782L, TenantIdReservationStatus.RESERVED);
    var prior =
        signature(replacement.getTenantId(), "superseded-reservation", SIGNED_AT, SIGNED_AT);
    var assignedWithNoTenant = reserve(9783L, TenantIdReservationStatus.ASSIGNED);
    var deleted =
        signature(
            assignedWithNoTenant.getTenantId(),
            assignedWithNoTenant.getToken(),
            SIGNED_AT,
            SIGNED_AT);

    assertThat(outbox.acknowledge(prior.getId(), SIGNED_AT)).isFalse();
    assertThat(outbox.acknowledge(deleted.getId(), SIGNED_AT)).isFalse();
    assertThat(outbox.readExact(deleted.getId())).isEmpty();
    assertThat(reload(prior).getAdminNoticePendingSinceEpochMillis()).isEqualTo(SIGNED_AT);
    assertThat(reload(deleted).getAdminNoticePendingSinceEpochMillis()).isEqualTo(SIGNED_AT);
  }

  private TenantDpaSignatureEntity reload(TenantDpaSignatureEntity signature) {
    return signatures.findById(signature.getId()).orElseThrow();
  }

  private TenantIdReservationEntity reserve(long id, TenantIdReservationStatus status) {
    var now = LocalDateTime.now();
    return reservations.saveAndFlush(
        TenantIdReservationEntity.builder()
            .tenantId(id)
            .token("current-reservation-" + id)
            .status(status)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private TenantDpaSignatureEntity signature(
      long tenantId, String token, long signedAt, Long pending) {
    return signatures.saveAndFlush(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .status(DpaSignatureStatus.SIGNED)
            .source(TenantDpaService.SOURCE_FORWARDED_EXTERNAL)
            .createDate(LocalDateTime.now())
            .signedAtEpochMillis(signedAt)
            .reservationTokenHash(DpaSignToken.hash(token))
            .adminNoticePendingSinceEpochMillis(pending)
            .build());
  }
}
