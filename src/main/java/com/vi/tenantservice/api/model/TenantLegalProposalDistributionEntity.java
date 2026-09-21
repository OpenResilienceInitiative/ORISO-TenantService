package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "tenant_legal_proposal_distribution",
    uniqueConstraints = @UniqueConstraint(columnNames = "request_key"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalProposalDistributionEntity {
  @Id
  @Column(length = 36, updatable = false)
  private String id;

  @Column(name = "request_key", nullable = false, updatable = false, length = 128)
  private String requestKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private TenantLegalDraftKind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private TenantLegalProposalAudience audience;

  @Column(name = "source_draft_id", nullable = false, updatable = false)
  private Long sourceDraftId;

  @Column(name = "source_draft_version", nullable = false, updatable = false)
  private Long sourceDraftVersion;

  @Column(name = "source_updated_at", nullable = false, updatable = false)
  private LocalDateTime sourceUpdatedAt;

  @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 64)
  private String requestFingerprint;

  /** Immutable original audience, retained even when a recipient tenant is later deleted. */
  @Column(
      name = "recipient_ids",
      nullable = false,
      updatable = false,
      columnDefinition = "LONGTEXT")
  private String recipientIds;

  @Column(name = "created_by", nullable = false, updatable = false)
  private String createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
