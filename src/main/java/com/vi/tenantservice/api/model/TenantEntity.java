package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "tenant")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TenantEntity implements TenantData {

  @Id
  @GeneratedValue(generator = "id_seq")
  @GenericGenerator(
      name = "id_seq",
      type = AssignedOrSequenceIdGenerator.class,
      parameters = {
        @Parameter(name = "sequence_name", value = "SEQUENCE_TENANT"),
        @Parameter(name = "increment_size", value = "1")
      })
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "subdomain", nullable = false)
  private String subdomain;

  @Column(name = "address")
  private String address;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "licensing_allowed_users")
  private Integer licensingAllowedNumberOfUsers;

  @Column(name = "theming_logo")
  private String themingLogo;

  @Column(name = "theming_association_logo")
  private String themingAssociationLogo;

  @Column(name = "theming_favicon")
  private String themingFavicon;

  @Column(name = "theming_primary_color")
  private String themingPrimaryColor;

  @Column(name = "theming_secondary_color")
  private String themingSecondaryColor;

  /** Light accent ("Helle Akzentfarbe"), the counterpart of {@link #themingPrimaryColor}. */
  @Column(name = "theming_accent")
  private String themingAccent;

  /** Optional signal/attention colour of the tenant seed set. */
  @Column(name = "theming_signal")
  private String themingSignal;

  /**
   * Which decorative effect the login stage runs; {@code null} means never configured and is read
   * as NONE. Stored as the enum name rather than an ordinal so the column stays readable and
   * reordering the enum cannot silently repoint existing tenants.
   */
  @Column(name = "theming_login_effect", length = 32)
  private String themingLoginEffect;

  @Column(name = "content_impressum")
  private String contentImpressum;

  @Column(name = "content_claim")
  private String contentClaim;

  @Column(name = "content_privacy")
  private String contentPrivacy;

  @Column(name = "privacy_activation_date")
  private LocalDateTime contentPrivacyActivationDate;

  @Column(name = "content_termsandconditions")
  private String contentTermsAndConditions;

  @Column(name = "termsandconditions_activation_date")
  private LocalDateTime contentTermsAndConditionsActivationDate;

  @Column(name = "content_dpa")
  private String contentDataProcessingAgreement;

  @Column(name = "dpa_activation_date")
  private LocalDateTime contentDataProcessingAgreementActivationDate;

  // TEXT on MariaDB since changeset 0022 (media flag families outgrew VARCHAR(4000));
  // the length here only sizes the generated H2 test schema.
  @Column(name = "settings", length = 65535)
  private String settings;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @Column(name = "update_date")
  private LocalDateTime updateDate;

  public interface TenantBase {

    Long getId();

    String getName();
  }
}
