package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantAdminControlsService {

  private final @NonNull TenantAdminControlsRepository tenantAdminControlsRepository;
  private final @NonNull TenantConverter tenantConverter;

  public TenantAdminControls getControls() {
    return tenantConverter.toTenantAdminControls(getControlsSettings());
  }

  @Transactional
  public TenantAdminControls updateControls(TenantAdminControls tenantAdminControls) {
    Optional<TenantAdminControlsEntity> existingEntity = findExistingControls();
    TenantAdminControlsSettings controlsSettings =
        tenantConverter.toTenantAdminControlsSettings(tenantAdminControls);
    TenantAdminControlsSettings existingSettings =
        existingEntity
            .map(entity -> parseControlsSettings(entity.getControls()))
            .orElseGet(this::createDefaultControlsSettings);
    if (existingSettings != null) {
      if (controlsSettings.getPermissionPolicies() == null
          || controlsSettings.getPermissionPolicies().isEmpty()) {
        controlsSettings.setPermissionPolicies(existingSettings.getPermissionPolicies());
      }
      if (controlsSettings.getCaseHandoverPolicies() == null) {
        controlsSettings.setCaseHandoverPolicies(existingSettings.getCaseHandoverPolicies());
      }
      // the DTO never carries the translation API keys - preserve the stored ones
      controlsSettings.setTranslationApiKeys(existingSettings.getTranslationApiKeys());
    }
    hydrateCanonicalPolicies(controlsSettings);
    saveControlsSettings(
        controlsSettings, existingEntity.orElseGet(TenantAdminControlsEntity::new));
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
  @Transactional
  public void setTranslationApiKey(String provider, String apiKey) {
    Optional<TenantAdminControlsEntity> existingEntity = findExistingControls();
    TenantAdminControlsSettings controlsSettings =
        existingEntity
            .map(entity -> parseControlsSettings(entity.getControls()))
            .orElseGet(this::createDefaultControlsSettings);
    Map<String, String> keys = new HashMap<>();
    if (controlsSettings.getTranslationApiKeys() != null) {
      keys.putAll(controlsSettings.getTranslationApiKeys());
    }
    keys.put(provider, apiKey);
    controlsSettings.setTranslationApiKeys(keys);
    saveControlsSettings(
        controlsSettings, existingEntity.orElseGet(TenantAdminControlsEntity::new));
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

  private void saveControlsSettings(
      TenantAdminControlsSettings controlsSettings, TenantAdminControlsEntity entity) {
    entity.setControls(serializeControlsSettings(controlsSettings));
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    try {
      tenantAdminControlsRepository.saveAndFlush(entity);
    } catch (OptimisticLockingFailureException | DataIntegrityViolationException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  private Optional<TenantAdminControlsEntity> findExistingControls() {
    return tenantAdminControlsRepository.findTopByOrderByIdAsc();
  }

  /**
   * Mapper for the version-shared storage blob. Reading tolerates unknown fields at ANY depth:
   * {@code @JsonIgnoreProperties} on {@link TenantAdminControlsSettings} only shields the root's
   * own properties, while the nested generated types (e.g. {@code CaseHandoverPolicies}) carry no
   * such annotation — a strict mapper would just move the version-skew outage one level deeper.
   * Serialization is unaffected by the flag. Syntactically broken JSON still fails loudly.
   */
  private static final ObjectMapper STORED_CONTROLS_MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private TenantAdminControlsSettings parseControlsSettings(String controlsJson) {
    if (StringUtils.isBlank(controlsJson) || "{}".equals(controlsJson.trim())) {
      return createDefaultControlsSettings();
    }
    try {
      TenantAdminControlsSettings settings =
          STORED_CONTROLS_MAPPER.readValue(controlsJson, TenantAdminControlsSettings.class);
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
      return STORED_CONTROLS_MAPPER.writeValueAsString(controlsSettings);
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
    // The reason registry is defined by this build (CaseHandoverPolicyDefaults); the stored blob
    // only customises the policies of known reasons. A section written by another build - or one
    // whose shape this build cannot read - may miss reason codes entirely; the resolver and the
    // override sanitiser dereference every registry code unconditionally, so missing codes are
    // completed from the defaults while stored ones keep their customisation.
    if (settings.getCaseHandoverPolicies() == null
        || settings.getCaseHandoverPolicies().getReasons() == null) {
      settings.setCaseHandoverPolicies(CaseHandoverPolicyDefaults.create());
    } else {
      Map<String, CaseHandoverReasonPolicy> completedReasons =
          new LinkedHashMap<>(CaseHandoverPolicyDefaults.create().getReasons());
      settings
          .getCaseHandoverPolicies()
          .getReasons()
          .forEach(
              (code, storedReason) -> {
                if (storedReason != null) {
                  completedReasons.put(code, storedReason);
                }
              });
      settings.getCaseHandoverPolicies().setReasons(completedReasons);
    }
  }
}
