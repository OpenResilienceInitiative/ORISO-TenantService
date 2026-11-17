package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.converter.EffectivePermissionSettingsApplier;
import com.vi.tenantservice.api.policy.PermissionFeature;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * UserService#1662: which guest account modes (delete after the session, guest chooses, permanent)
 * a counsellor may offer on a direct or share link, per conversation type.
 */
class GuestAccountModePermissionSettingsTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "featureGuestAccountTemporaryChatEnabled",
        "featureGuestAccountGuestChoosesChatEnabled",
        "featureGuestAccountPermanentChatEnabled",
        "featureGuestAccountTemporaryAudioEnabled",
        "featureGuestAccountGuestChoosesAudioEnabled",
        "featureGuestAccountPermanentAudioEnabled",
        "featureGuestAccountTemporaryVideoEnabled",
        "featureGuestAccountGuestChoosesVideoEnabled",
        "featureGuestAccountPermanentVideoEnabled"
      })
  void everyModeStaysAllowedUntilAnAdminRestrictsIt(String feature) {
    var settings = new TenantSettings().applyDefaults();
    Map<String, Object> publicSettings =
        new ObjectMapper().convertValue(settings, new TypeReference<>() {});
    assertThat(publicSettings.get(feature)).isEqualTo(true);
    assertThat(PermissionFeature.byApiKey(feature))
        .hasValueSatisfying(
            registered -> {
              assertThat(registered.defaultValue()).isTrue();
              assertThat(registered.legacyToggleKey()).isNull();
            });
  }

  @Test
  void enforcedAndTraegerPoliciesDecideEachModeSeparately() {
    var settings =
        new Settings()
            .featureGuestAccountTemporaryChatEnabled(true)
            .featureGuestAccountGuestChoosesAudioEnabled(true)
            .featureGuestAccountPermanentVideoEnabled(false);
    new EffectivePermissionSettingsApplier()
        .applyPolicies(
            settings,
            Map.of(
                // platform force-deny
                "featureGuestAccountTemporaryChatEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
                // Träger's own choice
                "featureGuestAccountGuestChoosesAudioEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED).inherited(false),
                // platform force-allow
                "featureGuestAccountPermanentVideoEnabled",
                new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED)));
    assertThat(settings.getFeatureGuestAccountTemporaryChatEnabled()).isFalse();
    assertThat(settings.getFeatureGuestAccountGuestChoosesAudioEnabled()).isFalse();
    assertThat(settings.getFeatureGuestAccountPermanentVideoEnabled()).isTrue();
  }
}
