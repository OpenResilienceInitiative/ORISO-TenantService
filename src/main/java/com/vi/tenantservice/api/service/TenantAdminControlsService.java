package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantDTO;
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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final @NonNull TenantAdminControlsRepository tenantAdminControlsRepository;
  private final @NonNull TenantConverter tenantConverter;

  public TenantAdminControls getControls() {
    return tenantConverter.toTenantAdminControls(getControlsSettings());
  }

  public TenantAdminControls updateControls(TenantAdminControls tenantAdminControls) {
    TenantAdminControlsSettings controlsSettings =
        tenantConverter.toTenantAdminControlsSettings(tenantAdminControls);
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

  /**
   * Writes the settings back into the stored blob <b>without dropping fields this build does not
   * know</b>.
   *
   * <p>{@link TenantAdminControlsSettings} ignores unknown fields on purpose (see its javadoc):
   * reading leniently is what keeps a rollback from turning into HTTP 500 on every session
   * bootstrap. Serializing that narrower type back over the column is the same version skew from
   * the other side - every key a newer build wrote would be silently deleted by the next ordinary
   * admin save. So the stored document is edited as a JSON tree: the fields this build owns are
   * overwritten, every other byte is left alone. Same reasoning as {@code
   * TranslationApiKeyEncryptionMigration}.
   *
   * <p>The merge is deliberately top-level only. Going deeper would preserve unknown nested keys
   * but would also make clearing a known nested field impossible, because a value this build
   * intentionally removed would keep being merged back in from the stored document.
   */
  private void saveControlsSettings(TenantAdminControlsSettings controlsSettings) {
    TenantAdminControlsEntity entity =
        findExistingControls().orElseGet(TenantAdminControlsEntity::new);
    entity.setControls(mergeIntoStoredControls(entity.getControls(), controlsSettings));
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    tenantAdminControlsRepository.save(entity);
  }

  private String mergeIntoStoredControls(
      String storedControlsJson, TenantAdminControlsSettings controlsSettings) {
    ObjectNode controls = readControlsTree(storedControlsJson);
    controls.setAll(serializeControlsSettingsToTree(controlsSettings));
    return controls.toString();
  }

  private ObjectNode readControlsTree(String controlsJson) {
    if (StringUtils.isBlank(controlsJson)) {
      return OBJECT_MAPPER.createObjectNode();
    }
    try {
      JsonNode tree = OBJECT_MAPPER.readTree(controlsJson);
      // A blob that is not a JSON object is not something this service wrote, and there is nothing
      // in it to preserve. The read path throws on it first, so this is defence in depth rather
      // than a reachable admin flow.
      return tree.isObject() ? (ObjectNode) tree : OBJECT_MAPPER.createObjectNode();
    } catch (JsonProcessingException unreadableBlob) {
      return OBJECT_MAPPER.createObjectNode();
    }
  }

  private ObjectNode serializeControlsSettingsToTree(TenantAdminControlsSettings controlsSettings) {
    return OBJECT_MAPPER.valueToTree(controlsSettings);
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
          OBJECT_MAPPER.readValue(controlsJson, TenantAdminControlsSettings.class);
      return settings != null ? settings : createDefaultControlsSettings();
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private TenantAdminControlsSettings createDefaultControlsSettings() {
    return tenantConverter.toTenantAdminControlsSettings(new TenantAdminControls());
  }
}
