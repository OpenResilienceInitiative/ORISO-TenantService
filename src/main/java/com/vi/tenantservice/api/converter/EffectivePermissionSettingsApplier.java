package com.vi.tenantservice.api.converter;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Applies an upper role's effective permission constraints (platform {@link TenantAdminControls})
 * to the public feature flags served to the counselling app. {@code allowedPermissionToggles ==
 * false} forces the feature off; {@code enforcedPermissionToggles == true} forces it on; anything
 * else is left as the tenant set it. This is the server-side single source of truth so the Frontend
 * needs no awareness of the controls (which are stripped from the restricted DTO). See ADR-013 P4.
 */
@Component
public class EffectivePermissionSettingsApplier {

  private static final Map<String, BiConsumer<Settings, Boolean>> POLICY_SETTERS =
      Map.ofEntries(
          Map.entry("featureAnonymousChatEnabled", Settings::setFeatureAnonymousChatEnabled),
          Map.entry("featureGroupChatV2Enabled", Settings::setFeatureGroupChatV2Enabled),
          Map.entry("featureCallsEnabled", Settings::setFeatureCallsEnabled),
          Map.entry("featureSupervisionEnabled", Settings::setFeatureSupervisionEnabled),
          Map.entry(
              "featureSupervisionAnonymousChatsEnabled",
              Settings::setFeatureSupervisionAnonymousChatsEnabled),
          Map.entry(
              "featureSupervisionOneOnOneChatsEnabled",
              Settings::setFeatureSupervisionOneOnOneChatsEnabled),
          Map.entry("featureAudioCallsEnabled", Settings::setFeatureAudioCallsEnabled),
          Map.entry(
              "featureAudioCallsAnonymousChatsEnabled",
              Settings::setFeatureAudioCallsAnonymousChatsEnabled),
          Map.entry(
              "featureAudioCallsOneOnOneChatsEnabled",
              Settings::setFeatureAudioCallsOneOnOneChatsEnabled),
          Map.entry(
              "featureAudioCallsGroupChatsEnabled",
              Settings::setFeatureAudioCallsGroupChatsEnabled),
          Map.entry(
              "featureAudioCallsSupervisionChatsEnabled",
              Settings::setFeatureAudioCallsSupervisionChatsEnabled),
          Map.entry("featureVideoCallsEnabled", Settings::setFeatureVideoCallsEnabled),
          Map.entry(
              "featureVideoCallsAnonymousChatsEnabled",
              Settings::setFeatureVideoCallsAnonymousChatsEnabled),
          Map.entry(
              "featureVideoCallsOneOnOneChatsEnabled",
              Settings::setFeatureVideoCallsOneOnOneChatsEnabled),
          Map.entry(
              "featureVideoCallsGroupChatsEnabled",
              Settings::setFeatureVideoCallsGroupChatsEnabled),
          Map.entry(
              "featureVideoCallsSupervisionChatsEnabled",
              Settings::setFeatureVideoCallsSupervisionChatsEnabled),
          Map.entry("featureThreadsEnabled", Settings::setFeatureThreadsEnabled),
          Map.entry(
              "featureThreadsAnonymousChatsEnabled",
              Settings::setFeatureThreadsAnonymousChatsEnabled),
          Map.entry("featureThreadsOneOnOneEnabled", Settings::setFeatureThreadsOneOnOneEnabled),
          Map.entry(
              "featureThreadsGroupChatsEnabled", Settings::setFeatureThreadsGroupChatsEnabled),
          Map.entry(
              "featureThreadsSupervisionChatsEnabled",
              Settings::setFeatureThreadsSupervisionChatsEnabled),
          Map.entry("featureVoiceMessagesEnabled", Settings::setFeatureVoiceMessagesEnabled),
          Map.entry(
              "featureVoiceMessagesAnonymousChatsEnabled",
              Settings::setFeatureVoiceMessagesAnonymousChatsEnabled),
          Map.entry(
              "featureVoiceMessagesOneOnOneChatsEnabled",
              Settings::setFeatureVoiceMessagesOneOnOneChatsEnabled),
          Map.entry(
              "featureVoiceMessagesGroupChatsEnabled",
              Settings::setFeatureVoiceMessagesGroupChatsEnabled),
          Map.entry(
              "featureVoiceMessagesSupervisionChatsEnabled",
              Settings::setFeatureVoiceMessagesSupervisionChatsEnabled),
          Map.entry("featureMediaUploadEnabled", Settings::setFeatureMediaUploadEnabled),
          Map.entry(
              "featureMediaUploadAnonymousChatsEnabled",
              Settings::setFeatureMediaUploadAnonymousChatsEnabled),
          Map.entry(
              "featureMediaUploadOneOnOneChatsEnabled",
              Settings::setFeatureMediaUploadOneOnOneChatsEnabled),
          Map.entry(
              "featureMediaUploadGroupChatsEnabled",
              Settings::setFeatureMediaUploadGroupChatsEnabled),
          Map.entry(
              "featureMediaUploadSupervisionChatsEnabled",
              Settings::setFeatureMediaUploadSupervisionChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplayEnabled", Settings::setFeatureMediaInlineDisplayEnabled),
          Map.entry(
              "featureMediaInlineDisplayAnonymousChatsEnabled",
              Settings::setFeatureMediaInlineDisplayAnonymousChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplayOneOnOneChatsEnabled",
              Settings::setFeatureMediaInlineDisplayOneOnOneChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplayGroupChatsEnabled",
              Settings::setFeatureMediaInlineDisplayGroupChatsEnabled),
          Map.entry(
              "featureMediaInlineDisplaySupervisionChatsEnabled",
              Settings::setFeatureMediaInlineDisplaySupervisionChatsEnabled),
          Map.entry("featureMediaAiScanEnabled", Settings::setFeatureMediaAiScanEnabled),
          Map.entry(
              "featureMediaAiScanAnonymousChatsEnabled",
              Settings::setFeatureMediaAiScanAnonymousChatsEnabled),
          Map.entry(
              "featureMediaAiScanOneOnOneChatsEnabled",
              Settings::setFeatureMediaAiScanOneOnOneChatsEnabled),
          Map.entry(
              "featureMediaAiScanGroupChatsEnabled",
              Settings::setFeatureMediaAiScanGroupChatsEnabled),
          Map.entry(
              "featureMediaAiScanSupervisionChatsEnabled",
              Settings::setFeatureMediaAiScanSupervisionChatsEnabled),
          Map.entry("featureDisplayNameEditable", Settings::setFeatureDisplayNameEditable),
          Map.entry("featureAskerEmailEnabled", Settings::setFeatureAskerEmailEnabled));

  private record ToggleBinding(
      Function<TenantAdminAllowedPermissionToggles, Boolean> getter,
      BiConsumer<Settings, Boolean> setter) {}

  // One binding per conversation feature: the toggle getter <-> the Settings feature-flag setter it
  // governs. Mirrors ORISO-Admin's TOGGLE_KEY_TO_FIELD (note the irregular threadsOneOnOneChats ->
  // featureThreadsOneOnOneEnabled).
  private static final List<ToggleBinding> BINDINGS =
      List.of(
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getAnonymousChat,
              Settings::setFeatureAnonymousChatEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getGroupChat,
              Settings::setFeatureGroupChatV2Enabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getCalls, Settings::setFeatureCallsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getSupervision,
              Settings::setFeatureSupervisionEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getSupervisionAnonymousChats,
              Settings::setFeatureSupervisionAnonymousChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getSupervisionOneOnOneChats,
              Settings::setFeatureSupervisionOneOnOneChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getAudioCalls,
              Settings::setFeatureAudioCallsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getAudioCallsAnonymousChats,
              Settings::setFeatureAudioCallsAnonymousChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getAudioCallsOneOnOneChats,
              Settings::setFeatureAudioCallsOneOnOneChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getAudioCallsGroupChats,
              Settings::setFeatureAudioCallsGroupChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getAudioCallsSupervisionChats,
              Settings::setFeatureAudioCallsSupervisionChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVideoCalls,
              Settings::setFeatureVideoCallsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVideoCallsAnonymousChats,
              Settings::setFeatureVideoCallsAnonymousChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVideoCallsOneOnOneChats,
              Settings::setFeatureVideoCallsOneOnOneChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVideoCallsGroupChats,
              Settings::setFeatureVideoCallsGroupChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVideoCallsSupervisionChats,
              Settings::setFeatureVideoCallsSupervisionChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getThreads, Settings::setFeatureThreadsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getThreadsAnonymousChats,
              Settings::setFeatureThreadsAnonymousChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getThreadsOneOnOneChats,
              Settings::setFeatureThreadsOneOnOneEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getThreadsGroupChats,
              Settings::setFeatureThreadsGroupChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getThreadsSupervisionChats,
              Settings::setFeatureThreadsSupervisionChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVoiceMessages,
              Settings::setFeatureVoiceMessagesEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVoiceMessagesAnonymousChats,
              Settings::setFeatureVoiceMessagesAnonymousChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVoiceMessagesOneOnOneChats,
              Settings::setFeatureVoiceMessagesOneOnOneChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVoiceMessagesGroupChats,
              Settings::setFeatureVoiceMessagesGroupChatsEnabled),
          new ToggleBinding(
              TenantAdminAllowedPermissionToggles::getVoiceMessagesSupervisionChats,
              Settings::setFeatureVoiceMessagesSupervisionChatsEnabled));

  public void applyTo(Settings settings, TenantAdminControls controls) {
    if (settings == null || controls == null) {
      return;
    }
    TenantAdminAllowedPermissionToggles allowed = controls.getAllowedPermissionToggles();
    TenantAdminAllowedPermissionToggles enforced = controls.getEnforcedPermissionToggles();
    for (ToggleBinding binding : BINDINGS) {
      if (allowed != null && Boolean.FALSE.equals(binding.getter().apply(allowed))) {
        binding.setter().accept(settings, false);
      }
      if (enforced != null && Boolean.TRUE.equals(binding.getter().apply(enforced))) {
        binding.setter().accept(settings, true);
      }
    }
  }

  /** Applies locked policies and resolved tenant-local values. Inherited suggestions are hints. */
  public void applyPolicies(
      Settings settings, Map<String, BooleanPermissionPolicy> permissionPolicies) {
    if (settings == null || permissionPolicies == null) {
      return;
    }
    permissionPolicies.forEach(
        (feature, policy) -> {
          BiConsumer<Settings, Boolean> setter = POLICY_SETTERS.get(feature);
          if (setter != null
              && policy != null
              && (policy.getMode() == PermissionPolicyMode.ENFORCED
                  || Boolean.FALSE.equals(policy.getInherited()))) {
            setter.accept(settings, policy.getValue());
          }
        });
  }
}
