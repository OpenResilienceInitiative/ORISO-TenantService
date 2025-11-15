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
  boolean featureAttachmentUploadDisabled;
  boolean isVideoCallAllowed;
  boolean showAskerProfile;

  String featureToolsOIDCToken;
  List<String> activeLanguages;

  boolean featureCentralDataProtectionEnabled;

  boolean featureCentralDataProtectionTemplateEnabled;
}
