package com.vi.tenantservice.api.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TenantRestrictedDataView implements TenantRestrictedData {

  private final Long id;
  private final String name;
  private final String subdomain;
  private final String themingLogo;
  private final String themingAssociationLogo;
  private final String themingFavicon;
  private final String themingPrimaryColor;
  private final String themingSecondaryColor;
  private final String themingAccent;
  private final String themingSignal;
  private final String contentImpressum;
  private final String contentClaim;
  private final String contentPrivacy;
  private final LocalDateTime contentPrivacyActivationDate;
  private final String contentTermsAndConditions;
  private final LocalDateTime contentTermsAndConditionsActivationDate;
  private final String settings;
}
