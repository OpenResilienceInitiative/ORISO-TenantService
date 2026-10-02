package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TenantDpaVersionRepository extends JpaRepository<TenantDpaVersionEntity, Long> {

  /** Current locking read: an adopted deadline policy cannot be downgraded by a legacy writer. */
  @Lock(LockModeType.PESSIMISTIC_READ)
  Optional<TenantDpaVersionEntity>
      findFirstByTenantIdAndSigningDeadlineAtIsNotNullOrderByActivationDateDescIdDesc(
          Long tenantId);

  Optional<TenantDpaVersionEntity> findFirstByTenantIdOrderByActivationDateDescIdDesc(
      Long tenantId);

  /** Published versions for a tenant, newest first (so the UI can default to the latest). */
  List<TenantDpaVersionEntity> findByTenantIdOrderByActivationDateDesc(Long tenantId);

  /** Exact immutable contract snapshot referenced by a public signing invitation. */
  Optional<TenantDpaVersionEntity> findFirstByTenantIdAndActivationDate(
      Long tenantId, LocalDateTime activationDate);
}
