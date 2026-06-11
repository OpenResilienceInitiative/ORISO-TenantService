package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantAdminControlsServiceTest {

  @Mock private TenantAdminControlsRepository tenantAdminControlsRepository;
  @Mock private TenantConverter tenantConverter;

  @InjectMocks private TenantAdminControlsService tenantAdminControlsService;

  @Test
  void stripTenantAdminControlsFromTenantDto_Should_removeControlsFromSettings() {
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO()
            .settings(
                new Settings()
                    .tenantAdminControls(
                        new TenantAdminControls()
                            .allowedPermissionToggles(
                                new TenantAdminAllowedPermissionToggles().appearance(false))));

    tenantAdminControlsService.stripTenantAdminControlsFromTenantDto(tenantDTO);

    assertThat(tenantDTO.getSettings().getTenantAdminControls()).isNull();
  }

  @Test
  void enrichTenantDtoWithTenantAdminControls_Should_setControlsFromGlobalStore() {
    TenantAdminControls globalControls =
        new TenantAdminControls()
            .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles().appearance(true));
    when(tenantConverter.toTenantAdminControls(any())).thenReturn(globalControls);
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(
                TenantAdminControlsEntity.builder()
                    .id(1L)
                    .controls("{\"permissionsPageEnabled\":true}")
                    .build()));

    MultilingualTenantDTO tenantDTO = new MultilingualTenantDTO().settings(new Settings());

    tenantAdminControlsService.enrichTenantDtoWithTenantAdminControls(tenantDTO);

    assertThat(tenantDTO.getSettings().getTenantAdminControls()).isEqualTo(globalControls);
  }

  @Test
  void updateControls_Should_persistSerializedControls() {
    TenantAdminControls request =
        new TenantAdminControls()
            .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles().groupChat(false));
    TenantAdminControlsSettings settings =
        TenantAdminControlsSettings.builder()
            .permissionsPageEnabled(true)
            .allowedPermissionToggles(
                TenantAdminAllowedPermissionTogglesSettings.builder().groupChat(false).build())
            .build();

    when(tenantConverter.toTenantAdminControlsSettings(request)).thenReturn(settings);
    when(tenantConverter.toTenantAdminControls(settings)).thenReturn(request);
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

    tenantAdminControlsService.updateControls(request);

    ArgumentCaptor<TenantAdminControlsEntity> captor =
        ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(captor.capture());
    assertThat(captor.getValue().getControls()).contains("groupChat");
  }
}
