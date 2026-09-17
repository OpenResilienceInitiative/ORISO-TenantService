package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantLegalDraftRepository extends JpaRepository<TenantLegalDraftEntity, Long> {
  Optional<TenantLegalDraftEntity> findByTenantIdAndKind(Long tenantId, TenantLegalDraftKind kind);
}
