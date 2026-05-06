package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.SmtpConfig;
import com.vi.tenantservice.api.model.TenantAdminControls;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-11T15:41:30.641955926Z[Etc/UTC]")
public class Settings {

  private Boolean featureStatisticsEnabled;

  private Boolean featureTopicsEnabled;

  private Boolean topicsInRegistrationEnabled;

  private Boolean featureDemographicsEnabled;

  private Boolean featureAppointmentsEnabled;

  private Boolean featureGroupChatV2Enabled;

  private Boolean featureToolsEnabled;

  private Boolean featureAnonymousChatEnabled;

  private Boolean featureCallsEnabled;

  private Boolean featureSupervisionEnabled;

  private Boolean featureSupervisionAnonymousChatsEnabled;

  private Boolean featureSupervisionOneOnOneChatsEnabled;

  private Boolean featureAudioCallsEnabled;

  private Boolean featureAudioCallsAnonymousChatsEnabled;

  private Boolean featureAudioCallsOneOnOneChatsEnabled;

  private Boolean featureAudioCallsGroupChatsEnabled;

  private Boolean featureAudioCallsSupervisionChatsEnabled;

  private Boolean featureVideoCallsEnabled;

  private Boolean featureVideoCallsAnonymousChatsEnabled;

  private Boolean featureVideoCallsOneOnOneChatsEnabled;

  private Boolean featureVideoCallsGroupChatsEnabled;

  private Boolean featureVideoCallsSupervisionChatsEnabled;

  private Boolean featureThreadsEnabled;

  private Boolean featureThreadsAnonymousChatsEnabled;

  private Boolean featureThreadsGroupChatsEnabled;

  private Boolean featureThreadsOneOnOneEnabled;

  private Boolean featureThreadsSupervisionChatsEnabled;

  private Boolean featureVoiceMessagesEnabled;

  private Boolean featureVoiceMessagesAnonymousChatsEnabled;

  private Boolean featureVoiceMessagesOneOnOneChatsEnabled;

  private Boolean featureVoiceMessagesGroupChatsEnabled;

  private Boolean featureVoiceMessagesSupervisionChatsEnabled;

  private Boolean featureAttachmentUploadDisabled;

  private String featureToolsOICDToken;

  @Valid
  private List<String> activeLanguages;

  private Boolean showAskerProfile = false;

  private Boolean isVideoCallAllowed = false;

  private Boolean featureSystemNotificationEmailsEnabled;

  private SmtpConfig smtp;

  private ConsultingTypePatchDTO extendedSettings;

  private Boolean featureCentralDataProtectionTemplateEnabled;

  private TenantAdminControls tenantAdminControls;

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

  public Settings featureAnonymousChatEnabled(Boolean featureAnonymousChatEnabled) {
    this.featureAnonymousChatEnabled = featureAnonymousChatEnabled;
    return this;
  }

  /**
   * Get featureAnonymousChatEnabled
   * @return featureAnonymousChatEnabled
  */
  
  @Schema(name = "featureAnonymousChatEnabled", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAnonymousChatEnabled")
  public Boolean getFeatureAnonymousChatEnabled() {
    return featureAnonymousChatEnabled;
  }

  public void setFeatureAnonymousChatEnabled(Boolean featureAnonymousChatEnabled) {
    this.featureAnonymousChatEnabled = featureAnonymousChatEnabled;
  }

  public Settings featureCallsEnabled(Boolean featureCallsEnabled) {
    this.featureCallsEnabled = featureCallsEnabled;
    return this;
  }

  /**
   * Get featureCallsEnabled
   * @return featureCallsEnabled
  */
  
  @Schema(name = "featureCallsEnabled", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureCallsEnabled")
  public Boolean getFeatureCallsEnabled() {
    return featureCallsEnabled;
  }

  public void setFeatureCallsEnabled(Boolean featureCallsEnabled) {
    this.featureCallsEnabled = featureCallsEnabled;
  }

  public Settings featureSupervisionEnabled(Boolean featureSupervisionEnabled) {
    this.featureSupervisionEnabled = featureSupervisionEnabled;
    return this;
  }

  /**
   * Master toggle for supervision functionality (adding/removing supervisors, supervision views).
   * @return featureSupervisionEnabled
  */
  
  @Schema(name = "featureSupervisionEnabled", example = "true", description = "Master toggle for supervision functionality (adding/removing supervisors, supervision views).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureSupervisionEnabled")
  public Boolean getFeatureSupervisionEnabled() {
    return featureSupervisionEnabled;
  }

  public void setFeatureSupervisionEnabled(Boolean featureSupervisionEnabled) {
    this.featureSupervisionEnabled = featureSupervisionEnabled;
  }

  public Settings featureSupervisionAnonymousChatsEnabled(Boolean featureSupervisionAnonymousChatsEnabled) {
    this.featureSupervisionAnonymousChatsEnabled = featureSupervisionAnonymousChatsEnabled;
    return this;
  }

  /**
   * Enable supervision functionality in anonymous chats.
   * @return featureSupervisionAnonymousChatsEnabled
  */
  
  @Schema(name = "featureSupervisionAnonymousChatsEnabled", example = "true", description = "Enable supervision functionality in anonymous chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureSupervisionAnonymousChatsEnabled")
  public Boolean getFeatureSupervisionAnonymousChatsEnabled() {
    return featureSupervisionAnonymousChatsEnabled;
  }

  public void setFeatureSupervisionAnonymousChatsEnabled(Boolean featureSupervisionAnonymousChatsEnabled) {
    this.featureSupervisionAnonymousChatsEnabled = featureSupervisionAnonymousChatsEnabled;
  }

  public Settings featureSupervisionOneOnOneChatsEnabled(Boolean featureSupervisionOneOnOneChatsEnabled) {
    this.featureSupervisionOneOnOneChatsEnabled = featureSupervisionOneOnOneChatsEnabled;
    return this;
  }

  /**
   * Enable supervision functionality in 1-on-1 chats.
   * @return featureSupervisionOneOnOneChatsEnabled
  */
  
  @Schema(name = "featureSupervisionOneOnOneChatsEnabled", example = "true", description = "Enable supervision functionality in 1-on-1 chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureSupervisionOneOnOneChatsEnabled")
  public Boolean getFeatureSupervisionOneOnOneChatsEnabled() {
    return featureSupervisionOneOnOneChatsEnabled;
  }

  public void setFeatureSupervisionOneOnOneChatsEnabled(Boolean featureSupervisionOneOnOneChatsEnabled) {
    this.featureSupervisionOneOnOneChatsEnabled = featureSupervisionOneOnOneChatsEnabled;
  }

  public Settings featureAudioCallsEnabled(Boolean featureAudioCallsEnabled) {
    this.featureAudioCallsEnabled = featureAudioCallsEnabled;
    return this;
  }

  /**
   * Master toggle for audio call button availability (all chat types).
   * @return featureAudioCallsEnabled
  */
  
  @Schema(name = "featureAudioCallsEnabled", example = "true", description = "Master toggle for audio call button availability (all chat types).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAudioCallsEnabled")
  public Boolean getFeatureAudioCallsEnabled() {
    return featureAudioCallsEnabled;
  }

  public void setFeatureAudioCallsEnabled(Boolean featureAudioCallsEnabled) {
    this.featureAudioCallsEnabled = featureAudioCallsEnabled;
  }

  public Settings featureAudioCallsAnonymousChatsEnabled(Boolean featureAudioCallsAnonymousChatsEnabled) {
    this.featureAudioCallsAnonymousChatsEnabled = featureAudioCallsAnonymousChatsEnabled;
    return this;
  }

  /**
   * Enable audio call button in anonymous chats.
   * @return featureAudioCallsAnonymousChatsEnabled
  */
  
  @Schema(name = "featureAudioCallsAnonymousChatsEnabled", example = "true", description = "Enable audio call button in anonymous chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAudioCallsAnonymousChatsEnabled")
  public Boolean getFeatureAudioCallsAnonymousChatsEnabled() {
    return featureAudioCallsAnonymousChatsEnabled;
  }

  public void setFeatureAudioCallsAnonymousChatsEnabled(Boolean featureAudioCallsAnonymousChatsEnabled) {
    this.featureAudioCallsAnonymousChatsEnabled = featureAudioCallsAnonymousChatsEnabled;
  }

  public Settings featureAudioCallsOneOnOneChatsEnabled(Boolean featureAudioCallsOneOnOneChatsEnabled) {
    this.featureAudioCallsOneOnOneChatsEnabled = featureAudioCallsOneOnOneChatsEnabled;
    return this;
  }

  /**
   * Enable audio call button in 1-on-1 chats.
   * @return featureAudioCallsOneOnOneChatsEnabled
  */
  
  @Schema(name = "featureAudioCallsOneOnOneChatsEnabled", example = "true", description = "Enable audio call button in 1-on-1 chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAudioCallsOneOnOneChatsEnabled")
  public Boolean getFeatureAudioCallsOneOnOneChatsEnabled() {
    return featureAudioCallsOneOnOneChatsEnabled;
  }

  public void setFeatureAudioCallsOneOnOneChatsEnabled(Boolean featureAudioCallsOneOnOneChatsEnabled) {
    this.featureAudioCallsOneOnOneChatsEnabled = featureAudioCallsOneOnOneChatsEnabled;
  }

  public Settings featureAudioCallsGroupChatsEnabled(Boolean featureAudioCallsGroupChatsEnabled) {
    this.featureAudioCallsGroupChatsEnabled = featureAudioCallsGroupChatsEnabled;
    return this;
  }

  /**
   * Enable audio call button in group chats.
   * @return featureAudioCallsGroupChatsEnabled
  */
  
  @Schema(name = "featureAudioCallsGroupChatsEnabled", example = "true", description = "Enable audio call button in group chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAudioCallsGroupChatsEnabled")
  public Boolean getFeatureAudioCallsGroupChatsEnabled() {
    return featureAudioCallsGroupChatsEnabled;
  }

  public void setFeatureAudioCallsGroupChatsEnabled(Boolean featureAudioCallsGroupChatsEnabled) {
    this.featureAudioCallsGroupChatsEnabled = featureAudioCallsGroupChatsEnabled;
  }

  public Settings featureAudioCallsSupervisionChatsEnabled(Boolean featureAudioCallsSupervisionChatsEnabled) {
    this.featureAudioCallsSupervisionChatsEnabled = featureAudioCallsSupervisionChatsEnabled;
    return this;
  }

  /**
   * Enable audio call button when a user is in supervision mode.
   * @return featureAudioCallsSupervisionChatsEnabled
  */
  
  @Schema(name = "featureAudioCallsSupervisionChatsEnabled", example = "true", description = "Enable audio call button when a user is in supervision mode.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureAudioCallsSupervisionChatsEnabled")
  public Boolean getFeatureAudioCallsSupervisionChatsEnabled() {
    return featureAudioCallsSupervisionChatsEnabled;
  }

  public void setFeatureAudioCallsSupervisionChatsEnabled(Boolean featureAudioCallsSupervisionChatsEnabled) {
    this.featureAudioCallsSupervisionChatsEnabled = featureAudioCallsSupervisionChatsEnabled;
  }

  public Settings featureVideoCallsEnabled(Boolean featureVideoCallsEnabled) {
    this.featureVideoCallsEnabled = featureVideoCallsEnabled;
    return this;
  }

  /**
   * Master toggle for video call button availability (all chat types).
   * @return featureVideoCallsEnabled
  */
  
  @Schema(name = "featureVideoCallsEnabled", example = "true", description = "Master toggle for video call button availability (all chat types).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVideoCallsEnabled")
  public Boolean getFeatureVideoCallsEnabled() {
    return featureVideoCallsEnabled;
  }

  public void setFeatureVideoCallsEnabled(Boolean featureVideoCallsEnabled) {
    this.featureVideoCallsEnabled = featureVideoCallsEnabled;
  }

  public Settings featureVideoCallsAnonymousChatsEnabled(Boolean featureVideoCallsAnonymousChatsEnabled) {
    this.featureVideoCallsAnonymousChatsEnabled = featureVideoCallsAnonymousChatsEnabled;
    return this;
  }

  /**
   * Enable video call button in anonymous chats.
   * @return featureVideoCallsAnonymousChatsEnabled
  */
  
  @Schema(name = "featureVideoCallsAnonymousChatsEnabled", example = "true", description = "Enable video call button in anonymous chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVideoCallsAnonymousChatsEnabled")
  public Boolean getFeatureVideoCallsAnonymousChatsEnabled() {
    return featureVideoCallsAnonymousChatsEnabled;
  }

  public void setFeatureVideoCallsAnonymousChatsEnabled(Boolean featureVideoCallsAnonymousChatsEnabled) {
    this.featureVideoCallsAnonymousChatsEnabled = featureVideoCallsAnonymousChatsEnabled;
  }

  public Settings featureVideoCallsOneOnOneChatsEnabled(Boolean featureVideoCallsOneOnOneChatsEnabled) {
    this.featureVideoCallsOneOnOneChatsEnabled = featureVideoCallsOneOnOneChatsEnabled;
    return this;
  }

  /**
   * Enable video call button in 1-on-1 chats.
   * @return featureVideoCallsOneOnOneChatsEnabled
  */
  
  @Schema(name = "featureVideoCallsOneOnOneChatsEnabled", example = "true", description = "Enable video call button in 1-on-1 chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVideoCallsOneOnOneChatsEnabled")
  public Boolean getFeatureVideoCallsOneOnOneChatsEnabled() {
    return featureVideoCallsOneOnOneChatsEnabled;
  }

  public void setFeatureVideoCallsOneOnOneChatsEnabled(Boolean featureVideoCallsOneOnOneChatsEnabled) {
    this.featureVideoCallsOneOnOneChatsEnabled = featureVideoCallsOneOnOneChatsEnabled;
  }

  public Settings featureVideoCallsGroupChatsEnabled(Boolean featureVideoCallsGroupChatsEnabled) {
    this.featureVideoCallsGroupChatsEnabled = featureVideoCallsGroupChatsEnabled;
    return this;
  }

  /**
   * Enable video call button in group chats.
   * @return featureVideoCallsGroupChatsEnabled
  */
  
  @Schema(name = "featureVideoCallsGroupChatsEnabled", example = "true", description = "Enable video call button in group chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVideoCallsGroupChatsEnabled")
  public Boolean getFeatureVideoCallsGroupChatsEnabled() {
    return featureVideoCallsGroupChatsEnabled;
  }

  public void setFeatureVideoCallsGroupChatsEnabled(Boolean featureVideoCallsGroupChatsEnabled) {
    this.featureVideoCallsGroupChatsEnabled = featureVideoCallsGroupChatsEnabled;
  }

  public Settings featureVideoCallsSupervisionChatsEnabled(Boolean featureVideoCallsSupervisionChatsEnabled) {
    this.featureVideoCallsSupervisionChatsEnabled = featureVideoCallsSupervisionChatsEnabled;
    return this;
  }

  /**
   * Enable video call button when a user is in supervision mode.
   * @return featureVideoCallsSupervisionChatsEnabled
  */
  
  @Schema(name = "featureVideoCallsSupervisionChatsEnabled", example = "true", description = "Enable video call button when a user is in supervision mode.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVideoCallsSupervisionChatsEnabled")
  public Boolean getFeatureVideoCallsSupervisionChatsEnabled() {
    return featureVideoCallsSupervisionChatsEnabled;
  }

  public void setFeatureVideoCallsSupervisionChatsEnabled(Boolean featureVideoCallsSupervisionChatsEnabled) {
    this.featureVideoCallsSupervisionChatsEnabled = featureVideoCallsSupervisionChatsEnabled;
  }

  public Settings featureThreadsEnabled(Boolean featureThreadsEnabled) {
    this.featureThreadsEnabled = featureThreadsEnabled;
    return this;
  }

  /**
   * Master toggle for threads availability (all chat types).
   * @return featureThreadsEnabled
  */
  
  @Schema(name = "featureThreadsEnabled", example = "true", description = "Master toggle for threads availability (all chat types).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureThreadsEnabled")
  public Boolean getFeatureThreadsEnabled() {
    return featureThreadsEnabled;
  }

  public void setFeatureThreadsEnabled(Boolean featureThreadsEnabled) {
    this.featureThreadsEnabled = featureThreadsEnabled;
  }

  public Settings featureThreadsAnonymousChatsEnabled(Boolean featureThreadsAnonymousChatsEnabled) {
    this.featureThreadsAnonymousChatsEnabled = featureThreadsAnonymousChatsEnabled;
    return this;
  }

  /**
   * Enable threads in anonymous chats.
   * @return featureThreadsAnonymousChatsEnabled
  */
  
  @Schema(name = "featureThreadsAnonymousChatsEnabled", example = "true", description = "Enable threads in anonymous chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureThreadsAnonymousChatsEnabled")
  public Boolean getFeatureThreadsAnonymousChatsEnabled() {
    return featureThreadsAnonymousChatsEnabled;
  }

  public void setFeatureThreadsAnonymousChatsEnabled(Boolean featureThreadsAnonymousChatsEnabled) {
    this.featureThreadsAnonymousChatsEnabled = featureThreadsAnonymousChatsEnabled;
  }

  public Settings featureThreadsGroupChatsEnabled(Boolean featureThreadsGroupChatsEnabled) {
    this.featureThreadsGroupChatsEnabled = featureThreadsGroupChatsEnabled;
    return this;
  }

  /**
   * Enable threads in group chats.
   * @return featureThreadsGroupChatsEnabled
  */
  
  @Schema(name = "featureThreadsGroupChatsEnabled", example = "true", description = "Enable threads in group chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureThreadsGroupChatsEnabled")
  public Boolean getFeatureThreadsGroupChatsEnabled() {
    return featureThreadsGroupChatsEnabled;
  }

  public void setFeatureThreadsGroupChatsEnabled(Boolean featureThreadsGroupChatsEnabled) {
    this.featureThreadsGroupChatsEnabled = featureThreadsGroupChatsEnabled;
  }

  public Settings featureThreadsOneOnOneEnabled(Boolean featureThreadsOneOnOneEnabled) {
    this.featureThreadsOneOnOneEnabled = featureThreadsOneOnOneEnabled;
    return this;
  }

  /**
   * Enable threads in 1-on-1 chats.
   * @return featureThreadsOneOnOneEnabled
  */
  
  @Schema(name = "featureThreadsOneOnOneEnabled", example = "true", description = "Enable threads in 1-on-1 chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureThreadsOneOnOneEnabled")
  public Boolean getFeatureThreadsOneOnOneEnabled() {
    return featureThreadsOneOnOneEnabled;
  }

  public void setFeatureThreadsOneOnOneEnabled(Boolean featureThreadsOneOnOneEnabled) {
    this.featureThreadsOneOnOneEnabled = featureThreadsOneOnOneEnabled;
  }

  public Settings featureThreadsSupervisionChatsEnabled(Boolean featureThreadsSupervisionChatsEnabled) {
    this.featureThreadsSupervisionChatsEnabled = featureThreadsSupervisionChatsEnabled;
    return this;
  }

  /**
   * Enable threads when a user is in supervision mode.
   * @return featureThreadsSupervisionChatsEnabled
  */
  
  @Schema(name = "featureThreadsSupervisionChatsEnabled", example = "true", description = "Enable threads when a user is in supervision mode.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureThreadsSupervisionChatsEnabled")
  public Boolean getFeatureThreadsSupervisionChatsEnabled() {
    return featureThreadsSupervisionChatsEnabled;
  }

  public void setFeatureThreadsSupervisionChatsEnabled(Boolean featureThreadsSupervisionChatsEnabled) {
    this.featureThreadsSupervisionChatsEnabled = featureThreadsSupervisionChatsEnabled;
  }

  public Settings featureVoiceMessagesEnabled(Boolean featureVoiceMessagesEnabled) {
    this.featureVoiceMessagesEnabled = featureVoiceMessagesEnabled;
    return this;
  }

  /**
   * Master toggle for voice messages availability (all chat types).
   * @return featureVoiceMessagesEnabled
  */
  
  @Schema(name = "featureVoiceMessagesEnabled", example = "true", description = "Master toggle for voice messages availability (all chat types).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVoiceMessagesEnabled")
  public Boolean getFeatureVoiceMessagesEnabled() {
    return featureVoiceMessagesEnabled;
  }

  public void setFeatureVoiceMessagesEnabled(Boolean featureVoiceMessagesEnabled) {
    this.featureVoiceMessagesEnabled = featureVoiceMessagesEnabled;
  }

  public Settings featureVoiceMessagesAnonymousChatsEnabled(Boolean featureVoiceMessagesAnonymousChatsEnabled) {
    this.featureVoiceMessagesAnonymousChatsEnabled = featureVoiceMessagesAnonymousChatsEnabled;
    return this;
  }

  /**
   * Enable voice messages in anonymous chats.
   * @return featureVoiceMessagesAnonymousChatsEnabled
  */
  
  @Schema(name = "featureVoiceMessagesAnonymousChatsEnabled", example = "true", description = "Enable voice messages in anonymous chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVoiceMessagesAnonymousChatsEnabled")
  public Boolean getFeatureVoiceMessagesAnonymousChatsEnabled() {
    return featureVoiceMessagesAnonymousChatsEnabled;
  }

  public void setFeatureVoiceMessagesAnonymousChatsEnabled(Boolean featureVoiceMessagesAnonymousChatsEnabled) {
    this.featureVoiceMessagesAnonymousChatsEnabled = featureVoiceMessagesAnonymousChatsEnabled;
  }

  public Settings featureVoiceMessagesOneOnOneChatsEnabled(Boolean featureVoiceMessagesOneOnOneChatsEnabled) {
    this.featureVoiceMessagesOneOnOneChatsEnabled = featureVoiceMessagesOneOnOneChatsEnabled;
    return this;
  }

  /**
   * Enable voice messages in 1-on-1 chats.
   * @return featureVoiceMessagesOneOnOneChatsEnabled
  */
  
  @Schema(name = "featureVoiceMessagesOneOnOneChatsEnabled", example = "true", description = "Enable voice messages in 1-on-1 chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVoiceMessagesOneOnOneChatsEnabled")
  public Boolean getFeatureVoiceMessagesOneOnOneChatsEnabled() {
    return featureVoiceMessagesOneOnOneChatsEnabled;
  }

  public void setFeatureVoiceMessagesOneOnOneChatsEnabled(Boolean featureVoiceMessagesOneOnOneChatsEnabled) {
    this.featureVoiceMessagesOneOnOneChatsEnabled = featureVoiceMessagesOneOnOneChatsEnabled;
  }

  public Settings featureVoiceMessagesGroupChatsEnabled(Boolean featureVoiceMessagesGroupChatsEnabled) {
    this.featureVoiceMessagesGroupChatsEnabled = featureVoiceMessagesGroupChatsEnabled;
    return this;
  }

  /**
   * Enable voice messages in group chats.
   * @return featureVoiceMessagesGroupChatsEnabled
  */
  
  @Schema(name = "featureVoiceMessagesGroupChatsEnabled", example = "true", description = "Enable voice messages in group chats.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVoiceMessagesGroupChatsEnabled")
  public Boolean getFeatureVoiceMessagesGroupChatsEnabled() {
    return featureVoiceMessagesGroupChatsEnabled;
  }

  public void setFeatureVoiceMessagesGroupChatsEnabled(Boolean featureVoiceMessagesGroupChatsEnabled) {
    this.featureVoiceMessagesGroupChatsEnabled = featureVoiceMessagesGroupChatsEnabled;
  }

  public Settings featureVoiceMessagesSupervisionChatsEnabled(Boolean featureVoiceMessagesSupervisionChatsEnabled) {
    this.featureVoiceMessagesSupervisionChatsEnabled = featureVoiceMessagesSupervisionChatsEnabled;
    return this;
  }

  /**
   * Enable voice messages when a user is in supervision mode.
   * @return featureVoiceMessagesSupervisionChatsEnabled
  */
  
  @Schema(name = "featureVoiceMessagesSupervisionChatsEnabled", example = "true", description = "Enable voice messages when a user is in supervision mode.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureVoiceMessagesSupervisionChatsEnabled")
  public Boolean getFeatureVoiceMessagesSupervisionChatsEnabled() {
    return featureVoiceMessagesSupervisionChatsEnabled;
  }

  public void setFeatureVoiceMessagesSupervisionChatsEnabled(Boolean featureVoiceMessagesSupervisionChatsEnabled) {
    this.featureVoiceMessagesSupervisionChatsEnabled = featureVoiceMessagesSupervisionChatsEnabled;
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

  public Settings featureSystemNotificationEmailsEnabled(Boolean featureSystemNotificationEmailsEnabled) {
    this.featureSystemNotificationEmailsEnabled = featureSystemNotificationEmailsEnabled;
    return this;
  }

  /**
   * Get featureSystemNotificationEmailsEnabled
   * @return featureSystemNotificationEmailsEnabled
  */
  
  @Schema(name = "featureSystemNotificationEmailsEnabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureSystemNotificationEmailsEnabled")
  public Boolean getFeatureSystemNotificationEmailsEnabled() {
    return featureSystemNotificationEmailsEnabled;
  }

  public void setFeatureSystemNotificationEmailsEnabled(Boolean featureSystemNotificationEmailsEnabled) {
    this.featureSystemNotificationEmailsEnabled = featureSystemNotificationEmailsEnabled;
  }

  public Settings smtp(SmtpConfig smtp) {
    this.smtp = smtp;
    return this;
  }

  /**
   * Get smtp
   * @return smtp
  */
  @Valid 
  @Schema(name = "smtp", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("smtp")
  public SmtpConfig getSmtp() {
    return smtp;
  }

  public void setSmtp(SmtpConfig smtp) {
    this.smtp = smtp;
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

  public Settings tenantAdminControls(TenantAdminControls tenantAdminControls) {
    this.tenantAdminControls = tenantAdminControls;
    return this;
  }

  /**
   * Get tenantAdminControls
   * @return tenantAdminControls
  */
  @Valid 
  @Schema(name = "tenantAdminControls", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tenantAdminControls")
  public TenantAdminControls getTenantAdminControls() {
    return tenantAdminControls;
  }

  public void setTenantAdminControls(TenantAdminControls tenantAdminControls) {
    this.tenantAdminControls = tenantAdminControls;
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
        Objects.equals(this.featureAnonymousChatEnabled, settings.featureAnonymousChatEnabled) &&
        Objects.equals(this.featureCallsEnabled, settings.featureCallsEnabled) &&
        Objects.equals(this.featureSupervisionEnabled, settings.featureSupervisionEnabled) &&
        Objects.equals(this.featureSupervisionAnonymousChatsEnabled, settings.featureSupervisionAnonymousChatsEnabled) &&
        Objects.equals(this.featureSupervisionOneOnOneChatsEnabled, settings.featureSupervisionOneOnOneChatsEnabled) &&
        Objects.equals(this.featureAudioCallsEnabled, settings.featureAudioCallsEnabled) &&
        Objects.equals(this.featureAudioCallsAnonymousChatsEnabled, settings.featureAudioCallsAnonymousChatsEnabled) &&
        Objects.equals(this.featureAudioCallsOneOnOneChatsEnabled, settings.featureAudioCallsOneOnOneChatsEnabled) &&
        Objects.equals(this.featureAudioCallsGroupChatsEnabled, settings.featureAudioCallsGroupChatsEnabled) &&
        Objects.equals(this.featureAudioCallsSupervisionChatsEnabled, settings.featureAudioCallsSupervisionChatsEnabled) &&
        Objects.equals(this.featureVideoCallsEnabled, settings.featureVideoCallsEnabled) &&
        Objects.equals(this.featureVideoCallsAnonymousChatsEnabled, settings.featureVideoCallsAnonymousChatsEnabled) &&
        Objects.equals(this.featureVideoCallsOneOnOneChatsEnabled, settings.featureVideoCallsOneOnOneChatsEnabled) &&
        Objects.equals(this.featureVideoCallsGroupChatsEnabled, settings.featureVideoCallsGroupChatsEnabled) &&
        Objects.equals(this.featureVideoCallsSupervisionChatsEnabled, settings.featureVideoCallsSupervisionChatsEnabled) &&
        Objects.equals(this.featureThreadsEnabled, settings.featureThreadsEnabled) &&
        Objects.equals(this.featureThreadsAnonymousChatsEnabled, settings.featureThreadsAnonymousChatsEnabled) &&
        Objects.equals(this.featureThreadsGroupChatsEnabled, settings.featureThreadsGroupChatsEnabled) &&
        Objects.equals(this.featureThreadsOneOnOneEnabled, settings.featureThreadsOneOnOneEnabled) &&
        Objects.equals(this.featureThreadsSupervisionChatsEnabled, settings.featureThreadsSupervisionChatsEnabled) &&
        Objects.equals(this.featureVoiceMessagesEnabled, settings.featureVoiceMessagesEnabled) &&
        Objects.equals(this.featureVoiceMessagesAnonymousChatsEnabled, settings.featureVoiceMessagesAnonymousChatsEnabled) &&
        Objects.equals(this.featureVoiceMessagesOneOnOneChatsEnabled, settings.featureVoiceMessagesOneOnOneChatsEnabled) &&
        Objects.equals(this.featureVoiceMessagesGroupChatsEnabled, settings.featureVoiceMessagesGroupChatsEnabled) &&
        Objects.equals(this.featureVoiceMessagesSupervisionChatsEnabled, settings.featureVoiceMessagesSupervisionChatsEnabled) &&
        Objects.equals(this.featureAttachmentUploadDisabled, settings.featureAttachmentUploadDisabled) &&
        Objects.equals(this.featureToolsOICDToken, settings.featureToolsOICDToken) &&
        Objects.equals(this.activeLanguages, settings.activeLanguages) &&
        Objects.equals(this.showAskerProfile, settings.showAskerProfile) &&
        Objects.equals(this.isVideoCallAllowed, settings.isVideoCallAllowed) &&
        Objects.equals(this.featureSystemNotificationEmailsEnabled, settings.featureSystemNotificationEmailsEnabled) &&
        Objects.equals(this.smtp, settings.smtp) &&
        Objects.equals(this.extendedSettings, settings.extendedSettings) &&
        Objects.equals(this.featureCentralDataProtectionTemplateEnabled, settings.featureCentralDataProtectionTemplateEnabled) &&
        Objects.equals(this.tenantAdminControls, settings.tenantAdminControls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureStatisticsEnabled, featureTopicsEnabled, topicsInRegistrationEnabled, featureDemographicsEnabled, featureAppointmentsEnabled, featureGroupChatV2Enabled, featureToolsEnabled, featureAnonymousChatEnabled, featureCallsEnabled, featureSupervisionEnabled, featureSupervisionAnonymousChatsEnabled, featureSupervisionOneOnOneChatsEnabled, featureAudioCallsEnabled, featureAudioCallsAnonymousChatsEnabled, featureAudioCallsOneOnOneChatsEnabled, featureAudioCallsGroupChatsEnabled, featureAudioCallsSupervisionChatsEnabled, featureVideoCallsEnabled, featureVideoCallsAnonymousChatsEnabled, featureVideoCallsOneOnOneChatsEnabled, featureVideoCallsGroupChatsEnabled, featureVideoCallsSupervisionChatsEnabled, featureThreadsEnabled, featureThreadsAnonymousChatsEnabled, featureThreadsGroupChatsEnabled, featureThreadsOneOnOneEnabled, featureThreadsSupervisionChatsEnabled, featureVoiceMessagesEnabled, featureVoiceMessagesAnonymousChatsEnabled, featureVoiceMessagesOneOnOneChatsEnabled, featureVoiceMessagesGroupChatsEnabled, featureVoiceMessagesSupervisionChatsEnabled, featureAttachmentUploadDisabled, featureToolsOICDToken, activeLanguages, showAskerProfile, isVideoCallAllowed, featureSystemNotificationEmailsEnabled, smtp, extendedSettings, featureCentralDataProtectionTemplateEnabled, tenantAdminControls);
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
    sb.append("    featureAnonymousChatEnabled: ").append(toIndentedString(featureAnonymousChatEnabled)).append("\n");
    sb.append("    featureCallsEnabled: ").append(toIndentedString(featureCallsEnabled)).append("\n");
    sb.append("    featureSupervisionEnabled: ").append(toIndentedString(featureSupervisionEnabled)).append("\n");
    sb.append("    featureSupervisionAnonymousChatsEnabled: ").append(toIndentedString(featureSupervisionAnonymousChatsEnabled)).append("\n");
    sb.append("    featureSupervisionOneOnOneChatsEnabled: ").append(toIndentedString(featureSupervisionOneOnOneChatsEnabled)).append("\n");
    sb.append("    featureAudioCallsEnabled: ").append(toIndentedString(featureAudioCallsEnabled)).append("\n");
    sb.append("    featureAudioCallsAnonymousChatsEnabled: ").append(toIndentedString(featureAudioCallsAnonymousChatsEnabled)).append("\n");
    sb.append("    featureAudioCallsOneOnOneChatsEnabled: ").append(toIndentedString(featureAudioCallsOneOnOneChatsEnabled)).append("\n");
    sb.append("    featureAudioCallsGroupChatsEnabled: ").append(toIndentedString(featureAudioCallsGroupChatsEnabled)).append("\n");
    sb.append("    featureAudioCallsSupervisionChatsEnabled: ").append(toIndentedString(featureAudioCallsSupervisionChatsEnabled)).append("\n");
    sb.append("    featureVideoCallsEnabled: ").append(toIndentedString(featureVideoCallsEnabled)).append("\n");
    sb.append("    featureVideoCallsAnonymousChatsEnabled: ").append(toIndentedString(featureVideoCallsAnonymousChatsEnabled)).append("\n");
    sb.append("    featureVideoCallsOneOnOneChatsEnabled: ").append(toIndentedString(featureVideoCallsOneOnOneChatsEnabled)).append("\n");
    sb.append("    featureVideoCallsGroupChatsEnabled: ").append(toIndentedString(featureVideoCallsGroupChatsEnabled)).append("\n");
    sb.append("    featureVideoCallsSupervisionChatsEnabled: ").append(toIndentedString(featureVideoCallsSupervisionChatsEnabled)).append("\n");
    sb.append("    featureThreadsEnabled: ").append(toIndentedString(featureThreadsEnabled)).append("\n");
    sb.append("    featureThreadsAnonymousChatsEnabled: ").append(toIndentedString(featureThreadsAnonymousChatsEnabled)).append("\n");
    sb.append("    featureThreadsGroupChatsEnabled: ").append(toIndentedString(featureThreadsGroupChatsEnabled)).append("\n");
    sb.append("    featureThreadsOneOnOneEnabled: ").append(toIndentedString(featureThreadsOneOnOneEnabled)).append("\n");
    sb.append("    featureThreadsSupervisionChatsEnabled: ").append(toIndentedString(featureThreadsSupervisionChatsEnabled)).append("\n");
    sb.append("    featureVoiceMessagesEnabled: ").append(toIndentedString(featureVoiceMessagesEnabled)).append("\n");
    sb.append("    featureVoiceMessagesAnonymousChatsEnabled: ").append(toIndentedString(featureVoiceMessagesAnonymousChatsEnabled)).append("\n");
    sb.append("    featureVoiceMessagesOneOnOneChatsEnabled: ").append(toIndentedString(featureVoiceMessagesOneOnOneChatsEnabled)).append("\n");
    sb.append("    featureVoiceMessagesGroupChatsEnabled: ").append(toIndentedString(featureVoiceMessagesGroupChatsEnabled)).append("\n");
    sb.append("    featureVoiceMessagesSupervisionChatsEnabled: ").append(toIndentedString(featureVoiceMessagesSupervisionChatsEnabled)).append("\n");
    sb.append("    featureAttachmentUploadDisabled: ").append(toIndentedString(featureAttachmentUploadDisabled)).append("\n");
    sb.append("    featureToolsOICDToken: ").append(toIndentedString(featureToolsOICDToken)).append("\n");
    sb.append("    activeLanguages: ").append(toIndentedString(activeLanguages)).append("\n");
    sb.append("    showAskerProfile: ").append(toIndentedString(showAskerProfile)).append("\n");
    sb.append("    isVideoCallAllowed: ").append(toIndentedString(isVideoCallAllowed)).append("\n");
    sb.append("    featureSystemNotificationEmailsEnabled: ").append(toIndentedString(featureSystemNotificationEmailsEnabled)).append("\n");
    sb.append("    smtp: ").append(toIndentedString(smtp)).append("\n");
    sb.append("    extendedSettings: ").append(toIndentedString(extendedSettings)).append("\n");
    sb.append("    featureCentralDataProtectionTemplateEnabled: ").append(toIndentedString(featureCentralDataProtectionTemplateEnabled)).append("\n");
    sb.append("    tenantAdminControls: ").append(toIndentedString(tenantAdminControls)).append("\n");
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

