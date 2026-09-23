package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EffectivePermissionSettingsApplierTest {

  private final EffectivePermissionSettingsApplier applier =
      new EffectivePermissionSettingsApplier();

  @Test
  void applyTo_should_forceFeatureOff_whenUpperRoleDisallowsIt() {
    var settings = new Settings().featureVideoCallsEnabled(true);
    var controls =
        new TenantAdminControls()
            .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles().videoCalls(false));

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureVideoCallsEnabled()).isFalse();
  }

  @Test
  void applyTo_should_forceFeatureOn_whenUpperRoleEnforcesIt() {
    var settings = new Settings().featureAudioCallsEnabled(false);
    var controls =
        new TenantAdminControls()
            .enforcedPermissionToggles(new TenantAdminAllowedPermissionToggles().audioCalls(true));

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureAudioCallsEnabled()).isTrue();
  }

  @Test
  void applyTo_should_leaveUnconstrainedFeatureUnchanged() {
    var settings = new Settings().featureThreadsEnabled(true);
    var controls =
        new TenantAdminControls()
            .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles());

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureThreadsEnabled()).isTrue();
  }

  @Test
  void applyTo_should_doNothing_whenControlsAreNull() {
    var settings = new Settings().featureVideoCallsEnabled(true);

    applier.applyTo(settings, null);

    assertThat(settings.getFeatureVideoCallsEnabled()).isTrue();
  }

  @Test
  void applyTo_should_mapTheIrregularOneOnOneThreadsField() {
    // threadsOneOnOneChats maps to the irregularly named featureThreadsOneOnOneEnabled field.
    var settings = new Settings().featureThreadsOneOnOneEnabled(true);
    var controls =
        new TenantAdminControls()
            .allowedPermissionToggles(
                new TenantAdminAllowedPermissionToggles().threadsOneOnOneChats(false));

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureThreadsOneOnOneEnabled()).isFalse();
  }

  @Test
  void applyTo_should_forceOnlySelfHelpGroupsOff_whenPlatformDisallowsSelfHelpGroups() {
    var settings =
        new Settings().featureInternalGroupChatEnabled(true).featureSelfHelpGroupsEnabled(true);
    var controls =
        new TenantAdminControls()
            .allowedPermissionToggles(
                new TenantAdminAllowedPermissionToggles().selfHelpGroups(false));

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureSelfHelpGroupsEnabled()).isFalse();
    assertThat(settings.getFeatureInternalGroupChatEnabled()).isTrue();
  }

  @Test
  void applyTo_should_forceOnlyInternalGroupChatOn_whenPlatformEnforcesInternalGroupChat() {
    var settings =
        new Settings().featureInternalGroupChatEnabled(false).featureSelfHelpGroupsEnabled(false);
    var controls =
        new TenantAdminControls()
            .enforcedPermissionToggles(
                new TenantAdminAllowedPermissionToggles().internalGroupChat(true));

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureInternalGroupChatEnabled()).isTrue();
    assertThat(settings.getFeatureSelfHelpGroupsEnabled()).isFalse();
  }

  @Test
  void applyPolicies_should_forceOnlyTheEnforcedGroupChatFormat() {
    var settings =
        new Settings().featureInternalGroupChatEnabled(true).featureSelfHelpGroupsEnabled(true);

    applier.applyPolicies(
        settings,
        Map.of(
            "featureSelfHelpGroupsEnabled",
            new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
            "featureInternalGroupChatEnabled",
            new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED)));

    assertThat(settings.getFeatureSelfHelpGroupsEnabled()).isFalse();
    assertThat(settings.getFeatureInternalGroupChatEnabled()).isTrue();
  }

  @Test
  void applyPolicies_shouldApplyEnforcedAndLeaveOriginlessSuggestionUnchanged() {
    var settings = new Settings().featureVideoCallsEnabled(true).featureAudioCallsEnabled(false);

    applier.applyPolicies(
        settings,
        Map.of(
            "featureVideoCallsEnabled",
            new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
            "featureAudioCallsEnabled",
            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED)));

    assertThat(settings.getFeatureVideoCallsEnabled()).isFalse();
    assertThat(settings.getFeatureAudioCallsEnabled()).isFalse();
  }

  @Test
  void applyPolicies_shouldApplyATenantLocalSuggestedValue() {
    var settings = new Settings().featureGroupChatV2Enabled(false);

    applier.applyPolicies(
        settings,
        Map.of(
            "featureGroupChatV2Enabled",
            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED).inherited(false)));

    assertThat(settings.getFeatureGroupChatV2Enabled()).isTrue();
  }

  @Test
  void applyPolicies_shouldLeaveAnInheritedSuggestedValueNonBinding() {
    var settings = new Settings().featureGroupChatV2Enabled(false);

    applier.applyPolicies(
        settings,
        Map.of(
            "featureGroupChatV2Enabled",
            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED).inherited(true)));

    assertThat(settings.getFeatureGroupChatV2Enabled()).isFalse();
  }
}
