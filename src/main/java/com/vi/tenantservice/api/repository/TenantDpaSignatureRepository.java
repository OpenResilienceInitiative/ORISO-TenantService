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

  /**
   * Atomically consumes a PENDING sign token: flips it to SIGNED, records the signer, and clears
   * the token — all in one conditional UPDATE. Only the row that is still PENDING is affected, so
   * under a concurrent double-submit exactly one caller wins (rows affected = 1) and the rest get
   * 0. This is what makes "single-use" hold under concurrency (the read-then-write path cannot).
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "update TenantDpaSignatureEntity s set "
          + "s.status = com.vi.tenantservice.api.model.DpaSignatureStatus.SIGNED, "
          + "s.signerName = :signerName, s.signerPosition = :signerPosition, "
          + "s.signerIsMember = :signerIsMember, s.language = :language, "
          + "s.signedAt = :now, s.tokenHash = null "
          + "where s.tokenHash = :tokenHash "
          + "and s.status = com.vi.tenantservice.api.model.DpaSignatureStatus.PENDING")
  int consumeSignToken(
      @Param("tokenHash") String tokenHash,
      @Param("signerName") String signerName,
      @Param("signerPosition") String signerPosition,
      @Param("signerIsMember") Boolean signerIsMember,
      @Param("language") String language,
      @Param("now") LocalDateTime now);
}
