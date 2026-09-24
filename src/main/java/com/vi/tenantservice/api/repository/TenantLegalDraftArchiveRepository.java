package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantLegalDraftArchiveRepository
    extends JpaRepository<TenantLegalDraftArchiveEntity, Long> {
  List<TenantLegalDraftArchiveEntity> findByTenantIdOrderByArchivedAtDescIdDesc(Long tenantId);

  List<TenantLegalDraftArchiveEntity> findByTenantIdAndKindOrderByArchivedAtDescIdDesc(
      Long tenantId, TenantLegalDraftKind kind);

  Optional<TenantLegalDraftArchiveEntity> findByIdAndTenantId(Long id, Long tenantId);
}
