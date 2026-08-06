package com.vi.tenantservice.api.service;

import static com.vi.tenantservice.api.converter.ConverterUtils.nullAsFalse;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.TenantRestrictedData;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.FeatureToggleDTO;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SingleDomainTenantOverrideService {

  private final @NonNull TenantConverter tenantConverter;
  private final @NonNull TranslationService translationService;

  private final @NonNull ApplicationSettingsService applicationSettingsService;

  public RestrictedTenantDTO overridePrivacyAndCertainSettings(
      TenantRestrictedData mainTenant, TenantRestrictedData actualTenant) {
    String lang = translationService.getCurrentLanguageContext();

    RestrictedTenantDTO mainTenantRestrictedDTO = getRestrictedTenantDTO(mainTenant, lang);
    RestrictedTenantDTO overridingRestrictedTenantDTO = getRestrictedTenantDTO(actualTenant, lang);

    if (isContentOverrideAllowed()) {
      overrideContent(mainTenantRestrictedDTO, overridingRestrictedTenantDTO);
    }
    overrideSettings(mainTenantRestrictedDTO, overridingRestrictedTenantDTO);
    return mainTenantRestrictedDTO;
  }

  private boolean isContentOverrideAllowed() {
    FeatureToggleDTO legalContentChangesBySingleTenantAdminsAllowed =
        applicationSettingsService
            .getApplicationSettings()
            .getLegalContentChangesBySingleTenantAdminsAllowed();
    return legalContentChangesBySingleTenantAdminsAllowed != null
        && nullAsFalse(legalContentChangesBySingleTenantAdminsAllowed.getValue());
  }

  private static void overrideSettings(
      RestrictedTenantDTO mainTenantRestrictedDTO,
      RestrictedTenantDTO overridingRestrictedTenantDTO) {
    var mainSettings = mainTenantRestrictedDTO.getSettings();
    var actualSettings = overridingRestrictedTenantDTO.getSettings();
    mainSettings.setFeatureMediaUploadEnabled(actualSettings.getFeatureMediaUploadEnabled());
    mainSettings.setFeatureMediaUploadAnonymousChatsEnabled(
        actualSettings.getFeatureMediaUploadAnonymousChatsEnabled());
    mainSettings.setFeatureMediaUploadOneOnOneChatsEnabled(
        actualSettings.getFeatureMediaUploadOneOnOneChatsEnabled());
    mainSettings.setFeatureMediaUploadGroupChatsEnabled(
        actualSettings.getFeatureMediaUploadGroupChatsEnabled());
    mainSettings.setFeatureMediaUploadSupervisionChatsEnabled(
        actualSettings.getFeatureMediaUploadSupervisionChatsEnabled());
    mainSettings.setFeatureMediaInlineDisplayEnabled(
        actualSettings.getFeatureMediaInlineDisplayEnabled());
    mainSettings.setFeatureMediaInlineDisplayAnonymousChatsEnabled(
        actualSettings.getFeatureMediaInlineDisplayAnonymousChatsEnabled());
    mainSettings.setFeatureMediaInlineDisplayOneOnOneChatsEnabled(
        actualSettings.getFeatureMediaInlineDisplayOneOnOneChatsEnabled());
    mainSettings.setFeatureMediaInlineDisplayGroupChatsEnabled(
        actualSettings.getFeatureMediaInlineDisplayGroupChatsEnabled());
    mainSettings.setFeatureMediaInlineDisplaySupervisionChatsEnabled(
        actualSettings.getFeatureMediaInlineDisplaySupervisionChatsEnabled());
    mainSettings.setFeatureMediaAiScanEnabled(actualSettings.getFeatureMediaAiScanEnabled());
    mainSettings.setFeatureMediaAiScanAnonymousChatsEnabled(
        actualSettings.getFeatureMediaAiScanAnonymousChatsEnabled());
    mainSettings.setFeatureMediaAiScanOneOnOneChatsEnabled(
        actualSettings.getFeatureMediaAiScanOneOnOneChatsEnabled());
    mainSettings.setFeatureMediaAiScanGroupChatsEnabled(
        actualSettings.getFeatureMediaAiScanGroupChatsEnabled());
    mainSettings.setFeatureMediaAiScanSupervisionChatsEnabled(
        actualSettings.getFeatureMediaAiScanSupervisionChatsEnabled());
  }

  private RestrictedTenantDTO getRestrictedTenantDTO(TenantRestrictedData mainTenant, String lang) {
    return tenantConverter.toRestrictedTenantDTO(mainTenant, lang);
  }

  private static void overrideContent(
      RestrictedTenantDTO restrictedTenantDTO, RestrictedTenantDTO overridingRestrictedTenantDTO) {
    if (overridingRestrictedTenantDTO.getContent() != null) {
      restrictedTenantDTO
          .getContent()
          .dataPrivacyConfirmation(
              overridingRestrictedTenantDTO.getContent().getDataPrivacyConfirmation())
          // keep the raw language map (incl. __meta keys) consistent with the overridden privacy
          .privacyLanguages(overridingRestrictedTenantDTO.getContent().getPrivacyLanguages())
          .setPrivacy(overridingRestrictedTenantDTO.getContent().getPrivacy());
    }
  }
}
