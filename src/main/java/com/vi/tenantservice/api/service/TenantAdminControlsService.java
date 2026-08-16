package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
import com.vi.tenantservice.api.policy.LegacyPermissionPolicyMapper;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantAdminControlsService {

  private final @NonNull TenantAdminControlsRepository tenantAdminControlsRepository;
  private final @NonNull TenantConverter tenantConverter;

  public TenantAdminControls getControls() {
    return tenantConverter.toTenantAdminControls(getControlsSettings());
  }

  public TenantAdminControls updateControls(TenantAdminControls tenantAdminControls) {
    TenantAdminControlsSettings controlsSettings =
        tenantConverter.toTenantAdminControlsSettings(tenantAdminControls);
    hydrateCanonicalPolicies(controlsSettings);
    // the DTO never carries the translation API keys - preserve the stored ones
    TenantAdminControlsSettings existingSettings = getControlsSettings();
    if (existingSettings != null) {
      controlsSettings.setTranslationApiKeys(existingSettings.getTranslationApiKeys());
    }
    saveControlsSettings(controlsSettings);
    return tenantConverter.toTenantAdminControls(controlsSettings);
  }

  /**
   * Raw machine-translation provider API keys (provider id -> key) from the platform-global admin
   * controls. Internal use only - admin endpoints must expose keys masked.
   */
  public Map<String, String> getTranslationApiKeys() {
    TenantAdminControlsSettings controlsSettings = getControlsSettings();
    return controlsSettings != null && controlsSettings.getTranslationApiKeys() != null
        ? controlsSettings.getTranslationApiKeys()
        : Map.of();
  }

  /** Stores the machine-translation API key for a provider in the platform-global controls. */
  public void setTranslationApiKey(String provider, String apiKey) {
    TenantAdminControlsSettings controlsSettings = getControlsSettings();
    if (controlsSettings == null) {
      controlsSettings = new TenantAdminControlsSettings();
    }
    Map<String, String> keys = new HashMap<>();
    if (controlsSettings.getTranslationApiKeys() != null) {
      keys.putAll(controlsSettings.getTranslationApiKeys());
    }
    keys.put(provider, apiKey);
    controlsSettings.setTranslationApiKeys(keys);
    saveControlsSettings(controlsSettings);
  }

  public void stripTenantAdminControlsFromTenantDto(MultilingualTenantDTO tenantDTO) {
    if (tenantDTO == null || tenantDTO.getSettings() == null) {
      return;
    }
    tenantDTO.getSettings().setTenantAdminControls(null);
  }

  public void enrichTenantDtoWithTenantAdminControls(MultilingualTenantDTO tenantDTO) {
    if (tenantDTO == null) {
      return;
    }
    tenantDTO.setSettings(enrichSettingsWithTenantAdminControls(tenantDTO.getSettings()));
  }

  public void enrichTenantDtoWithTenantAdminControls(TenantDTO tenantDTO) {
    if (tenantDTO == null) {
      return;
    }
    tenantDTO.setSettings(enrichSettingsWithTenantAdminControls(tenantDTO.getSettings()));
  }

  private Settings enrichSettingsWithTenantAdminControls(Settings settings) {
    Settings enrichedSettings = settings != null ? settings : new Settings();
    enrichedSettings.setTenantAdminControls(getControls());
    return enrichedSettings;
  }

  private TenantAdminControlsSettings getControlsSettings() {
    return findExistingControls()
        .map(entity -> parseControlsSettings(entity.getControls()))
        .orElseGet(this::createDefaultControlsSettings);
  }

  private void saveControlsSettings(TenantAdminControlsSettings controlsSettings) {
    TenantAdminControlsEntity entity =
        findExistingControls().orElseGet(TenantAdminControlsEntity::new);
    entity.setControls(serializeControlsSettings(controlsSettings));
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    tenantAdminControlsRepository.save(entity);
  }

  private Optional<TenantAdminControlsEntity> findExistingControls() {
    return tenantAdminControlsRepository.findTopByOrderByIdAsc();
  }

  private TenantAdminControlsSettings parseControlsSettings(String controlsJson) {
    if (StringUtils.isBlank(controlsJson) || "{}".equals(controlsJson.trim())) {
      return createDefaultControlsSettings();
    }
    try {
      TenantAdminControlsSettings settings =
          new ObjectMapper().readValue(controlsJson, TenantAdminControlsSettings.class);
      if (settings == null) {
        return createDefaultControlsSettings();
      }
      hydrateCanonicalPolicies(settings);
      return settings;
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private String serializeControlsSettings(TenantAdminControlsSettings controlsSettings) {
    try {
      return new ObjectMapper().writeValueAsString(controlsSettings);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private TenantAdminControlsSettings createDefaultControlsSettings() {
    TenantAdminControlsSettings settings =
        tenantConverter.toTenantAdminControlsSettings(new TenantAdminControls());
    hydrateCanonicalPolicies(settings);
    return settings;
  }

  private void hydrateCanonicalPolicies(TenantAdminControlsSettings settings) {
    if (settings == null) {
      return;
    }
    if (settings.getPermissionPolicies() == null || settings.getPermissionPolicies().isEmpty()) {
      settings.setPermissionPolicies(
          LegacyPermissionPolicyMapper.fromLegacyMaps(
              settings.getAllowedPermissionToggles(), settings.getEnforcedPermissionToggles()));
    }
    if (settings.getCaseHandoverPolicies() == null) {
      settings.setCaseHandoverPolicies(CaseHandoverPolicyDefaults.create());
    }
  }
}
