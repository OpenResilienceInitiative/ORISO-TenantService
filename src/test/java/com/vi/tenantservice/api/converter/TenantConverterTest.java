package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.NoAgencyContextDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.SmtpConfig;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.TemplateDescriptionServiceException;
import com.vi.tenantservice.api.service.TemplateRenderer;
import com.vi.tenantservice.api.service.TemplateService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantConverterTest {

  @InjectMocks TenantConverter tenantConverter;

  @Mock TemplateService templateService;

  @Mock TemplateRenderer templateRenderer;

  @Test
  void toEntity_should_convertToEntityAndBackToDTO() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .withSettings()
            .build();
    tenantDTO.getSettings().extendedSettings(null);
    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then
    TenantDTO converted = tenantConverter.toDTO(entity, "de");
    assertThat(converted.getId()).isEqualTo(tenantDTO.getId());
    assertThat(converted.getName()).isEqualTo(tenantDTO.getName());
    assertThat(converted.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(converted.getLicensing()).isEqualTo(tenantDTO.getLicensing());
    assertCoreSettingsAreConverted(tenantDTO.getSettings(), converted.getSettings());
    assertThat(converted.getTheming()).isEqualTo(tenantDTO.getTheming());
    assertThat(converted.getSettings().getIsVideoCallAllowed()).isTrue();
    assertThat(converted.getSettings().getShowAskerProfile()).isTrue();
    // content comparision is skipped, due to i18n feature, so the structure is different
  }

  @Test
  void toEntity_should_roundTripAddressAndDescription() {
    // given
    MultilingualTenantDTO tenantDTO = new MultilingualTenantTestDataBuilder().tenantDTO().build();
    tenantDTO.setAddress("Musterstraße 1, 12345 Musterstadt");
    tenantDTO.setDescription("Short description of the tenant.");

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then
    assertThat(entity.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(entity.getDescription()).isEqualTo("Short description of the tenant.");

    // and entity -> DTO conversions preserve the values
    TenantDTO converted = tenantConverter.toDTO(entity, "de");
    assertThat(converted.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(converted.getDescription()).isEqualTo("Short description of the tenant.");

    MultilingualTenantDTO multilingualConverted = tenantConverter.toMultilingualDTO(entity);
    assertThat(multilingualConverted.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(multilingualConverted.getDescription())
        .isEqualTo("Short description of the tenant.");

    AdminTenantDTO adminConverted = tenantConverter.toAdminTenantDTO(entity);
    assertThat(adminConverted.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(adminConverted.getDescription()).isEqualTo("Short description of the tenant.");
  }

  @Test
  void toRestrictedTenantDTO_should_convertAttributesProperly()
      throws TemplateException, IOException, TemplateDescriptionServiceException {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .withSettings()
            .build();
    tenantDTO.getSettings().extendedSettings(null);

    when(templateService.getMultilingualDataProtectionTemplate())
        .thenReturn(
            Map.of(
                "de",
                new DataProtectionContactTemplateDTO()
                    .noAgencyContext(
                        new NoAgencyContextDTO().dataProtectionOfficerContact("test"))));

    TenantEntity entity = tenantConverter.toEntity(tenantDTO);
    when(templateRenderer.renderTemplate(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("renderedPrivacy");
    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);
    // then
    assertThat(restrictedTenantDTO.getName()).isEqualTo(tenantDTO.getName());
    assertThat(restrictedTenantDTO.getId()).isEqualTo(tenantDTO.getId());
    assertThat(restrictedTenantDTO.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(restrictedTenantDTO.getTheming()).isEqualTo(tenantDTO.getTheming());
    assertContentIsProperlyConverted(tenantDTO, restrictedTenantDTO);
    assertRestrictedPublicSettingsAreConverted(
        tenantDTO.getSettings(), restrictedTenantDTO.getSettings());
    Mockito.verify(templateRenderer).renderTemplate(Mockito.anyString(), Mockito.anyMap());
    assertThat(restrictedTenantDTO.getContent().getRenderedPrivacy()).isEqualTo("renderedPrivacy");
  }

  @Test
  void toRestrictedTenantDTO_should_redactSensitivePublicSettings() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .withSettings()
            .build();
    tenantDTO
        .getSettings()
        .featureToolsOICDToken("secret-oidc-token")
        .smtp(
            new SmtpConfig()
                .enabled(true)
                .host("smtp.example.org")
                .port(587)
                .secure(false)
                .username("smtp-user")
                .password("smtp-pass")
                .from("notifications@example.org")
                .emailThemeColor("#123456"));

    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);

    // then
    assertThat(restrictedTenantDTO.getSettings().getFeatureToolsOICDToken()).isNull();
    assertThat(restrictedTenantDTO.getSettings().getSmtp()).isNotNull();
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getHost()).isEqualTo("smtp.example.org");
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getPort()).isEqualTo(587);
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getFrom())
        .isEqualTo("notifications@example.org");
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getUsername()).isNull();
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getPassword()).isNull();
  }

  @Test
  void toRestrictedTenantDTO_should_convertDefaultValuesForSettingsInCaseOfNull() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .build();
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);

    // then
    assertThat(restrictedTenantDTO.getName()).isEqualTo(tenantDTO.getName());
    assertThat(restrictedTenantDTO.getId()).isEqualTo(tenantDTO.getId());
    assertThat(restrictedTenantDTO.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(restrictedTenantDTO.getTheming()).isEqualTo(tenantDTO.getTheming());
    assertContentIsProperlyConverted(tenantDTO, restrictedTenantDTO);
    assertThat(restrictedTenantDTO.getContent().getRenderedPrivacy())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getPrivacy()));
    assertThat(restrictedTenantDTO.getSettings()).isEqualTo(new Settings());
  }

  @Test
  void toDTO_should_preserveAppearancePermissionToggle() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .allowedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().appearance(false)));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(
            converted
                .getSettings()
                .getTenantAdminControls()
                .getAllowedPermissionToggles()
                .getAppearance())
        .isFalse();
  }

  @Test
  void toDTO_should_defaultMissingAppearancePermissionToggleToTrue() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles()));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(
            converted
                .getSettings()
                .getTenantAdminControls()
                .getAllowedPermissionToggles()
                .getAppearance())
        .isTrue();
  }

  @Test
  void toDTO_should_preserveSensitiveSettingsForNonPublicDtos() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .featureToolsOICDToken("secret-oidc-token")
        .smtp(
            new SmtpConfig()
                .enabled(true)
                .host("smtp.example.org")
                .port(587)
                .secure(false)
                .username("smtp-user")
                .password("smtp-pass")
                .from("notifications@example.org")
                .emailThemeColor("#123456"));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(converted.getSettings().getFeatureToolsOICDToken()).isEqualTo("secret-oidc-token");
    assertThat(converted.getSettings().getSmtp()).isNotNull();
    assertThat(converted.getSettings().getSmtp().getUsername()).isEqualTo("smtp-user");
    assertThat(converted.getSettings().getSmtp().getPassword()).isEqualTo("smtp-pass");
  }

  private static void assertContentIsProperlyConverted(
      MultilingualTenantDTO tenantDTO, RestrictedTenantDTO restrictedTenantDTO) {
    assertThat(restrictedTenantDTO.getContent().getClaim())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getClaim()));
    assertThat(restrictedTenantDTO.getContent().getPrivacy())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getPrivacy()));
    assertThat(restrictedTenantDTO.getContent().getTermsAndConditions())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getTermsAndConditions()));
    assertThat(restrictedTenantDTO.getContent().getImpressum())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getImpressum()));
  }

  private static void assertCoreSettingsAreConverted(Settings expected, Settings actual) {
    assertThat(actual.getFeatureStatisticsEnabled())
        .isEqualTo(expected.getFeatureStatisticsEnabled());
    assertThat(actual.getFeatureTopicsEnabled()).isEqualTo(expected.getFeatureTopicsEnabled());
    assertThat(actual.getTopicsInRegistrationEnabled())
        .isEqualTo(expected.getTopicsInRegistrationEnabled());
    assertThat(actual.getFeatureDemographicsEnabled())
        .isEqualTo(expected.getFeatureDemographicsEnabled());
    assertThat(actual.getFeatureAppointmentsEnabled())
        .isEqualTo(expected.getFeatureAppointmentsEnabled());
    assertThat(actual.getFeatureGroupChatV2Enabled())
        .isEqualTo(expected.getFeatureGroupChatV2Enabled());
    assertThat(actual.getFeatureToolsEnabled()).isEqualTo(expected.getFeatureToolsEnabled());
    assertThat(actual.getFeatureAttachmentUploadDisabled())
        .isEqualTo(expected.getFeatureAttachmentUploadDisabled());
    assertThat(actual.getFeatureToolsOICDToken()).isEqualTo(expected.getFeatureToolsOICDToken());
    assertThat(actual.getActiveLanguages()).isEqualTo(expected.getActiveLanguages());
    assertThat(actual.getShowAskerProfile()).isEqualTo(expected.getShowAskerProfile());
    assertThat(actual.getIsVideoCallAllowed()).isEqualTo(expected.getIsVideoCallAllowed());
    assertThat(actual.getFeatureCentralDataProtectionTemplateEnabled())
        .isEqualTo(expected.getFeatureCentralDataProtectionTemplateEnabled());
    assertThat(actual.getTenantAdminControls()).isEqualTo(expected.getTenantAdminControls());
  }

  private static void assertRestrictedPublicSettingsAreConverted(
      Settings expected, Settings actual) {
    assertThat(actual.getFeatureStatisticsEnabled())
        .isEqualTo(expected.getFeatureStatisticsEnabled());
    assertThat(actual.getFeatureTopicsEnabled()).isEqualTo(expected.getFeatureTopicsEnabled());
    assertThat(actual.getTopicsInRegistrationEnabled())
        .isEqualTo(expected.getTopicsInRegistrationEnabled());
    assertThat(actual.getFeatureDemographicsEnabled())
        .isEqualTo(expected.getFeatureDemographicsEnabled());
    assertThat(actual.getFeatureAppointmentsEnabled())
        .isEqualTo(expected.getFeatureAppointmentsEnabled());
    assertThat(actual.getFeatureGroupChatV2Enabled())
        .isEqualTo(expected.getFeatureGroupChatV2Enabled());
    assertThat(actual.getFeatureToolsEnabled()).isEqualTo(expected.getFeatureToolsEnabled());
    assertThat(actual.getFeatureAttachmentUploadDisabled())
        .isEqualTo(expected.getFeatureAttachmentUploadDisabled());
    assertThat(actual.getActiveLanguages()).isEqualTo(expected.getActiveLanguages());
    assertThat(actual.getShowAskerProfile()).isEqualTo(expected.getShowAskerProfile());
    assertThat(actual.getIsVideoCallAllowed()).isEqualTo(expected.getIsVideoCallAllowed());
    assertThat(actual.getFeatureCentralDataProtectionTemplateEnabled())
        .isEqualTo(expected.getFeatureCentralDataProtectionTemplateEnabled());
    assertThat(actual.getTenantAdminControls()).isEqualTo(expected.getTenantAdminControls());
    assertThat(actual.getFeatureToolsOICDToken()).isNull();
    if (expected.getSmtp() == null) {
      assertThat(actual.getSmtp()).isNull();
      return;
    }

    assertThat(actual.getSmtp()).isNotNull();
    assertThat(actual.getSmtp().getEnabled()).isEqualTo(expected.getSmtp().getEnabled());
    assertThat(actual.getSmtp().getHost()).isEqualTo(expected.getSmtp().getHost());
    assertThat(actual.getSmtp().getPort()).isEqualTo(expected.getSmtp().getPort());
    assertThat(actual.getSmtp().getSecure()).isEqualTo(expected.getSmtp().getSecure());
    assertThat(actual.getSmtp().getFrom()).isEqualTo(expected.getSmtp().getFrom());
    assertThat(actual.getSmtp().getEmailThemeColor())
        .isEqualTo(expected.getSmtp().getEmailThemeColor());
    assertThat(actual.getSmtp().getUsername()).isNull();
    assertThat(actual.getSmtp().getPassword()).isNull();
  }

  private static String getGermanTranslation(Map<String, String> translations) {
    return translations.get("de");
  }

  @Test
  void toBasicLicensingTenantDTO_should_convertAttributesProperly() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .build();
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // when
    BasicTenantLicensingDTO basicTenantLicensingDTO =
        tenantConverter.toBasicLicensingTenantDTO(entity);

    // then
    assertThat(basicTenantLicensingDTO.getId()).isEqualTo(tenantDTO.getId());
    assertThat(basicTenantLicensingDTO.getCreateDate()).isEqualTo(tenantDTO.getCreateDate());
    assertThat(basicTenantLicensingDTO.getUpdateDate()).isEqualTo(tenantDTO.getUpdateDate());
    assertThat(basicTenantLicensingDTO.getName()).isEqualTo(tenantDTO.getName());
    assertThat(basicTenantLicensingDTO.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(basicTenantLicensingDTO.getLicensing()).isEqualTo(tenantDTO.getLicensing());
  }
}
