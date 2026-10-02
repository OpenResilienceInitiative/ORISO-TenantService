package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalTextVersionEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantLegalTextVersionRepository
    extends JpaRepository<TenantLegalTextVersionEntity, Long> {

  List<TenantLegalTextVersionEntity> findByTenantIdAndKindOrderByPublishedAtDescIdDesc(
      Long tenantId, TenantLegalDraftKind kind);

  /** Locked so two concurrent publishes cannot both leave their version open. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select v from TenantLegalTextVersionEntity v where v.tenantId = :tenantId and v.kind = :kind and v.supersededAt is null order by v.id")
  List<TenantLegalTextVersionEntity> findLockedOpen(
      @Param("tenantId") Long tenantId, @Param("kind") TenantLegalDraftKind kind);
}
