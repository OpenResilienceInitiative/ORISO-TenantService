package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * An immutable snapshot of a published DPA version. One row is written every time a tenant
 * publishes its DPA, so the admin UI can offer a lightweight "look back" at earlier wordings (the
 * tenant table only keeps the current content). Intentionally simple: append-only, never edited.
 */
@Entity
@Table(name = "tenant_dpa_version")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TenantDpaVersionEntity {

  @Id
  @SequenceGenerator(
      name = "dpa_version_id_seq",
      allocationSize = 1,
      sequenceName = "SEQUENCE_TENANT_DPA_VERSION")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dpa_version_id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  /** The published multilingual content (JSON map language -> HTML), as stored at that version. */
  @Column(name = "content")
  private String content;

  /**
   * The activation timestamp that identifies this version (matches the tenant's at publish time).
   */
  @Column(name = "activation_date", nullable = false)
  private LocalDateTime activationDate;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @PrePersist
  void applyDefaults() {
    if (createDate == null) {
      createDate = LocalDateTime.now();
    }
  }
}
