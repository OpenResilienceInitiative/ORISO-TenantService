package com.vi.tenantservice.api.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantSettings {

  Boolean featureStatisticsEnabled;
  Boolean featureTopicsEnabled;
  Boolean topicsInRegistrationEnabled;
  Boolean featureDemographicsEnabled;
  Boolean featureAppointmentsEnabled;
  Boolean featureGroupChatV2Enabled;
  Boolean featureToolsEnabled;
  Boolean featureAnonymousChatEnabled;
  Boolean featureCallsEnabled;
  Boolean featureSupervisionEnabled;
  Boolean featureSupervisionAnonymousChatsEnabled;
  Boolean featureSupervisionOneOnOneChatsEnabled;
  Boolean featureAudioCallsEnabled;
  Boolean featureAudioCallsAnonymousChatsEnabled;
  Boolean featureAudioCallsOneOnOneChatsEnabled;
  Boolean featureAudioCallsGroupChatsEnabled;
  Boolean featureAudioCallsSupervisionChatsEnabled;
  Boolean featureVideoCallsEnabled;
  Boolean featureVideoCallsAnonymousChatsEnabled;
  Boolean featureVideoCallsOneOnOneChatsEnabled;
  Boolean featureVideoCallsGroupChatsEnabled;
  Boolean featureVideoCallsSupervisionChatsEnabled;
  Boolean featureThreadsEnabled;
  Boolean featureThreadsAnonymousChatsEnabled;
  Boolean featureThreadsGroupChatsEnabled;
  Boolean featureThreadsOneOnOneEnabled;
  Boolean featureThreadsSupervisionChatsEnabled;
  Boolean featureVoiceMessagesEnabled;
  Boolean featureVoiceMessagesAnonymousChatsEnabled;
  Boolean featureVoiceMessagesOneOnOneChatsEnabled;
  Boolean featureVoiceMessagesGroupChatsEnabled;
  Boolean featureVoiceMessagesSupervisionChatsEnabled;
  Boolean featureSystemNotificationEmailsEnabled;
  TenantSmtpSettings smtp;
  Boolean featureAttachmentUploadDisabled;
  Boolean isVideoCallAllowed;
  Boolean showAskerProfile;

  String featureToolsOIDCToken;
  List<String> activeLanguages;

  Boolean featureCentralDataProtectionEnabled;

  Boolean featureCentralDataProtectionTemplateEnabled;

  TenantAdminControlsSettings tenantAdminControls;

  /**
   * Documented defaults for boolean feature flags when a key is absent from stored JSON. Keyed by
   * Jackson property / field name. Excludes {@code featureCentralDataProtectionEnabled} (orphan
   * field, out of scope).
   */
  private static final Map<String, Boolean> BOOLEAN_FIELD_DEFAULTS =
      Collections.unmodifiableMap(
          Map.ofEntries(
              Map.entry("featureStatisticsEnabled", false),
              Map.entry("featureTopicsEnabled", false),
              Map.entry("topicsInRegistrationEnabled", false),
              Map.entry("featureDemographicsEnabled", false),
              Map.entry("featureAppointmentsEnabled", false),
              Map.entry("featureGroupChatV2Enabled", false),
              Map.entry("featureToolsEnabled", false),
              Map.entry("featureAnonymousChatEnabled", true),
              Map.entry("featureCallsEnabled", true),
              Map.entry("featureSupervisionEnabled", true),
              Map.entry("featureSupervisionAnonymousChatsEnabled", true),
              Map.entry("featureSupervisionOneOnOneChatsEnabled", true),
              Map.entry("featureAudioCallsEnabled", true),
              Map.entry("featureAudioCallsAnonymousChatsEnabled", true),
              Map.entry("featureAudioCallsOneOnOneChatsEnabled", true),
              Map.entry("featureAudioCallsGroupChatsEnabled", true),
              Map.entry("featureAudioCallsSupervisionChatsEnabled", true),
              Map.entry("featureVideoCallsEnabled", true),
              Map.entry("featureVideoCallsAnonymousChatsEnabled", true),
              Map.entry("featureVideoCallsOneOnOneChatsEnabled", true),
              Map.entry("featureVideoCallsGroupChatsEnabled", true),
              Map.entry("featureVideoCallsSupervisionChatsEnabled", true),
              Map.entry("featureThreadsEnabled", true),
              Map.entry("featureThreadsAnonymousChatsEnabled", true),
              Map.entry("featureThreadsGroupChatsEnabled", true),
              Map.entry("featureThreadsOneOnOneEnabled", true),
              Map.entry("featureThreadsSupervisionChatsEnabled", true),
              Map.entry("featureVoiceMessagesEnabled", true),
              Map.entry("featureVoiceMessagesAnonymousChatsEnabled", true),
              Map.entry("featureVoiceMessagesOneOnOneChatsEnabled", true),
              Map.entry("featureVoiceMessagesGroupChatsEnabled", true),
              Map.entry("featureVoiceMessagesSupervisionChatsEnabled", true),
              Map.entry("featureSystemNotificationEmailsEnabled", false),
              Map.entry("featureAttachmentUploadDisabled", false),
              Map.entry("isVideoCallAllowed", false),
              Map.entry("showAskerProfile", false),
              Map.entry("featureCentralDataProtectionTemplateEnabled", false)));

  public static Map<String, Boolean> getBooleanFieldDefaults() {
    return BOOLEAN_FIELD_DEFAULTS;
  }

  /**
   * Fills any null boolean wrapper field with its documented default. Does not touch non-boolean
   * fields or {@code featureCentralDataProtectionEnabled}. Call after Jackson deserialization when
   * absent keys must be distinguished from explicit {@code false}.
   */
  public TenantSettings applyDefaults() {
    if (featureStatisticsEnabled == null) {
      featureStatisticsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureStatisticsEnabled");
    }
    if (featureTopicsEnabled == null) {
      featureTopicsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureTopicsEnabled");
    }
    if (topicsInRegistrationEnabled == null) {
      topicsInRegistrationEnabled = BOOLEAN_FIELD_DEFAULTS.get("topicsInRegistrationEnabled");
    }
    if (featureDemographicsEnabled == null) {
      featureDemographicsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureDemographicsEnabled");
    }
    if (featureAppointmentsEnabled == null) {
      featureAppointmentsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureAppointmentsEnabled");
    }
    if (featureGroupChatV2Enabled == null) {
      featureGroupChatV2Enabled = BOOLEAN_FIELD_DEFAULTS.get("featureGroupChatV2Enabled");
    }
    if (featureToolsEnabled == null) {
      featureToolsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureToolsEnabled");
    }
    if (featureAnonymousChatEnabled == null) {
      featureAnonymousChatEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureAnonymousChatEnabled");
    }
    if (featureCallsEnabled == null) {
      featureCallsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureCallsEnabled");
    }
    if (featureSupervisionEnabled == null) {
      featureSupervisionEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureSupervisionEnabled");
    }
    if (featureSupervisionAnonymousChatsEnabled == null) {
      featureSupervisionAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureSupervisionAnonymousChatsEnabled");
    }
    if (featureSupervisionOneOnOneChatsEnabled == null) {
      featureSupervisionOneOnOneChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureSupervisionOneOnOneChatsEnabled");
    }
    if (featureAudioCallsEnabled == null) {
      featureAudioCallsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureAudioCallsEnabled");
    }
    if (featureAudioCallsAnonymousChatsEnabled == null) {
      featureAudioCallsAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureAudioCallsAnonymousChatsEnabled");
    }
    if (featureAudioCallsOneOnOneChatsEnabled == null) {
      featureAudioCallsOneOnOneChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureAudioCallsOneOnOneChatsEnabled");
    }
    if (featureAudioCallsGroupChatsEnabled == null) {
      featureAudioCallsGroupChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureAudioCallsGroupChatsEnabled");
    }
    if (featureAudioCallsSupervisionChatsEnabled == null) {
      featureAudioCallsSupervisionChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureAudioCallsSupervisionChatsEnabled");
    }
    if (featureVideoCallsEnabled == null) {
      featureVideoCallsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureVideoCallsEnabled");
    }
    if (featureVideoCallsAnonymousChatsEnabled == null) {
      featureVideoCallsAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVideoCallsAnonymousChatsEnabled");
    }
    if (featureVideoCallsOneOnOneChatsEnabled == null) {
      featureVideoCallsOneOnOneChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVideoCallsOneOnOneChatsEnabled");
    }
    if (featureVideoCallsGroupChatsEnabled == null) {
      featureVideoCallsGroupChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVideoCallsGroupChatsEnabled");
    }
    if (featureVideoCallsSupervisionChatsEnabled == null) {
      featureVideoCallsSupervisionChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVideoCallsSupervisionChatsEnabled");
    }
    if (featureThreadsEnabled == null) {
      featureThreadsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureThreadsEnabled");
    }
    if (featureThreadsAnonymousChatsEnabled == null) {
      featureThreadsAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureThreadsAnonymousChatsEnabled");
    }
    if (featureThreadsGroupChatsEnabled == null) {
      featureThreadsGroupChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureThreadsGroupChatsEnabled");
    }
    if (featureThreadsOneOnOneEnabled == null) {
      featureThreadsOneOnOneEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureThreadsOneOnOneEnabled");
    }
    if (featureThreadsSupervisionChatsEnabled == null) {
      featureThreadsSupervisionChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureThreadsSupervisionChatsEnabled");
    }
    if (featureVoiceMessagesEnabled == null) {
      featureVoiceMessagesEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureVoiceMessagesEnabled");
    }
    if (featureVoiceMessagesAnonymousChatsEnabled == null) {
      featureVoiceMessagesAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVoiceMessagesAnonymousChatsEnabled");
    }
    if (featureVoiceMessagesOneOnOneChatsEnabled == null) {
      featureVoiceMessagesOneOnOneChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVoiceMessagesOneOnOneChatsEnabled");
    }
    if (featureVoiceMessagesGroupChatsEnabled == null) {
      featureVoiceMessagesGroupChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVoiceMessagesGroupChatsEnabled");
    }
    if (featureVoiceMessagesSupervisionChatsEnabled == null) {
      featureVoiceMessagesSupervisionChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureVoiceMessagesSupervisionChatsEnabled");
    }
    if (featureSystemNotificationEmailsEnabled == null) {
      featureSystemNotificationEmailsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureSystemNotificationEmailsEnabled");
    }
    if (featureAttachmentUploadDisabled == null) {
      featureAttachmentUploadDisabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureAttachmentUploadDisabled");
    }
    if (isVideoCallAllowed == null) {
      isVideoCallAllowed = BOOLEAN_FIELD_DEFAULTS.get("isVideoCallAllowed");
    }
    if (showAskerProfile == null) {
      showAskerProfile = BOOLEAN_FIELD_DEFAULTS.get("showAskerProfile");
    }
    if (featureCentralDataProtectionTemplateEnabled == null) {
      featureCentralDataProtectionTemplateEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureCentralDataProtectionTemplateEnabled");
    }
    return this;
  }
}
