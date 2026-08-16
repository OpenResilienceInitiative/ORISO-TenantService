package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantPermissionPolicyRepository
    extends JpaRepository<TenantPermissionPolicyEntity, Long> {
  Optional<TenantPermissionPolicyEntity> findByTenantId(Long tenantId);
}
