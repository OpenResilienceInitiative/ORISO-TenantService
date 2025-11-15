package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.model.TenantSetting.*;
import static com.vi.tenantservice.api.util.JsonConverter.convertToJson;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantContent;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantFacadeChangeDetectionServiceTest {

  @InjectMocks TenantFacadeChangeDetectionService tenantFacadeChangeDetectionService;

  @Test
  void determineChangedSettings_Should_NotDetectAnyChanges_When_InputDTOIsNull() {
    Object o = null;
    assertThat(Sets.newHashSet(o)).isEqualTo(Sets.newHashSet(o));
    // given
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO();
    TenantEntity existingTenant = new TenantEntity();

    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .isEmpty();
  }

  @Test
  void
      determineChangedSettings_Should_NotDetectAnyChanges_When_InputDTOIsEmptyAndEntitySettingsAreNull() {
    // given
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(new Settings());
    TenantEntity existingTenant = new TenantEntity();

    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .isEmpty();
  }

  @Test
  void determineChangedSettings_Should_DetectChanges_When_InputDTOContainsChangesToDefaultValues() {
    // given
    Settings settings =
        new Settings()
            .featureDemographicsEnabled(true)
            .featureTopicsEnabled(true)
            .topicsInRegistrationEnabled(true)
            .featureGroupChatV2Enabled(true);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);
    TenantEntity existingTenant = new TenantEntity();

    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .contains(
            FEATURE_DEMOGRAPHICS_ENABLED,
            FEATURE_TOPICS_ENABLED,
            ENABLE_TOPICS_IN_REGISTRATION,
            FEATURE_GROUP_CHAT_V2_ENABLED);
  }

  @Test
  void
      determineChangedSettings_Should_Not_DetectChanges_When_InputDTOContainsSameSettingsAsEntity() {
    // given
    Settings settings =
        new Settings()
            .featureDemographicsEnabled(true)
            .featureTopicsEnabled(true)
            .topicsInRegistrationEnabled(true)
            .featureGroupChatV2Enabled(true);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder()
            .featureDemographicsEnabled(true)
            .featureTopicsEnabled(true)
            .topicsInRegistrationEnabled(true)
            .featureGroupChatV2Enabled(true)
            .build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .isEmpty();
  }

  @Test
  void
      determineChangedSettings_Should_DetectStatisticsFeatureChanges_When_InputDTOContainsDifferentSettingsAsEntity() {
    // given
    Settings settings = new Settings().featureStatisticsEnabled(false);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder().featureStatisticsEnabled(true).build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .containsOnly(FEATURE_STATISTICS_ENABLED);
  }

  @Test
  void
      determineChangedSettings_Should_DetectAppointmentFeatureChanges_When_InputDTOContainsDifferentSettingsAsEntity() {
    // given
    Settings settings = new Settings().featureAppointmentsEnabled(false);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder().featureAppointmentsEnabled(true).build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .containsOnly(FEATURE_APPOINTMENTS_ENABLED);
  }

  @Test
  void
      determineChangedSettings_Should_DetectChanges_When_InputDTOContainsDifferentSettingsAsEntity() {
    // given
    Settings settings =
        new Settings()
            .featureDemographicsEnabled(false)
            .featureTopicsEnabled(false)
            .topicsInRegistrationEnabled(true);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder()
            .featureDemographicsEnabled(true)
            .featureTopicsEnabled(true)
            .topicsInRegistrationEnabled(true)
            .build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .contains(FEATURE_DEMOGRAPHICS_ENABLED, FEATURE_TOPICS_ENABLED);
  }

  @Test
  void
      determineChangedSettings_Should_DetectEnableTopicInRegistrationSettingChanges_When_InputDTOContainsDifferentSettingsAsEntity() {
    // given
    Settings settings =
        new Settings()
            .featureDemographicsEnabled(true)
            .featureTopicsEnabled(true)
            .topicsInRegistrationEnabled(false);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder()
            .featureDemographicsEnabled(true)
            .featureTopicsEnabled(true)
            .topicsInRegistrationEnabled(true)
            .build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .contains(ENABLE_TOPICS_IN_REGISTRATION);
  }

  @Test
  void
      determineChangedSettings_Should_DetectGroupChatV2FeatureChanges_When_InputDTOContainsDifferentSettingsAsEntity() {
    // given
    Settings settings = new Settings().featureGroupChatV2Enabled(false);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder().featureGroupChatV2Enabled(true).build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .containsOnly(FEATURE_GROUP_CHAT_V2_ENABLED);
  }

  @Test
  void
      determineChangedSettings_Should_DetectFeatureAttachmentUploadDisabledChanges_When_InputDTOContainsDifferentSettingsAsEntity() {
    // given
    Settings settings = new Settings().featureAttachmentUploadDisabled(true);
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO().settings(settings);

    TenantSettings existingTenantSettings =
        TenantSettings.builder().featureAttachmentUploadDisabled(false).build();
    TenantEntity existingTenant =
        TenantEntity.builder().settings(convertToJson(existingTenantSettings)).build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedSettings(
                sanitizedTenantDTO, existingTenant))
        .containsOnly(FEATURE_ATTACHMENT_UPLOAD_DISABLED);
  }

  @Test
  void determineChangedContent_Should_DetectContentChanges_When_ContentChanged() {
    // given
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO();

    sanitizedTenantDTO.setContent(
        new MultilingualContent()
            .impressum(mapWithGermanValue("impressum"))
            .privacy(mapWithGermanValue("privacy"))
            .termsAndConditions(mapWithGermanValue("termsAndConditions")));

    TenantEntity existingTenant =
        TenantEntity.builder()
            .contentImpressum("{\"de\":\"impressum change\"}")
            .contentPrivacy("{\"de\":\"privacy change\"}")
            .contentTermsAndConditions("{\"de\":\"termsAndConditions change\"}")
            .build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedContent(
                sanitizedTenantDTO, existingTenant))
        .containsOnly(
            TenantContent.IMPRESSUM, TenantContent.PRIVACY, TenantContent.TERMS_AND_CONDITIONS);
  }

  @Test
  void
      determineChangedContent_Should_NotDetectAnyContentChange_When_NoContentTextAttributeChanged() {
    // given
    MultilingualTenantDTO sanitizedTenantDTO = new MultilingualTenantDTO();

    sanitizedTenantDTO.setContent(
        new MultilingualContent()
            .impressum(mapWithGermanValue("impressum"))
            .privacy(mapWithGermanValue("privacy"))
            .termsAndConditions(mapWithGermanValue("termsAndConditions")));

    TenantEntity existingTenant =
        TenantEntity.builder()
            .contentImpressum("{\"de\":\"impressum\"}")
            .contentPrivacy("{\"de\":\"privacy\"}")
            .contentTermsAndConditions("{\"de\":\"termsAndConditions\"}")
            .build();
    // when, then
    assertThat(
            tenantFacadeChangeDetectionService.determineChangedContent(
                sanitizedTenantDTO, existingTenant))
        .isEmpty();
  }

  private Map<String, String> mapWithGermanValue(String value) {
    HashMap<String, String> result = Maps.newHashMap();
    result.put("de", value);
    return result;
  }
}
