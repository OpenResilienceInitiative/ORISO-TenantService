package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAdminControlsRepository
    extends JpaRepository<TenantAdminControlsEntity, Long> {

  Optional<TenantAdminControlsEntity> findTopByOrderByIdAsc();
}
