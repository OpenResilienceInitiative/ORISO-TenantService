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

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "privacy_consent", columnDefinition = "TEXT")
  private String privacyConsent;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;
}
