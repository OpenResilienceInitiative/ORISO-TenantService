package com.vi.tenantservice.api.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantSettings {

  boolean featureStatisticsEnabled;
  boolean featureTopicsEnabled;
  boolean topicsInRegistrationEnabled;
  boolean featureDemographicsEnabled;
  boolean featureAppointmentsEnabled;
  boolean featureGroupChatV2Enabled;
  boolean featureToolsEnabled;
  boolean featureAnonymousChatEnabled;
  boolean featureCallsEnabled;
  boolean featureSupervisionEnabled;
  boolean featureSupervisionAnonymousChatsEnabled;
  boolean featureSupervisionOneOnOneChatsEnabled;
  boolean featureAudioCallsEnabled;
  boolean featureAudioCallsAnonymousChatsEnabled;
  boolean featureAudioCallsOneOnOneChatsEnabled;
  boolean featureAudioCallsGroupChatsEnabled;
  boolean featureAudioCallsSupervisionChatsEnabled;
  boolean featureVideoCallsEnabled;
  boolean featureVideoCallsAnonymousChatsEnabled;
  boolean featureVideoCallsOneOnOneChatsEnabled;
  boolean featureVideoCallsGroupChatsEnabled;
  boolean featureVideoCallsSupervisionChatsEnabled;
  boolean featureThreadsEnabled;
  boolean featureThreadsAnonymousChatsEnabled;
  boolean featureThreadsGroupChatsEnabled;
  boolean featureThreadsOneOnOneEnabled;
  boolean featureThreadsSupervisionChatsEnabled;
  boolean featureVoiceMessagesEnabled;
  boolean featureVoiceMessagesAnonymousChatsEnabled;
  boolean featureVoiceMessagesOneOnOneChatsEnabled;
  boolean featureVoiceMessagesGroupChatsEnabled;
  boolean featureVoiceMessagesSupervisionChatsEnabled;
  boolean featureSystemNotificationEmailsEnabled;
  TenantSmtpSettings smtp;
  boolean featureAttachmentUploadDisabled;
  boolean isVideoCallAllowed;
  boolean showAskerProfile;

  String featureToolsOIDCToken;
  List<String> activeLanguages;

  boolean featureCentralDataProtectionEnabled;

  boolean featureCentralDataProtectionTemplateEnabled;

  TenantAdminControlsSettings tenantAdminControls;
}
