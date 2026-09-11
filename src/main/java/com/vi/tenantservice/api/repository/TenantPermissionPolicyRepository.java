package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantPermissionPolicyRepository
    extends JpaRepository<TenantPermissionPolicyEntity, Long> {
  Optional<TenantPermissionPolicyEntity> findByTenantId(Long tenantId);

  List<TenantPermissionPolicyEntity> findByTenantIdIn(Set<Long> tenantIds);
}
