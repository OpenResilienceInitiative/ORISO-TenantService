package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for the authoritative tenant ID allocation ledger (TEN-INV-U1). */
public interface TenantIdReservationRepository
    extends JpaRepository<TenantIdReservationEntity, Long> {

  @Query("SELECT r.tenantId FROM TenantIdReservationEntity r")
  List<Long> findAllLedgerIds();

  /**
   * Loads the ledger row under a write lock. The public DPA forward uses this to serialise
   * concurrent link creation for one onboarding: the reservation row is the natural per-onboarding
   * mutex (it already has to be read to authorise the call), so locking it makes the
   * count-then-insert of the outstanding-link cap atomic without inventing a second lock table.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM TenantIdReservationEntity r WHERE r.tenantId = :tenantId")
  Optional<TenantIdReservationEntity> findByTenantIdForUpdate(@Param("tenantId") Long tenantId);

  /**
   * Atomically consumes an open reservation: flips it to ASSIGNED only if it is still RESERVED and
   * the caller presents the matching reservation token. Returns the number of updated rows, so of
   * two parallel consumers exactly one sees 1 and the other 0.
   */
  @Modifying
  @Query(
      "UPDATE TenantIdReservationEntity r SET r.status = :assigned, r.updateDate = :now "
          + "WHERE r.tenantId = :tenantId AND r.status = :reserved AND r.token = :token")
  int consumeReservation(
      @Param("tenantId") long tenantId,
      @Param("token") String token,
      @Param("reserved") TenantIdReservationStatus reserved,
      @Param("assigned") TenantIdReservationStatus assigned,
      @Param("now") LocalDateTime now);

  /**
   * Compensating update for a rolled-back tenant creation that had consumed an open invite's
   * reservation: flips the row back from ASSIGNED to RESERVED, but only when the caller presents
   * the original reservation token. Returns the number of updated rows (0 when the row was created
   * fresh in the failed creation, i.e. its token does not match).
   */
  @Modifying
  @Query(
      "UPDATE TenantIdReservationEntity r SET r.status = :reserved, r.updateDate = :now "
          + "WHERE r.tenantId = :tenantId AND r.status = :assigned AND r.token = :token")
  int restoreReservation(
      @Param("tenantId") long tenantId,
      @Param("token") String token,
      @Param("assigned") TenantIdReservationStatus assigned,
      @Param("reserved") TenantIdReservationStatus reserved,
      @Param("now") LocalDateTime now);

  @Modifying
  @Query(
      "DELETE FROM TenantIdReservationEntity r "
          + "WHERE r.tenantId = :tenantId AND r.status = :status")
  int deleteByTenantIdAndStatus(
      @Param("tenantId") long tenantId, @Param("status") TenantIdReservationStatus status);
}
