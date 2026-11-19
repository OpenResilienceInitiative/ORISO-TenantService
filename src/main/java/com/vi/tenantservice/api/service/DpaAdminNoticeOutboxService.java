package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.util.List;
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
            .filter(
                row ->
                    eligible(
                        row.getReservationBinding(),
                        row.getReservationToken(),
                        row.getReservationStatus(),
                        row.getRegisteredTenantId() != null))
            .map(
                row ->
                    new PendingEvent(row.getId(), row.getTenantId(), row.getSignedAtEpochMillis()))
            .toList();
    // Move past examined stale rows even when this page has no eligible events. A new scan starts
    // at zero, since a previously examined low-ID sign link may be confirmed later.
    return new PendingPage(events, page.hasNext() ? examined.getLast().getId() : null);
  }

  /**
   * Clears one exact event after a future trusted caller has independently proved durable ownership
   * of it. Intentionally has no controller or legacy-hint caller.
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

  public record PendingEvent(long id, long tenantId, long signedAtEpochMillis) {}

  public record PendingPage(List<PendingEvent> events, Long nextAfterId) {}
}
