package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalProposalAudience;
import com.vi.tenantservice.api.model.TenantLegalProposalDistributionEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantLegalProposalDistributionRepository
    extends JpaRepository<TenantLegalProposalDistributionEntity, String> {
  Optional<TenantLegalProposalDistributionEntity> findByRequestKey(String requestKey);

  /**
   * created_at is second-precision TIMESTAMP on MariaDB and the id a random UUID, so two sends in
   * one second are ordered by the draft version they sent — the later revision is the newer one.
   */
  List<TenantLegalProposalDistributionEntity>
      findByKindOrderByCreatedAtDescSourceDraftVersionDescIdDesc(TenantLegalDraftKind kind);

  /** The template a Träger created after the send should still receive (ORISO-Admin#1070). */
  Optional<TenantLegalProposalDistributionEntity>
      findFirstByKindAndAudienceOrderByCreatedAtDescSourceDraftVersionDescIdDesc(
          TenantLegalDraftKind kind, TenantLegalProposalAudience audience);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from TenantLegalProposalDistributionEntity d where d.requestKey = :requestKey")
  Optional<TenantLegalProposalDistributionEntity> findLockedByRequestKey(
      @Param("requestKey") String requestKey);
}
