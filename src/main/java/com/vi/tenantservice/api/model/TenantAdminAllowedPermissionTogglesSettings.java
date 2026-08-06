package com.vi.tenantservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAdminAllowedPermissionTogglesSettings {
  Boolean appearance;
  Boolean anonymousChat;
  Boolean calls;
  Boolean groupChat;
  Boolean supervision;
  Boolean supervisionAnonymousChats;
  Boolean supervisionOneOnOneChats;
  Boolean audioCalls;
  Boolean audioCallsAnonymousChats;
  Boolean audioCallsOneOnOneChats;
  Boolean audioCallsGroupChats;
  Boolean audioCallsSupervisionChats;
  Boolean videoCalls;
  Boolean videoCallsAnonymousChats;
  Boolean videoCallsOneOnOneChats;
  Boolean videoCallsGroupChats;
  Boolean videoCallsSupervisionChats;
  Boolean threads;
  Boolean threadsAnonymousChats;
  Boolean threadsOneOnOneChats;
  Boolean threadsGroupChats;
  Boolean threadsSupervisionChats;
  Boolean voiceMessages;
  Boolean voiceMessagesAnonymousChats;
  Boolean voiceMessagesOneOnOneChats;
  Boolean voiceMessagesGroupChats;
  Boolean voiceMessagesSupervisionChats;
  Boolean mediaUpload;
  Boolean mediaUploadAnonymousChats;
  Boolean mediaUploadOneOnOneChats;
  Boolean mediaUploadGroupChats;
  Boolean mediaUploadSupervisionChats;
  Boolean mediaInlineDisplay;
  Boolean mediaInlineDisplayAnonymousChats;
  Boolean mediaInlineDisplayOneOnOneChats;
  Boolean mediaInlineDisplayGroupChats;
  Boolean mediaInlineDisplaySupervisionChats;
  Boolean mediaAiScan;
  Boolean mediaAiScanAnonymousChats;
  Boolean mediaAiScanOneOnOneChats;
  Boolean mediaAiScanGroupChats;
  Boolean mediaAiScanSupervisionChats;
}
