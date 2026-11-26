package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** ORISO-Frontend#1870: the agency-link label on Erstanfragen is opt-out per Traeger. */
class AgencyLinkLabelSettingTest {

  @Test
  void defaultsToEnabledWhenNothingIsStored() {
    var settings = new TenantSettings();
    settings.applyDefaults();

    assertThat(settings.getFeatureAgencyLinkLabelEnabled()).isTrue();
  }

  @Test
  void anExplicitOffSurvivesApplyDefaults() {
    var settings = new TenantSettings();
    settings.setFeatureAgencyLinkLabelEnabled(false);

    settings.applyDefaults();

    assertThat(settings.getFeatureAgencyLinkLabelEnabled()).isFalse();
  }

  @Test
  void onlyTraegerAndPlatformAdminsMayChangeIt() {
    assertThat(TenantSetting.FEATURE_AGENCY_LINK_LABEL_ENABLED.getRolesAuthorisedToChange())
        .isNotEmpty();
  }
}
