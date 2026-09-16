package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantSettings;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * #251: a new Träger starts from the platform admin's preset <b>as it is at creation time</b>.
 *
 * <p>The values are written into the new tenant's stored settings, so a later change of the preset
 * only affects Träger created afterwards. Existing tenants and the read defaults in {@link
 * TenantSettings} are deliberately not touched.
 *
 * <p>Scope: the conversation features. A feature the preset does not name starts <b>on</b> (the
 * initial platform preset is "everything on"). Media handling (upload, inline display, AI scan) and
 * the advice-seeker switches are not conversation features and keep their existing defaults.
 */
@Service
@RequiredArgsConstructor
public class NewTenantPresetService {

  private static final Map<String, BiConsumer<TenantSettings, Boolean>> CONVERSATION_FEATURES =
      Map.ofEntries(
          Map.entry("featureAnonymousChatEnabled", TenantSettings::setFeatureAnonymousChatEnabled),
          Map.entry("featureGroupChatV2Enabled", TenantSettings::setFeatureGroupChatV2Enabled),
          Map.entry(
              "featureInternalGroupChatEnabled",
              TenantSettings::setFeatureInternalGroupChatEnabled),
          Map.entry(
              "featureSelfHelpGroupsEnabled", TenantSettings::setFeatureSelfHelpGroupsEnabled),
          // not part of the permission registry, so the preset never names it: starts on
          Map.entry(
              "featureTeamDiscussionEnabled", TenantSettings::setFeatureTeamDiscussionEnabled),
          Map.entry("featureCallsEnabled", TenantSettings::setFeatureCallsEnabled),
          Map.entry("featureSupervisionEnabled", TenantSettings::setFeatureSupervisionEnabled),
          Map.entry(
              "featureSupervisionAnonymousChatsEnabled",
              TenantSettings::setFeatureSupervisionAnonymousChatsEnabled),
          Map.entry(
              "featureSupervisionOneOnOneChatsEnabled",
              TenantSettings::setFeatureSupervisionOneOnOneChatsEnabled),
          Map.entry("featureAudioCallsEnabled", TenantSettings::setFeatureAudioCallsEnabled),
          Map.entry(
              "featureAudioCallsAnonymousChatsEnabled",
              TenantSettings::setFeatureAudioCallsAnonymousChatsEnabled),
          Map.entry(
              "featureAudioCallsOneOnOneChatsEnabled",
              TenantSettings::setFeatureAudioCallsOneOnOneChatsEnabled),
          Map.entry(
              "featureAudioCallsGroupChatsEnabled",
              TenantSettings::setFeatureAudioCallsGroupChatsEnabled),
          Map.entry(
              "featureAudioCallsSupervisionChatsEnabled",
              TenantSettings::setFeatureAudioCallsSupervisionChatsEnabled),
          Map.entry("featureVideoCallsEnabled", TenantSettings::setFeatureVideoCallsEnabled),
          Map.entry(
              "featureVideoCallsAnonymousChatsEnabled",
              TenantSettings::setFeatureVideoCallsAnonymousChatsEnabled),
          Map.entry(
              "featureVideoCallsOneOnOneChatsEnabled",
              TenantSettings::setFeatureVideoCallsOneOnOneChatsEnabled),
          Map.entry(
              "featureVideoCallsGroupChatsEnabled",
              TenantSettings::setFeatureVideoCallsGroupChatsEnabled),
          Map.entry(
              "featureVideoCallsSupervisionChatsEnabled",
              TenantSettings::setFeatureVideoCallsSupervisionChatsEnabled),
          Map.entry("featureThreadsEnabled", TenantSettings::setFeatureThreadsEnabled),
          Map.entry(
              "featureThreadsAnonymousChatsEnabled",
              TenantSettings::setFeatureThreadsAnonymousChatsEnabled),
          Map.entry(
              "featureThreadsOneOnOneEnabled", TenantSettings::setFeatureThreadsOneOnOneEnabled),
          Map.entry(
              "featureThreadsGroupChatsEnabled",
              TenantSettings::setFeatureThreadsGroupChatsEnabled),
          Map.entry(
              "featureThreadsSupervisionChatsEnabled",
              TenantSettings::setFeatureThreadsSupervisionChatsEnabled),
          Map.entry("featureVoiceMessagesEnabled", TenantSettings::setFeatureVoiceMessagesEnabled),
          Map.entry(
              "featureVoiceMessagesAnonymousChatsEnabled",
              TenantSettings::setFeatureVoiceMessagesAnonymousChatsEnabled),
          Map.entry(
              "featureVoiceMessagesOneOnOneChatsEnabled",
              TenantSettings::setFeatureVoiceMessagesOneOnOneChatsEnabled),
          Map.entry(
              "featureVoiceMessagesGroupChatsEnabled",
              TenantSettings::setFeatureVoiceMessagesGroupChatsEnabled),
          Map.entry(
              "featureVoiceMessagesSupervisionChatsEnabled",
              TenantSettings::setFeatureVoiceMessagesSupervisionChatsEnabled));

  private final @NonNull TenantAdminControlsService tenantAdminControlsService;

  /** Writes an explicit value for every conversation feature into {@code newTenantSettings}. */
  public TenantSettings applyCurrentPlatformPreset(TenantSettings newTenantSettings) {
    TenantAdminControls controls = tenantAdminControlsService.getControls();
    Map<String, BooleanPermissionPolicy> preset =
        controls == null || controls.getPermissionPolicies() == null
            ? Map.of()
            : controls.getPermissionPolicies();
    CONVERSATION_FEATURES.forEach(
        (feature, setter) -> setter.accept(newTenantSettings, presetValue(preset.get(feature))));
    return newTenantSettings;
  }

  private static boolean presetValue(BooleanPermissionPolicy policy) {
    return policy == null || policy.getValue() == null || policy.getValue();
  }
}
