package com.vi.tenantservice.api.converter;

import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import java.util.List;
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
}
