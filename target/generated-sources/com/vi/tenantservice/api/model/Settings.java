package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Settings
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class Settings {

  private Boolean featureStatisticsEnabled;

  private Boolean featureTopicsEnabled;

  private Boolean topicsInRegistrationEnabled;

  private Boolean featureDemographicsEnabled;

  private Boolean featureAppointmentsEnabled;

  private Boolean featureGroupChatV2Enabled;

  private Boolean featureToolsEnabled;

  private Boolean featureAttachmentUploadDisabled;

  private String featureToolsOICDToken;

  @Valid
  private List<String> activeLanguages;

  private Boolean showAskerProfile = false;

  private Boolean isVideoCallAllowed = false;

  private ConsultingTypePatchDTO extendedSettings;

  private Boolean featureCentralDataProtectionTemplateEnabled;

  public Settings featureStatisticsEnabled(Boolean featureStatisticsEnabled) {
    this.featureStatisticsEnabled = featureStatisticsEnabled;
    return this;
  }

  /**
   * Get featureStatisticsEnabled
   * @return featureStatisticsEnabled
  */
  
  @Schema(name = "featureStatisticsEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureStatisticsEnabled")
  public Boolean getFeatureStatisticsEnabled() {
    return featureStatisticsEnabled;
  }

  public void setFeatureStatisticsEnabled(Boolean featureStatisticsEnabled) {
    this.featureStatisticsEnabled = featureStatisticsEnabled;
  }

  public Settings featureTopicsEnabled(Boolean featureTopicsEnabled) {
    this.featureTopicsEnabled = featureTopicsEnabled;
    return this;
  }

  /**
   * Get featureTopicsEnabled
   * @return featureTopicsEnabled
  */
  
  @Schema(name = "featureTopicsEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureTopicsEnabled")
  public Boolean getFeatureTopicsEnabled() {
    return featureTopicsEnabled;
  }

  public void setFeatureTopicsEnabled(Boolean featureTopicsEnabled) {
    this.featureTopicsEnabled = featureTopicsEnabled;
  }

  public Settings topicsInRegistrationEnabled(Boolean topicsInRegistrationEnabled) {
    this.topicsInRegistrationEnabled = topicsInRegistrationEnabled;
    return this;
  }

  /**
   * Get topicsInRegistrationEnabled
   * @return topicsInRegistrationEnabled
  */
  
  @Schema(name = "topicsInRegistrationEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("topicsInRegistrationEnabled")
  public Boolean getTopicsInRegistrationEnabled() {
    return topicsInRegistrationEnabled;
  }

  public void setTopicsInRegistrationEnabled(Boolean topicsInRegistrationEnabled) {
    this.topicsInRegistrationEnabled = topicsInRegistrationEnabled;
  }

  public Settings featureDemographicsEnabled(Boolean featureDemographicsEnabled) {
    this.featureDemographicsEnabled = featureDemographicsEnabled;
    return this;
  }

  /**
   * Get featureDemographicsEnabled
   * @return featureDemographicsEnabled
  */
  
  @Schema(name = "featureDemographicsEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureDemographicsEnabled")
  public Boolean getFeatureDemographicsEnabled() {
    return featureDemographicsEnabled;
  }

  public void setFeatureDemographicsEnabled(Boolean featureDemographicsEnabled) {
    this.featureDemographicsEnabled = featureDemographicsEnabled;
  }

  public Settings featureAppointmentsEnabled(Boolean featureAppointmentsEnabled) {
    this.featureAppointmentsEnabled = featureAppointmentsEnabled;
    return this;
  }

  /**
   * Get featureAppointmentsEnabled
   * @return featureAppointmentsEnabled
  */
  
  @Schema(name = "featureAppointmentsEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAppointmentsEnabled")
  public Boolean getFeatureAppointmentsEnabled() {
    return featureAppointmentsEnabled;
  }

  public void setFeatureAppointmentsEnabled(Boolean featureAppointmentsEnabled) {
    this.featureAppointmentsEnabled = featureAppointmentsEnabled;
  }

  public Settings featureGroupChatV2Enabled(Boolean featureGroupChatV2Enabled) {
    this.featureGroupChatV2Enabled = featureGroupChatV2Enabled;
    return this;
  }

  /**
   * Get featureGroupChatV2Enabled
   * @return featureGroupChatV2Enabled
  */
  
  @Schema(name = "featureGroupChatV2Enabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureGroupChatV2Enabled")
  public Boolean getFeatureGroupChatV2Enabled() {
    return featureGroupChatV2Enabled;
  }

  public void setFeatureGroupChatV2Enabled(Boolean featureGroupChatV2Enabled) {
    this.featureGroupChatV2Enabled = featureGroupChatV2Enabled;
  }

  public Settings featureToolsEnabled(Boolean featureToolsEnabled) {
    this.featureToolsEnabled = featureToolsEnabled;
    return this;
  }

  /**
   * Get featureToolsEnabled
   * @return featureToolsEnabled
  */
  
  @Schema(name = "featureToolsEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureToolsEnabled")
  public Boolean getFeatureToolsEnabled() {
    return featureToolsEnabled;
  }

  public void setFeatureToolsEnabled(Boolean featureToolsEnabled) {
    this.featureToolsEnabled = featureToolsEnabled;
  }

  public Settings featureAttachmentUploadDisabled(Boolean featureAttachmentUploadDisabled) {
    this.featureAttachmentUploadDisabled = featureAttachmentUploadDisabled;
    return this;
  }

  /**
   * Get featureAttachmentUploadDisabled
   * @return featureAttachmentUploadDisabled
  */
  
  @Schema(name = "featureAttachmentUploadDisabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAttachmentUploadDisabled")
  public Boolean getFeatureAttachmentUploadDisabled() {
    return featureAttachmentUploadDisabled;
  }

  public void setFeatureAttachmentUploadDisabled(Boolean featureAttachmentUploadDisabled) {
    this.featureAttachmentUploadDisabled = featureAttachmentUploadDisabled;
  }

  public Settings featureToolsOICDToken(String featureToolsOICDToken) {
    this.featureToolsOICDToken = featureToolsOICDToken;
    return this;
  }

  /**
   * Get featureToolsOICDToken
   * @return featureToolsOICDToken
  */
  
  @Schema(name = "featureToolsOICDToken", example = "1234-1234-1234-1234", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureToolsOICDToken")
  public String getFeatureToolsOICDToken() {
    return featureToolsOICDToken;
  }

  public void setFeatureToolsOICDToken(String featureToolsOICDToken) {
    this.featureToolsOICDToken = featureToolsOICDToken;
  }

  public Settings activeLanguages(List<String> activeLanguages) {
    this.activeLanguages = activeLanguages;
    return this;
  }

  public Settings addActiveLanguagesItem(String activeLanguagesItem) {
    if (this.activeLanguages == null) {
      this.activeLanguages = new ArrayList<>();
    }
    this.activeLanguages.add(activeLanguagesItem);
    return this;
  }

  /**
   * Get activeLanguages
   * @return activeLanguages
  */
  
  @Schema(name = "activeLanguages", example = "[en, de, fr]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("activeLanguages")
  public List<String> getActiveLanguages() {
    return activeLanguages;
  }

  public void setActiveLanguages(List<String> activeLanguages) {
    this.activeLanguages = activeLanguages;
  }

  public Settings showAskerProfile(Boolean showAskerProfile) {
    this.showAskerProfile = showAskerProfile;
    return this;
  }

  /**
   * Get showAskerProfile
   * @return showAskerProfile
  */
  
  @Schema(name = "showAskerProfile", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("showAskerProfile")
  public Boolean getShowAskerProfile() {
    return showAskerProfile;
  }

  public void setShowAskerProfile(Boolean showAskerProfile) {
    this.showAskerProfile = showAskerProfile;
  }

  public Settings isVideoCallAllowed(Boolean isVideoCallAllowed) {
    this.isVideoCallAllowed = isVideoCallAllowed;
    return this;
  }

  /**
   * Get isVideoCallAllowed
   * @return isVideoCallAllowed
  */
  
  @Schema(name = "isVideoCallAllowed", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("isVideoCallAllowed")
  public Boolean getIsVideoCallAllowed() {
    return isVideoCallAllowed;
  }

  public void setIsVideoCallAllowed(Boolean isVideoCallAllowed) {
    this.isVideoCallAllowed = isVideoCallAllowed;
  }

  public Settings extendedSettings(ConsultingTypePatchDTO extendedSettings) {
    this.extendedSettings = extendedSettings;
    return this;
  }

  /**
   * Get extendedSettings
   * @return extendedSettings
  */
  @Valid 
  @Schema(name = "extendedSettings", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("extendedSettings")
  public ConsultingTypePatchDTO getExtendedSettings() {
    return extendedSettings;
  }

  public void setExtendedSettings(ConsultingTypePatchDTO extendedSettings) {
    this.extendedSettings = extendedSettings;
  }

  public Settings featureCentralDataProtectionTemplateEnabled(Boolean featureCentralDataProtectionTemplateEnabled) {
    this.featureCentralDataProtectionTemplateEnabled = featureCentralDataProtectionTemplateEnabled;
    return this;
  }

  /**
   * Get featureCentralDataProtectionTemplateEnabled
   * @return featureCentralDataProtectionTemplateEnabled
  */
  
  @Schema(name = "featureCentralDataProtectionTemplateEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureCentralDataProtectionTemplateEnabled")
  public Boolean getFeatureCentralDataProtectionTemplateEnabled() {
    return featureCentralDataProtectionTemplateEnabled;
  }

  public void setFeatureCentralDataProtectionTemplateEnabled(Boolean featureCentralDataProtectionTemplateEnabled) {
    this.featureCentralDataProtectionTemplateEnabled = featureCentralDataProtectionTemplateEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Settings settings = (Settings) o;
    return Objects.equals(this.featureStatisticsEnabled, settings.featureStatisticsEnabled) &&
        Objects.equals(this.featureTopicsEnabled, settings.featureTopicsEnabled) &&
        Objects.equals(this.topicsInRegistrationEnabled, settings.topicsInRegistrationEnabled) &&
        Objects.equals(this.featureDemographicsEnabled, settings.featureDemographicsEnabled) &&
        Objects.equals(this.featureAppointmentsEnabled, settings.featureAppointmentsEnabled) &&
        Objects.equals(this.featureGroupChatV2Enabled, settings.featureGroupChatV2Enabled) &&
        Objects.equals(this.featureToolsEnabled, settings.featureToolsEnabled) &&
        Objects.equals(this.featureAttachmentUploadDisabled, settings.featureAttachmentUploadDisabled) &&
        Objects.equals(this.featureToolsOICDToken, settings.featureToolsOICDToken) &&
        Objects.equals(this.activeLanguages, settings.activeLanguages) &&
        Objects.equals(this.showAskerProfile, settings.showAskerProfile) &&
        Objects.equals(this.isVideoCallAllowed, settings.isVideoCallAllowed) &&
        Objects.equals(this.extendedSettings, settings.extendedSettings) &&
        Objects.equals(this.featureCentralDataProtectionTemplateEnabled, settings.featureCentralDataProtectionTemplateEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureStatisticsEnabled, featureTopicsEnabled, topicsInRegistrationEnabled, featureDemographicsEnabled, featureAppointmentsEnabled, featureGroupChatV2Enabled, featureToolsEnabled, featureAttachmentUploadDisabled, featureToolsOICDToken, activeLanguages, showAskerProfile, isVideoCallAllowed, extendedSettings, featureCentralDataProtectionTemplateEnabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Settings {\n");
    sb.append("    featureStatisticsEnabled: ").append(toIndentedString(featureStatisticsEnabled)).append("\n");
    sb.append("    featureTopicsEnabled: ").append(toIndentedString(featureTopicsEnabled)).append("\n");
    sb.append("    topicsInRegistrationEnabled: ").append(toIndentedString(topicsInRegistrationEnabled)).append("\n");
    sb.append("    featureDemographicsEnabled: ").append(toIndentedString(featureDemographicsEnabled)).append("\n");
    sb.append("    featureAppointmentsEnabled: ").append(toIndentedString(featureAppointmentsEnabled)).append("\n");
    sb.append("    featureGroupChatV2Enabled: ").append(toIndentedString(featureGroupChatV2Enabled)).append("\n");
    sb.append("    featureToolsEnabled: ").append(toIndentedString(featureToolsEnabled)).append("\n");
    sb.append("    featureAttachmentUploadDisabled: ").append(toIndentedString(featureAttachmentUploadDisabled)).append("\n");
    sb.append("    featureToolsOICDToken: ").append(toIndentedString(featureToolsOICDToken)).append("\n");
    sb.append("    activeLanguages: ").append(toIndentedString(activeLanguages)).append("\n");
    sb.append("    showAskerProfile: ").append(toIndentedString(showAskerProfile)).append("\n");
    sb.append("    isVideoCallAllowed: ").append(toIndentedString(isVideoCallAllowed)).append("\n");
    sb.append("    extendedSettings: ").append(toIndentedString(extendedSettings)).append("\n");
    sb.append("    featureCentralDataProtectionTemplateEnabled: ").append(toIndentedString(featureCentralDataProtectionTemplateEnabled)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

