package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for the authoritative tenant ID allocation ledger (TEN-INV-U1). */
public interface TenantIdReservationRepository
    extends JpaRepository<TenantIdReservationEntity, Long> {

  @Query("SELECT r.tenantId FROM TenantIdReservationEntity r")
  List<Long> findAllLedgerIds();

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
