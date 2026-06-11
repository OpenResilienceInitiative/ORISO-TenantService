package com.vi.tenantservice.api.converter;

import static com.vi.tenantservice.api.converter.ConverterUtils.nullAsFalse;
import static com.vi.tenantservice.api.converter.ConverterUtils.nullAsGerman;
import static com.vi.tenantservice.api.converter.ConverterUtils.nullAsTrue;
import static com.vi.tenantservice.api.model.DataProtectionPlaceHolderType.DATA_PROTECTION_OFFICER;
import static com.vi.tenantservice.api.model.DataProtectionPlaceHolderType.DATA_PROTECTION_RESPONSIBLE;
import static com.vi.tenantservice.api.util.JsonConverter.convertMapFromJson;
import static com.vi.tenantservice.api.util.JsonConverter.convertToJson;

import com.google.common.collect.Maps;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.NoAgencyContextDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.SmtpConfig;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantEntity.TenantEntityBuilder;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.TenantSmtpSettings;
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.service.TemplateDescriptionServiceException;
import com.vi.tenantservice.api.service.TemplateRenderer;
import com.vi.tenantservice.api.service.TemplateService;
import com.vi.tenantservice.api.util.JsonConverter;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TenantConverter {

  public static final String DE = "de";

  private final @NonNull TemplateService templateService;

  private final @NonNull TemplateRenderer templateRenderer;

  public TenantEntity toEntity(MultilingualTenantDTO tenantDTO) {
    var builder =
        TenantEntity.builder()
            .id(tenantDTO.getId())
            .name(tenantDTO.getName())
            .subdomain(tenantDTO.getSubdomain());
    contentToEntity(tenantDTO, builder);
    licensingToEntity(tenantDTO, builder);
    themingToEntity(tenantDTO, builder);
    settingsToEntity(tenantDTO, builder);
    return builder.build();
  }

  private void settingsToEntity(MultilingualTenantDTO tenantDTO, TenantEntityBuilder builder) {
    if (tenantDTO.getSettings() != null) {
      TenantSettings tenantSettings = toEntitySettings(tenantDTO.getSettings());
      builder.settings(convertToJson(tenantSettings)).build();
    }
  }

  private TenantSettings toEntitySettings(Settings settings) {
    return TenantSettings.builder()
        .topicsInRegistrationEnabled(nullAsFalse(settings.getTopicsInRegistrationEnabled()))
        .featureDemographicsEnabled(nullAsFalse(settings.getFeatureDemographicsEnabled()))
        .featureTopicsEnabled(nullAsFalse(settings.getFeatureTopicsEnabled()))
        .featureAppointmentsEnabled(nullAsFalse(settings.getFeatureAppointmentsEnabled()))
        .featureStatisticsEnabled(nullAsFalse(settings.getFeatureStatisticsEnabled()))
        .featureGroupChatV2Enabled(nullAsFalse(settings.getFeatureGroupChatV2Enabled()))
        .featureToolsEnabled(nullAsFalse(settings.getFeatureToolsEnabled()))
        .featureAnonymousChatEnabled(nullAsTrue(settings.getFeatureAnonymousChatEnabled()))
        .featureCallsEnabled(nullAsTrue(settings.getFeatureCallsEnabled()))
        .featureSupervisionEnabled(nullAsTrue(settings.getFeatureSupervisionEnabled()))
        .featureSupervisionAnonymousChatsEnabled(
            nullAsTrue(settings.getFeatureSupervisionAnonymousChatsEnabled()))
        .featureSupervisionOneOnOneChatsEnabled(
            nullAsTrue(settings.getFeatureSupervisionOneOnOneChatsEnabled()))
        .featureAudioCallsEnabled(nullAsTrue(settings.getFeatureAudioCallsEnabled()))
        .featureAudioCallsAnonymousChatsEnabled(
            nullAsTrue(settings.getFeatureAudioCallsAnonymousChatsEnabled()))
        .featureAudioCallsOneOnOneChatsEnabled(
            nullAsTrue(settings.getFeatureAudioCallsOneOnOneChatsEnabled()))
        .featureAudioCallsGroupChatsEnabled(
            nullAsTrue(settings.getFeatureAudioCallsGroupChatsEnabled()))
        .featureAudioCallsSupervisionChatsEnabled(
            nullAsTrue(settings.getFeatureAudioCallsSupervisionChatsEnabled()))
        .featureVideoCallsEnabled(nullAsTrue(settings.getFeatureVideoCallsEnabled()))
        .featureVideoCallsAnonymousChatsEnabled(
            nullAsTrue(settings.getFeatureVideoCallsAnonymousChatsEnabled()))
        .featureVideoCallsOneOnOneChatsEnabled(
            nullAsTrue(settings.getFeatureVideoCallsOneOnOneChatsEnabled()))
        .featureVideoCallsGroupChatsEnabled(
            nullAsTrue(settings.getFeatureVideoCallsGroupChatsEnabled()))
        .featureVideoCallsSupervisionChatsEnabled(
            nullAsTrue(settings.getFeatureVideoCallsSupervisionChatsEnabled()))
        .featureThreadsEnabled(nullAsTrue(settings.getFeatureThreadsEnabled()))
        .featureThreadsAnonymousChatsEnabled(
            nullAsTrue(settings.getFeatureThreadsAnonymousChatsEnabled()))
        .featureThreadsGroupChatsEnabled(nullAsTrue(settings.getFeatureThreadsGroupChatsEnabled()))
        .featureThreadsOneOnOneEnabled(nullAsTrue(settings.getFeatureThreadsOneOnOneEnabled()))
        .featureThreadsSupervisionChatsEnabled(
            nullAsTrue(settings.getFeatureThreadsSupervisionChatsEnabled()))
        .featureVoiceMessagesEnabled(nullAsTrue(settings.getFeatureVoiceMessagesEnabled()))
        .featureVoiceMessagesAnonymousChatsEnabled(
            nullAsTrue(settings.getFeatureVoiceMessagesAnonymousChatsEnabled()))
        .featureVoiceMessagesOneOnOneChatsEnabled(
            nullAsTrue(settings.getFeatureVoiceMessagesOneOnOneChatsEnabled()))
        .featureVoiceMessagesGroupChatsEnabled(
            nullAsTrue(settings.getFeatureVoiceMessagesGroupChatsEnabled()))
        .featureVoiceMessagesSupervisionChatsEnabled(
            nullAsTrue(settings.getFeatureVoiceMessagesSupervisionChatsEnabled()))
        .featureSystemNotificationEmailsEnabled(
            nullAsFalse(settings.getFeatureSystemNotificationEmailsEnabled()))
        .smtp(toTenantSmtpSettings(settings.getSmtp()))
        .featureToolsOIDCToken(settings.getFeatureToolsOICDToken())
        .featureAttachmentUploadDisabled(nullAsFalse(settings.getFeatureAttachmentUploadDisabled()))
        .activeLanguages(nullAsGerman(settings.getActiveLanguages()))
        .isVideoCallAllowed(nullAsFalse(settings.getIsVideoCallAllowed()))
        .showAskerProfile(nullAsFalse(settings.getShowAskerProfile()))
        .featureCentralDataProtectionTemplateEnabled(
            nullAsFalse(settings.getFeatureCentralDataProtectionTemplateEnabled()))
        .tenantAdminControls(toTenantAdminControlsSettings(settings.getTenantAdminControls()))
        .build();
  }

  public TenantEntity toEntity(TenantEntity targetEntity, MultilingualTenantDTO tenantDTO) {
    var sourceEntity = toEntity(tenantDTO);
    BeanUtils.copyProperties(
        sourceEntity,
        targetEntity,
        "id",
        "createDate",
        "updateDate",
        "contentPrivacyActivationDate",
        "contentTermsAndConditionsActivationDate");
    return targetEntity;
  }

  private void contentToEntity(
      MultilingualTenantDTO tenantDTO, TenantEntity.TenantEntityBuilder builder) {
    if (tenantDTO.getContent() != null) {
      builder
          .contentClaim(convertToJson(tenantDTO.getContent().getClaim()))
          .contentImpressum(convertToJson(tenantDTO.getContent().getImpressum()))
          .contentPrivacy(convertToJson(tenantDTO.getContent().getPrivacy()))
          .contentTermsAndConditions(convertToJson(tenantDTO.getContent().getTermsAndConditions()));
    }
  }

  private void licensingToEntity(
      MultilingualTenantDTO tenantDTO, TenantEntity.TenantEntityBuilder builder) {
    if (tenantDTO.getLicensing() != null) {
      builder.licensingAllowedNumberOfUsers(tenantDTO.getLicensing().getAllowedNumberOfUsers());
    }
  }

  private void themingToEntity(
      MultilingualTenantDTO tenantDTO, TenantEntity.TenantEntityBuilder builder) {
    if (tenantDTO.getTheming() != null) {
      builder
          .themingFavicon(tenantDTO.getTheming().getFavicon())
          .themingLogo(tenantDTO.getTheming().getLogo())
          .themingAssociationLogo(tenantDTO.getTheming().getAssociationLogo())
          .themingPrimaryColor(tenantDTO.getTheming().getPrimaryColor())
          .themingSecondaryColor(tenantDTO.getTheming().getSecondaryColor());
    }
  }

  public MultilingualTenantDTO toMultilingualDTO(TenantEntity tenant) {
    var tenantDTO =
        new MultilingualTenantDTO(tenant.getName())
            .id(tenant.getId())
            .subdomain(tenant.getSubdomain())
            .content(toMultilingualContentDTO(tenant))
            .theming(toThemingDTO(tenant))
            .licensing(toLicensingDTO(tenant))
            .settings(getSettings(tenant));
    if (tenant.getCreateDate() != null) {
      tenantDTO.setCreateDate(tenant.getCreateDate().toString());
    }
    if (tenant.getUpdateDate() != null) {
      tenantDTO.setUpdateDate(tenant.getUpdateDate().toString());
    }
    return tenantDTO;
  }

  public TenantDTO toDTO(TenantEntity tenant, String lang) {
    var tenantDTO =
        new TenantDTO(tenant.getId(), tenant.getName(), tenant.getSubdomain())
            .content(toContentDTO(tenant, lang))
            .theming(toThemingDTO(tenant))
            .licensing(toLicensingDTO(tenant))
            .settings(getSettings(tenant));
    if (tenant.getCreateDate() != null) {
      tenantDTO.setCreateDate(tenant.getCreateDate().toString());
    }
    if (tenant.getUpdateDate() != null) {
      tenantDTO.setUpdateDate(tenant.getUpdateDate().toString());
    }
    return tenantDTO;
  }

  private Settings getSettings(TenantEntity tenant) {
    if (tenant.getSettings() == null) {
      return new Settings();
    } else {
      return getSettingsIfNotNull(tenant.getSettings());
    }
  }

  private Settings getSettingsIfNotNull(String settingsJson) {
    TenantSettings tenantSettings = JsonConverter.convertFromJson(settingsJson);
    final boolean anonymousChatEnabled =
        settingsJson != null && settingsJson.contains("\"featureAnonymousChatEnabled\"")
            ? tenantSettings.isFeatureAnonymousChatEnabled()
            : true;
    final boolean callsEnabled =
        settingsJson != null && settingsJson.contains("\"featureCallsEnabled\"")
            ? tenantSettings.isFeatureCallsEnabled()
            : true;
    final boolean supervisionEnabled =
        settingsJson != null && settingsJson.contains("\"featureSupervisionEnabled\"")
            ? tenantSettings.isFeatureSupervisionEnabled()
            : true;
    final boolean supervisionAnonymousChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureSupervisionAnonymousChatsEnabled\"")
            ? tenantSettings.isFeatureSupervisionAnonymousChatsEnabled()
            : true;
    final boolean supervisionOneOnOneChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureSupervisionOneOnOneChatsEnabled\"")
            ? tenantSettings.isFeatureSupervisionOneOnOneChatsEnabled()
            : true;
    final boolean audioCallsEnabled =
        settingsJson != null && settingsJson.contains("\"featureAudioCallsEnabled\"")
            ? tenantSettings.isFeatureAudioCallsEnabled()
            : true;
    final boolean audioCallsAnonymousChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureAudioCallsAnonymousChatsEnabled\"")
            ? tenantSettings.isFeatureAudioCallsAnonymousChatsEnabled()
            : true;
    final boolean audioCallsOneOnOneChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureAudioCallsOneOnOneChatsEnabled\"")
            ? tenantSettings.isFeatureAudioCallsOneOnOneChatsEnabled()
            : true;
    final boolean audioCallsGroupChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureAudioCallsGroupChatsEnabled\"")
            ? tenantSettings.isFeatureAudioCallsGroupChatsEnabled()
            : true;
    final boolean audioCallsSupervisionChatsEnabled =
        settingsJson != null
                && settingsJson.contains("\"featureAudioCallsSupervisionChatsEnabled\"")
            ? tenantSettings.isFeatureAudioCallsSupervisionChatsEnabled()
            : true;
    final boolean videoCallsEnabled =
        settingsJson != null && settingsJson.contains("\"featureVideoCallsEnabled\"")
            ? tenantSettings.isFeatureVideoCallsEnabled()
            : true;
    final boolean videoCallsAnonymousChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureVideoCallsAnonymousChatsEnabled\"")
            ? tenantSettings.isFeatureVideoCallsAnonymousChatsEnabled()
            : true;
    final boolean videoCallsOneOnOneChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureVideoCallsOneOnOneChatsEnabled\"")
            ? tenantSettings.isFeatureVideoCallsOneOnOneChatsEnabled()
            : true;
    final boolean videoCallsGroupChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureVideoCallsGroupChatsEnabled\"")
            ? tenantSettings.isFeatureVideoCallsGroupChatsEnabled()
            : true;
    final boolean videoCallsSupervisionChatsEnabled =
        settingsJson != null
                && settingsJson.contains("\"featureVideoCallsSupervisionChatsEnabled\"")
            ? tenantSettings.isFeatureVideoCallsSupervisionChatsEnabled()
            : true;
    final boolean threadsEnabled =
        settingsJson != null && settingsJson.contains("\"featureThreadsEnabled\"")
            ? tenantSettings.isFeatureThreadsEnabled()
            : true;
    final boolean threadsAnonymousChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureThreadsAnonymousChatsEnabled\"")
            ? tenantSettings.isFeatureThreadsAnonymousChatsEnabled()
            : true;
    final boolean threadsGroupChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureThreadsGroupChatsEnabled\"")
            ? tenantSettings.isFeatureThreadsGroupChatsEnabled()
            : true;
    final boolean threadsOneOnOneEnabled =
        settingsJson != null && settingsJson.contains("\"featureThreadsOneOnOneEnabled\"")
            ? tenantSettings.isFeatureThreadsOneOnOneEnabled()
            : true;
    final boolean threadsSupervisionChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureThreadsSupervisionChatsEnabled\"")
            ? tenantSettings.isFeatureThreadsSupervisionChatsEnabled()
            : true;
    final boolean voiceMessagesEnabled =
        settingsJson != null && settingsJson.contains("\"featureVoiceMessagesEnabled\"")
            ? tenantSettings.isFeatureVoiceMessagesEnabled()
            : true;
    final boolean voiceMessagesAnonymousChatsEnabled =
        settingsJson != null
                && settingsJson.contains("\"featureVoiceMessagesAnonymousChatsEnabled\"")
            ? tenantSettings.isFeatureVoiceMessagesAnonymousChatsEnabled()
            : true;
    final boolean voiceMessagesOneOnOneChatsEnabled =
        settingsJson != null
                && settingsJson.contains("\"featureVoiceMessagesOneOnOneChatsEnabled\"")
            ? tenantSettings.isFeatureVoiceMessagesOneOnOneChatsEnabled()
            : true;
    final boolean voiceMessagesGroupChatsEnabled =
        settingsJson != null && settingsJson.contains("\"featureVoiceMessagesGroupChatsEnabled\"")
            ? tenantSettings.isFeatureVoiceMessagesGroupChatsEnabled()
            : true;
    final boolean voiceMessagesSupervisionChatsEnabled =
        settingsJson != null
                && settingsJson.contains("\"featureVoiceMessagesSupervisionChatsEnabled\"")
            ? tenantSettings.isFeatureVoiceMessagesSupervisionChatsEnabled()
            : true;
    return new Settings()
        .topicsInRegistrationEnabled(tenantSettings.isTopicsInRegistrationEnabled())
        .featureDemographicsEnabled(tenantSettings.isFeatureDemographicsEnabled())
        .featureTopicsEnabled(tenantSettings.isFeatureTopicsEnabled())
        .featureAppointmentsEnabled(tenantSettings.isFeatureAppointmentsEnabled())
        .featureStatisticsEnabled(tenantSettings.isFeatureStatisticsEnabled())
        .featureGroupChatV2Enabled(tenantSettings.isFeatureGroupChatV2Enabled())
        .featureToolsOICDToken(tenantSettings.getFeatureToolsOIDCToken())
        .featureToolsEnabled(tenantSettings.isFeatureToolsEnabled())
        .featureAnonymousChatEnabled(anonymousChatEnabled)
        .featureCallsEnabled(callsEnabled)
        .featureSupervisionEnabled(supervisionEnabled)
        .featureSupervisionAnonymousChatsEnabled(supervisionAnonymousChatsEnabled)
        .featureSupervisionOneOnOneChatsEnabled(supervisionOneOnOneChatsEnabled)
        .featureAudioCallsEnabled(audioCallsEnabled)
        .featureAudioCallsAnonymousChatsEnabled(audioCallsAnonymousChatsEnabled)
        .featureAudioCallsOneOnOneChatsEnabled(audioCallsOneOnOneChatsEnabled)
        .featureAudioCallsGroupChatsEnabled(audioCallsGroupChatsEnabled)
        .featureAudioCallsSupervisionChatsEnabled(audioCallsSupervisionChatsEnabled)
        .featureVideoCallsEnabled(videoCallsEnabled)
        .featureVideoCallsAnonymousChatsEnabled(videoCallsAnonymousChatsEnabled)
        .featureVideoCallsOneOnOneChatsEnabled(videoCallsOneOnOneChatsEnabled)
        .featureVideoCallsGroupChatsEnabled(videoCallsGroupChatsEnabled)
        .featureVideoCallsSupervisionChatsEnabled(videoCallsSupervisionChatsEnabled)
        .featureThreadsEnabled(threadsEnabled)
        .featureThreadsAnonymousChatsEnabled(threadsAnonymousChatsEnabled)
        .featureThreadsGroupChatsEnabled(threadsGroupChatsEnabled)
        .featureThreadsOneOnOneEnabled(threadsOneOnOneEnabled)
        .featureThreadsSupervisionChatsEnabled(threadsSupervisionChatsEnabled)
        .featureVoiceMessagesEnabled(voiceMessagesEnabled)
        .featureVoiceMessagesAnonymousChatsEnabled(voiceMessagesAnonymousChatsEnabled)
        .featureVoiceMessagesOneOnOneChatsEnabled(voiceMessagesOneOnOneChatsEnabled)
        .featureVoiceMessagesGroupChatsEnabled(voiceMessagesGroupChatsEnabled)
        .featureVoiceMessagesSupervisionChatsEnabled(voiceMessagesSupervisionChatsEnabled)
        .featureSystemNotificationEmailsEnabled(
            tenantSettings.isFeatureSystemNotificationEmailsEnabled())
        .smtp(toSmtpConfig(tenantSettings.getSmtp()))
        .featureAttachmentUploadDisabled(tenantSettings.isFeatureAttachmentUploadDisabled())
        .isVideoCallAllowed(tenantSettings.isVideoCallAllowed())
        .showAskerProfile(tenantSettings.isShowAskerProfile())
        .featureCentralDataProtectionTemplateEnabled(
            tenantSettings.isFeatureCentralDataProtectionTemplateEnabled())
        .tenantAdminControls(toTenantAdminControls(tenantSettings.getTenantAdminControls()))
        .activeLanguages(nullAsGerman(tenantSettings.getActiveLanguages()));
  }

  public TenantAdminControlsSettings toTenantAdminControlsSettings(
      TenantAdminControls tenantAdminControls) {
    if (tenantAdminControls == null) {
      return null;
    }
    return TenantAdminControlsSettings.builder()
        .permissionsPageEnabled(nullAsTrue(tenantAdminControls.getPermissionsPageEnabled()))
        .allowedPermissionToggles(
            toTenantAdminAllowedPermissionTogglesSettings(
                tenantAdminControls.getAllowedPermissionToggles()))
        .build();
  }

  private TenantAdminAllowedPermissionTogglesSettings toTenantAdminAllowedPermissionTogglesSettings(
      TenantAdminAllowedPermissionToggles allowedPermissionToggles) {
    if (allowedPermissionToggles == null) {
      return null;
    }
    return TenantAdminAllowedPermissionTogglesSettings.builder()
        .appearance(nullAsTrue(allowedPermissionToggles.getAppearance()))
        .anonymousChat(nullAsTrue(allowedPermissionToggles.getAnonymousChat()))
        .calls(nullAsTrue(allowedPermissionToggles.getCalls()))
        .groupChat(nullAsTrue(allowedPermissionToggles.getGroupChat()))
        .supervision(nullAsTrue(allowedPermissionToggles.getSupervision()))
        .supervisionAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getSupervisionAnonymousChats()))
        .supervisionOneOnOneChats(
            nullAsTrue(allowedPermissionToggles.getSupervisionOneOnOneChats()))
        .audioCalls(nullAsTrue(allowedPermissionToggles.getAudioCalls()))
        .audioCallsAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getAudioCallsAnonymousChats()))
        .audioCallsOneOnOneChats(nullAsTrue(allowedPermissionToggles.getAudioCallsOneOnOneChats()))
        .audioCallsGroupChats(nullAsTrue(allowedPermissionToggles.getAudioCallsGroupChats()))
        .audioCallsSupervisionChats(
            nullAsTrue(allowedPermissionToggles.getAudioCallsSupervisionChats()))
        .videoCalls(nullAsTrue(allowedPermissionToggles.getVideoCalls()))
        .videoCallsAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getVideoCallsAnonymousChats()))
        .videoCallsOneOnOneChats(nullAsTrue(allowedPermissionToggles.getVideoCallsOneOnOneChats()))
        .videoCallsGroupChats(nullAsTrue(allowedPermissionToggles.getVideoCallsGroupChats()))
        .videoCallsSupervisionChats(
            nullAsTrue(allowedPermissionToggles.getVideoCallsSupervisionChats()))
        .threads(nullAsTrue(allowedPermissionToggles.getThreads()))
        .threadsAnonymousChats(nullAsTrue(allowedPermissionToggles.getThreadsAnonymousChats()))
        .threadsOneOnOneChats(nullAsTrue(allowedPermissionToggles.getThreadsOneOnOneChats()))
        .threadsGroupChats(nullAsTrue(allowedPermissionToggles.getThreadsGroupChats()))
        .threadsSupervisionChats(nullAsTrue(allowedPermissionToggles.getThreadsSupervisionChats()))
        .voiceMessages(nullAsTrue(allowedPermissionToggles.getVoiceMessages()))
        .voiceMessagesAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getVoiceMessagesAnonymousChats()))
        .voiceMessagesOneOnOneChats(
            nullAsTrue(allowedPermissionToggles.getVoiceMessagesOneOnOneChats()))
        .voiceMessagesGroupChats(nullAsTrue(allowedPermissionToggles.getVoiceMessagesGroupChats()))
        .voiceMessagesSupervisionChats(
            nullAsTrue(allowedPermissionToggles.getVoiceMessagesSupervisionChats()))
        .build();
  }

  public TenantAdminControls toTenantAdminControls(
      TenantAdminControlsSettings tenantAdminControlsSettings) {
    if (tenantAdminControlsSettings == null) {
      return null;
    }
    return new TenantAdminControls()
        .permissionsPageEnabled(tenantAdminControlsSettings.isPermissionsPageEnabled())
        .allowedPermissionToggles(
            toTenantAdminAllowedPermissionToggles(
                tenantAdminControlsSettings.getAllowedPermissionToggles()));
  }

  private TenantAdminAllowedPermissionToggles toTenantAdminAllowedPermissionToggles(
      TenantAdminAllowedPermissionTogglesSettings allowedPermissionTogglesSettings) {
    if (allowedPermissionTogglesSettings == null) {
      return null;
    }
    return new TenantAdminAllowedPermissionToggles()
        .appearance(nullAsTrue(allowedPermissionTogglesSettings.getAppearance()))
        .anonymousChat(nullAsTrue(allowedPermissionTogglesSettings.getAnonymousChat()))
        .calls(nullAsTrue(allowedPermissionTogglesSettings.getCalls()))
        .groupChat(nullAsTrue(allowedPermissionTogglesSettings.getGroupChat()))
        .supervision(nullAsTrue(allowedPermissionTogglesSettings.getSupervision()))
        .supervisionAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getSupervisionAnonymousChats()))
        .supervisionOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getSupervisionOneOnOneChats()))
        .audioCalls(nullAsTrue(allowedPermissionTogglesSettings.getAudioCalls()))
        .audioCallsAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getAudioCallsAnonymousChats()))
        .audioCallsOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getAudioCallsOneOnOneChats()))
        .audioCallsGroupChats(
            nullAsTrue(allowedPermissionTogglesSettings.getAudioCallsGroupChats()))
        .audioCallsSupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getAudioCallsSupervisionChats()))
        .videoCalls(nullAsTrue(allowedPermissionTogglesSettings.getVideoCalls()))
        .videoCallsAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVideoCallsAnonymousChats()))
        .videoCallsOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVideoCallsOneOnOneChats()))
        .videoCallsGroupChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVideoCallsGroupChats()))
        .videoCallsSupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVideoCallsSupervisionChats()))
        .threads(nullAsTrue(allowedPermissionTogglesSettings.getThreads()))
        .threadsAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getThreadsAnonymousChats()))
        .threadsOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getThreadsOneOnOneChats()))
        .threadsGroupChats(nullAsTrue(allowedPermissionTogglesSettings.getThreadsGroupChats()))
        .threadsSupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getThreadsSupervisionChats()))
        .voiceMessages(nullAsTrue(allowedPermissionTogglesSettings.getVoiceMessages()))
        .voiceMessagesAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVoiceMessagesAnonymousChats()))
        .voiceMessagesOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVoiceMessagesOneOnOneChats()))
        .voiceMessagesGroupChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVoiceMessagesGroupChats()))
        .voiceMessagesSupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getVoiceMessagesSupervisionChats()));
  }

  private TenantSmtpSettings toTenantSmtpSettings(SmtpConfig smtpConfig) {
    if (smtpConfig == null) {
      return null;
    }
    return TenantSmtpSettings.builder()
        .enabled(nullAsFalse(smtpConfig.getEnabled()))
        .host(smtpConfig.getHost())
        .port(smtpConfig.getPort())
        .secure(nullAsFalse(smtpConfig.getSecure()))
        .username(smtpConfig.getUsername())
        .password(smtpConfig.getPassword())
        .from(smtpConfig.getFrom())
        .emailThemeColor(smtpConfig.getEmailThemeColor())
        .build();
  }

  private SmtpConfig toSmtpConfig(TenantSmtpSettings smtpSettings) {
    if (smtpSettings == null) {
      return null;
    }
    return new SmtpConfig()
        .enabled(smtpSettings.isEnabled())
        .host(smtpSettings.getHost())
        .port(smtpSettings.getPort())
        .secure(smtpSettings.isSecure())
        .username(smtpSettings.getUsername())
        .password(smtpSettings.getPassword())
        .from(smtpSettings.getFrom())
        .emailThemeColor(smtpSettings.getEmailThemeColor());
  }

  public RestrictedTenantDTO toRestrictedTenantDTO(TenantEntity tenant, String lang) {
    return new RestrictedTenantDTO(tenant.getId(), tenant.getName())
        .content(toContentDTO(tenant, lang))
        .theming(toThemingDTO(tenant))
        .subdomain(tenant.getSubdomain())
        .settings(getSettings(tenant));
  }

  public BasicTenantLicensingDTO toBasicLicensingTenantDTO(TenantEntity tenant) {
    var basicTenantLicensingDTO =
        new BasicTenantLicensingDTO(tenant.getId(), tenant.getName(), tenant.getSubdomain())
            .licensing(toLicensingDTO(tenant));

    if (tenant.getCreateDate() != null) {
      basicTenantLicensingDTO.setCreateDate(tenant.getCreateDate().toString());
    }
    if (tenant.getUpdateDate() != null) {
      basicTenantLicensingDTO.setUpdateDate(tenant.getUpdateDate().toString());
    }
    return basicTenantLicensingDTO;
  }

  public Licensing toLicensingDTO(TenantEntity tenant) {
    return new Licensing(tenant.getLicensingAllowedNumberOfUsers());
  }

  private Theming toThemingDTO(TenantEntity tenant) {
    return new Theming()
        .favicon(tenant.getThemingFavicon())
        .logo(tenant.getThemingLogo())
        .associationLogo(tenant.getThemingAssociationLogo())
        .primaryColor(tenant.getThemingPrimaryColor())
        .secondaryColor(tenant.getThemingSecondaryColor());
  }

  private Content toContentDTO(TenantEntity tenant, String lang) {
    String privacyPotentiallyWithPlaceholders =
        getTranslatedStringFromMap(tenant.getContentPrivacy(), lang);
    DataProtectionContactTemplateDTO dataProtectionContactTemplate =
        getDataProtectionContactTemplate(lang);
    return new Content(getTranslatedStringFromMap(tenant.getContentImpressum(), lang))
        .claim(getTranslatedStringFromMap(tenant.getContentClaim(), lang))
        .privacy(privacyPotentiallyWithPlaceholders)
        .termsAndConditions(getTranslatedStringFromMap(tenant.getContentTermsAndConditions(), lang))
        .dataPrivacyConfirmation(tenant.getContentPrivacyActivationDate())
        .termsAndConditionsConfirmation(tenant.getContentTermsAndConditionsActivationDate())
        .dataProtectionContactTemplate(dataProtectionContactTemplate)
        .renderedPrivacy(
            renderPrivacyForNoAgencyContext(
                privacyPotentiallyWithPlaceholders, dataProtectionContactTemplate));
  }

  private String renderPrivacyForNoAgencyContext(
      String privacyPotentiallyWithPlaceholders,
      DataProtectionContactTemplateDTO dataProtectionContactTemplate) {
    if (dataProtectionContactTemplate == null
        || dataProtectionContactTemplate.getNoAgencyContext() == null) {
      log.info("No data protection contact template found. Skipping privacy rendering.");
      return privacyPotentiallyWithPlaceholders;
    }
    return tryRenderTemplate(privacyPotentiallyWithPlaceholders, dataProtectionContactTemplate);
  }

  private String tryRenderTemplate(
      String privacyPotentiallyWithPlaceholders,
      DataProtectionContactTemplateDTO dataProtectionContactTemplate) {
    try {
      return templateRenderer.renderTemplate(
          privacyPotentiallyWithPlaceholders,
          placeHolderKeyValueMap(dataProtectionContactTemplate.getNoAgencyContext()));
    } catch (IOException | TemplateException e) {
      log.error("Error while rendering privacy template", e);
      return privacyPotentiallyWithPlaceholders;
    }
  }

  private Map<String, Object> placeHolderKeyValueMap(NoAgencyContextDTO noAgencyContext) {
    Map<String, Object> dataModel = Maps.newHashMap();
    dataModel.put(
        DATA_PROTECTION_OFFICER.getPlaceholderVariable(),
        noAgencyContext.getDataProtectionOfficerContact());
    dataModel.put(
        DATA_PROTECTION_RESPONSIBLE.getPlaceholderVariable(),
        noAgencyContext.getResponsibleContact());
    return dataModel;
  }

  private DataProtectionContactTemplateDTO getDataProtectionContactTemplate(String lang) {
    var map = getMultilingualDataProtectionTemplate();
    if (map.containsKey(lang)) {
      return map.get(lang);
    }
    return null;
  }

  private Map<String, DataProtectionContactTemplateDTO> getMultilingualDataProtectionTemplate() {
    try {
      return templateService.getMultilingualDataProtectionTemplate();
    } catch (TemplateDescriptionServiceException e) {
      log.error("Error while loading data protection contact template", e);
    }
    return Maps.newHashMap();
  }

  private static String getTranslatedStringFromMap(String jsonValue, String lang) {
    Map<String, String> translations = convertMapFromJson(jsonValue);
    if (lang == null || !translations.containsKey(lang)) {
      if (translations.containsKey(DE)) {
        return translations.get(DE);
      } else {
        log.warn("Default translation for value not available");
        return "";
      }
    } else {
      return translations.get(lang);
    }
  }

  private MultilingualContent toMultilingualContentDTO(TenantEntity tenant) {
    return new MultilingualContent(convertMapFromJson(tenant.getContentImpressum()))
        .claim(convertMapFromJson(tenant.getContentClaim()))
        .privacy(convertMapFromJson(tenant.getContentPrivacy()))
        .termsAndConditions(convertMapFromJson(tenant.getContentTermsAndConditions()))
        .dataProtectionContactTemplate(getMultilingualDataProtectionTemplate());
  }

  public AdminTenantDTO toAdminTenantDTO(TenantEntity tenant) {
    var adminTenantDTO =
        new AdminTenantDTO(tenant.getId(), tenant.getName(), tenant.getSubdomain())
            .beraterCount(tenant.getLicensingAllowedNumberOfUsers());
    if (tenant.getCreateDate() != null) {
      adminTenantDTO.setCreateDate(tenant.getCreateDate().toString());
    }
    if (tenant.getUpdateDate() != null) {
      adminTenantDTO.setUpdateDate(tenant.getUpdateDate().toString());
    }
    return adminTenantDTO;
  }
}
