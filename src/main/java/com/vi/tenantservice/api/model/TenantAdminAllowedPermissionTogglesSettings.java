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
  Boolean anonymousChat;
  Boolean calls;
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
}
