package com.vi.tenantservice.api.policy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Canonical feature registry shared through the TenantService policy contract. */
public enum PermissionFeature {
  APPEARANCE("appearance", "appearance"),
  ANONYMOUS_CHAT("featureAnonymousChatEnabled", "anonymousChat"),
  GROUP_CHAT("featureGroupChatV2Enabled", "groupChat"),
  INTERNAL_GROUP_CHAT("featureInternalGroupChatEnabled", "internalGroupChat"),
  SELF_HELP_GROUPS("featureSelfHelpGroupsEnabled", "selfHelpGroups"),
  CALLS("featureCallsEnabled", "calls"),
  SUPERVISION("featureSupervisionEnabled", "supervision"),
  SUPERVISION_ANONYMOUS_CHATS(
      "featureSupervisionAnonymousChatsEnabled", "supervisionAnonymousChats"),
  SUPERVISION_ONE_ON_ONE_CHATS(
      "featureSupervisionOneOnOneChatsEnabled", "supervisionOneOnOneChats"),
  AUDIO_CALLS("featureAudioCallsEnabled", "audioCalls"),
  AUDIO_CALLS_ANONYMOUS_CHATS("featureAudioCallsAnonymousChatsEnabled", "audioCallsAnonymousChats"),
  AUDIO_CALLS_ONE_ON_ONE_CHATS("featureAudioCallsOneOnOneChatsEnabled", "audioCallsOneOnOneChats"),
  AUDIO_CALLS_GROUP_CHATS("featureAudioCallsGroupChatsEnabled", "audioCallsGroupChats"),
  AUDIO_CALLS_SUPERVISION_CHATS(
      "featureAudioCallsSupervisionChatsEnabled", "audioCallsSupervisionChats"),
  VIDEO_CALLS("featureVideoCallsEnabled", "videoCalls"),
  VIDEO_CALLS_ANONYMOUS_CHATS("featureVideoCallsAnonymousChatsEnabled", "videoCallsAnonymousChats"),
  VIDEO_CALLS_ONE_ON_ONE_CHATS("featureVideoCallsOneOnOneChatsEnabled", "videoCallsOneOnOneChats"),
  VIDEO_CALLS_GROUP_CHATS("featureVideoCallsGroupChatsEnabled", "videoCallsGroupChats"),
  VIDEO_CALLS_SUPERVISION_CHATS(
      "featureVideoCallsSupervisionChatsEnabled", "videoCallsSupervisionChats"),
  THREADS("featureThreadsEnabled", "threads"),
  THREADS_ANONYMOUS_CHATS("featureThreadsAnonymousChatsEnabled", "threadsAnonymousChats"),
  THREADS_ONE_ON_ONE_CHATS("featureThreadsOneOnOneEnabled", "threadsOneOnOneChats"),
  THREADS_GROUP_CHATS("featureThreadsGroupChatsEnabled", "threadsGroupChats"),
  THREADS_SUPERVISION_CHATS("featureThreadsSupervisionChatsEnabled", "threadsSupervisionChats"),
  VOICE_MESSAGES("featureVoiceMessagesEnabled", "voiceMessages"),
  VOICE_MESSAGES_ANONYMOUS_CHATS(
      "featureVoiceMessagesAnonymousChatsEnabled", "voiceMessagesAnonymousChats"),
  VOICE_MESSAGES_ONE_ON_ONE_CHATS(
      "featureVoiceMessagesOneOnOneChatsEnabled", "voiceMessagesOneOnOneChats"),
  VOICE_MESSAGES_GROUP_CHATS("featureVoiceMessagesGroupChatsEnabled", "voiceMessagesGroupChats"),
  VOICE_MESSAGES_SUPERVISION_CHATS(
      "featureVoiceMessagesSupervisionChatsEnabled", "voiceMessagesSupervisionChats"),
  MEDIA_UPLOAD("featureMediaUploadEnabled", "mediaUpload"),
  MEDIA_UPLOAD_ANONYMOUS_CHATS(
      "featureMediaUploadAnonymousChatsEnabled", "mediaUploadAnonymousChats"),
  MEDIA_UPLOAD_ONE_ON_ONE_CHATS(
      "featureMediaUploadOneOnOneChatsEnabled", "mediaUploadOneOnOneChats"),
  MEDIA_UPLOAD_GROUP_CHATS("featureMediaUploadGroupChatsEnabled", "mediaUploadGroupChats"),
  MEDIA_UPLOAD_SUPERVISION_CHATS(
      "featureMediaUploadSupervisionChatsEnabled", "mediaUploadSupervisionChats"),
  MEDIA_INLINE_DISPLAY("featureMediaInlineDisplayEnabled", "mediaInlineDisplay"),
  MEDIA_INLINE_DISPLAY_ANONYMOUS_CHATS(
      "featureMediaInlineDisplayAnonymousChatsEnabled", "mediaInlineDisplayAnonymousChats"),
  MEDIA_INLINE_DISPLAY_ONE_ON_ONE_CHATS(
      "featureMediaInlineDisplayOneOnOneChatsEnabled", "mediaInlineDisplayOneOnOneChats"),
  MEDIA_INLINE_DISPLAY_GROUP_CHATS(
      "featureMediaInlineDisplayGroupChatsEnabled", "mediaInlineDisplayGroupChats"),
  MEDIA_INLINE_DISPLAY_SUPERVISION_CHATS(
      "featureMediaInlineDisplaySupervisionChatsEnabled", "mediaInlineDisplaySupervisionChats"),
  MEDIA_AI_SCAN("featureMediaAiScanEnabled", "mediaAiScan"),
  MEDIA_AI_SCAN_ANONYMOUS_CHATS(
      "featureMediaAiScanAnonymousChatsEnabled", "mediaAiScanAnonymousChats"),
  MEDIA_AI_SCAN_ONE_ON_ONE_CHATS(
      "featureMediaAiScanOneOnOneChatsEnabled", "mediaAiScanOneOnOneChats"),
  MEDIA_AI_SCAN_GROUP_CHATS("featureMediaAiScanGroupChatsEnabled", "mediaAiScanGroupChats"),
  MEDIA_AI_SCAN_SUPERVISION_CHATS(
      "featureMediaAiScanSupervisionChatsEnabled", "mediaAiScanSupervisionChats"),
  ASKER_DISPLAY_NAME("featureDisplayNameEditable", null),
  ASKER_EMAIL("featureAskerEmailEnabled", null),
  CASE_HANDOVER("caseHandoverEnabled", null);

  private static final Map<String, PermissionFeature> BY_API_KEY =
      Arrays.stream(values())
          .collect(Collectors.toUnmodifiableMap(PermissionFeature::apiKey, Function.identity()));

  private final String apiKey;
  private final String legacyToggleKey;

  PermissionFeature(String apiKey, String legacyToggleKey) {
    this.apiKey = apiKey;
    this.legacyToggleKey = legacyToggleKey;
  }

  public String apiKey() {
    return apiKey;
  }

  public String legacyToggleKey() {
    return legacyToggleKey;
  }

  /**
   * The feature whose rule applies while no rule was stored for this one yet. The two group chat
   * formats (#250) were split out of {@link #GROUP_CHAT}; platform and tenant rules written before
   * the split must keep governing both formats.
   */
  public Optional<PermissionFeature> transitionFallback() {
    return switch (this) {
      case INTERNAL_GROUP_CHAT, SELF_HELP_GROUPS -> Optional.of(GROUP_CHAT);
      default -> Optional.empty();
    };
  }

  /**
   * Returns {@code policies} with every missing split-out feature filled from its {@link
   * #transitionFallback()} entry. Present entries are never touched; the input is not modified.
   */
  public static <V> Map<String, V> withTransitionFallbacks(Map<String, V> policies) {
    if (policies == null || policies.isEmpty()) {
      return policies;
    }
    Map<String, V> result = null;
    for (PermissionFeature feature : values()) {
      if (policies.containsKey(feature.apiKey())) {
        continue;
      }
      V inherited =
          feature
              .transitionFallback()
              .map(fallback -> policies.get(fallback.apiKey()))
              .orElse(null);
      if (inherited != null) {
        if (result == null) {
          result = new LinkedHashMap<>(policies);
        }
        result.put(feature.apiKey(), inherited);
      }
    }
    return result == null ? policies : Map.copyOf(result);
  }

  public static Optional<PermissionFeature> byApiKey(String apiKey) {
    return Optional.ofNullable(BY_API_KEY.get(apiKey));
  }
}
