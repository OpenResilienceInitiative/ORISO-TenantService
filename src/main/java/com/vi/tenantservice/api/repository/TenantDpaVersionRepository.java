package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDpaVersionRepository extends JpaRepository<TenantDpaVersionEntity, Long> {

  /** Published versions for a tenant, newest first (so the UI can default to the latest). */
  List<TenantDpaVersionEntity> findByTenantIdOrderByActivationDateDesc(Long tenantId);

  /** Exact immutable contract snapshot referenced by a public signing invitation. */
  Optional<TenantDpaVersionEntity> findFirstByTenantIdAndActivationDate(
      Long tenantId, LocalDateTime activationDate);
}
