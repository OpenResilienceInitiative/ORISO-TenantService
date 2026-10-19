package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantLegalProposalDeliveryEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantLegalProposalDeliveryRepository
    extends JpaRepository<TenantLegalProposalDeliveryEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query(
      "select d from TenantLegalProposalDeliveryEntity d where d.distributionId = :distributionId order by d.id")
  List<TenantLegalProposalDeliveryEntity> findLockedByDistributionId(
      @Param("distributionId") String distributionId);
}
