package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TenantLegalProposalRepository
    extends JpaRepository<TenantLegalProposalEntity, Long> {
  /**
   * The same lookup as a locking read. A delivery must decide which recipients already hold this
   * exact revision <em>after</em> it has waited for the platform-draft lock. A plain read answers
   * from the transaction's MariaDB REPEATABLE READ snapshot, which was taken before that wait, so
   * it cannot see a proposal another overlapping delivery has just committed — and the insert that
   * follows then hits {@code uq_tenant_legal_proposal_source_recipient}. Locking makes it a current
   * read.
   *
   * <p>The platform-draft lock serialises deliveries of the <em>same</em> kind, so this costs
   * nothing there. It is not free across kinds: a privacy and an imprint delivery running at the
   * same time take gap locks on this unique index and can deadlock on the insert that follows.
   * InnoDB rolls one of them back, and the caller gets the 503 with Retry-After that {@code
   * TenantController#handleLockContention} answers with — the same shape the sibling {@code
   * findLockedByRequestKey} already has.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select p from TenantLegalProposalEntity p where p.sourceDraftId = :sourceDraftId and p.sourceDraftVersion = :sourceDraftVersion and p.recipientTenantId in :recipientTenantIds")
  List<TenantLegalProposalEntity>
      findLockedBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
          @Param("sourceDraftId") Long sourceDraftId,
          @Param("sourceDraftVersion") Long sourceDraftVersion,
          @Param("recipientTenantIds") Collection<Long> recipientTenantIds);

  List<TenantLegalProposalEntity> findByRecipientTenantIdOrderByCreatedAtDescIdDesc(Long tenantId);

  /** Every proposal of one source revision carries the same snapshot; any one of them shows it. */
  Optional<TenantLegalProposalEntity> findFirstBySourceDraftIdAndSourceDraftVersionOrderByIdAsc(
      Long sourceDraftId, Long sourceDraftVersion);

  List<TenantLegalProposalEntity> findByRecipientTenantIdAndKindOrderByCreatedAtDescIdDesc(
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
