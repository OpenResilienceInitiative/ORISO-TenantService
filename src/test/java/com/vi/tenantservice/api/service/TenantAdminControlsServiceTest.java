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
import com.vi.tenantservice.api.model.TenantDTO;
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
  void enrichTenantDtoWithTenantAdminControls_Should_setControlsOnTenantDto() {
    TenantAdminControls globalControls =
        new TenantAdminControls()
            .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles().calls(false));
    when(tenantConverter.toTenantAdminControls(any())).thenReturn(globalControls);
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

    TenantDTO tenantDTO = new TenantDTO(1L, "tenant", "subdomain").settings(new Settings());

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

  // --- machine-translation provider API keys (stored in the same controls JSON blob) ---

  private void givenStoredControlsJson(String controlsJson) {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(TenantAdminControlsEntity.builder().id(1L).controls(controlsJson).build()));
  }

  private String capturedSavedControls() {
    ArgumentCaptor<TenantAdminControlsEntity> captor =
        ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(captor.capture());
    return captor.getValue().getControls();
  }

  @Test
  void getTranslationApiKeys_Should_returnEmptyMap_When_noKeysStored() {
    givenStoredControlsJson("{\"permissionsPageEnabled\":true}");

    assertThat(tenantAdminControlsService.getTranslationApiKeys()).isEmpty();
  }

  @Test
  void getTranslationApiKeys_Should_returnStoredKeys() {
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,"
            + "\"translationApiKeys\":{\"openrouter\":\"sk-or-key\",\"mistral\":\"mi-key\"}}");

    assertThat(tenantAdminControlsService.getTranslationApiKeys())
        .containsEntry("openrouter", "sk-or-key")
        .containsEntry("mistral", "mi-key");
  }

  @Test
  void setTranslationApiKey_Should_persistKeyInControlsJson() {
    givenStoredControlsJson("{\"permissionsPageEnabled\":true}");

    tenantAdminControlsService.setTranslationApiKey("openrouter", "sk-or-new-key");

    assertThat(capturedSavedControls())
        .contains("\"openrouter\":\"sk-or-new-key\"")
        .contains("\"permissionsPageEnabled\":true");
  }

  @Test
  void setTranslationApiKey_Should_keepOtherProviderKey() {
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,\"translationApiKeys\":{\"mistral\":\"mi-key\"}}");

    tenantAdminControlsService.setTranslationApiKey("openrouter", "sk-or-new-key");

    assertThat(capturedSavedControls())
        .contains("\"mistral\":\"mi-key\"")
        .contains("\"openrouter\":\"sk-or-new-key\"");
  }

  @Test
  void updateControls_Should_preserveStoredTranslationApiKeys() {
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,\"translationApiKeys\":{\"openrouter\":\"sk-or-key\"}}");
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(false);
    when(tenantConverter.toTenantAdminControlsSettings(request))
        .thenReturn(TenantAdminControlsSettings.builder().permissionsPageEnabled(false).build());
    when(tenantConverter.toTenantAdminControls(any(TenantAdminControlsSettings.class)))
        .thenReturn(request);

    tenantAdminControlsService.updateControls(request);

    assertThat(capturedSavedControls()).contains("\"openrouter\":\"sk-or-key\"");
  }
}
