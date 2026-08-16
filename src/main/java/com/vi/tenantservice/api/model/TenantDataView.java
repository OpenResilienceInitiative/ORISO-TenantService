package com.vi.tenantservice.api.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TenantDataView implements TenantData {

  private final Long id;
  private final String name;
  private final String subdomain;
  private final String address;
  private final String description;
  private final Integer licensingAllowedNumberOfUsers;
  private final String themingLogo;
  private final String themingAssociationLogo;
  private final String themingFavicon;
  private final String themingPrimaryColor;
  private final String themingSecondaryColor;
  private final String themingAccent;
  private final String themingSignal;
  private final String themingLoginEffect;
  private final String contentImpressum;
  private final String contentClaim;
  private final String contentPrivacy;
  private final LocalDateTime contentPrivacyActivationDate;
  private final String contentTermsAndConditions;
  private final LocalDateTime contentTermsAndConditionsActivationDate;
  /* ORISO-Admin#601 — selected so the Admin editor reads back what it wrote.
  Order matters: this is a JPQL constructor projection, so these must stay in
  lockstep with the SELECT lists in TenantRepository. */
  private final String contentErstantwortGreeting;
  private final String contentErstantwortWhoReadsAlong;
  private final String contentErstantwortEmergencyAddition;
  private final String contentErstantwortFreeNotice;
  private final String contentErstantwortClosing;
  private final Integer erstantwortResponseDeadlineDays;
  private final String settings;
  private final LocalDateTime createDate;
  private final LocalDateTime updateDate;
}
