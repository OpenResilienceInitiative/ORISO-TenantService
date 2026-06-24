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
            .subdomain(tenantDTO.getSubdomain())
            .address(tenantDTO.getAddress())
            .description(tenantDTO.getDescription());
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
        .topicsInRegistrationEnabled(settings.getTopicsInRegistrationEnabled())
        .featureDemographicsEnabled(settings.getFeatureDemographicsEnabled())
        .featureTopicsEnabled(settings.getFeatureTopicsEnabled())
        .featureAppointmentsEnabled(settings.getFeatureAppointmentsEnabled())
        .featureStatisticsEnabled(settings.getFeatureStatisticsEnabled())
        .featureGroupChatV2Enabled(settings.getFeatureGroupChatV2Enabled())
        .featureToolsEnabled(settings.getFeatureToolsEnabled())
        .featureAnonymousChatEnabled(settings.getFeatureAnonymousChatEnabled())
        .featureCallsEnabled(settings.getFeatureCallsEnabled())
        .featureSupervisionEnabled(settings.getFeatureSupervisionEnabled())
        .featureSupervisionAnonymousChatsEnabled(
            settings.getFeatureSupervisionAnonymousChatsEnabled())
        .featureSupervisionOneOnOneChatsEnabled(
            settings.getFeatureSupervisionOneOnOneChatsEnabled())
        .featureAudioCallsEnabled(settings.getFeatureAudioCallsEnabled())
        .featureAudioCallsAnonymousChatsEnabled(
            settings.getFeatureAudioCallsAnonymousChatsEnabled())
        .featureAudioCallsOneOnOneChatsEnabled(settings.getFeatureAudioCallsOneOnOneChatsEnabled())
        .featureAudioCallsGroupChatsEnabled(settings.getFeatureAudioCallsGroupChatsEnabled())
        .featureAudioCallsSupervisionChatsEnabled(
            settings.getFeatureAudioCallsSupervisionChatsEnabled())
        .featureVideoCallsEnabled(settings.getFeatureVideoCallsEnabled())
        .featureVideoCallsAnonymousChatsEnabled(
            settings.getFeatureVideoCallsAnonymousChatsEnabled())
        .featureVideoCallsOneOnOneChatsEnabled(settings.getFeatureVideoCallsOneOnOneChatsEnabled())
        .featureVideoCallsGroupChatsEnabled(settings.getFeatureVideoCallsGroupChatsEnabled())
        .featureVideoCallsSupervisionChatsEnabled(
            settings.getFeatureVideoCallsSupervisionChatsEnabled())
        .featureThreadsEnabled(settings.getFeatureThreadsEnabled())
        .featureThreadsAnonymousChatsEnabled(settings.getFeatureThreadsAnonymousChatsEnabled())
        .featureThreadsGroupChatsEnabled(settings.getFeatureThreadsGroupChatsEnabled())
        .featureThreadsOneOnOneEnabled(settings.getFeatureThreadsOneOnOneEnabled())
        .featureThreadsSupervisionChatsEnabled(settings.getFeatureThreadsSupervisionChatsEnabled())
        .featureVoiceMessagesEnabled(settings.getFeatureVoiceMessagesEnabled())
        .featureVoiceMessagesAnonymousChatsEnabled(
            settings.getFeatureVoiceMessagesAnonymousChatsEnabled())
        .featureVoiceMessagesOneOnOneChatsEnabled(
            settings.getFeatureVoiceMessagesOneOnOneChatsEnabled())
        .featureVoiceMessagesGroupChatsEnabled(settings.getFeatureVoiceMessagesGroupChatsEnabled())
        .featureVoiceMessagesSupervisionChatsEnabled(
            settings.getFeatureVoiceMessagesSupervisionChatsEnabled())
        .featureSystemNotificationEmailsEnabled(
            settings.getFeatureSystemNotificationEmailsEnabled())
        .smtp(toTenantSmtpSettings(settings.getSmtp()))
        .featureToolsOIDCToken(settings.getFeatureToolsOICDToken())
        .featureAttachmentUploadDisabled(settings.getFeatureAttachmentUploadDisabled())
        .activeLanguages(nullAsGerman(settings.getActiveLanguages()))
        .isVideoCallAllowed(settings.getIsVideoCallAllowed())
        .showAskerProfile(settings.getShowAskerProfile())
        .featureCentralDataProtectionTemplateEnabled(
            settings.getFeatureCentralDataProtectionTemplateEnabled())
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
            .address(tenant.getAddress())
            .description(tenant.getDescription())
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
            .address(tenant.getAddress())
            .description(tenant.getDescription())
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
    TenantSettings tenantSettings = JsonConverter.convertFromJson(settingsJson).applyDefaults();
    return new Settings()
        .topicsInRegistrationEnabled(tenantSettings.getTopicsInRegistrationEnabled())
        .featureDemographicsEnabled(tenantSettings.getFeatureDemographicsEnabled())
        .featureTopicsEnabled(tenantSettings.getFeatureTopicsEnabled())
        .featureAppointmentsEnabled(tenantSettings.getFeatureAppointmentsEnabled())
        .featureStatisticsEnabled(tenantSettings.getFeatureStatisticsEnabled())
        .featureGroupChatV2Enabled(tenantSettings.getFeatureGroupChatV2Enabled())
        .featureToolsOICDToken(tenantSettings.getFeatureToolsOIDCToken())
        .featureToolsEnabled(tenantSettings.getFeatureToolsEnabled())
        .featureAnonymousChatEnabled(tenantSettings.getFeatureAnonymousChatEnabled())
        .featureCallsEnabled(tenantSettings.getFeatureCallsEnabled())
        .featureSupervisionEnabled(tenantSettings.getFeatureSupervisionEnabled())
        .featureSupervisionAnonymousChatsEnabled(
            tenantSettings.getFeatureSupervisionAnonymousChatsEnabled())
        .featureSupervisionOneOnOneChatsEnabled(
            tenantSettings.getFeatureSupervisionOneOnOneChatsEnabled())
        .featureAudioCallsEnabled(tenantSettings.getFeatureAudioCallsEnabled())
        .featureAudioCallsAnonymousChatsEnabled(
            tenantSettings.getFeatureAudioCallsAnonymousChatsEnabled())
        .featureAudioCallsOneOnOneChatsEnabled(
            tenantSettings.getFeatureAudioCallsOneOnOneChatsEnabled())
        .featureAudioCallsGroupChatsEnabled(tenantSettings.getFeatureAudioCallsGroupChatsEnabled())
        .featureAudioCallsSupervisionChatsEnabled(
            tenantSettings.getFeatureAudioCallsSupervisionChatsEnabled())
        .featureVideoCallsEnabled(tenantSettings.getFeatureVideoCallsEnabled())
        .featureVideoCallsAnonymousChatsEnabled(
            tenantSettings.getFeatureVideoCallsAnonymousChatsEnabled())
        .featureVideoCallsOneOnOneChatsEnabled(
            tenantSettings.getFeatureVideoCallsOneOnOneChatsEnabled())
        .featureVideoCallsGroupChatsEnabled(tenantSettings.getFeatureVideoCallsGroupChatsEnabled())
        .featureVideoCallsSupervisionChatsEnabled(
            tenantSettings.getFeatureVideoCallsSupervisionChatsEnabled())
        .featureThreadsEnabled(tenantSettings.getFeatureThreadsEnabled())
        .featureThreadsAnonymousChatsEnabled(
            tenantSettings.getFeatureThreadsAnonymousChatsEnabled())
        .featureThreadsGroupChatsEnabled(tenantSettings.getFeatureThreadsGroupChatsEnabled())
        .featureThreadsOneOnOneEnabled(tenantSettings.getFeatureThreadsOneOnOneEnabled())
        .featureThreadsSupervisionChatsEnabled(
            tenantSettings.getFeatureThreadsSupervisionChatsEnabled())
        .featureVoiceMessagesEnabled(tenantSettings.getFeatureVoiceMessagesEnabled())
        .featureVoiceMessagesAnonymousChatsEnabled(
            tenantSettings.getFeatureVoiceMessagesAnonymousChatsEnabled())
        .featureVoiceMessagesOneOnOneChatsEnabled(
            tenantSettings.getFeatureVoiceMessagesOneOnOneChatsEnabled())
        .featureVoiceMessagesGroupChatsEnabled(
            tenantSettings.getFeatureVoiceMessagesGroupChatsEnabled())
        .featureVoiceMessagesSupervisionChatsEnabled(
            tenantSettings.getFeatureVoiceMessagesSupervisionChatsEnabled())
        .featureSystemNotificationEmailsEnabled(
            tenantSettings.getFeatureSystemNotificationEmailsEnabled())
        .smtp(toSmtpConfig(tenantSettings.getSmtp()))
        .featureAttachmentUploadDisabled(tenantSettings.getFeatureAttachmentUploadDisabled())
        .isVideoCallAllowed(tenantSettings.getIsVideoCallAllowed())
        .showAskerProfile(tenantSettings.getShowAskerProfile())
        .featureCentralDataProtectionTemplateEnabled(
            tenantSettings.getFeatureCentralDataProtectionTemplateEnabled())
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
        .settings(getRestrictedPublicSettings(tenant));
  }

  private Settings getRestrictedPublicSettings(TenantEntity tenant) {
    Settings settings = getSettings(tenant);
    settings.setFeatureToolsOICDToken(null);
    settings.setSmtp(toPublicSmtpConfig(settings.getSmtp()));
    return settings;
  }

  private SmtpConfig toPublicSmtpConfig(SmtpConfig smtpConfig) {
    if (smtpConfig == null) {
      return null;
    }
    return new SmtpConfig()
        .enabled(smtpConfig.getEnabled())
        .host(smtpConfig.getHost())
        .port(smtpConfig.getPort())
        .secure(smtpConfig.getSecure())
        .from(smtpConfig.getFrom())
        .emailThemeColor(smtpConfig.getEmailThemeColor());
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
            .address(tenant.getAddress())
            .description(tenant.getDescription())
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
