package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Platform-level DPIA operator master data singleton (ORISO-Admin#735). One row, maintained by the
 * platform super admin, publicly readable through /tenant/public/dpia.
 *
 * <p>Deliberately typed columns instead of a JSON blob: the tenant settings JSON has already proven
 * that untyped blobs invite secrets and silent drift. Nothing in this table is secret and nothing
 * secret must ever be added to it.
 */
@Entity
@Table(name = "platform_dpia_master_data")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlatformDpiaMasterDataEntity {

  @Id
  @SequenceGenerator(
      name = "platform_dpia_master_data_id_seq",
      allocationSize = 1,
      sequenceName = "SEQUENCE_PLATFORM_DPIA_MASTER_DATA")
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "platform_dpia_master_data_id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "operator_legal_name")
  private String operatorLegalName;

  @Column(name = "operator_short_name")
  private String operatorShortName;

  @Column(name = "operator_address", length = 512)
  private String operatorAddress;

  @Column(name = "operator_contact_email")
  private String operatorContactEmail;

  @Column(name = "operator_contact_phone", length = 64)
  private String operatorContactPhone;

  @Column(name = "operator_dpo_name")
  private String operatorDpoName;

  @Column(name = "operator_department")
  private String operatorDepartment;

  @Column(name = "operator_responsible_person")
  private String operatorResponsiblePerson;

  /** KDG or GDPR — mirrors DpiaSupervisoryAuthorityDTO.LegalFrameworkEnum. */
  @Column(name = "supervisory_legal_framework", length = 16)
  private String supervisoryLegalFramework;

  @Column(name = "supervisory_authority_name")
  private String supervisoryAuthorityName;

  @Column(name = "supervisory_authority_address", length = 512)
  private String supervisoryAuthorityAddress;

  @Column(name = "supervisory_authority_email")
  private String supervisoryAuthorityEmail;

  @Column(name = "document_date")
  private LocalDate documentDate;

  @Column(name = "document_next_review_date")
  private LocalDate documentNextReviewDate;

  @Column(name = "key_figure_tenants")
  private Long keyFigureTenants;

  @Column(name = "key_figure_tenants_as_of")
  private LocalDate keyFigureTenantsAsOf;

  @Column(name = "key_figure_counselling_centres")
  private Long keyFigureCounsellingCentres;

  @Column(name = "key_figure_counselling_centres_as_of")
  private LocalDate keyFigureCounsellingCentresAsOf;

  @Column(name = "key_figure_active_counsellors")
  private Long keyFigureActiveCounsellors;

  @Column(name = "key_figure_active_counsellors_as_of")
  private LocalDate keyFigureActiveCounsellorsAsOf;

  @Column(name = "key_figure_registered_clients")
  private Long keyFigureRegisteredClients;

  @Column(name = "key_figure_registered_clients_as_of")
  private LocalDate keyFigureRegisteredClientsAsOf;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;
}
