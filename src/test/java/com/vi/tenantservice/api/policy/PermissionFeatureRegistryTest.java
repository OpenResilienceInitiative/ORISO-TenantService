package com.vi.tenantservice.api.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PermissionFeatureRegistryTest {

  @Test
  void registry_shouldCoverEveryLegacyPermissionToggleExactlyOnce() {
    Set<String> legacyFields =
        Arrays.stream(TenantAdminAllowedPermissionTogglesSettings.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(field -> field.getName())
            .collect(Collectors.toSet());
    Set<String> mappedLegacyFields =
        Arrays.stream(PermissionFeature.values())
            .map(PermissionFeature::legacyToggleKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    assertThat(mappedLegacyFields).containsExactlyInAnyOrderElementsOf(legacyFields);
    assertThat(
            Arrays.stream(PermissionFeature.values())
                .map(PermissionFeature::legacyToggleKey)
                .filter(Objects::nonNull))
        .doesNotHaveDuplicates();
  }

  @Test
  void registry_shouldIncludeAskerAndCaseHandoverFeaturesWithoutLegacyAliases() {
    assertThat(PermissionFeature.byApiKey("featureDisplayNameEditable")).isPresent();
    assertThat(PermissionFeature.byApiKey("featureAskerEmailEnabled")).isPresent();
    assertThat(PermissionFeature.byApiKey("caseHandoverEnabled")).isPresent();
    assertThat(PermissionFeature.byApiKey("caseHandoverTeamAccessOptOut")).isEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "featureAskerEmailCollectionAgencyCounsellingEnabled",
        "featureDisplayNameEditableAgencyCounsellingEnabled",
        "featureDisplayNameEditableLiveChatEnabled",
        "featureDisplayNameEditableSelfHelpEnabled",
        "featureAskerEmailCollectionSelfHelpEnabled"
      })
  void registryProvidesIndependentAccountPreferences(String key) {
    var collection = PermissionFeature.byApiKey(key);
    assertThat(collection).isPresent();
    assertThat(collection.orElseThrow().defaultValue()).isTrue();
    assertThat(collection.orElseThrow().legacyToggleKey()).isNull();
    assertThat(PermissionFeature.ASKER_EMAIL_LIVE_CHAT.defaultValue()).isFalse();
  }

  @Test
  void registry_shouldGovernTeamDiscussionAndItsChatFeaturesLikeSupervision() {
    // ORISO-Frontend#2491: same policy model as supervision, Admin group "teamDiscussion".
    assertThat(PermissionFeature.TEAM_DISCUSSION.apiKey())
        .isEqualTo("featureTeamDiscussionEnabled");
    assertThat(PermissionFeature.TEAM_DISCUSSION.legacyToggleKey()).isEqualTo("teamDiscussion");
    Map<String, String> subFeatures =
        Map.of(
            "featureVideoCallsTeamDiscussionsEnabled", "videoCallsTeamDiscussions",
            "featureAudioCallsTeamDiscussionsEnabled", "audioCallsTeamDiscussions",
            "featureVoiceMessagesTeamDiscussionsEnabled", "voiceMessagesTeamDiscussions",
            "featureThreadsTeamDiscussionsEnabled", "threadsTeamDiscussions",
            "featureMediaUploadTeamDiscussionsEnabled", "mediaUploadTeamDiscussions",
            "featureMediaInlineDisplayTeamDiscussionsEnabled", "mediaInlineDisplayTeamDiscussions",
            "featureMediaAiScanTeamDiscussionsEnabled", "mediaAiScanTeamDiscussions");
    subFeatures.forEach(
        (apiKey, toggleKey) -> {
          var feature = PermissionFeature.byApiKey(apiKey);
          assertThat(feature).as(apiKey).isPresent();
          assertThat(feature.orElseThrow().legacyToggleKey()).isEqualTo(toggleKey);
          assertThat(feature.orElseThrow().transitionFallback()).isEmpty();
        });
  }

  @Test
  void registry_shouldKeepTheTeamChatAfterAcceptOffByDefault() {
    // ORISO-Frontend#2491: today the team chat is archived at accept.
    var afterAccept = PermissionFeature.byApiKey("featureTeamDiscussionAfterAcceptEnabled");
    assertThat(afterAccept).contains(PermissionFeature.TEAM_DISCUSSION_AFTER_ACCEPT);
    assertThat(afterAccept.orElseThrow().legacyToggleKey()).isEqualTo("teamDiscussionAfterAccept");
    assertThat(afterAccept.orElseThrow().defaultValue()).isFalse();
  }
}
