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
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    saveControlsSettings(controlsSettings);
    return tenantConverter.toTenantAdminControls(controlsSettings);
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
    if (tenantDTO.getSettings() == null) {
      tenantDTO.setSettings(new Settings());
    }
    tenantDTO.getSettings().setTenantAdminControls(getControls());
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
      return settings != null ? settings : createDefaultControlsSettings();
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
    return tenantConverter.toTenantAdminControlsSettings(new TenantAdminControls());
  }
}
