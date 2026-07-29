package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantIdAllocationStatus;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Authoritative allocation of tenant IDs (TEN-INV-U1, ORISO-TenantService#143).
 *
 * <p>An ID is FREE, RESERVED (held by an open invite) or ASSIGNED (consumed by a real tenant).
 * Every reservation and every new tenant creation writes a row into the {@code
 * tenant_id_reservation} ledger inside the allocating transaction, so the ledger's primary key -
 * not an application-level check - guarantees that an ID is handed out exactly once. AUTO mode
 * assigns the smallest currently free ID; a lost race on the primary key is retried with the next
 * candidate in a fresh transaction.
 */
@Service
@Slf4j
public class TenantIdAllocationService {

  /** Tenant ID 0 is the technical tenant and never assignable. */
  private static final long MIN_ASSIGNABLE_TENANT_ID = 1L;

  private static final int MAX_AUTO_RESERVE_ATTEMPTS = 20;

  private final TenantRepository tenantRepository;
  private final TenantIdReservationRepository reservationRepository;
  private final TransactionTemplate requiresNewTransaction;

  public TenantIdAllocationService(
      TenantRepository tenantRepository,
      TenantIdReservationRepository reservationRepository,
      PlatformTransactionManager transactionManager) {
    this.tenantRepository = tenantRepository;
    this.reservationRepository = reservationRepository;
    this.requiresNewTransaction = new TransactionTemplate(transactionManager);
    this.requiresNewTransaction.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Returns the allocation status of a single tenant ID. */
  public TenantIdAllocationStatus getStatus(long tenantId) {
    if (tenantId < MIN_ASSIGNABLE_TENANT_ID) {
      return TenantIdAllocationStatus.ASSIGNED;
    }
    Optional<TenantIdReservationEntity> ledgerRow = reservationRepository.findById(tenantId);
    if (ledgerRow.isPresent()) {
      return ledgerRow.get().getStatus() == TenantIdReservationStatus.RESERVED
          ? TenantIdAllocationStatus.RESERVED
          : TenantIdAllocationStatus.ASSIGNED;
    }
    return tenantRepository.existsById(tenantId)
        ? TenantIdAllocationStatus.ASSIGNED
        : TenantIdAllocationStatus.FREE;
  }

  /**
   * Returns the next free tenant ID from {@code from} (exclusive) in the given direction, skipping
   * assigned and reserved IDs. Empty if no free ID exists in that direction.
   */
  public Optional<Long> nextFreeId(long from, boolean upwards) {
    Set<Long> usedIds = loadUsedIds();
    if (upwards) {
      long candidate = Math.max(from + 1, MIN_ASSIGNABLE_TENANT_ID);
      while (usedIds.contains(candidate)) {
        candidate++;
      }
      return Optional.of(candidate);
    }
    long candidate = from - 1;
    while (candidate >= MIN_ASSIGNABLE_TENANT_ID && usedIds.contains(candidate)) {
      candidate--;
    }
    return candidate >= MIN_ASSIGNABLE_TENANT_ID ? Optional.of(candidate) : Optional.empty();
  }

  /**
   * Reserves a tenant ID for an open invite. With {@code requestedId == null} (AUTO mode) the
   * smallest currently free ID is reserved; a concurrent loser on the ledger's primary key retries
   * with the next candidate. For a specific ID a conflict is reported as {@link
   * TenantIdAllocationConflictException}.
   */
  public TenantIdReservationEntity reserve(Long requestedId, String reservedBy) {
    if (requestedId != null) {
      if (requestedId < MIN_ASSIGNABLE_TENANT_ID) {
        throw new TenantIdAllocationConflictException(
            "Tenant ID " + requestedId + " is not assignable");
      }
      try {
        return insertReservationInNewTransaction(requestedId, reservedBy);
      } catch (DataAccessException e) {
        throw new TenantIdAllocationConflictException(
            "Tenant ID " + requestedId + " is already assigned or reserved", e);
      }
    }
    return reserveSmallestFreeId(reservedBy);
  }

  private TenantIdReservationEntity reserveSmallestFreeId(String reservedBy) {
    for (int attempt = 1; attempt <= MAX_AUTO_RESERVE_ATTEMPTS; attempt++) {
      long candidate = smallestFreeId();
      try {
        return insertReservationInNewTransaction(candidate, reservedBy);
      } catch (DataAccessException | TenantIdAllocationConflictException e) {
        log.info(
            "Lost the race for AUTO tenant ID {} (attempt {}/{}), retrying with next candidate",
            candidate,
            attempt,
            MAX_AUTO_RESERVE_ATTEMPTS);
      }
    }
    throw new IllegalStateException(
        "Could not auto-reserve a tenant ID after " + MAX_AUTO_RESERVE_ATTEMPTS + " attempts");
  }

  private TenantIdReservationEntity insertReservationInNewTransaction(
      long tenantId, String reservedBy) {
    return requiresNewTransaction.execute(
        transactionStatus -> {
          assertIdIsFree(tenantId);
          return reservationRepository.saveAndFlush(
              newLedgerRow(tenantId, TenantIdReservationStatus.RESERVED, reservedBy));
        });
  }

  /**
   * Releases an unconsumed reservation so the ID becomes assignable again. Returns {@code false} if
   * no open reservation exists for the ID (assigned IDs are not releasable).
   */
  @Transactional
  public boolean release(long tenantId) {
    return reservationRepository.deleteByTenantIdAndStatus(
            tenantId, TenantIdReservationStatus.RESERVED)
        > 0;
  }

  /**
   * Assigns the definitive ID to a tenant that is about to be created, inside the creating
   * transaction (atomic consumption).
   *
   * <ul>
   *   <li>ID not set: AUTO mode - the smallest currently free ID is assigned and recorded in the
   *       ledger. The ledger's primary key resolves races; the caller may retry on conflict.
   *   <li>ID set and FREE: recorded in the ledger and assigned.
   *   <li>ID set and RESERVED: consumed only when the matching reservation token is presented,
   *       otherwise rejected with a conflict.
   *   <li>ID set and ASSIGNED: rejected with a conflict.
   * </ul>
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void assignIdForNewTenant(TenantEntity entity, String reservationToken) {
    if (entity.getId() == null) {
      long candidate = smallestFreeId();
      insertAssignedLedgerRow(candidate);
      entity.setId(candidate);
      return;
    }

    long requestedId = entity.getId();
    if (requestedId < MIN_ASSIGNABLE_TENANT_ID) {
      throw new TenantIdAllocationConflictException(
          "Tenant ID " + requestedId + " is not assignable");
    }
    Optional<TenantIdReservationEntity> ledgerRow = reservationRepository.findById(requestedId);
    if (ledgerRow.isPresent()) {
      consumeReservationOrConflict(requestedId, reservationToken);
      return;
    }
    if (tenantRepository.existsById(requestedId)) {
      throw new TenantIdAllocationConflictException(
          "Tenant ID " + requestedId + " is already assigned");
    }
    insertAssignedLedgerRow(requestedId);
  }

  private void consumeReservationOrConflict(long requestedId, String reservationToken) {
    if (reservationToken == null || reservationToken.isBlank()) {
      throw new TenantIdAllocationConflictException(
          "Tenant ID " + requestedId + " is reserved by an open invite");
    }
    int consumed =
        reservationRepository.consumeReservation(
            requestedId,
            reservationToken,
            TenantIdReservationStatus.RESERVED,
            TenantIdReservationStatus.ASSIGNED,
            LocalDateTime.now(ZoneOffset.UTC));
    if (consumed != 1) {
      throw new TenantIdAllocationConflictException(
          "Tenant ID " + requestedId + " is already assigned or reserved with a different token");
    }
  }

  /**
   * Removes the ledger row of a tenant ID whose creation was rolled back after commit (e.g.
   * consulting type creation failed), so the ID becomes assignable again.
   */
  @Transactional
  public void releaseAssignment(long tenantId) {
    reservationRepository.deleteByTenantIdAndStatus(tenantId, TenantIdReservationStatus.ASSIGNED);
  }

  private void insertAssignedLedgerRow(long tenantId) {
    try {
      reservationRepository.saveAndFlush(
          newLedgerRow(tenantId, TenantIdReservationStatus.ASSIGNED, null));
    } catch (DataAccessException e) {
      throw new TenantIdAllocationConflictException(
          "Tenant ID " + tenantId + " is already assigned or reserved", e);
    }
  }

  private void assertIdIsFree(long tenantId) {
    if (reservationRepository.existsById(tenantId) || tenantRepository.existsById(tenantId)) {
      throw new TenantIdAllocationConflictException(
          "Tenant ID " + tenantId + " is already assigned or reserved");
    }
  }

  private long smallestFreeId() {
    Set<Long> usedIds = loadUsedIds();
    long candidate = MIN_ASSIGNABLE_TENANT_ID;
    while (usedIds.contains(candidate)) {
      candidate++;
    }
    return candidate;
  }

  private Set<Long> loadUsedIds() {
    Set<Long> usedIds = new HashSet<>(tenantRepository.findAllIds());
    usedIds.addAll(reservationRepository.findAllLedgerIds());
    return usedIds;
  }

  private TenantIdReservationEntity newLedgerRow(
      long tenantId, TenantIdReservationStatus status, String reservedBy) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return TenantIdReservationEntity.builder()
        .tenantId(tenantId)
        .status(status)
        .token(UUID.randomUUID().toString())
        .reservedBy(reservedBy)
        .createDate(now)
        .updateDate(now)
        .build();
  }
}
