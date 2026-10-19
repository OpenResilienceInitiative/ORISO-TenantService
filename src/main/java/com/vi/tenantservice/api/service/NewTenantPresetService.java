package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.policy.PolicyValue;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * #251: a new Träger starts from the platform admin's preset <b>as it is at creation time</b>.
 *
 * <p>The preset is the set of platform policies the platform admin set explicitly ({@link
 * TenantAdminControlsService#getExplicitPlatformPolicies()}). Their values are written into the new
 * tenant's stored settings, so a later preset change only affects Träger created afterwards.
 * Existing tenants and the read defaults in {@link TenantSettings} are deliberately not touched.
 *
 * <p>Where the platform admin set nothing, the feature's existing default applies. For the
 * conversation features that default is <b>on</b> (decision 2026-09-16: the initial preset is
 * "everything on"), written explicitly so it overrides the older hard-coded "off" from the default
 * settings file. Supervision is the deliberate exception: it is not tested yet (decision
 * 2026-09-16), so the master switch {@code featureSupervisionEnabled}, its per-type switches
 * ({@code featureSupervisionOneOnOneChatsEnabled}, {@code featureSupervisionAnonymousChatsEnabled})
 * and the seven {@code feature*SupervisionChatsEnabled} sub-flags (video, audio, voice messages,
 * threads, media upload, media inline display, media AI scan) are written explicitly <b>off</b>
 * unless the platform admin set a policy for them. Every other registry feature (media handling,
 * advice-seeker switches) keeps whatever the default settings file or {@code
 * TenantSettings.applyDefaults()} already gives it — in particular AI scan is not switched on by a
 * policy entry that was merely derived from the legacy toggles.
 */
@Service
@RequiredArgsConstructor
public class NewTenantPresetService {

  /** Registry features that have a tenant settings field, keyed by policy key. */
  private static final Map<String, BiConsumer<TenantSettings, Boolean>> SETTERS =
      Map.ofEntries(
          Map.entry("featureAnonymousChatEnabled", TenantSettings::setFeatureAnonymousChatEnabled),
          Map.entry("featureGroupChatV2Enabled", TenantSettings::setFeatureGroupChatV2Enabled),
          Map.entry(
              "featureInternalGroupChatEnabled",
              TenantSettings::setFeatureInternalGroupChatEnabled),
          Map.entry(
              "featureSelfHelpGroupsEnabled", TenantSettings::setFeatureSelfHelpGroupsEnabled),
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
              TenantSettings::setFeatureVoiceMessagesSupervisionChatsEnabled),
          Map.entry("featureMediaUploadEnabled", TenantSettings::setFeatureMediaUploadEnabled),
          Map.entry(
              "featureMediaUploadAnonymousChatsEnabled",
              TenantSettings::setFeatureMediaUploadAnonymousChatsEnabled),
          Map.entry(
              "featureMediaUploadOneOnOneChatsEnabled",
              TenantSettings::setFeatureMediaUploadOneOnOneChatsEnabled),
          Map.entry(
              "featureMediaUploadGroupChatsEnabled",
              TenantSettings::setFeatureMediaUploadGroupChatsEnabled),
          Map.entry(
              "featureMediaUploadSupervisionChatsEnabled",
              TenantSettings::setFeatureMediaUploadSupervisionChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplayEnabled",
              TenantSettings::setFeatureMediaInlineDisplayEnabled),
          Map.entry(
              "featureMediaInlineDisplayAnonymousChatsEnabled",
              TenantSettings::setFeatureMediaInlineDisplayAnonymousChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplayOneOnOneChatsEnabled",
              TenantSettings::setFeatureMediaInlineDisplayOneOnOneChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplayGroupChatsEnabled",
              TenantSettings::setFeatureMediaInlineDisplayGroupChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplaySupervisionChatsEnabled",
              TenantSettings::setFeatureMediaInlineDisplaySupervisionChatsEnabled),
          Map.entry("featureMediaAiScanEnabled", TenantSettings::setFeatureMediaAiScanEnabled),
          Map.entry(
              "featureMediaAiScanAnonymousChatsEnabled",
              TenantSettings::setFeatureMediaAiScanAnonymousChatsEnabled),
          Map.entry(
              "featureMediaAiScanOneOnOneChatsEnabled",
              TenantSettings::setFeatureMediaAiScanOneOnOneChatsEnabled),
          Map.entry(
              "featureMediaAiScanGroupChatsEnabled",
              TenantSettings::setFeatureMediaAiScanGroupChatsEnabled),
          Map.entry(
              "featureMediaAiScanSupervisionChatsEnabled",
              TenantSettings::setFeatureMediaAiScanSupervisionChatsEnabled),
          Map.entry("featureDisplayNameEditable", TenantSettings::setFeatureDisplayNameEditable),
          Map.entry("featureAskerEmailEnabled", TenantSettings::setFeatureAskerEmailEnabled));

  /** Conversation features: without an explicit preset entry they start on. */
  private static final Set<String> CONVERSATION_FEATURES =
      Set.of(
          "featureAnonymousChatEnabled",
          "featureGroupChatV2Enabled",
          "featureInternalGroupChatEnabled",
          "featureSelfHelpGroupsEnabled",
          "featureCallsEnabled",
          "featureAudioCallsEnabled",
          "featureAudioCallsAnonymousChatsEnabled",
          "featureAudioCallsOneOnOneChatsEnabled",
          "featureAudioCallsGroupChatsEnabled",
          "featureVideoCallsEnabled",
          "featureVideoCallsAnonymousChatsEnabled",
          "featureVideoCallsOneOnOneChatsEnabled",
          "featureVideoCallsGroupChatsEnabled",
          "featureThreadsEnabled",
          "featureThreadsAnonymousChatsEnabled",
          "featureThreadsOneOnOneEnabled",
          "featureThreadsGroupChatsEnabled",
          "featureVoiceMessagesEnabled",
          "featureVoiceMessagesAnonymousChatsEnabled",
          "featureVoiceMessagesOneOnOneChatsEnabled",
          "featureVoiceMessagesGroupChatsEnabled");

  /**
   * Supervision (decision 2026-09-16, #251): not tested yet, so without an explicit platform policy
   * a new Träger starts with these off — the master switch, its two per-type switches, and every
   * {@code feature*SupervisionChatsEnabled} sub-flag across the other feature families.
   */
  private static final Set<String> SUPERVISION_FEATURES =
      Set.of(
          "featureSupervisionEnabled",
          "featureSupervisionAnonymousChatsEnabled",
          "featureSupervisionOneOnOneChatsEnabled",
          "featureAudioCallsSupervisionChatsEnabled",
          "featureVideoCallsSupervisionChatsEnabled",
          "featureThreadsSupervisionChatsEnabled",
          "featureVoiceMessagesSupervisionChatsEnabled",
          "featureMediaUploadSupervisionChatsEnabled",
          "featureMediaInlineDisplaySupervisionChatsEnabled",
          "featureMediaAiScanSupervisionChatsEnabled");

  private final @NonNull TenantAdminControlsService tenantAdminControlsService;

  /**
   * Writes the platform admin's explicit preset into {@code newTenantSettings}; conversation
   * features without a preset entry are written as on, supervision features without a preset entry
   * are written as off.
   */
  public TenantSettings applyCurrentPlatformPreset(TenantSettings newTenantSettings) {
    Map<String, PolicyValue<Boolean>> preset =
        tenantAdminControlsService.getExplicitPlatformPolicies();
    for (PermissionFeature feature : PermissionFeature.values()) {
      BiConsumer<TenantSettings, Boolean> setter = SETTERS.get(feature.apiKey());
      if (setter == null) {
        continue; // appearance, case handover: no tenant settings field
      }
      PolicyValue<Boolean> policy = preset.get(feature.apiKey());
      if (policy != null) {
        setter.accept(newTenantSettings, policy.value());
      } else if (CONVERSATION_FEATURES.contains(feature.apiKey())) {
        setter.accept(newTenantSettings, true);
      } else if (SUPERVISION_FEATURES.contains(feature.apiKey())) {
        setter.accept(newTenantSettings, false);
      }
    }
    // team discussions are a conversation feature outside the permission registry: start on
    newTenantSettings.setFeatureTeamDiscussionEnabled(true);
    return newTenantSettings;
  }
}
