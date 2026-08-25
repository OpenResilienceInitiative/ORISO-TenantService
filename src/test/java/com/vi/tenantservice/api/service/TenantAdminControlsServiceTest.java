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
  void getControls_Should_ignoreUnknownFields_When_storedBlobWasWrittenByANewerVersion() {
    // A newer build writes additional keys into the same tenant_admin_controls blob. Rolling that
    // build back must not take the service down: this blob feeds getControls() ->
    // withEffectivePermissions() -> GET /tenant/public/id/{id}, which is what the app bootstraps
    // from, so a parse failure here is a 500 on the first request of every session.
    // Observed on Pre-Dev 2026-08-18: "Unrecognized field \"permissionPolicies\"".
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,"
            + "\"permissionPolicies\":{\"featureVideoCall\":{\"value\":true}},"
            + "\"caseHandoverPolicies\":{\"requireConsent\":true}}");

    tenantAdminControlsService.getControls();

    // The converter is mocked, so assert on what the parser handed it: the blob was read, the
    // known field survived, and the unknown ones were dropped instead of throwing.
    ArgumentCaptor<TenantAdminControlsSettings> captor =
        ArgumentCaptor.forClass(TenantAdminControlsSettings.class);
    verify(tenantConverter).toTenantAdminControls(captor.capture());
    assertThat(captor.getValue().isPermissionsPageEnabled()).isTrue();
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

  // --- version skew: the controls blob is shared with builds that know more fields than this one
  // ---

  private static final String BLOB_FROM_A_NEWER_BUILD =
      "{\"permissionsPageEnabled\":true,"
          + "\"permissionPolicies\":{\"featureVideoCall\":{\"value\":true}},"
          + "\"caseHandoverPolicies\":{\"requireConsent\":true}}";

  /**
   * The blob is read leniently on purpose (see {@link TenantAdminControlsSettings}) because a
   * strict read turns version skew into HTTP 500 on every session bootstrap - that is what took
   * Pre-Dev down on 2026-08-18. Writing the narrower type back is the same skew from the other
   * side: it deletes whatever a newer build wrote. Unlike the read failure this one is silent, and
   * it happens on the ordinary admin path, not only at startup.
   */
  @Test
  void updateControls_Should_keepFieldsWrittenByANewerBuild() {
    givenStoredControlsJson(BLOB_FROM_A_NEWER_BUILD);
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(false);
    when(tenantConverter.toTenantAdminControlsSettings(request))
        .thenReturn(TenantAdminControlsSettings.builder().permissionsPageEnabled(false).build());
    when(tenantConverter.toTenantAdminControls(any(TenantAdminControlsSettings.class)))
        .thenReturn(request);

    tenantAdminControlsService.updateControls(request);

    assertThat(capturedSavedControls())
        .contains("\"permissionPolicies\"")
        .contains("\"featureVideoCall\"")
        .contains("\"caseHandoverPolicies\"")
        .contains("\"requireConsent\"")
        // and the field this build does own is still the one it just wrote
        .contains("\"permissionsPageEnabled\":false");
  }

  /** Same defect, reached through the other write on the ordinary admin path. */
  @Test
  void setTranslationApiKey_Should_keepFieldsWrittenByANewerBuild() {
    givenStoredControlsJson(BLOB_FROM_A_NEWER_BUILD);

    tenantAdminControlsService.setTranslationApiKey("openrouter", "sk-or-new-key");

    assertThat(capturedSavedControls())
        .contains("\"permissionPolicies\"")
        .contains("\"featureVideoCall\"")
        .contains("\"caseHandoverPolicies\"")
        .contains("\"requireConsent\"")
        .contains("\"openrouter\":\"sk-or-new-key\"")
        .contains("\"permissionsPageEnabled\":true");
  }
}
