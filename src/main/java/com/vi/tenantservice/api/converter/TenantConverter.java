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
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.NoAgencyContextDTO;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.SmtpConfig;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantData;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantEntity.TenantEntityBuilder;
import com.vi.tenantservice.api.model.TenantRestrictedData;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.TenantSmtpSettings;
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
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

  private final @NonNull SmtpPasswordEncryptionService smtpPasswordEncryptionService;

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
        .featureTeamDiscussionEnabled(settings.getFeatureTeamDiscussionEnabled())
        .featureToolsEnabled(settings.getFeatureToolsEnabled())
        .featureAnonymousChatEnabled(settings.getFeatureAnonymousChatEnabled())
        .featureDisplayNameEditable(settings.getFeatureDisplayNameEditable())
        .featureAskerEmailEnabled(settings.getFeatureAskerEmailEnabled())
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
        .featureMediaUploadEnabled(settings.getFeatureMediaUploadEnabled())
        .featureMediaUploadAnonymousChatsEnabled(
            settings.getFeatureMediaUploadAnonymousChatsEnabled())
        .featureMediaUploadOneOnOneChatsEnabled(
            settings.getFeatureMediaUploadOneOnOneChatsEnabled())
        .featureMediaUploadGroupChatsEnabled(settings.getFeatureMediaUploadGroupChatsEnabled())
        .featureMediaUploadSupervisionChatsEnabled(
            settings.getFeatureMediaUploadSupervisionChatsEnabled())
        .featureMediaInlineDisplayEnabled(settings.getFeatureMediaInlineDisplayEnabled())
        .featureMediaInlineDisplayAnonymousChatsEnabled(
            settings.getFeatureMediaInlineDisplayAnonymousChatsEnabled())
        .featureMediaInlineDisplayOneOnOneChatsEnabled(
            settings.getFeatureMediaInlineDisplayOneOnOneChatsEnabled())
        .featureMediaInlineDisplayGroupChatsEnabled(
            settings.getFeatureMediaInlineDisplayGroupChatsEnabled())
        .featureMediaInlineDisplaySupervisionChatsEnabled(
            settings.getFeatureMediaInlineDisplaySupervisionChatsEnabled())
        .featureMediaAiScanEnabled(settings.getFeatureMediaAiScanEnabled())
        .featureMediaAiScanAnonymousChatsEnabled(
            settings.getFeatureMediaAiScanAnonymousChatsEnabled())
        .featureMediaAiScanOneOnOneChatsEnabled(
            settings.getFeatureMediaAiScanOneOnOneChatsEnabled())
        .featureMediaAiScanGroupChatsEnabled(settings.getFeatureMediaAiScanGroupChatsEnabled())
        .featureMediaAiScanSupervisionChatsEnabled(
            settings.getFeatureMediaAiScanSupervisionChatsEnabled())
        .activeLanguages(nullAsGerman(settings.getActiveLanguages()))
        .isVideoCallAllowed(settings.getIsVideoCallAllowed())
        .showAskerProfile(settings.getShowAskerProfile())
        .emailVisible(settings.getEmailVisible())
        .emailRequired(settings.getEmailRequired())
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
        "contentTermsAndConditionsActivationDate",
        "contentDataProcessingAgreement",
        "contentDataProcessingAgreementActivationDate");
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
          .themingSecondaryColor(tenantDTO.getTheming().getSecondaryColor())
          .themingAccent(tenantDTO.getTheming().getAccent())
          .themingSignal(tenantDTO.getTheming().getSignal());
    }
  }

  public MultilingualTenantDTO toMultilingualDTO(TenantData tenant) {
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

  public TenantDTO toDTO(TenantData tenant, String lang) {
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

  private Settings getSettings(TenantRestrictedData tenant) {
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
        .featureTeamDiscussionEnabled(tenantSettings.getFeatureTeamDiscussionEnabled())
        .featureToolsOICDToken(tenantSettings.getFeatureToolsOIDCToken())
        .featureToolsEnabled(tenantSettings.getFeatureToolsEnabled())
        .featureAnonymousChatEnabled(tenantSettings.getFeatureAnonymousChatEnabled())
        .featureDisplayNameEditable(tenantSettings.getFeatureDisplayNameEditable())
        .featureAskerEmailEnabled(tenantSettings.getFeatureAskerEmailEnabled())
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
        .featureMediaUploadEnabled(tenantSettings.getFeatureMediaUploadEnabled())
        .featureMediaUploadAnonymousChatsEnabled(
            tenantSettings.getFeatureMediaUploadAnonymousChatsEnabled())
        .featureMediaUploadOneOnOneChatsEnabled(
            tenantSettings.getFeatureMediaUploadOneOnOneChatsEnabled())
        .featureMediaUploadGroupChatsEnabled(
            tenantSettings.getFeatureMediaUploadGroupChatsEnabled())
        .featureMediaUploadSupervisionChatsEnabled(
            tenantSettings.getFeatureMediaUploadSupervisionChatsEnabled())
        .featureMediaInlineDisplayEnabled(tenantSettings.getFeatureMediaInlineDisplayEnabled())
        .featureMediaInlineDisplayAnonymousChatsEnabled(
            tenantSettings.getFeatureMediaInlineDisplayAnonymousChatsEnabled())
        .featureMediaInlineDisplayOneOnOneChatsEnabled(
            tenantSettings.getFeatureMediaInlineDisplayOneOnOneChatsEnabled())
        .featureMediaInlineDisplayGroupChatsEnabled(
            tenantSettings.getFeatureMediaInlineDisplayGroupChatsEnabled())
        .featureMediaInlineDisplaySupervisionChatsEnabled(
            tenantSettings.getFeatureMediaInlineDisplaySupervisionChatsEnabled())
        .featureMediaAiScanEnabled(tenantSettings.getFeatureMediaAiScanEnabled())
        .featureMediaAiScanAnonymousChatsEnabled(
            tenantSettings.getFeatureMediaAiScanAnonymousChatsEnabled())
        .featureMediaAiScanOneOnOneChatsEnabled(
            tenantSettings.getFeatureMediaAiScanOneOnOneChatsEnabled())
        .featureMediaAiScanGroupChatsEnabled(
            tenantSettings.getFeatureMediaAiScanGroupChatsEnabled())
        .featureMediaAiScanSupervisionChatsEnabled(
            tenantSettings.getFeatureMediaAiScanSupervisionChatsEnabled())
        .isVideoCallAllowed(tenantSettings.getIsVideoCallAllowed())
        .showAskerProfile(tenantSettings.getShowAskerProfile())
        .emailVisible(tenantSettings.getEmailVisible())
        .emailRequired(tenantSettings.getEmailRequired())
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
        .enforcedPermissionToggles(
            toEnforcedPermissionTogglesSettings(tenantAdminControls.getEnforcedPermissionToggles()))
        .permissionPolicies(toPermissionPolicySettings(tenantAdminControls.getPermissionPolicies()))
        .caseHandoverPolicies(tenantAdminControls.getCaseHandoverPolicies())
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
        .mediaUpload(nullAsTrue(allowedPermissionToggles.getMediaUpload()))
        .mediaUploadAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getMediaUploadAnonymousChats()))
        .mediaUploadOneOnOneChats(
            nullAsTrue(allowedPermissionToggles.getMediaUploadOneOnOneChats()))
        .mediaUploadGroupChats(nullAsTrue(allowedPermissionToggles.getMediaUploadGroupChats()))
        .mediaUploadSupervisionChats(
            nullAsTrue(allowedPermissionToggles.getMediaUploadSupervisionChats()))
        .mediaInlineDisplay(nullAsTrue(allowedPermissionToggles.getMediaInlineDisplay()))
        .mediaInlineDisplayAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getMediaInlineDisplayAnonymousChats()))
        .mediaInlineDisplayOneOnOneChats(
            nullAsTrue(allowedPermissionToggles.getMediaInlineDisplayOneOnOneChats()))
        .mediaInlineDisplayGroupChats(
            nullAsTrue(allowedPermissionToggles.getMediaInlineDisplayGroupChats()))
        .mediaInlineDisplaySupervisionChats(
            nullAsTrue(allowedPermissionToggles.getMediaInlineDisplaySupervisionChats()))
        .mediaAiScan(nullAsTrue(allowedPermissionToggles.getMediaAiScan()))
        .mediaAiScanAnonymousChats(
            nullAsTrue(allowedPermissionToggles.getMediaAiScanAnonymousChats()))
        .mediaAiScanOneOnOneChats(
            nullAsTrue(allowedPermissionToggles.getMediaAiScanOneOnOneChats()))
        .mediaAiScanGroupChats(nullAsTrue(allowedPermissionToggles.getMediaAiScanGroupChats()))
        .mediaAiScanSupervisionChats(
            nullAsTrue(allowedPermissionToggles.getMediaAiScanSupervisionChats()))
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
                tenantAdminControlsSettings.getAllowedPermissionToggles()))
        .enforcedPermissionToggles(
            toEnforcedPermissionToggles(tenantAdminControlsSettings.getEnforcedPermissionToggles()))
        .permissionPolicies(
            toBooleanPermissionPolicies(tenantAdminControlsSettings.getPermissionPolicies()))
        .caseHandoverPolicies(tenantAdminControlsSettings.getCaseHandoverPolicies());
  }

  private Map<String, PolicyValue<Boolean>> toPermissionPolicySettings(
      Map<String, BooleanPermissionPolicy> permissionPolicies) {
    if (permissionPolicies == null) {
      return null;
    }
    return permissionPolicies.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry ->
                    new PolicyValue<>(
                        entry.getValue().getValue(),
                        com.vi.tenantservice.api.policy.PermissionPolicyMode.valueOf(
                            entry.getValue().getMode().name()))));
  }

  private Map<String, BooleanPermissionPolicy> toBooleanPermissionPolicies(
      Map<String, PolicyValue<Boolean>> permissionPolicies) {
    if (permissionPolicies == null) {
      return null;
    }
    return permissionPolicies.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry ->
                    new BooleanPermissionPolicy(
                        entry.getValue().value(),
                        PermissionPolicyMode.valueOf(entry.getValue().mode().name()))));
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
            nullAsTrue(allowedPermissionTogglesSettings.getVoiceMessagesSupervisionChats()))
        .mediaUpload(nullAsTrue(allowedPermissionTogglesSettings.getMediaUpload()))
        .mediaUploadAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaUploadAnonymousChats()))
        .mediaUploadOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaUploadOneOnOneChats()))
        .mediaUploadGroupChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaUploadGroupChats()))
        .mediaUploadSupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaUploadSupervisionChats()))
        .mediaInlineDisplay(nullAsTrue(allowedPermissionTogglesSettings.getMediaInlineDisplay()))
        .mediaInlineDisplayAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaInlineDisplayAnonymousChats()))
        .mediaInlineDisplayOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaInlineDisplayOneOnOneChats()))
        .mediaInlineDisplayGroupChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaInlineDisplayGroupChats()))
        .mediaInlineDisplaySupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaInlineDisplaySupervisionChats()))
        .mediaAiScan(nullAsTrue(allowedPermissionTogglesSettings.getMediaAiScan()))
        .mediaAiScanAnonymousChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaAiScanAnonymousChats()))
        .mediaAiScanOneOnOneChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaAiScanOneOnOneChats()))
        .mediaAiScanGroupChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaAiScanGroupChats()))
        .mediaAiScanSupervisionChats(
            nullAsTrue(allowedPermissionTogglesSettings.getMediaAiScanSupervisionChats()));
  }

  private TenantAdminAllowedPermissionTogglesSettings toEnforcedPermissionTogglesSettings(
      TenantAdminAllowedPermissionToggles allowedPermissionToggles) {
    if (allowedPermissionToggles == null) {
      return null;
    }
    return TenantAdminAllowedPermissionTogglesSettings.builder()
        .appearance(nullAsFalse(allowedPermissionToggles.getAppearance()))
        .anonymousChat(nullAsFalse(allowedPermissionToggles.getAnonymousChat()))
        .calls(nullAsFalse(allowedPermissionToggles.getCalls()))
        .groupChat(nullAsFalse(allowedPermissionToggles.getGroupChat()))
        .supervision(nullAsFalse(allowedPermissionToggles.getSupervision()))
        .supervisionAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getSupervisionAnonymousChats()))
        .supervisionOneOnOneChats(
            nullAsFalse(allowedPermissionToggles.getSupervisionOneOnOneChats()))
        .audioCalls(nullAsFalse(allowedPermissionToggles.getAudioCalls()))
        .audioCallsAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getAudioCallsAnonymousChats()))
        .audioCallsOneOnOneChats(nullAsFalse(allowedPermissionToggles.getAudioCallsOneOnOneChats()))
        .audioCallsGroupChats(nullAsFalse(allowedPermissionToggles.getAudioCallsGroupChats()))
        .audioCallsSupervisionChats(
            nullAsFalse(allowedPermissionToggles.getAudioCallsSupervisionChats()))
        .videoCalls(nullAsFalse(allowedPermissionToggles.getVideoCalls()))
        .videoCallsAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getVideoCallsAnonymousChats()))
        .videoCallsOneOnOneChats(nullAsFalse(allowedPermissionToggles.getVideoCallsOneOnOneChats()))
        .videoCallsGroupChats(nullAsFalse(allowedPermissionToggles.getVideoCallsGroupChats()))
        .videoCallsSupervisionChats(
            nullAsFalse(allowedPermissionToggles.getVideoCallsSupervisionChats()))
        .threads(nullAsFalse(allowedPermissionToggles.getThreads()))
        .threadsAnonymousChats(nullAsFalse(allowedPermissionToggles.getThreadsAnonymousChats()))
        .threadsOneOnOneChats(nullAsFalse(allowedPermissionToggles.getThreadsOneOnOneChats()))
        .threadsGroupChats(nullAsFalse(allowedPermissionToggles.getThreadsGroupChats()))
        .threadsSupervisionChats(nullAsFalse(allowedPermissionToggles.getThreadsSupervisionChats()))
        .voiceMessages(nullAsFalse(allowedPermissionToggles.getVoiceMessages()))
        .voiceMessagesAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getVoiceMessagesAnonymousChats()))
        .voiceMessagesOneOnOneChats(
            nullAsFalse(allowedPermissionToggles.getVoiceMessagesOneOnOneChats()))
        .voiceMessagesGroupChats(nullAsFalse(allowedPermissionToggles.getVoiceMessagesGroupChats()))
        .voiceMessagesSupervisionChats(
            nullAsFalse(allowedPermissionToggles.getVoiceMessagesSupervisionChats()))
        .mediaUpload(nullAsFalse(allowedPermissionToggles.getMediaUpload()))
        .mediaUploadAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getMediaUploadAnonymousChats()))
        .mediaUploadOneOnOneChats(
            nullAsFalse(allowedPermissionToggles.getMediaUploadOneOnOneChats()))
        .mediaUploadGroupChats(nullAsFalse(allowedPermissionToggles.getMediaUploadGroupChats()))
        .mediaUploadSupervisionChats(
            nullAsFalse(allowedPermissionToggles.getMediaUploadSupervisionChats()))
        .mediaInlineDisplay(nullAsFalse(allowedPermissionToggles.getMediaInlineDisplay()))
        .mediaInlineDisplayAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getMediaInlineDisplayAnonymousChats()))
        .mediaInlineDisplayOneOnOneChats(
            nullAsFalse(allowedPermissionToggles.getMediaInlineDisplayOneOnOneChats()))
        .mediaInlineDisplayGroupChats(
            nullAsFalse(allowedPermissionToggles.getMediaInlineDisplayGroupChats()))
        .mediaInlineDisplaySupervisionChats(
            nullAsFalse(allowedPermissionToggles.getMediaInlineDisplaySupervisionChats()))
        .mediaAiScan(nullAsFalse(allowedPermissionToggles.getMediaAiScan()))
        .mediaAiScanAnonymousChats(
            nullAsFalse(allowedPermissionToggles.getMediaAiScanAnonymousChats()))
        .mediaAiScanOneOnOneChats(
            nullAsFalse(allowedPermissionToggles.getMediaAiScanOneOnOneChats()))
        .mediaAiScanGroupChats(nullAsFalse(allowedPermissionToggles.getMediaAiScanGroupChats()))
        .mediaAiScanSupervisionChats(
            nullAsFalse(allowedPermissionToggles.getMediaAiScanSupervisionChats()))
        .build();
  }

  private TenantAdminAllowedPermissionToggles toEnforcedPermissionToggles(
      TenantAdminAllowedPermissionTogglesSettings allowedPermissionTogglesSettings) {
    if (allowedPermissionTogglesSettings == null) {
      return null;
    }
    return new TenantAdminAllowedPermissionToggles()
        .appearance(nullAsFalse(allowedPermissionTogglesSettings.getAppearance()))
        .anonymousChat(nullAsFalse(allowedPermissionTogglesSettings.getAnonymousChat()))
        .calls(nullAsFalse(allowedPermissionTogglesSettings.getCalls()))
        .groupChat(nullAsFalse(allowedPermissionTogglesSettings.getGroupChat()))
        .supervision(nullAsFalse(allowedPermissionTogglesSettings.getSupervision()))
        .supervisionAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getSupervisionAnonymousChats()))
        .supervisionOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getSupervisionOneOnOneChats()))
        .audioCalls(nullAsFalse(allowedPermissionTogglesSettings.getAudioCalls()))
        .audioCallsAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getAudioCallsAnonymousChats()))
        .audioCallsOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getAudioCallsOneOnOneChats()))
        .audioCallsGroupChats(
            nullAsFalse(allowedPermissionTogglesSettings.getAudioCallsGroupChats()))
        .audioCallsSupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getAudioCallsSupervisionChats()))
        .videoCalls(nullAsFalse(allowedPermissionTogglesSettings.getVideoCalls()))
        .videoCallsAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVideoCallsAnonymousChats()))
        .videoCallsOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVideoCallsOneOnOneChats()))
        .videoCallsGroupChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVideoCallsGroupChats()))
        .videoCallsSupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVideoCallsSupervisionChats()))
        .threads(nullAsFalse(allowedPermissionTogglesSettings.getThreads()))
        .threadsAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getThreadsAnonymousChats()))
        .threadsOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getThreadsOneOnOneChats()))
        .threadsGroupChats(nullAsFalse(allowedPermissionTogglesSettings.getThreadsGroupChats()))
        .threadsSupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getThreadsSupervisionChats()))
        .voiceMessages(nullAsFalse(allowedPermissionTogglesSettings.getVoiceMessages()))
        .voiceMessagesAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVoiceMessagesAnonymousChats()))
        .voiceMessagesOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVoiceMessagesOneOnOneChats()))
        .voiceMessagesGroupChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVoiceMessagesGroupChats()))
        .voiceMessagesSupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getVoiceMessagesSupervisionChats()))
        .mediaUpload(nullAsFalse(allowedPermissionTogglesSettings.getMediaUpload()))
        .mediaUploadAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaUploadAnonymousChats()))
        .mediaUploadOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaUploadOneOnOneChats()))
        .mediaUploadGroupChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaUploadGroupChats()))
        .mediaUploadSupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaUploadSupervisionChats()))
        .mediaInlineDisplay(nullAsFalse(allowedPermissionTogglesSettings.getMediaInlineDisplay()))
        .mediaInlineDisplayAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaInlineDisplayAnonymousChats()))
        .mediaInlineDisplayOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaInlineDisplayOneOnOneChats()))
        .mediaInlineDisplayGroupChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaInlineDisplayGroupChats()))
        .mediaInlineDisplaySupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaInlineDisplaySupervisionChats()))
        .mediaAiScan(nullAsFalse(allowedPermissionTogglesSettings.getMediaAiScan()))
        .mediaAiScanAnonymousChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaAiScanAnonymousChats()))
        .mediaAiScanOneOnOneChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaAiScanOneOnOneChats()))
        .mediaAiScanGroupChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaAiScanGroupChats()))
        .mediaAiScanSupervisionChats(
            nullAsFalse(allowedPermissionTogglesSettings.getMediaAiScanSupervisionChats()));
  }

  /** Placeholder some clients send back instead of a real password; never a stored value. */
  public static final String SMTP_PASSWORD_MASK = "********";

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
        .password(
            smtpPasswordEncryptionService.encryptNewPassword(
                normalizeIncomingSmtpPassword(smtpConfig.getPassword())))
        .from(smtpConfig.getFrom())
        .emailThemeColor(smtpConfig.getEmailThemeColor())
        .build();
  }

  /** Blank or masked passwords mean "unchanged" (write-only contract, #182). */
  private static String normalizeIncomingSmtpPassword(String password) {
    if (password == null || password.isBlank() || SMTP_PASSWORD_MASK.equals(password)) {
      return null;
    }
    return password;
  }

  private SmtpConfig toSmtpConfig(TenantSmtpSettings smtpSettings) {
    if (smtpSettings == null) {
      return null;
    }
    // write-only contract (#182): the stored password never leaves the service
    return new SmtpConfig()
        .enabled(smtpSettings.isEnabled())
        .host(smtpSettings.getHost())
        .port(smtpSettings.getPort())
        .secure(smtpSettings.isSecure())
        .username(smtpSettings.getUsername())
        .passwordSet(smtpSettings.getPassword() != null && !smtpSettings.getPassword().isBlank())
        .from(smtpSettings.getFrom())
        .emailThemeColor(smtpSettings.getEmailThemeColor());
  }

  public RestrictedTenantDTO toRestrictedTenantDTO(TenantRestrictedData tenant, String lang) {
    return new RestrictedTenantDTO(tenant.getId(), tenant.getName())
        .content(toContentDTO(tenant, lang))
        .theming(toThemingDTO(tenant))
        .subdomain(tenant.getSubdomain())
        .settings(getRestrictedPublicSettings(tenant));
  }

  private Settings getRestrictedPublicSettings(TenantRestrictedData tenant) {
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

  public BasicTenantLicensingDTO toBasicLicensingTenantDTO(TenantData tenant) {
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

  public Licensing toLicensingDTO(TenantData tenant) {
    return new Licensing(tenant.getLicensingAllowedNumberOfUsers());
  }

  private Theming toThemingDTO(TenantRestrictedData tenant) {
    return new Theming()
        .favicon(tenant.getThemingFavicon())
        .logo(tenant.getThemingLogo())
        .associationLogo(tenant.getThemingAssociationLogo())
        .primaryColor(tenant.getThemingPrimaryColor())
        .secondaryColor(tenant.getThemingSecondaryColor())
        .accent(tenant.getThemingAccent())
        .signal(tenant.getThemingSignal());
  }

  private Content toContentDTO(TenantRestrictedData tenant, String lang) {
    String privacyPotentiallyWithPlaceholders =
        getTranslatedStringFromMap(tenant.getContentPrivacy(), lang);
    DataProtectionContactTemplateDTO dataProtectionContactTemplate =
        getDataProtectionContactTemplate(lang);
    return new Content(getTranslatedStringFromMap(tenant.getContentImpressum(), lang))
        // Raw stored language maps (incl. the <lang>__meta machine-translation metadata keys)
        // alongside the resolved strings, so public clients can show a "machine translated"
        // notice. Kept lean on purpose: only for the legal contents impressum and privacy.
        .impressumLanguages(convertMapFromJson(tenant.getContentImpressum()))
        .privacyLanguages(convertMapFromJson(tenant.getContentPrivacy()))
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

  private MultilingualContent toMultilingualContentDTO(TenantData tenant) {
    return new MultilingualContent(convertMapFromJson(tenant.getContentImpressum()))
        .claim(convertMapFromJson(tenant.getContentClaim()))
        .privacy(convertMapFromJson(tenant.getContentPrivacy()))
        .termsAndConditions(convertMapFromJson(tenant.getContentTermsAndConditions()))
        .dataProtectionContactTemplate(getMultilingualDataProtectionTemplate());
  }

  public AdminTenantDTO toAdminTenantDTO(TenantData tenant) {
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
