package com.vi.tenantservice.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  Boolean featureTeamDiscussionEnabled;
  Boolean featureToolsEnabled;
  Boolean featureAnonymousChatEnabled;
  Boolean allowAdviceSeekerUsernameEditing;
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
  Boolean featureMediaUploadEnabled;
  Boolean featureMediaUploadAnonymousChatsEnabled;
  Boolean featureMediaUploadOneOnOneChatsEnabled;
  Boolean featureMediaUploadGroupChatsEnabled;
  Boolean featureMediaUploadSupervisionChatsEnabled;
  Boolean featureMediaInlineDisplayEnabled;
  Boolean featureMediaInlineDisplayAnonymousChatsEnabled;
  Boolean featureMediaInlineDisplayOneOnOneChatsEnabled;
  Boolean featureMediaInlineDisplayGroupChatsEnabled;
  Boolean featureMediaInlineDisplaySupervisionChatsEnabled;
  Boolean featureMediaAiScanEnabled;
  Boolean featureMediaAiScanAnonymousChatsEnabled;
  Boolean featureMediaAiScanOneOnOneChatsEnabled;
  Boolean featureMediaAiScanGroupChatsEnabled;
  Boolean featureMediaAiScanSupervisionChatsEnabled;

  /**
   * Retired key, superseded by the featureMediaUpload* family (ADR-015). Still read from stored
   * JSON so {@link #applyDefaults()} can translate it once, but never serialized back and no longer
   * part of the API surface.
   */
  @JsonProperty(value = "featureAttachmentUploadDisabled", access = JsonProperty.Access.WRITE_ONLY)
  Boolean legacyFeatureAttachmentUploadDisabled;

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
              Map.entry("featureTeamDiscussionEnabled", false),
              Map.entry("featureToolsEnabled", false),
              Map.entry("featureAnonymousChatEnabled", true),
              Map.entry("allowAdviceSeekerUsernameEditing", true),
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
              Map.entry("featureMediaUploadEnabled", true),
              Map.entry("featureMediaUploadAnonymousChatsEnabled", true),
              Map.entry("featureMediaUploadOneOnOneChatsEnabled", true),
              Map.entry("featureMediaUploadGroupChatsEnabled", true),
              Map.entry("featureMediaUploadSupervisionChatsEnabled", true),
              Map.entry("featureMediaInlineDisplayEnabled", true),
              Map.entry("featureMediaInlineDisplayAnonymousChatsEnabled", true),
              Map.entry("featureMediaInlineDisplayOneOnOneChatsEnabled", true),
              Map.entry("featureMediaInlineDisplayGroupChatsEnabled", true),
              Map.entry("featureMediaInlineDisplaySupervisionChatsEnabled", true),
              Map.entry("featureMediaAiScanEnabled", false),
              Map.entry("featureMediaAiScanAnonymousChatsEnabled", false),
              Map.entry("featureMediaAiScanOneOnOneChatsEnabled", false),
              Map.entry("featureMediaAiScanGroupChatsEnabled", false),
              Map.entry("featureMediaAiScanSupervisionChatsEnabled", false),
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
    if (featureTeamDiscussionEnabled == null) {
      featureTeamDiscussionEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureTeamDiscussionEnabled");
    }
    if (featureToolsEnabled == null) {
      featureToolsEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureToolsEnabled");
    }
    if (featureAnonymousChatEnabled == null) {
      featureAnonymousChatEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureAnonymousChatEnabled");
    }
    if (allowAdviceSeekerUsernameEditing == null) {
      allowAdviceSeekerUsernameEditing =
          BOOLEAN_FIELD_DEFAULTS.get("allowAdviceSeekerUsernameEditing");
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
    applyMediaDefaults();
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

  /**
   * Defaults for the media flag families. The upload family additionally translates the retired
   * {@code featureAttachmentUploadDisabled} key once (ADR-015): a stored {@code true} means the
   * tenant had switched uploads off, so absent upload keys default to {@code false} instead of the
   * documented {@code true}. Explicitly stored media keys always win over the legacy translation.
   */
  private void applyMediaDefaults() {
    boolean legacyUploadDisabled = Boolean.TRUE.equals(legacyFeatureAttachmentUploadDisabled);
    if (featureMediaUploadEnabled == null) {
      featureMediaUploadEnabled =
          !legacyUploadDisabled && BOOLEAN_FIELD_DEFAULTS.get("featureMediaUploadEnabled");
    }
    if (featureMediaUploadAnonymousChatsEnabled == null) {
      featureMediaUploadAnonymousChatsEnabled =
          !legacyUploadDisabled
              && BOOLEAN_FIELD_DEFAULTS.get("featureMediaUploadAnonymousChatsEnabled");
    }
    if (featureMediaUploadOneOnOneChatsEnabled == null) {
      featureMediaUploadOneOnOneChatsEnabled =
          !legacyUploadDisabled
              && BOOLEAN_FIELD_DEFAULTS.get("featureMediaUploadOneOnOneChatsEnabled");
    }
    if (featureMediaUploadGroupChatsEnabled == null) {
      featureMediaUploadGroupChatsEnabled =
          !legacyUploadDisabled
              && BOOLEAN_FIELD_DEFAULTS.get("featureMediaUploadGroupChatsEnabled");
    }
    if (featureMediaUploadSupervisionChatsEnabled == null) {
      featureMediaUploadSupervisionChatsEnabled =
          !legacyUploadDisabled
              && BOOLEAN_FIELD_DEFAULTS.get("featureMediaUploadSupervisionChatsEnabled");
    }
    if (featureMediaInlineDisplayEnabled == null) {
      featureMediaInlineDisplayEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaInlineDisplayEnabled");
    }
    if (featureMediaInlineDisplayAnonymousChatsEnabled == null) {
      featureMediaInlineDisplayAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaInlineDisplayAnonymousChatsEnabled");
    }
    if (featureMediaInlineDisplayOneOnOneChatsEnabled == null) {
      featureMediaInlineDisplayOneOnOneChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaInlineDisplayOneOnOneChatsEnabled");
    }
    if (featureMediaInlineDisplayGroupChatsEnabled == null) {
      featureMediaInlineDisplayGroupChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaInlineDisplayGroupChatsEnabled");
    }
    if (featureMediaInlineDisplaySupervisionChatsEnabled == null) {
      featureMediaInlineDisplaySupervisionChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaInlineDisplaySupervisionChatsEnabled");
    }
    if (featureMediaAiScanEnabled == null) {
      featureMediaAiScanEnabled = BOOLEAN_FIELD_DEFAULTS.get("featureMediaAiScanEnabled");
    }
    if (featureMediaAiScanAnonymousChatsEnabled == null) {
      featureMediaAiScanAnonymousChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaAiScanAnonymousChatsEnabled");
    }
    if (featureMediaAiScanOneOnOneChatsEnabled == null) {
      featureMediaAiScanOneOnOneChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaAiScanOneOnOneChatsEnabled");
    }
    if (featureMediaAiScanGroupChatsEnabled == null) {
      featureMediaAiScanGroupChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaAiScanGroupChatsEnabled");
    }
    if (featureMediaAiScanSupervisionChatsEnabled == null) {
      featureMediaAiScanSupervisionChatsEnabled =
          BOOLEAN_FIELD_DEFAULTS.get("featureMediaAiScanSupervisionChatsEnabled");
    }
  }
}
