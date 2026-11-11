package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "tenant_legal_draft",
    uniqueConstraints = @UniqueConstraint(columnNames = {"owner_key", "kind"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalDraftEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_legal_draft_id_seq")
  @SequenceGenerator(
      name = "tenant_legal_draft_id_seq",
      sequenceName = "SEQUENCE_TENANT_LEGAL_DRAFT",
      allocationSize = 1)
  private Long id;

  @Version private Long version;

  /** API owner id: {@code 0} for the platform sentinel, otherwise the real tenant id. */
  @Column(name = "owner_key", nullable = false, updatable = false)
  private Long ownerKey;

  /** Real tenant FK. Platform-owned drafts deliberately store {@code null}. */
  @Column(name = "tenant_id", updatable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TenantLegalDraftKind kind;

  // LONGTEXT like the published `content_privacy` / `content_impressum`: a policy
  // that publishes must also be storable as a draft, and a multilingual HTML map
  // passes MariaDB's 64 KB TEXT limit long before the API refuses it.
  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String content;

  @Column(name = "privacy_consent", columnDefinition = "LONGTEXT")
  private String privacyConsent;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;

  @Column(name = "origin_proposal_id")
  private Long originProposalId;

  @Column(name = "origin_distribution_id", length = 36)
  private String originDistributionId;

  @Column(name = "origin_source_revision", length = 64)
  private String originSourceRevision;

  @Column(name = "origin_source_updated_at")
  private LocalDateTime originSourceUpdatedAt;

  @Column(name = "origin_shared_by")
  private String originSharedBy;
}
