package com.vi.tenantservice.api.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.FeatureToggleDTO;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleDomainTenantOverrideServiceTest {

  @Mock TranslationService translationService;

  @Mock TenantConverter tenantConverter;

  @Mock ApplicationSettingsService applicationSettingsService;

  @InjectMocks SingleDomainTenantOverrideService singleDomainTenantOverrideService;

  @Test
  void overridePrivacyAndCertainSettings_Should_OverridePrivacyAndSettingFromMainTenant() {

    // given
    var mainTenant = new TenantEntity();
    mainTenant.setId(1L);
    var actualTenant = new TenantEntity();
    when(translationService.getCurrentLanguageContext()).thenReturn("de");
    when(tenantConverter.toRestrictedTenantDTO(mainTenant, "de"))
        .thenReturn(restrictedDTO("main privacy", LocalDateTime.now().minusDays(1), true));
    LocalDateTime actualPrivacyChangedDate = LocalDateTime.now();
    when(tenantConverter.toRestrictedTenantDTO(actualTenant, "de"))
        .thenReturn(restrictedDTO("actual privacy", actualPrivacyChangedDate, false));

    var applicationSettings =
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
            .ApplicationSettingsDTO();
    applicationSettings.setLegalContentChangesBySingleTenantAdminsAllowed(
        new FeatureToggleDTO().value(true));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(applicationSettings);
    // when
    RestrictedTenantDTO restrictedTenantDTO =
        singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            mainTenant, actualTenant);

    // then
    assertThat(restrictedTenantDTO.getContent().getPrivacy()).isEqualTo("actual privacy");
    assertThat(restrictedTenantDTO.getContent().getPrivacyLanguages())
        .isEqualTo(Map.of("de", "actual privacy"));
    assertThat(restrictedTenantDTO.getContent().getDataPrivacyConfirmation())
        .isEqualTo(actualPrivacyChangedDate);
    assertThat(restrictedTenantDTO.getSettings().getFeatureMediaUploadEnabled()).isFalse();
  }

  @Test
  void
      overridePrivacyAndCertainSettings_Should_NotOverridePrivacyButAllowOverrideSettingFromMainTenant_WhenTenantSpecificLegalTextEditionIsDisallowed() {

    // given
    var mainTenant = new TenantEntity();
    mainTenant.setId(1L);
    var actualTenant = new TenantEntity();
    when(translationService.getCurrentLanguageContext()).thenReturn("de");
    LocalDateTime mainPrivacyChangedDate = LocalDateTime.now().minusDays(1);
    when(tenantConverter.toRestrictedTenantDTO(mainTenant, "de"))
        .thenReturn(restrictedDTO("main privacy", mainPrivacyChangedDate, true));
    LocalDateTime actualPrivacyChangedDate = LocalDateTime.now();
    when(tenantConverter.toRestrictedTenantDTO(actualTenant, "de"))
        .thenReturn(restrictedDTO("actual privacy", actualPrivacyChangedDate, false));

    var applicationSettings =
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
            .ApplicationSettingsDTO();
    applicationSettings.setLegalContentChangesBySingleTenantAdminsAllowed(
        new FeatureToggleDTO().value(false));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(applicationSettings);
    // when
    RestrictedTenantDTO restrictedTenantDTO =
        singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            mainTenant, actualTenant);

    // then
    assertThat(restrictedTenantDTO.getContent().getPrivacy()).isEqualTo("main privacy");
    assertThat(restrictedTenantDTO.getContent().getDataPrivacyConfirmation())
        .isEqualTo(mainPrivacyChangedDate);
    assertThat(restrictedTenantDTO.getSettings().getFeatureMediaUploadEnabled()).isFalse();
  }

  private static RestrictedTenantDTO restrictedDTO(
      String privacy, LocalDateTime privacyChangedDate, boolean mediaUploadEnabled) {
    return new RestrictedTenantDTO()
        .content(
            new Content()
                .privacy(privacy)
                .privacyLanguages(Map.of("de", privacy))
                .dataPrivacyConfirmation(privacyChangedDate))
        .settings(new Settings().featureMediaUploadEnabled(mediaUploadEnabled));
  }
}
