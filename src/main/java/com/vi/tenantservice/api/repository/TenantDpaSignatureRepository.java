package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantDpaSignatureRepository
    extends JpaRepository<TenantDpaSignatureEntity, Long> {

  List<TenantDpaSignatureEntity> findByTenantId(Long tenantId);

  List<TenantDpaSignatureEntity> findByTenantIdAndStatus(Long tenantId, DpaSignatureStatus status);

  Optional<TenantDpaSignatureEntity> findByTokenHashAndStatus(
      String tokenHash, DpaSignatureStatus status);

  /** Retention purge: removes signatures of a given status created before the cutoff. */
  long deleteByStatusAndCreateDateBefore(DpaSignatureStatus status, LocalDateTime cutoff);

  /**
   * Whether an unexpired sign link for exactly this DPA version is still outstanding — the
   * "contract on hold" predicate. The version is part of the question on purpose (#179): a link
   * minted for a superseded version can only ever produce an OUTDATED signature, so reporting the
   * CURRENT contract as "awaiting a signer" because of it would mislead the wizard and the gate.
   */
  boolean existsByTenantIdAndDpaVersionAndStatusAndTokenExpiresAtAfter(
      Long tenantId, LocalDateTime dpaVersion, DpaSignatureStatus status, LocalDateTime now);

  /** App-level replacement for the dropped DB cascade: removes all rows of a deleted tenant. */
  long deleteByTenantId(Long tenantId);

  /** Outstanding, unexpired links of a tenant — throttle input for the public forward endpoint. */
  long countByTenantIdAndStatusAndTokenExpiresAtAfter(
      Long tenantId, DpaSignatureStatus status, LocalDateTime now);

  /**
   * Atomically consumes a PENDING sign token: flips it to SIGNED, records the signer, and clears
   * the token — all in one conditional UPDATE. Only the row that is still PENDING is affected, so
   * under a concurrent double-submit exactly one caller wins (rows affected = 1) and the rest get
   * 0. This is what makes "single-use" hold under concurrency (the read-then-write path cannot).
   *
   * <p>{@code forwardedByUserId} is deliberately NOT part of the update (ORISO-TenantService#179):
   * it is stamped when the sign link is created and must not be overridable by the signing client.
   * {@code source} is likewise never taken from the client — it is only defaulted here for links
   * minted before source stamping existed, so the persisted column matches what the API returns (a
   * legacy row would otherwise stay null in the database and be missed by every later reader).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update TenantDpaSignatureEntity s set "
          + "s.status = com.vi.tenantservice.api.model.DpaSignatureStatus.SIGNED, "
          + "s.signerName = :signerName, s.signerPosition = :signerPosition, "
          + "s.signerEmail = :signerEmail, s.signerOrganisation = :signerOrganisation, "
          + "s.signerIsMember = :signerIsMember, s.language = :language, "
          + "s.source = coalesce(s.source, :defaultSource), "
          + "s.signedAt = :now, s.tokenHash = null "
          + "where s.tokenHash = :tokenHash "
          + "and s.status = com.vi.tenantservice.api.model.DpaSignatureStatus.PENDING")
  int consumeSignToken(
      @Param("tokenHash") String tokenHash,
      @Param("signerName") String signerName,
      @Param("signerPosition") String signerPosition,
      @Param("signerEmail") String signerEmail,
      @Param("signerOrganisation") String signerOrganisation,
      @Param("signerIsMember") Boolean signerIsMember,
      @Param("language") String language,
      @Param("defaultSource") String defaultSource,
      @Param("now") LocalDateTime now);

  /**
   * Invalidates every outstanding sign link of the tenant (ORISO-TenantService#179): the moment any
   * signature is recorded, all still-PENDING rows flip to INVALIDATED and lose their token, so
   * every outstanding link resolves to the defined "no longer valid" state.
   */
  // flushAutomatically matters: this runs in the same transaction as the signature insert, and a
  // bulk update that only cleared the persistence context would discard the not-yet-flushed row.
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update TenantDpaSignatureEntity s set "
          + "s.status = com.vi.tenantservice.api.model.DpaSignatureStatus.INVALIDATED, "
          + "s.tokenHash = null "
          + "where s.tenantId = :tenantId "
          + "and s.status = com.vi.tenantservice.api.model.DpaSignatureStatus.PENDING")
  int invalidateOutstandingByTenantId(@Param("tenantId") Long tenantId);
}
