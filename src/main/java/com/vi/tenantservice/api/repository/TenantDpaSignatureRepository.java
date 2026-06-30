package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDpaSignatureRepository
    extends JpaRepository<TenantDpaSignatureEntity, Long> {

  List<TenantDpaSignatureEntity> findByTenantId(Long tenantId);

  List<TenantDpaSignatureEntity> findByTenantIdAndStatus(Long tenantId, DpaSignatureStatus status);

  Optional<TenantDpaSignatureEntity> findByTokenHashAndStatus(
      String tokenHash, DpaSignatureStatus status);
}
