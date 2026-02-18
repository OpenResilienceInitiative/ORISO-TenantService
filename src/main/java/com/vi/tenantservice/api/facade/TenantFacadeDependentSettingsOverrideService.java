package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSetting;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TenantFacadeDependentSettingsOverrideService {

  private final @NonNull TenantFacadeChangeDetectionService tenantFacadeChangeDetectionService;

  public void overrideDependentSettingsOnCreate(MultilingualTenantDTO sanitizedTenantDTO) {
    turnOffTopicsInRegistrationEnabledWhenTopicFeatureIsOff(sanitizedTenantDTO);
  }

  public void overrideDependentSettingsOnUpdate(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenantEntity) {
    turnOffTopicsInRegistrationEnabledWhenTopicFeatureGetsTurnedOff(
        sanitizedTenantDTO, existingTenantEntity);
  }

  private void turnOffTopicsInRegistrationEnabledWhenTopicFeatureIsOff(
      MultilingualTenantDTO sanitizedTenantDTO) {
    if (sanitizedTenantDTO.getSettings() != null
        && (sanitizedTenantDTO.getSettings().getFeatureTopicsEnabled() == null
            || !sanitizedTenantDTO.getSettings().getFeatureTopicsEnabled())) {
      sanitizedTenantDTO.getSettings().setTopicsInRegistrationEnabled(false);
      log.info("Setting TopicsInRegistrationEnabled feature to false");
    }
  }

  private void turnOffTopicsInRegistrationEnabledWhenTopicFeatureGetsTurnedOff(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenantEntity) {
    List<TenantSetting> tenantSettings =
        tenantFacadeChangeDetectionService.determineChangedSettings(
            sanitizedTenantDTO, existingTenantEntity);

    Optional<TenantSetting> changedTopicFeature =
        tenantSettings.stream()
            .filter(setting -> TenantSetting.FEATURE_TOPICS_ENABLED.equals(setting))
            .findFirst();
    if (changedTopicFeature.isPresent()
        && !sanitizedTenantDTO.getSettings().getFeatureTopicsEnabled()) {
      sanitizedTenantDTO.getSettings().setTopicsInRegistrationEnabled(false);
    }
  }
}
