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

  @Test
  void publicChannelSettingsRespectIndependentInheritedPolicies() {
    var settings =
        new Settings()
            .featureAskerEmailAgencyCounsellingEnabled(true)
            .featureAskerBrowserAgencyCounsellingEnabled(true)
            .featureAskerEmailLiveChatEnabled(false);
    applier.applyPolicies(
        settings,
        Map.of(
            "featureAskerEmailAgencyCounsellingEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
            "featureAskerBrowserAgencyCounsellingEnabled",
                new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED).inherited(true),
            "featureAskerEmailLiveChatEnabled",
                new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED)));
    assertThat(settings.getFeatureAskerEmailAgencyCounsellingEnabled()).isFalse();
    assertThat(settings.getFeatureAskerBrowserAgencyCounsellingEnabled()).isTrue();
    assertThat(settings.getFeatureAskerEmailLiveChatEnabled()).isTrue();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "featureAskerEmailCollectionAgencyCounsellingEnabled",
        "featureDisplayNameEditableLiveChatEnabled",
        "featureAskerEmailCollectionSelfHelpEnabled"
      })
  void projectsAnInheritedCollectionRequirementWithoutChangingDelivery(String key) {
    var settings =
        new Settings().featureAskerEmailEnabled(true).featureAskerEmailLiveChatEnabled(false);
    applier.applyPolicies(
        settings,
        Map.of(
            key,
            new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED).inherited(true)));
    var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(settings);
    assertThat(json.has(key)).isTrue();
    assertThat(json.path(key).asBoolean()).isFalse();
    assertThat(settings.getFeatureAskerEmailEnabled()).isTrue();
    assertThat(settings.getFeatureAskerEmailLiveChatEnabled()).isFalse();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "featureAskerEmailCollectionAgencyCounsellingEnabled,true",
    "featureAskerEmailCollectionAgencyCounsellingEnabled,false",
    "featureDisplayNameEditableLiveChatEnabled,true",
    "featureDisplayNameEditableLiveChatEnabled,false",
    "featureAskerEmailCollectionSelfHelpEnabled,true",
    "featureAskerEmailCollectionSelfHelpEnabled,false"
  })
  void newCollectionDefaultsFollowTheInheritedSuggestionWithoutMakingEmailRequired(
      String key, boolean allowed) {
    var settings =
        new Settings()
            .featureAskerEmailEnabled(true)
            .featureAskerEmailLiveChatEnabled(false)
            .emailVisible(true)
            .emailRequired(false);
    var inheritedSuggestion =
        new BooleanPermissionPolicy(allowed, PermissionPolicyMode.SUGGESTED).inherited(true);
    applier.applyPolicies(settings, Map.of(key, inheritedSuggestion));
    var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(settings);
    assertThat(json.path(key).asBoolean()).as(key).isEqualTo(allowed);
    assertThat(inheritedSuggestion.getMode()).isEqualTo(PermissionPolicyMode.SUGGESTED);
    assertThat(inheritedSuggestion.getInherited()).isTrue();
    assertThat(settings.getEmailRequired()).isFalse();
    assertThat(settings.getEmailVisible()).isTrue();
    assertThat(settings.getFeatureAskerEmailLiveChatEnabled()).isFalse();
  }

  @Test
  void applyPolicies_should_governTeamDiscussionLikeSupervision() {
    // ORISO-Frontend#2491: a platform requirement wins, an inherited suggestion is only a hint.
    var settings =
        new Settings()
            .featureTeamDiscussionEnabled(true)
            .featureVideoCallsTeamDiscussionsEnabled(false)
            .featureMediaAiScanTeamDiscussionsEnabled(false);

    applier.applyPolicies(
        settings,
        Map.of(
            "featureTeamDiscussionEnabled",
            new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED).inherited(true),
            "featureVideoCallsTeamDiscussionsEnabled",
            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED).inherited(true),
            "featureMediaAiScanTeamDiscussionsEnabled",
            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED).inherited(false)));

    assertThat(settings.getFeatureTeamDiscussionEnabled()).isFalse();
    assertThat(settings.getFeatureVideoCallsTeamDiscussionsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaAiScanTeamDiscussionsEnabled()).isTrue();
  }

  @Test
  void applyTo_should_forceTeamDiscussionTogglesLikeSupervision() {
    var settings =
        new Settings()
            .featureTeamDiscussionEnabled(true)
            .featureThreadsTeamDiscussionsEnabled(false);
    var controls =
        new TenantAdminControls()
            .allowedPermissionToggles(
                new TenantAdminAllowedPermissionToggles().teamDiscussion(false))
            .enforcedPermissionToggles(
                new TenantAdminAllowedPermissionToggles().threadsTeamDiscussions(true));

    applier.applyTo(settings, controls);

    assertThat(settings.getFeatureTeamDiscussionEnabled()).isFalse();
    assertThat(settings.getFeatureThreadsTeamDiscussionsEnabled()).isTrue();
  }

  private static final String[][] TEAM_DISCUSSION_MAPPINGS = {
    {"featureTeamDiscussionEnabled", "teamDiscussion"},
    {"featureVideoCallsTeamDiscussionsEnabled", "videoCallsTeamDiscussions"},
    {"featureAudioCallsTeamDiscussionsEnabled", "audioCallsTeamDiscussions"},
    {"featureVoiceMessagesTeamDiscussionsEnabled", "voiceMessagesTeamDiscussions"},
    {"featureThreadsTeamDiscussionsEnabled", "threadsTeamDiscussions"},
    {"featureMediaUploadTeamDiscussionsEnabled", "mediaUploadTeamDiscussions"},
    {"featureMediaInlineDisplayTeamDiscussionsEnabled", "mediaInlineDisplayTeamDiscussions"},
    {"featureMediaAiScanTeamDiscussionsEnabled", "mediaAiScanTeamDiscussions"},
    {"featureTeamDiscussionAfterAcceptEnabled", "teamDiscussionAfterAccept"}
  };

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments>
      teamDiscussionMappings() {
    return java.util.Arrays.stream(TEAM_DISCUSSION_MAPPINGS)
        .flatMap(
            m ->
                java.util.stream.Stream.of(true, false)
                    .map(v -> org.junit.jupiter.params.provider.Arguments.of(m[0], m[1], v)));
  }

  private static boolean settingValue(Settings settings, String key) {
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .valueToTree(settings)
        .path(key)
        .asBoolean();
  }

  private static Settings settingsWith(String key, boolean value) {
    var node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put(key, value);
    return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(node, Settings.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("teamDiscussionMappings")
  void applyPolicies_should_mapEveryTeamDiscussionPolicy(String key, String toggle, boolean value) {
    // enforced value wins over the opposite stored value
    var enforced = settingsWith(key, !value);
    applier.applyPolicies(
        enforced,
        Map.of(
            key,
            new BooleanPermissionPolicy(value, PermissionPolicyMode.ENFORCED).inherited(true)));
    assertThat(settingValue(enforced, key)).as(key + " enforced").isEqualTo(value);

    // a local suggestion is the own value
    var local = settingsWith(key, !value);
    applier.applyPolicies(
        local,
        Map.of(
            key,
            new BooleanPermissionPolicy(value, PermissionPolicyMode.SUGGESTED).inherited(false)));
    assertThat(settingValue(local, key)).as(key + " local").isEqualTo(value);

    // an inherited suggestion is only a hint: the stored value stays
    var inherited = settingsWith(key, !value);
    applier.applyPolicies(
        inherited,
        Map.of(
            key,
            new BooleanPermissionPolicy(value, PermissionPolicyMode.SUGGESTED).inherited(true)));
    assertThat(settingValue(inherited, key)).as(key + " inherited").isEqualTo(!value);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("teamDiscussionMappings")
  void applyTo_should_mapEveryTeamDiscussionControl(String key, String toggle, boolean value) {
    var settings = settingsWith(key, !value);
    var toggles =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .convertValue(
                new com.fasterxml.jackson.databind.ObjectMapper()
                    .createObjectNode()
                    .put(toggle, value),
                TenantAdminAllowedPermissionToggles.class);
    var controls =
        value
            ? new TenantAdminControls().enforcedPermissionToggles(toggles)
            : new TenantAdminControls().allowedPermissionToggles(toggles);

    applier.applyTo(settings, controls);

    assertThat(settingValue(settings, key)).as(key + " control").isEqualTo(value);
  }
}
