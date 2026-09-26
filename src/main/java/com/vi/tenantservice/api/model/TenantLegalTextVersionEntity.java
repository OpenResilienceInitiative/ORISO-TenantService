package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * One published wording of a Träger's (or, for tenant 0, the platform's) imprint or privacy policy
 * (ORISO-Admin#270). Append-only: a newer publish only closes this row via {@link #supersededAt}.
 * Identity is the surrogate id, never the timestamp.
 */
@Entity
@Table(name = "tenant_legal_text_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalTextVersionEntity {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "tenant_legal_text_version_id_seq")
  @SequenceGenerator(
      name = "tenant_legal_text_version_id_seq",
      sequenceName = "SEQUENCE_TENANT_LEGAL_TEXT_VERSION",
      allocationSize = 1)
  private Long id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private TenantLegalDraftKind kind;

  /** The language→HTML map as stored on the tenant, verbatim. */
  @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
  private String content;

  @Column(name = "published_at", nullable = false, updatable = false)
  private LocalDateTime publishedAt;

  /** Keycloak user id of the publisher; null when no user id was known. */
  @Column(name = "published_by", updatable = false)
  private String publishedBy;

  @Column(name = "superseded_at")
  private LocalDateTime supersededAt;
}
