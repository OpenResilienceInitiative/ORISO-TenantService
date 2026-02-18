package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantFacadeDependentSettingsOverrideServiceTest {

  private final MultilingualTenantDTO sanitizedTenantDTO = getMultilingualTenantDTO();
  private final TenantEntity tenantEntity = new TenantEntity();

  @Mock private TenantFacadeChangeDetectionService tenantFacadeChangeDetectionService;

  @InjectMocks
  private TenantFacadeDependentSettingsOverrideService tenantFacadeDependentSettingsOverrideService;

  @Test
  void
      overrideDependentSettingsOnCreate_ShouldSetTopicsToRegistrationEnabledToFalse_When_TopicFeatureGetsDisabled() {

    // given
    sanitizedTenantDTO.getSettings().setFeatureTopicsEnabled(false);
    sanitizedTenantDTO.getSettings().setTopicsInRegistrationEnabled(true);

    // when
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnCreate(
        sanitizedTenantDTO);

    // then
    assertThat(sanitizedTenantDTO.getSettings().getTopicsInRegistrationEnabled()).isFalse();
  }

  @Test
  void
      overrideDependentSettingsOnCreate_ShouldNotChangeTopicsToRegistrationEnabledValue_When_TopicFeatureGetsEnabled() {

    // given
    sanitizedTenantDTO.getSettings().setFeatureTopicsEnabled(true);
    sanitizedTenantDTO.getSettings().setTopicsInRegistrationEnabled(true);

    // when
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnCreate(
        sanitizedTenantDTO);

    // then
    assertThat(sanitizedTenantDTO.getSettings().getTopicsInRegistrationEnabled()).isTrue();
  }

  @Test
  void
      overrideDependentSettingsOnUpdate_ShouldSetTopicsToRegistrationEnabledToFalse_When_TopicFeatureGetsDisabled() {
    // given
    sanitizedTenantDTO.setSettings(
        new Settings().topicsInRegistrationEnabled(true).featureTopicsEnabled(false));
    when(tenantFacadeChangeDetectionService.determineChangedSettings(
            sanitizedTenantDTO, tenantEntity))
        .thenReturn(Lists.newArrayList(TenantSetting.FEATURE_TOPICS_ENABLED));
    // when
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnUpdate(
        sanitizedTenantDTO, tenantEntity);
    // then
    assertThat(sanitizedTenantDTO.getSettings().getTopicsInRegistrationEnabled()).isFalse();
  }

  @Test
  void
      overrideDependentSettingsOnUpdate_ShouldNotChangeTopicsToRegistrationEnabledValue_When_TopicFeatureGetsEnabled() {
    // given
    sanitizedTenantDTO.setSettings(
        new Settings().topicsInRegistrationEnabled(false).featureTopicsEnabled(true));
    when(tenantFacadeChangeDetectionService.determineChangedSettings(
            sanitizedTenantDTO, tenantEntity))
        .thenReturn(Lists.newArrayList(TenantSetting.FEATURE_TOPICS_ENABLED));
    // when
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnUpdate(
        sanitizedTenantDTO, tenantEntity);
    // then
    assertThat(sanitizedTenantDTO.getSettings().getTopicsInRegistrationEnabled()).isFalse();
  }

  private MultilingualTenantDTO getMultilingualTenantDTO() {
    var tenantDTO = new MultilingualTenantDTO();
    Settings settings = new Settings();
    settings.setExtendedSettings(new ConsultingTypePatchDTO());
    tenantDTO.settings(settings);
    return tenantDTO;
  }
}
