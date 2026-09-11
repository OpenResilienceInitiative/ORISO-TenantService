package com.vi.tenantservice.api.model;

import java.time.LocalDateTime;

public interface TenantRestrictedData {

  Long getId();

  String getName();

  String getSubdomain();

  String getThemingLogo();

  String getThemingAssociationLogo();

  String getThemingFavicon();

  String getThemingPrimaryColor();

  String getThemingSecondaryColor();

  String getThemingAccent();

  String getThemingSignal();

  String getThemingLoginEffect();

  String getContentImpressum();

  String getContentClaim();

  String getContentPrivacy();

  LocalDateTime getContentPrivacyActivationDate();

  String getContentTermsAndConditions();

  LocalDateTime getContentTermsAndConditionsActivationDate();

  String getSettings();
}
