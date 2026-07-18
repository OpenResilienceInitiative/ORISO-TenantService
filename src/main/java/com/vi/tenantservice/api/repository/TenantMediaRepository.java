package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMediaRepository extends JpaRepository<TenantMediaEntity, String> {}
