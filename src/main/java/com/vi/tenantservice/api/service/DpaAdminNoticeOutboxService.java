package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository.PendingAdminNotice;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DpaAdminNoticeOutboxService {

  private static final int MAX_BATCH = 100;

  private final TenantDpaSignatureRepository signatures;
  private final TenantIdReservationRepository reservations;
  private final TenantRepository tenants;

  @Transactional(readOnly = true)
  public PendingPage readPending(long afterId, int limit) {
    var page =
        signatures.findPendingAdminNotices(
            afterId, PageRequest.of(0, Math.clamp(limit, 1, MAX_BATCH)));
    var examined = page.getContent();
    var events =
        examined.stream()
            .filter(DpaAdminNoticeOutboxService::eligible)
            .map(
                row ->
                    new PendingEvent(row.getId(), row.getTenantId(), row.getSignedAtEpochMillis()))
            .toList();
    // Move past examined stale rows even when this page has no eligible events. A new scan starts
    // at zero, since a previously examined low-ID sign link may be confirmed later.
    return new PendingPage(events, page.hasNext() ? examined.getLast().getId() : null);
  }

  @Transactional(readOnly = true)
  public Optional<ExactSignature> readExact(long eventId) {
    return signatures
        .findExactAdminNotice(eventId)
        .filter(DpaAdminNoticeOutboxService::eligible)
        .map(
            row ->
                new ExactSignature(
                    row.getId(),
                    row.getTenantId(),
                    row.getSignedAtEpochMillis(),
                    auditTimestamp(row.getDpaVersion()),
                    auditTimestamp(row.getSignedAt()),
                    row.getForwardedByUserId(),
                    row.getSignerName(),
                    row.getSignerPosition()));
  }

  private static String auditTimestamp(LocalDateTime time) {
    return time == null ? null : time.toString();
  }

  /**
   * Records the original signed-event proof and clears its pending status after a future trusted
   * caller independently proves durable ownership. Intentionally has no controller or legacy-hint
   * caller.
   */
  @Transactional
  boolean acknowledge(long eventId, long signedAtEpochMillis) {
    var event = signatures.findPendingAdminNotice(eventId, signedAtEpochMillis);
    if (event.isEmpty()) {
      return false;
    }
    var signature = event.get();
    var tenantId = signature.getTenantId();
    // Hold the authoritative reservation row through the conditional update so it cannot be
    // released and reused between validation and acknowledgement.
    var reservation = reservations.findByTenantIdForUpdate(tenantId);
    if (!eligible(
        signature.getReservationTokenHash(),
        reservation.map(TenantIdReservationEntity::getToken).orElse(null),
        reservation.map(TenantIdReservationEntity::getStatus).orElse(null),
        tenants.existsById(tenantId))) {
      return false;
    }
    return signatures.clearPendingAdminNotice(eventId, signedAtEpochMillis) == 1;
  }

  private static boolean eligible(
      String reservationBinding,
      String reservationToken,
      TenantIdReservationStatus status,
      boolean registered) {
    if (!registered
        && (reservationBinding == null || status != TenantIdReservationStatus.RESERVED)) {
      return false;
    }
    var currentHash = reservationToken == null ? null : DpaSignToken.hash(reservationToken);
    return DpaSignatureOwnership.belongsToCurrentOccupant(reservationBinding, currentHash);
  }

  private static boolean eligible(PendingAdminNotice row) {
    return eligible(
        row.getReservationBinding(),
        row.getReservationToken(),
        row.getReservationStatus(),
        row.getRegisteredTenantId() != null);
  }

  public record PendingEvent(long id, long tenantId, long signedAtEpochMillis) {}

  public record PendingPage(List<PendingEvent> events, Long nextAfterId) {}

  public record ExactSignature(
      long id,
      long tenantId,
      long signedAtEpochMillis,
      String dpaVersion,
      String signedAt,
      String forwardedByUserId,
      String signerName,
      String signerPosition) {}
}
