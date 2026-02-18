package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenant")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TenantEntity {

  @Id
  @SequenceGenerator(name = "id_seq", allocationSize = 1, sequenceName = "SEQUENCE_TENANT")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "subdomain", nullable = false)
  private String subdomain;

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

  @Column(name = "settings")
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
