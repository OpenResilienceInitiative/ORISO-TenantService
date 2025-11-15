package com.vi.tenantservice.api.converter;

import static com.vi.tenantservice.api.converter.ConverterUtils.nullAsFalse;
import static com.vi.tenantservice.api.converter.ConverterUtils.nullAsGerman;
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
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantEntity.TenantEntityBuilder;
import com.vi.tenantservice.api.model.TenantSettings;
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
        .featureToolsOIDCToken(settings.getFeatureToolsOICDToken())
        .featureAttachmentUploadDisabled(nullAsFalse(settings.getFeatureAttachmentUploadDisabled()))
        .activeLanguages(nullAsGerman(settings.getActiveLanguages()))
        .isVideoCallAllowed(nullAsFalse(settings.getIsVideoCallAllowed()))
        .showAskerProfile(nullAsFalse(settings.getShowAskerProfile()))
        .featureCentralDataProtectionTemplateEnabled(
            nullAsFalse(settings.getFeatureCentralDataProtectionTemplateEnabled()))
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
    return new Settings()
        .topicsInRegistrationEnabled(tenantSettings.isTopicsInRegistrationEnabled())
        .featureDemographicsEnabled(tenantSettings.isFeatureDemographicsEnabled())
        .featureTopicsEnabled(tenantSettings.isFeatureTopicsEnabled())
        .featureAppointmentsEnabled(tenantSettings.isFeatureAppointmentsEnabled())
        .featureStatisticsEnabled(tenantSettings.isFeatureStatisticsEnabled())
        .featureGroupChatV2Enabled(tenantSettings.isFeatureGroupChatV2Enabled())
        .featureToolsOICDToken(tenantSettings.getFeatureToolsOIDCToken())
        .featureToolsEnabled(tenantSettings.isFeatureToolsEnabled())
        .featureAttachmentUploadDisabled(tenantSettings.isFeatureAttachmentUploadDisabled())
        .isVideoCallAllowed(tenantSettings.isVideoCallAllowed())
        .showAskerProfile(tenantSettings.isShowAskerProfile())
        .featureCentralDataProtectionTemplateEnabled(
            tenantSettings.isFeatureCentralDataProtectionTemplateEnabled())
        .activeLanguages(nullAsGerman(tenantSettings.getActiveLanguages()));
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
