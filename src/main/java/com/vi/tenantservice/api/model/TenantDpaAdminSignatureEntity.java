package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Audit-proof record of an authenticated tenant admin signing the tenant's currently published Data
 * Processing Agreement version (TEN-INV-U9, ORISO-TenantService#144).
 *
 * <p>The table is append-only: rows are only ever inserted, never updated or deleted by the
 * application (the repository interface exposes no update/delete operations and the entity has no
 * setters). Each row is a self-contained, revision-safe snapshot — signer identity from the access
 * token, the exact DPA version confirmed, the signing timestamp, and the submitted form data as a
 * verbatim JSON snapshot.
 */
@Entity
@Table(
    name = "tenant_dpa_admin_signature",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_tenant_dpa_admin_signature_version",
            columnNames = {"tenant_id", "dpa_version"}))
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantDpaAdminSignatureEntity {

  @Id
  @SequenceGenerator(
      name = "dpa_admin_signature_id_seq",
      allocationSize = 1,
      sequenceName = "SEQUENCE_TENANT_DPA_ADMIN_SIGNATURE")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dpa_admin_signature_id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private Long tenantId;

  /** The published DPA version (activation date) that was confirmed. */
  @Column(name = "dpa_version", nullable = false, updatable = false)
  private LocalDateTime dpaVersion;

  /** Keycloak user id (subject) of the authenticated tenant admin who signed. */
  @Column(name = "signer_user_id", nullable = false, updatable = false)
  private String signerUserId;

  /** Username of the authenticated tenant admin at signing time. */
  @Column(name = "signer_username", updatable = false)
  private String signerUsername;

  @Column(name = "signer_name", updatable = false)
  private String signerName;

  @Column(name = "signer_position", updatable = false)
  private String signerPosition;

  @Column(name = "signer_email", updatable = false)
  private String signerEmail;

  @Column(name = "signer_organisation", updatable = false)
  private String signerOrganisation;

  /** Language the DPA was presented/confirmed in (e.g. {@code "de"}). */
  @Column(name = "lang", updatable = false)
  private String language;

  /** Verbatim JSON snapshot of the submitted sign form (revision-safe audit evidence). */
  @Column(name = "form_data", nullable = false, updatable = false, columnDefinition = "text")
  private String formData;

  @Column(name = "signed_at", nullable = false, updatable = false)
  private LocalDateTime signedAt;

  @Column(name = "create_date", nullable = false, updatable = false)
  private LocalDateTime createDate;

  /** Defends the NOT NULL timestamps regardless of how the entity was constructed. */
  @PrePersist
  void applyDefaults() {
    var now = LocalDateTime.now();
    if (signedAt == null) {
      signedAt = now;
    }
    if (createDate == null) {
      createDate = now;
    }
  }
}
