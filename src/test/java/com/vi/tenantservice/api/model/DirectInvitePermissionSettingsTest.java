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

/** TS#331 / ADR-029 §6: "Direkteinladung erlauben" per conversation type. */
class DirectInvitePermissionSettingsTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "featureDirectInviteChatEnabled",
        "featureDirectInviteAudioEnabled",
        "featureDirectInviteVideoEnabled"
      })
  void existingTenantsAllowDirectInvitesUntilAnAdminSwitchesThemOff(String feature) {
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
  void registryNamesTheThreeChatTypes() {
    assertThat(PermissionFeature.DIRECT_INVITE_CHAT.apiKey())
        .isEqualTo("featureDirectInviteChatEnabled");
    assertThat(PermissionFeature.DIRECT_INVITE_AUDIO.apiKey())
        .isEqualTo("featureDirectInviteAudioEnabled");
    assertThat(PermissionFeature.DIRECT_INVITE_VIDEO.apiKey())
        .isEqualTo("featureDirectInviteVideoEnabled");
  }

  @Test
  void enforcedAndTraegerPoliciesDecideEachChatTypeSeparately() {
    var settings =
        new Settings()
            .featureDirectInviteChatEnabled(true)
            .featureDirectInviteAudioEnabled(true)
            .featureDirectInviteVideoEnabled(false);
    new EffectivePermissionSettingsApplier()
        .applyPolicies(
            settings,
            Map.of(
                // platform force-deny
                "featureDirectInviteChatEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
                // Träger's own choice
                "featureDirectInviteAudioEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED).inherited(false),
                // platform force-allow
                "featureDirectInviteVideoEnabled",
                new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED)));
    assertThat(settings.getFeatureDirectInviteChatEnabled()).isFalse();
    assertThat(settings.getFeatureDirectInviteAudioEnabled()).isFalse();
    assertThat(settings.getFeatureDirectInviteVideoEnabled()).isTrue();
  }

  @Test
  void anInheritedSuggestionIsOnlyAHint() {
    var settings = new Settings().featureDirectInviteChatEnabled(true);
    new EffectivePermissionSettingsApplier()
        .applyPolicies(
            settings,
            Map.of(
                "featureDirectInviteChatEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED)
                    .inherited(true)));
    assertThat(settings.getFeatureDirectInviteChatEnabled()).isTrue();
  }
}
