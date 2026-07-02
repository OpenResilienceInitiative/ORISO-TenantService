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
        existingSettingsToCompare.getFeatureDemographicsEnabled())) {
      resultList.add(TenantSetting.FEATURE_DEMOGRAPHICS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureTopicsEnabled(),
        existingSettingsToCompare.getFeatureTopicsEnabled())) {
      resultList.add(TenantSetting.FEATURE_TOPICS_ENABLED);
    }
    if (isChanged(
        inputSettings.getTopicsInRegistrationEnabled(),
        existingSettingsToCompare.getTopicsInRegistrationEnabled())) {
      resultList.add(TenantSetting.ENABLE_TOPICS_IN_REGISTRATION);
    }
    if (isChanged(
        inputSettings.getFeatureStatisticsEnabled(),
        existingSettingsToCompare.getFeatureStatisticsEnabled())) {
      resultList.add(TenantSetting.FEATURE_STATISTICS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureAppointmentsEnabled(),
        existingSettingsToCompare.getFeatureAppointmentsEnabled())) {
      resultList.add(TenantSetting.FEATURE_APPOINTMENTS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureGroupChatV2Enabled(),
        existingSettingsToCompare.getFeatureGroupChatV2Enabled())) {
      resultList.add(TenantSetting.FEATURE_GROUP_CHAT_V2_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureToolsEnabled(),
        existingSettingsToCompare.getFeatureToolsEnabled())) {
      resultList.add(TenantSetting.FEATURE_TOOLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAnonymousChatEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureAnonymousChatEnabled())) {
      resultList.add(TenantSetting.FEATURE_ANONYMOUS_CHAT_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureCallsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureCallsEnabled())) {
      resultList.add(TenantSetting.FEATURE_CALLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureSupervisionEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureSupervisionEnabled())) {
      resultList.add(TenantSetting.FEATURE_SUPERVISION_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureSupervisionAnonymousChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureSupervisionAnonymousChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_SUPERVISION_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureSupervisionOneOnOneChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureSupervisionOneOnOneChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_SUPERVISION_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureAudioCallsEnabled())) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsAnonymousChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureAudioCallsAnonymousChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsOneOnOneChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureAudioCallsOneOnOneChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsGroupChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureAudioCallsGroupChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureAudioCallsSupervisionChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureAudioCallsSupervisionChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_AUDIO_CALLS_SUPERVISION_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVideoCallsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsAnonymousChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVideoCallsAnonymousChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsOneOnOneChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVideoCallsOneOnOneChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsGroupChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVideoCallsGroupChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVideoCallsSupervisionChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVideoCallsSupervisionChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VIDEO_CALLS_SUPERVISION_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureThreadsEnabled())) {
      resultList.add(TenantSetting.FEATURE_THREADS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsAnonymousChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureThreadsAnonymousChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_THREADS_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsGroupChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureThreadsGroupChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_THREADS_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsOneOnOneEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureThreadsOneOnOneEnabled())) {
      resultList.add(TenantSetting.FEATURE_THREADS_ONE_ON_ONE_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureThreadsSupervisionChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureThreadsSupervisionChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_THREADS_SUPERVISION_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVoiceMessagesEnabled())) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesAnonymousChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVoiceMessagesAnonymousChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_ANONYMOUS_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesOneOnOneChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVoiceMessagesOneOnOneChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_ONE_ON_ONE_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesGroupChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVoiceMessagesGroupChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_GROUP_CHATS_ENABLED);
    }
    if (nullAsTrue(inputSettings.getFeatureVoiceMessagesSupervisionChatsEnabled())
        != nullAsTrue(existingSettingsToCompare.getFeatureVoiceMessagesSupervisionChatsEnabled())) {
      resultList.add(TenantSetting.FEATURE_VOICE_MESSAGES_SUPERVISION_CHATS_ENABLED);
    }
    if (isChanged(
        inputSettings.getFeatureAttachmentUploadDisabled(),
        existingSettingsToCompare.getFeatureAttachmentUploadDisabled())) {
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

  private boolean isChanged(Boolean inputSettings, Boolean existingSettingsToCompare) {
    return nullAsFalse(inputSettings) != nullAsFalse(existingSettingsToCompare);
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
    if (existingTenant.getSettings() == null) {
      return new TenantSettings().applyDefaults();
    }
    return JsonConverter.convertFromJson(existingTenant.getSettings()).applyDefaults();
  }

  boolean nullAsFalse(Boolean value) {
    return value != null && value;
  }

  boolean nullAsTrue(Boolean value) {
    return value == null || value;
  }
}
