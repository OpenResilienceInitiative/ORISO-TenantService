package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDpaAdminSignatureEntity;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Append-only access to the tenant-admin DPA signature audit trail (TEN-INV-U9). Deliberately
 * extends the marker {@link Repository} instead of {@code JpaRepository} so that no update or
 * delete operation is ever exposed — signature rows are immutable audit evidence.
 */
public interface TenantDpaAdminSignatureRepository
    extends Repository<TenantDpaAdminSignatureEntity, Long> {

  TenantDpaAdminSignatureEntity save(TenantDpaAdminSignatureEntity entity);

  List<TenantDpaAdminSignatureEntity> findByTenantIdOrderBySignedAtDescIdDesc(Long tenantId);

  long countByTenantId(Long tenantId);
}
