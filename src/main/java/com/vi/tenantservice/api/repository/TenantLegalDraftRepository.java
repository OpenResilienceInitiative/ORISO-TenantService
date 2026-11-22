package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantLegalDraftRepository extends JpaRepository<TenantLegalDraftEntity, Long> {
  Optional<TenantLegalDraftEntity> findByOwnerKeyAndKind(Long ownerKey, TenantLegalDraftKind kind);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from TenantLegalDraftEntity d where d.ownerKey = :ownerKey and d.kind = :kind")
  Optional<TenantLegalDraftEntity> findLockedByOwnerKeyAndKind(
      @Param("ownerKey") Long ownerKey, @Param("kind") TenantLegalDraftKind kind);
}
