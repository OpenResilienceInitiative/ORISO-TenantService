package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDpaVersionRepository extends JpaRepository<TenantDpaVersionEntity, Long> {

  /** Published versions for a tenant, newest first (so the UI can default to the latest). */
  List<TenantDpaVersionEntity> findByTenantIdOrderByActivationDateDesc(Long tenantId);
}
