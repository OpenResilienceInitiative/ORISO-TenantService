package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "tenant_legal_proposal",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"source_draft_id", "source_draft_version", "recipient_tenant_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalProposalEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_legal_proposal_id_seq")
  @SequenceGenerator(
      name = "tenant_legal_proposal_id_seq",
      sequenceName = "SEQUENCE_TENANT_LEGAL_PROPOSAL",
      allocationSize = 1)
  private Long id;

  @Version private Long version;

  @Column(name = "recipient_tenant_id", nullable = false, updatable = false)
  private Long recipientTenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private TenantLegalDraftKind kind;

  @Column(name = "distribution_id", nullable = false, updatable = false, length = 36)
  private String distributionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private TenantLegalProposalAudience audience;

  @Column(name = "source_draft_id", nullable = false, updatable = false)
  private Long sourceDraftId;

  @Column(name = "source_draft_version", nullable = false, updatable = false)
  private Long sourceDraftVersion;

  @Column(name = "source_updated_at", nullable = false, updatable = false)
  private LocalDateTime sourceUpdatedAt;

  @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "privacy_consent", updatable = false, columnDefinition = "TEXT")
  private String privacyConsent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TenantLegalProposalStatus status;

  @Column(name = "created_by", nullable = false, updatable = false)
  private String createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "decided_by")
  private String decidedBy;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(name = "superseded_by_proposal_id")
  private Long supersededByProposalId;

  @Column(name = "superseded_at")
  private LocalDateTime supersededAt;
}
