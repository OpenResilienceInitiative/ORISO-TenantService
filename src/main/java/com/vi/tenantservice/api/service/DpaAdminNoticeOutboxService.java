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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DpaAdminNoticeOutboxService {

  private static final int MAX_BATCH = 100;

  private final TenantDpaSignatureRepository signatures;
  private final TenantIdReservationRepository reservations;
  private final TenantRepository tenants;
  private final TransactionTemplate acknowledgementTransaction;

  public DpaAdminNoticeOutboxService(
      TenantDpaSignatureRepository signatures,
      TenantIdReservationRepository reservations,
      TenantRepository tenants,
      PlatformTransactionManager transactionManager) {
    this.signatures = signatures;
    this.reservations = reservations;
    this.tenants = tenants;
    this.acknowledgementTransaction = new TransactionTemplate(transactionManager);
  }

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
   * Confirms the exact current event after the trusted consumer has durably taken ownership.
   * Retained handoff provenance lets the same consumer repeat a request whose response was lost.
   */
  @Transactional(propagation = Propagation.NEVER)
  public boolean acknowledge(long eventId, long signedAtEpochMillis) {
    // Discover outside the write transaction. Under MariaDB REPEATABLE READ the tenant-existence
    // snapshot must start after the reservation and signature locks have been acquired.
    var candidate =
        signatures
            .findExactAdminNotice(eventId)
            .filter(row -> row.getSignedAtEpochMillis() == signedAtEpochMillis);
    return candidate
        .map(
            row ->
                acknowledgementTransaction.execute(
                    status -> acknowledgeLocked(row.getTenantId(), eventId, signedAtEpochMillis)))
        .orElse(false);
  }

  private boolean acknowledgeLocked(long tenantId, long eventId, long signedAtEpochMillis) {
    // Hold the authoritative reservation row through the conditional update so it cannot be
    // released and reused between validation and acknowledgement.
    var reservation = reservations.findByTenantIdForUpdate(tenantId);
    // Use the current locked signature even if another request committed after discovery.
    var locked = signatures.lockExactAdminNotice(eventId, signedAtEpochMillis);
    if (locked.isEmpty()
        || !eligible(
            locked.get().getReservationTokenHash(),
            reservation.map(TenantIdReservationEntity::getToken).orElse(null),
            reservation.map(TenantIdReservationEntity::getStatus).orElse(null),
            tenants.existsById(tenantId))) {
      return false;
    }
    return Long.valueOf(signedAtEpochMillis)
            .equals(locked.get().getAdminNoticeHandoffSignedAtEpochMillis())
        || signatures.clearPendingAdminNotice(eventId, signedAtEpochMillis) == 1;
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
