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
    } else {
      existingSettingsToCompare = JsonConverter.convertFromJson(existingTenant.getSettings());
    }
    return existingSettingsToCompare;
  }

  boolean nullAsFalse(Boolean value) {
    return value != null && value;
  }
}
