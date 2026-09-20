package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "tenant_legal_draft_archive")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalDraftArchiveEntity {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "tenant_legal_draft_archive_id_seq")
  @SequenceGenerator(
      name = "tenant_legal_draft_archive_id_seq",
      sequenceName = "SEQUENCE_TENANT_LEGAL_DRAFT_ARCHIVE",
      allocationSize = 1)
  private Long id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private TenantLegalDraftKind kind;

  @Column(name = "draft_row_id", nullable = false, updatable = false)
  private Long draftRowId;

  @Column(name = "draft_revision", nullable = false, updatable = false, length = 64)
  private String draftRevision;

  @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "privacy_consent", updatable = false, columnDefinition = "TEXT")
  private String privacyConsent;

  @Column(name = "draft_saved_at", nullable = false, updatable = false)
  private LocalDateTime draftSavedAt;

  @Column(name = "origin_proposal_id", updatable = false)
  private Long originProposalId;

  @Column(name = "origin_distribution_id", updatable = false, length = 36)
  private String originDistributionId;

  @Column(name = "origin_source_revision", updatable = false, length = 64)
  private String originSourceRevision;

  @Column(name = "origin_source_updated_at", updatable = false)
  private LocalDateTime originSourceUpdatedAt;

  @Column(name = "origin_shared_by", updatable = false)
  private String originSharedBy;

  @Column(name = "archived_by", nullable = false, updatable = false)
  private String archivedBy;

  @Column(name = "archived_at", nullable = false, updatable = false)
  private LocalDateTime archivedAt;
}
