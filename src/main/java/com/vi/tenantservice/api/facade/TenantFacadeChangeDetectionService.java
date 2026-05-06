package com.vi.tenantservice.api.facade;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantContent;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSetting;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.util.JsonConverter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TenantFacadeChangeDetectionService {

  public List<TenantSetting> determineChangedSettings(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    Settings inputSettings = sanitizedTenantDTO.getSettings();
    if (inputSettings != null) {
      TenantSettings existingSettingsToCompare = getExistingTenantSettings(existingTenant);
      return getChangedTenantSettings(inputSettings, existingSettingsToCompare);
    } else {
      return Lists.newArrayList();
    }
  }

  public List<TenantContent> determineChangedContent(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    var content = sanitizedTenantDTO.getContent();
    List<TenantContent> result = Lists.newArrayList();
    if (content != null) {
      determineChangedContent(existingTenant, content, result);
      return result;
    } else {
      return Lists.newArrayList();
    }
  }

  private void determineChangedContent(
      TenantEntity existingTenant, MultilingualContent content, List<TenantContent> result) {
    if (isChanged(
        JsonConverter.convertToJson(content.getImpressum()),
        existingTenant.getContentImpressum())) {
      result.add(TenantContent.IMPRESSUM);
    }
    if (isChanged(
        JsonConverter.convertToJson(content.getPrivacy()), existingTenant.getContentPrivacy())) {
      result.add(TenantContent.PRIVACY);
    }
    if (isChanged(
        JsonConverter.convertToJson(content.getTermsAndConditions()),
        existingTenant.getContentTermsAndConditions())) {
      result.add(TenantContent.TERMS_AND_CONDITIONS);
    }
  }

  private List<TenantSetting> getChangedTenantSettings(
      Settings inputSettings, TenantSettings existingSettingsToCompare) {
    List<TenantSetting> resultList = Lists.newArrayList();
    if (isChanged(
        inputSettings.getFeatureDemographicsEnabled(),
        existingSettingsToCompare.isFeatureDemographicsEnabled())) {
      resultList.add(TenantSetting.FEATURE_DEMOGRAPHICS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureTopicsEnabled(),
        existingSettingsToCompare.isFeatureTopicsEnabled())) {
      resultList.add(TenantSetting.FEATURE_TOPICS_ENABLED);
    }
    if (isChanged(
        inputSettings.getTopicsInRegistrationEnabled(),
        existingSettingsToCompare.isTopicsInRegistrationEnabled())) {
      resultList.add(TenantSetting.ENABLE_TOPICS_IN_REGISTRATION);
    }
    if (isChanged(
        inputSettings.getFeatureStatisticsEnabled(),
        existingSettingsToCompare.isFeatureStatisticsEnabled())) {
      resultList.add(TenantSetting.FEATURE_STATISTICS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureAppointmentsEnabled(),
        existingSettingsToCompare.isFeatureAppointmentsEnabled())) {
      resultList.add(TenantSetting.FEATURE_APPOINTMENTS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureGroupChatV2Enabled(),
        existingSettingsToCompare.isFeatureGroupChatV2Enabled())) {
      resultList.add(TenantSetting.FEATURE_GROUP_CHAT_V2_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureToolsEnabled(),
        existingSettingsToCompare.isFeatureToolsEnabled())) {
      resultList.add(TenantSetting.FEATURE_TOOLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAnonymousChatEnabled())
        != existingSettingsToCompare.isFeatureAnonymousChatEnabled()) {
      resultList.add(TenantSetting.FEATURE_ANONYMOUS_CHAT_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureCallsEnabled())
        != existingSettingsToCompare.isFeatureCallsEnabled()) {
      resultList.add(TenantSetting.FEATURE_CALLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureSupervisionEnabled())
        != existingSettingsToCompare.isFeatureSupervisionEnabled()) {
      resultList.add(TenantSetting.FEATURE_SUPERVISION_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureSupervisionAnonymousChatsEnabled())
        != existingSettingsToCompare.isFeatureSupervisionAnonymousChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_SUPERVISION_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureSupervisionOneOnOneChatsEnabled())
        != existingSettingsToCompare.isFeatureSupervisionOneOnOneChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_SUPERVISION_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsEnabled())
        != existingSettingsToCompare.isFeatureAudioCallsEnabled()) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsAnonymousChatsEnabled())
        != existingSettingsToCompare.isFeatureAudioCallsAnonymousChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsOneOnOneChatsEnabled())
        != existingSettingsToCompare.isFeatureAudioCallsOneOnOneChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsGroupChatsEnabled())
        != existingSettingsToCompare.isFeatureAudioCallsGroupChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsSupervisionChatsEnabled())
        != existingSettingsToCompare.isFeatureAudioCallsSupervisionChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_SUPERVISION_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsEnabled())
        != existingSettingsToCompare.isFeatureVideoCallsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsAnonymousChatsEnabled())
        != existingSettingsToCompare.isFeatureVideoCallsAnonymousChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsOneOnOneChatsEnabled())
        != existingSettingsToCompare.isFeatureVideoCallsOneOnOneChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsGroupChatsEnabled())
        != existingSettingsToCompare.isFeatureVideoCallsGroupChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsSupervisionChatsEnabled())
        != existingSettingsToCompare.isFeatureVideoCallsSupervisionChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_SUPERVISION_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsEnabled())
        != existingSettingsToCompare.isFeatureThreadsEnabled()) {
      resultList.add(TenantSetting.FEATURE_THREADS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsAnonymousChatsEnabled())
        != existingSettingsToCompare.isFeatureThreadsAnonymousChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_THREADS_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsGroupChatsEnabled())
        != existingSettingsToCompare.isFeatureThreadsGroupChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_THREADS_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsOneOnOneEnabled())
        != existingSettingsToCompare.isFeatureThreadsOneOnOneEnabled()) {
      resultList.add(TenantSetting.FEATURE_THREADS_ONE_ON_ONE_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsSupervisionChatsEnabled())
        != existingSettingsToCompare.isFeatureThreadsSupervisionChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_THREADS_SUPERVISION_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesEnabled())
        != existingSettingsToCompare.isFeatureVoiceMessagesEnabled()) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesAnonymousChatsEnabled())
        != existingSettingsToCompare.isFeatureVoiceMessagesAnonymousChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesOneOnOneChatsEnabled())
        != existingSettingsToCompare.isFeatureVoiceMessagesOneOnOneChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesGroupChatsEnabled())
        != existingSettingsToCompare.isFeatureVoiceMessagesGroupChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesSupervisionChatsEnabled())
        != existingSettingsToCompare.isFeatureVoiceMessagesSupervisionChatsEnabled()) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_SUPERVISION_CHATS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureAttachmentUploadDisabled(),
        existingSettingsToCompare.isFeatureAttachmentUploadDisabled())) {
      resultList.add(TenantSetting.FEATURE_ATTACHMENT_UPLOAD_DISABLED);
    }
    if (isChangedIgnoringOrder(
        inputSettings.getActiveLanguages(), existingSettingsToCompare.getActiveLanguages())) {
      resultList.add(TenantSetting.FEATURE_ACTIVE_LANGUAGES);
    }
    return resultList;
  }

  private boolean isChanged(String newContent, String existingContent) {
    return !StringUtils.equals(newContent, existingContent);
  }

  private boolean isChanged(Boolean inputSettings, boolean existingSettingsToCompare) {
    return nullAsFalse(inputSettings) != existingSettingsToCompare;
  }

  private boolean isChangedIgnoringOrder(
      List<String> updatedSettings, List<String> existingSettings) {
    return !areEqualIgnoringOrder(updatedSettings, existingSettings);
  }

  private boolean areEqualIgnoringOrder(
      List<String> updatedSettings, List<String> existingSettings) {
    if (updatedSettings == null || existingSettings == null) {
      return updatedSettings == existingSettings;
    }
    return Sets.newHashSet(updatedSettings).equals(Sets.newHashSet(existingSettings));
  }

  private TenantSettings getExistingTenantSettings(TenantEntity existingTenant) {
    TenantSettings existingSettingsToCompare;
    if (existingTenant.getSettings() == null) {
      existingSettingsToCompare = new TenantSettings();
      // Default should be enabled if missing completely.
      existingSettingsToCompare.setFeatureAnonymousChatEnabled(true);
      existingSettingsToCompare.setFeatureCallsEnabled(true);
      existingSettingsToCompare.setFeatureSupervisionEnabled(true);
      existingSettingsToCompare.setFeatureSupervisionAnonymousChatsEnabled(true);
      existingSettingsToCompare.setFeatureSupervisionOneOnOneChatsEnabled(true);
      existingSettingsToCompare.setFeatureAudioCallsEnabled(true);
      existingSettingsToCompare.setFeatureAudioCallsAnonymousChatsEnabled(true);
      existingSettingsToCompare.setFeatureAudioCallsOneOnOneChatsEnabled(true);
      existingSettingsToCompare.setFeatureAudioCallsGroupChatsEnabled(true);
      existingSettingsToCompare.setFeatureAudioCallsSupervisionChatsEnabled(true);
      existingSettingsToCompare.setFeatureVideoCallsEnabled(true);
      existingSettingsToCompare.setFeatureVideoCallsAnonymousChatsEnabled(true);
      existingSettingsToCompare.setFeatureVideoCallsOneOnOneChatsEnabled(true);
      existingSettingsToCompare.setFeatureVideoCallsGroupChatsEnabled(true);
      existingSettingsToCompare.setFeatureVideoCallsSupervisionChatsEnabled(true);
      existingSettingsToCompare.setFeatureThreadsEnabled(true);
      existingSettingsToCompare.setFeatureThreadsAnonymousChatsEnabled(true);
      existingSettingsToCompare.setFeatureThreadsGroupChatsEnabled(true);
      existingSettingsToCompare.setFeatureThreadsOneOnOneEnabled(true);
      existingSettingsToCompare.setFeatureThreadsSupervisionChatsEnabled(true);
      existingSettingsToCompare.setFeatureVoiceMessagesEnabled(true);
      existingSettingsToCompare.setFeatureVoiceMessagesAnonymousChatsEnabled(true);
      existingSettingsToCompare.setFeatureVoiceMessagesOneOnOneChatsEnabled(true);
      existingSettingsToCompare.setFeatureVoiceMessagesGroupChatsEnabled(true);
      existingSettingsToCompare.setFeatureVoiceMessagesSupervisionChatsEnabled(true);
    } else {
      final String settingsJson = existingTenant.getSettings();
      existingSettingsToCompare = JsonConverter.convertFromJson(settingsJson);
      // Default should be enabled if the setting is not present in stored JSON (backward compat).
      if (!settingsJson.contains("\"featureAnonymousChatEnabled\"")) {
        existingSettingsToCompare.setFeatureAnonymousChatEnabled(true);
      }
      if (!settingsJson.contains("\"featureCallsEnabled\"")) {
        existingSettingsToCompare.setFeatureCallsEnabled(true);
      }
      if (!settingsJson.contains("\"featureSupervisionEnabled\"")) {
        existingSettingsToCompare.setFeatureSupervisionEnabled(true);
      }
      if (!settingsJson.contains("\"featureSupervisionAnonymousChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureSupervisionAnonymousChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureSupervisionOneOnOneChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureSupervisionOneOnOneChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureAudioCallsEnabled\"")) {
        existingSettingsToCompare.setFeatureAudioCallsEnabled(true);
      }
      if (!settingsJson.contains("\"featureAudioCallsAnonymousChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureAudioCallsAnonymousChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureAudioCallsOneOnOneChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureAudioCallsOneOnOneChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureAudioCallsGroupChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureAudioCallsGroupChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureAudioCallsSupervisionChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureAudioCallsSupervisionChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVideoCallsEnabled\"")) {
        existingSettingsToCompare.setFeatureVideoCallsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVideoCallsAnonymousChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVideoCallsAnonymousChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVideoCallsOneOnOneChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVideoCallsOneOnOneChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVideoCallsGroupChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVideoCallsGroupChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVideoCallsSupervisionChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVideoCallsSupervisionChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureThreadsEnabled\"")) {
        existingSettingsToCompare.setFeatureThreadsEnabled(true);
      }
      if (!settingsJson.contains("\"featureThreadsAnonymousChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureThreadsAnonymousChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureThreadsGroupChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureThreadsGroupChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureThreadsOneOnOneEnabled\"")) {
        existingSettingsToCompare.setFeatureThreadsOneOnOneEnabled(true);
      }
      if (!settingsJson.contains("\"featureThreadsSupervisionChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureThreadsSupervisionChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVoiceMessagesEnabled\"")) {
        existingSettingsToCompare.setFeatureVoiceMessagesEnabled(true);
      }
      if (!settingsJson.contains("\"featureVoiceMessagesAnonymousChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVoiceMessagesAnonymousChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVoiceMessagesOneOnOneChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVoiceMessagesOneOnOneChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVoiceMessagesGroupChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVoiceMessagesGroupChatsEnabled(true);
      }
      if (!settingsJson.contains("\"featureVoiceMessagesSupervisionChatsEnabled\"")) {
        existingSettingsToCompare.setFeatureVoiceMessagesSupervisionChatsEnabled(true);
      }
    }
    return existingSettingsToCompare;
  }

  boolean nullAsFalse(Boolean value) {
    return value != null && value;
  }

  boolean nullAsTrue(Boolean value) {
    return value == null || value;
  }
}
