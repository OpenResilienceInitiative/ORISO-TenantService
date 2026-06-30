package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.type.NumericBooleanConverter;

/**
 * Records a tenant's confirmation ("signature") of a Data Processing Agreement
 * (Auftragsverarbeitungsvertrag). The platform stores the confirmation as a record only — there is
 * no PDF and no real e-signature; a checkbox plus signer details is the legally sufficient artefact
 * (KDG §29 / DSG-EKD Textform) as long as a real contract stands behind it. The signer may be a
 * non-member (e.g. a CEO) confirming via a forwarded, single-use invite link.
 */
@Entity
@Table(name = "tenant_dpa_signature")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TenantDpaSignatureEntity {

  @Id
  @SequenceGenerator(
      name = "dpa_signature_id_seq",
      allocationSize = 1,
      sequenceName = "SEQUENCE_TENANT_DPA_SIGNATURE")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dpa_signature_id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  /**
   * The version of the DPA that was confirmed — the tenant's {@code dpa_activation_date} at the
   * moment of confirmation. Lets the platform tell which contract version a signer agreed to.
   */
  @Column(name = "dpa_version")
  private LocalDateTime dpaVersion;

  @Column(name = "signer_name")
  private String signerName;

  @Column(name = "signer_position")
  private String signerPosition;

  /**
   * Whether the signer is a platform member. {@code false} = an external person (e.g. a CEO) who
   * confirmed via a forwarded link without holding a platform account.
   */
  @Column(name = "signer_is_member")
  @Convert(converter = NumericBooleanConverter.class)
  private Boolean signerIsMember;

  /** Language the DPA was presented/confirmed in (e.g. {@code "de"}, {@code "en"}). */
  @Column(name = "lang")
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(name = "signature_status", nullable = false)
  private DpaSignatureStatus status;

  @Column(name = "signed_at")
  private LocalDateTime signedAt;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  /** Defends the NOT NULL columns regardless of how the entity was constructed. */
  @PrePersist
  void applyDefaults() {
    if (createDate == null) {
      createDate = LocalDateTime.now();
    }
    if (status == null) {
      status = DpaSignatureStatus.PENDING;
    }
  }
}
