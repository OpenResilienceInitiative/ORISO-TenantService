package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.util.JsonConverter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantAdminControlsCascadeServiceTest {

  private static final Long PLATFORM_TENANT_ID = 1L;
  private static final Long OTHER_TENANT_ID = 2L;

  @Mock private TenantConverter tenantConverter;
  @Mock private TenantService tenantService;

  @InjectMocks private TenantAdminControlsCascadeService tenantAdminControlsCascadeService;

  @Test
  void
      preserveTenantAdminControlsUnlessPlatformTenant_Should_restoreExistingControls_When_tenantIsNotPlatformTenant() {
    MultilingualTenantDTO tenantDTO = tenantWithAllowedPermissionToggles(false);
    TenantEntity existingTenant = existingTenantWithAllowedPermissionToggles(OTHER_TENANT_ID, true);

    TenantAdminControls restoredControls =
        new TenantAdminControls()
            .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles().appearance(true));
    when(tenantConverter.toTenantAdminControls(any())).thenReturn(restoredControls);

    tenantAdminControlsCascadeService.preserveTenantAdminControlsUnlessPlatformTenant(
        tenantDTO, existingTenant);

    assertThat(tenantDTO.getSettings().getTenantAdminControls()).isEqualTo(restoredControls);
  }

  @Test
  void
      preserveTenantAdminControlsUnlessPlatformTenant_Should_notModifyDto_When_tenantIsPlatformTenant() {
    MultilingualTenantDTO tenantDTO = tenantWithAllowedPermissionToggles(false);
    TenantEntity existingTenant =
        existingTenantWithAllowedPermissionToggles(PLATFORM_TENANT_ID, true);

    tenantAdminControlsCascadeService.preserveTenantAdminControlsUnlessPlatformTenant(
        tenantDTO, existingTenant);

    verify(tenantConverter, never()).toTenantAdminControls(any());
  }

  @Test
  void
      cascadeTenantAdminControlsIfPlatformTenant_Should_notCascade_When_tenantIsNotPlatformTenant() {
    MultilingualTenantDTO tenantDTO = tenantWithAllowedPermissionToggles(false);
    TenantEntity existingTenant = existingTenantWithAllowedPermissionToggles(OTHER_TENANT_ID, true);

    tenantAdminControlsCascadeService.cascadeTenantAdminControlsIfPlatformTenant(
        tenantDTO, OTHER_TENANT_ID, existingTenant.getSettings());

    verify(tenantService, never()).getAllTenants();
  }

  @Test
  void
      cascadeTenantAdminControlsIfPlatformTenant_Should_notCascade_When_tenantAdminControlsDidNotChange() {
    MultilingualTenantDTO tenantDTO = tenantWithAllowedPermissionToggles(false);
    TenantEntity existingTenant =
        existingTenantWithAllowedPermissionToggles(PLATFORM_TENANT_ID, false);

    when(tenantConverter.toTenantAdminControlsSettings(any()))
        .thenReturn(
            TenantAdminControlsSettings.builder()
                .allowedPermissionToggles(
                    TenantAdminAllowedPermissionTogglesSettings.builder().appearance(false).build())
                .build());

    tenantAdminControlsCascadeService.cascadeTenantAdminControlsIfPlatformTenant(
        tenantDTO, PLATFORM_TENANT_ID, existingTenant.getSettings());

    verify(tenantService, never()).getAllTenants();
  }

  @Test
  void
      cascadeTenantAdminControlsIfPlatformTenant_Should_cascade_When_entityWasMutatedButPreviousSettingsDiffer() {
    MultilingualTenantDTO tenantDTO = tenantWithAllowedPermissionToggles(false);
    String previousSettingsJson =
        JsonConverter.convertToJson(
            TenantSettings.builder()
                .tenantAdminControls(
                    TenantAdminControlsSettings.builder()
                        .allowedPermissionToggles(
                            TenantAdminAllowedPermissionTogglesSettings.builder()
                                .appearance(true)
                                .build())
                        .build())
                .build());
    TenantEntity mutatedTenant =
        existingTenantWithAllowedPermissionToggles(PLATFORM_TENANT_ID, false);
    TenantEntity otherTenant = otherTenantWithSettings();

    when(tenantConverter.toTenantAdminControlsSettings(any()))
        .thenReturn(
            TenantAdminControlsSettings.builder()
                .allowedPermissionToggles(
                    TenantAdminAllowedPermissionTogglesSettings.builder().appearance(false).build())
                .build());
    when(tenantService.getAllTenants()).thenReturn(List.of(mutatedTenant, otherTenant));

    tenantAdminControlsCascadeService.cascadeTenantAdminControlsIfPlatformTenant(
        tenantDTO, PLATFORM_TENANT_ID, previousSettingsJson);

    verify(tenantService).updateTenantSettingsBatch(anyMap());
  }

  @Test
  void
      cascadeTenantAdminControlsIfPlatformTenant_Should_cascadeToOtherTenants_When_platformTenant() {
    MultilingualTenantDTO tenantDTO = tenantWithAllowedPermissionToggles(false);
    TenantEntity existingTenant =
        existingTenantWithAllowedPermissionToggles(PLATFORM_TENANT_ID, true);
    TenantEntity otherTenant = otherTenantWithSettings();

    when(tenantConverter.toTenantAdminControlsSettings(any()))
        .thenReturn(
            TenantAdminControlsSettings.builder()
                .permissionsPageEnabled(true)
                .allowedPermissionToggles(
                    TenantAdminAllowedPermissionTogglesSettings.builder().appearance(false).build())
                .build());
    when(tenantService.getAllTenants()).thenReturn(List.of(existingTenant, otherTenant));

    tenantAdminControlsCascadeService.cascadeTenantAdminControlsIfPlatformTenant(
        tenantDTO, PLATFORM_TENANT_ID, existingTenant.getSettings());

    verify(tenantService)
        .updateTenantSettingsBatch(
            argThat(
                settingsByTenantId ->
                    settingsByTenantId.size() == 1
                        && settingsByTenantId.containsKey(otherTenant.getId())
                        && settingsByTenantId
                            .get(otherTenant.getId())
                            .contains("\"appearance\":false")));
  }

  private MultilingualTenantDTO tenantWithAllowedPermissionToggles(boolean appearance) {
    return new MultilingualTenantDTO()
        .settings(
            new Settings()
                .tenantAdminControls(
                    new TenantAdminControls()
                        .allowedPermissionToggles(
                            new TenantAdminAllowedPermissionToggles().appearance(appearance))));
  }

  private TenantEntity existingTenantWithAllowedPermissionToggles(
      Long tenantId, boolean appearance) {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(tenantId);
    tenantEntity.setSettings(
        JsonConverter.convertToJson(
            TenantSettings.builder()
                .tenantAdminControls(
                    TenantAdminControlsSettings.builder()
                        .allowedPermissionToggles(
                            TenantAdminAllowedPermissionTogglesSettings.builder()
                                .appearance(appearance)
                                .build())
                        .build())
                .build()));
    return tenantEntity;
  }

  private TenantEntity otherTenantWithSettings() {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(OTHER_TENANT_ID);
    tenantEntity.setSettings(
        JsonConverter.convertToJson(
            TenantSettings.builder()
                .tenantAdminControls(
                    TenantAdminControlsSettings.builder()
                        .permissionsPageEnabled(true)
                        .allowedPermissionToggles(
                            TenantAdminAllowedPermissionTogglesSettings.builder()
                                .appearance(true)
                                .build())
                        .build())
                .featureTopicsEnabled(true)
                .build()));
    return tenantEntity;
  }
}
