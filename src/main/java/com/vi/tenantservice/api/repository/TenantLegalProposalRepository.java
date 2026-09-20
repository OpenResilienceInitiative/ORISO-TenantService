package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TenantLegalProposalRepository
    extends JpaRepository<TenantLegalProposalEntity, Long> {
  List<TenantLegalProposalEntity> findBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
      Long sourceDraftId, Long sourceDraftVersion, Collection<Long> recipientTenantIds);

  List<TenantLegalProposalEntity> findByRecipientTenantIdOrderByCreatedAtDesc(Long tenantId);

  List<TenantLegalProposalEntity> findByRecipientTenantIdAndKindOrderByCreatedAtDesc(
      Long tenantId, TenantLegalDraftKind kind);

  Optional<TenantLegalProposalEntity> findByIdAndRecipientTenantId(Long id, Long tenantId);

  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("select p from TenantLegalProposalEntity p where p.id in :ids")
  List<TenantLegalProposalEntity> findLockedByIdIn(@Param("ids") Collection<Long> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select p from TenantLegalProposalEntity p where p.recipientTenantId in :tenantIds and p.kind = :kind and p.status in :statuses order by p.recipientTenantId, p.id")
  List<TenantLegalProposalEntity> findLockedByRecipientTenantIdInAndKindAndStatusIn(
      @Param("tenantIds") Collection<Long> tenantIds,
      @Param("kind") TenantLegalDraftKind kind,
      @Param("statuses") Collection<TenantLegalProposalStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select p from TenantLegalProposalEntity p where p.id = :id and p.recipientTenantId = :tenantId")
  Optional<TenantLegalProposalEntity> findLockedByIdAndRecipientTenantId(
      @Param("id") Long id, @Param("tenantId") Long tenantId);
}
