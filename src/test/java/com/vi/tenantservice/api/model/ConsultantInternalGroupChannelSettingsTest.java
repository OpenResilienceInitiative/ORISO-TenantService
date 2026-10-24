package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.converter.EffectivePermissionSettingsApplier;
import com.vi.tenantservice.api.policy.PermissionFeature;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FE#1725 story 40: counsellor notification channels for internal group chats. */
class ConsultantInternalGroupChannelSettingsTest {

  @Test
  void legacyTenantsKeepBothInternalGroupChannelsOn() {
    var settings = new TenantSettings().applyDefaults();
    Map<String, Object> publicSettings =
        new ObjectMapper().convertValue(settings, new TypeReference<>() {});
    assertThat(publicSettings.get("featureConsultantEmailInternalGroupEnabled")).isEqualTo(true);
    assertThat(publicSettings.get("featureConsultantBrowserInternalGroupEnabled")).isEqualTo(true);
  }

  @Test
  void registryKnowsBothInternalGroupChannelsDefaultingOn() {
    assertThat(PermissionFeature.byApiKey("featureConsultantEmailInternalGroupEnabled"))
        .hasValueSatisfying(feature -> assertThat(feature.defaultValue()).isTrue());
    assertThat(PermissionFeature.byApiKey("featureConsultantBrowserInternalGroupEnabled"))
        .hasValueSatisfying(feature -> assertThat(feature.defaultValue()).isTrue());
  }

  @Test
  void enforcedPoliciesReachThePublicSettings() {
    var settings =
        new Settings()
            .featureConsultantEmailInternalGroupEnabled(true)
            .featureConsultantBrowserInternalGroupEnabled(true);
    new EffectivePermissionSettingsApplier()
        .applyPolicies(
            settings,
            Map.of(
                "featureConsultantEmailInternalGroupEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
                "featureConsultantBrowserInternalGroupEnabled",
                new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED)));
    assertThat(settings.getFeatureConsultantEmailInternalGroupEnabled()).isFalse();
    assertThat(settings.getFeatureConsultantBrowserInternalGroupEnabled()).isTrue();
  }
}
