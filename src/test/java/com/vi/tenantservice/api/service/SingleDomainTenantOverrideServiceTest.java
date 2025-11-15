package com.vi.tenantservice.api.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled;
import java.time.LocalDateTime;
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
        .thenReturn(restrictedDTO("main privacy", LocalDateTime.now().minusDays(1), false));
    LocalDateTime actualPrivacyChangedDate = LocalDateTime.now();
    when(tenantConverter.toRestrictedTenantDTO(actualTenant, "de"))
        .thenReturn(restrictedDTO("actual privacy", actualPrivacyChangedDate, true));

    var applicationSettings =
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
            .ApplicationSettingsDTO();
    applicationSettings.setLegalContentChangesBySingleTenantAdminsAllowed(
        new ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled().value(true));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(applicationSettings);
    // when
    RestrictedTenantDTO restrictedTenantDTO =
        singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            mainTenant, actualTenant);

    // then
    assertThat(restrictedTenantDTO.getContent().getPrivacy()).isEqualTo("actual privacy");
    assertThat(restrictedTenantDTO.getContent().getDataPrivacyConfirmation())
        .isEqualTo(actualPrivacyChangedDate);
    assertThat(restrictedTenantDTO.getSettings().getFeatureAttachmentUploadDisabled()).isTrue();
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
        .thenReturn(restrictedDTO("main privacy", mainPrivacyChangedDate, false));
    LocalDateTime actualPrivacyChangedDate = LocalDateTime.now();
    when(tenantConverter.toRestrictedTenantDTO(actualTenant, "de"))
        .thenReturn(restrictedDTO("actual privacy", actualPrivacyChangedDate, true));

    var applicationSettings =
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
            .ApplicationSettingsDTO();
    applicationSettings.setLegalContentChangesBySingleTenantAdminsAllowed(
        new ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled().value(false));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(applicationSettings);
    // when
    RestrictedTenantDTO restrictedTenantDTO =
        singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            mainTenant, actualTenant);

    // then
    assertThat(restrictedTenantDTO.getContent().getPrivacy()).isEqualTo("main privacy");
    assertThat(restrictedTenantDTO.getContent().getDataPrivacyConfirmation())
        .isEqualTo(mainPrivacyChangedDate);
    assertThat(restrictedTenantDTO.getSettings().getFeatureAttachmentUploadDisabled()).isTrue();
  }

  private static RestrictedTenantDTO restrictedDTO(
      String privacy, LocalDateTime privacyChangedDate, boolean featureAttachmentUploadDisabled) {
    return new RestrictedTenantDTO()
        .content(new Content().privacy(privacy).dataPrivacyConfirmation(privacyChangedDate))
        .settings(new Settings().featureAttachmentUploadDisabled(featureAttachmentUploadDisabled));
  }
}
