package com.vi.tenantservice.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.util.JsonConverter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TenantAdminControlsCascadeService {

  /**
   * Platform-level tenant admin controls are edited via {@code PUT /tenantadmin/1} in the admin UI.
   * Updates to other tenants must not change {@code tenantAdminControls}.
   */
  private static final Long PLATFORM_TENANT_ID = 1L;

  private final @NonNull TenantConverter tenantConverter;
  private final @NonNull TenantService tenantService;

  public void preserveTenantAdminControlsUnlessPlatformTenant(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenantEntity) {
    if (PLATFORM_TENANT_ID.equals(existingTenantEntity.getId())) {
      return;
    }
    if (sanitizedTenantDTO.getSettings() == null) {
      return;
    }
    sanitizedTenantDTO
        .getSettings()
        .setTenantAdminControls(
            tenantConverter.toTenantAdminControls(
                getTenantAdminControlsFromSettings(existingTenantEntity.getSettings())));
  }

  public void cascadeTenantAdminControlsIfPlatformTenant(
      MultilingualTenantDTO sanitizedTenantDTO, Long updatedTenantId, String previousSettingsJson) {
    if (!PLATFORM_TENANT_ID.equals(updatedTenantId)) {
      return;
    }
    log.info("Cascading tenant admin controls from platform tenant {}", PLATFORM_TENANT_ID);

    TenantAdminControls tenantAdminControls = getTenantAdminControls(sanitizedTenantDTO);
    if (tenantAdminControls == null || tenantAdminControls.getAllowedPermissionToggles() == null) {
      log.info("Tenant admin controls or allowed permission toggles is null");
      return;
    }

    TenantAdminControlsSettings tenantAdminControlsSettings =
        tenantConverter.toTenantAdminControlsSettings(tenantAdminControls);

    if (!tenantAdminControlsChanged(previousSettingsJson, tenantAdminControlsSettings)) {
      log.info("Tenant admin controls are not changed");
      return;
    }

    cascadeToAllTenants(tenantAdminControlsSettings, updatedTenantId);
  }

  private TenantAdminControls getTenantAdminControls(MultilingualTenantDTO sanitizedTenantDTO) {
    Settings settings = sanitizedTenantDTO.getSettings();
    return settings != null ? settings.getTenantAdminControls() : null;
  }

  private boolean tenantAdminControlsChanged(
      String previousSettingsJson, TenantAdminControlsSettings newTenantAdminControls) {
    TenantAdminControlsSettings previousTenantAdminControls =
        getTenantAdminControlsFromSettings(previousSettingsJson);
    return !Objects.equals(
        JsonConverter.convertToJson(previousTenantAdminControls),
        JsonConverter.convertToJson(newTenantAdminControls));
  }

  private TenantAdminControlsSettings getTenantAdminControlsFromSettings(String settingsJson) {
    if (settingsJson == null) {
      return null;
    }
    TenantSettings tenantSettings = JsonConverter.convertFromJson(settingsJson);
    return tenantSettings.getTenantAdminControls();
  }

  private void cascadeToAllTenants(
      TenantAdminControlsSettings tenantAdminControlsSettings, Long updatedTenantId) {
    TenantAdminControlsSettings controlsCopy = deepCopy(tenantAdminControlsSettings);
    Map<Long, String> settingsByTenantId = new HashMap<>();

    tenantService.getAllTenants().stream()
        .filter(tenant -> !tenant.getId().equals(updatedTenantId))
        .forEach(
            tenant -> {
              TenantSettings settings =
                  tenant.getSettings() == null
                      ? new TenantSettings()
                      : JsonConverter.convertFromJson(tenant.getSettings());
              applyTenantAdminControls(settings, controlsCopy);
              settingsByTenantId.put(tenant.getId(), JsonConverter.convertToJson(settings));
            });

    if (settingsByTenantId.isEmpty()) {
      return;
    }

    tenantService.updateTenantSettingsBatch(settingsByTenantId);
    log.info(
        "Cascaded tenantAdminControls.allowedPermissionToggles to {} tenants",
        settingsByTenantId.size());
  }

  private void applyTenantAdminControls(
      TenantSettings settings, TenantAdminControlsSettings tenantAdminControlsSettings) {
    TenantAdminControlsSettings existingControls = settings.getTenantAdminControls();
    if (existingControls == null) {
      settings.setTenantAdminControls(deepCopy(tenantAdminControlsSettings));
      return;
    }
    existingControls.setAllowedPermissionToggles(
        deepCopy(tenantAdminControlsSettings.getAllowedPermissionToggles()));
  }

  private TenantAdminControlsSettings deepCopy(TenantAdminControlsSettings source) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(
          objectMapper.writeValueAsString(source), TenantAdminControlsSettings.class);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private TenantAdminAllowedPermissionTogglesSettings deepCopy(
      TenantAdminAllowedPermissionTogglesSettings source) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(
          objectMapper.writeValueAsString(source),
          TenantAdminAllowedPermissionTogglesSettings.class);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }
}
