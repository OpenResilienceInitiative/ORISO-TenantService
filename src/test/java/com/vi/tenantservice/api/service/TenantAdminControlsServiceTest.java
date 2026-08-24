package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
import com.vi.tenantservice.api.policy.PermissionPolicyMode;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

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
    verify(tenantAdminControlsRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getControls()).contains("groupChat");
  }

  @Test
  void updateControls_Should_loadExistingRowOnceAndRejectAStaleWriter() {
    TenantAdminControlsEntity existing =
        TenantAdminControlsEntity.builder()
            .id(1L)
            .version(3L)
            .controls("{\"permissionsPageEnabled\":true}")
            .build();
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(false);
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existing));
    when(tenantConverter.toTenantAdminControlsSettings(request))
        .thenReturn(TenantAdminControlsSettings.builder().permissionsPageEnabled(false).build());
    when(tenantAdminControlsRepository.saveAndFlush(existing))
        .thenThrow(new OptimisticLockingFailureException("stale platform settings"));

    assertThatThrownBy(() -> tenantAdminControlsService.updateControls(request))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("changed while saving");

    verify(tenantAdminControlsRepository, times(1)).findTopByOrderByIdAsc();
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
    verify(tenantAdminControlsRepository).saveAndFlush(captor.capture());
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

  @Test
  void updateControls_Should_preserveCanonicalPoliciesOmittedByAPartialRequest() {
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,"
            + "\"permissionPolicies\":{\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}},"
            + "\"caseHandoverPolicies\":{\"reasons\":{\"COUNSELLOR_LEFT\":{\"code\":\"COUNSELLOR_LEFT\","
            + "\"enabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}}}}");
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(false);
    when(tenantConverter.toTenantAdminControlsSettings(request))
        .thenReturn(TenantAdminControlsSettings.builder().permissionsPageEnabled(false).build());
    when(tenantConverter.toTenantAdminControls(any(TenantAdminControlsSettings.class)))
        .thenReturn(request);

    tenantAdminControlsService.updateControls(request);

    assertThat(capturedSavedControls())
        .contains("\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"")
        // the stored customisation of a known reason survives the partial update ...
        .contains("\"enabled\":{\"value\":false,\"mode\":\"ENFORCED\"")
        // ... while reason codes the blob does not carry are completed from the registry defaults
        .contains("\"COUNSELLOR_ON_HOLIDAY\"");
  }

  @Test
  void getControls_shouldDualReadLegacyMapsAsCanonicalPolicies() {
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,"
            + "\"allowedPermissionToggles\":{\"videoCalls\":false},"
            + "\"enforcedPermissionToggles\":{\"supervision\":true}}");
    when(tenantConverter.toTenantAdminControls(any())).thenReturn(new TenantAdminControls());

    tenantAdminControlsService.getControls();

    ArgumentCaptor<TenantAdminControlsSettings> settings =
        ArgumentCaptor.forClass(TenantAdminControlsSettings.class);
    verify(tenantConverter).toTenantAdminControls(settings.capture());
    assertThat(settings.getValue().getPermissionPolicies().get("featureVideoCallsEnabled").value())
        .isFalse();
    assertThat(settings.getValue().getPermissionPolicies().get("featureSupervisionEnabled").mode())
        .isEqualTo(PermissionPolicyMode.ENFORCED);
  }

  // --- lenient storage read: the blob is shared across builds (see TenantAdminControlsSettings
  // javadoc). The stored JSON may have been written by a newer or older build; reading it must
  // never take down the bootstrap path. An entry this build cannot fully understand is DROPPED
  // (never defaulted, never fabricated) while every intelligible entry keeps its meaning. ---

  private TenantAdminControlsSettings parsedSettingsHandedToConverter() {
    ArgumentCaptor<TenantAdminControlsSettings> captor =
        ArgumentCaptor.forClass(TenantAdminControlsSettings.class);
    verify(tenantConverter).toTenantAdminControls(captor.capture());
    return captor.getValue();
  }

  @Test
  void getControls_Should_dropAStoredPolicyEntryWithoutAMode_AndKeepItsValidSiblings() {
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,"
            + "\"permissionPolicies\":{"
            + "\"featureVideoCallsEnabled\":{\"value\":true},"
            + "\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    tenantAdminControlsService.getControls();

    TenantAdminControlsSettings parsed = parsedSettingsHandedToConverter();
    assertThat(parsed.getPermissionPolicies())
        .containsEntry(
            "featureCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.ENFORCED))
        .doesNotContainKey("featureVideoCallsEnabled");
  }

  @Test
  void getControls_Should_dropAStoredPolicyEntryWithAModeUnknownToThisBuild() {
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"featureCallsEnabled\":{\"value\":true,\"mode\":\"LOCKED\"},"
            + "\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsEntry(
            "featureVideoCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.SUGGESTED))
        .doesNotContainKey("featureCallsEnabled");
  }

  @Test
  void getControls_Should_dropAStoredPolicyEntryWithoutAValue() {
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"featureCallsEnabled\":{\"mode\":\"ENFORCED\"},"
            + "\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsKey("featureVideoCallsEnabled")
        .doesNotContainKey("featureCallsEnabled");
  }

  @Test
  void getControls_Should_dropAStoredPolicyEntryWithANonBooleanValue() {
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"featureCallsEnabled\":{\"value\":\"yes\",\"mode\":\"ENFORCED\"},"
            + "\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsKey("featureVideoCallsEnabled")
        .doesNotContainKey("featureCallsEnabled");
  }

  @Test
  void getControls_Should_dropAStoredPolicyEntryThatIsNotAnObject() {
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"featureCallsEnabled\":true,"
            + "\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsKey("featureVideoCallsEnabled")
        .doesNotContainKey("featureCallsEnabled");
  }

  @Test
  void getControls_Should_dropAStoredPolicyEntryForAFeatureUnknownToThisBuild() {
    // A well-formed entry under a feature key this build has never heard of must not survive the
    // read: TenantPermissionPolicyService.toDomain asserts known features on the public bootstrap
    // path, so keeping the entry would turn the alien key into a 500 one layer further up.
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"someFutureFeatureEnabled\":{\"value\":true,\"mode\":\"ENFORCED\"},"
            + "\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsEntry(
            "featureCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.ENFORCED))
        .doesNotContainKey("someFutureFeatureEnabled");
  }

  @Test
  void getControls_Should_keepAStoredPolicyEntryCarryingUnknownExtraFields() {
    // Additive fields inside an entry (e.g. the API's read-only "inherited") are one-way
    // tolerated, exactly like unknown fields at the blob root.
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\",\"inherited\":true,\"note\":\"x\"}}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsEntry(
            "featureCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.ENFORCED));
  }

  @Test
  void getControls_Should_fallBackToLegacyDerivedPolicies_When_noStoredPolicyEntryIsIntelligible() {
    // When nothing in the canonical map survives, the read behaves as if the alien build's data
    // were invisible: the canonical policies are re-derived from the legacy toggle maps in the
    // same blob - the exact behaviour of a build that predates the canonical map.
    givenStoredControlsJson(
        "{\"permissionPolicies\":{\"someFutureFeatureEnabled\":{\"value\":true}},"
            + "\"allowedPermissionToggles\":{\"videoCalls\":false}}");

    tenantAdminControlsService.getControls();

    TenantAdminControlsSettings parsed = parsedSettingsHandedToConverter();
    assertThat(parsed.getPermissionPolicies())
        .doesNotContainKey("someFutureFeatureEnabled")
        .containsEntry(
            "featureVideoCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.ENFORCED));
  }

  @Test
  void getControls_Should_fallBackToLegacyDerivedPolicies_When_theStoredPolicyMapIsNotAnObject() {
    givenStoredControlsJson(
        "{\"permissionPolicies\":[]," + "\"allowedPermissionToggles\":{\"videoCalls\":false}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies())
        .containsEntry(
            "featureVideoCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.ENFORCED));
  }

  @Test
  void getControls_Should_ignoreUnknownFieldsInsideNestedSections_When_writtenByAnotherBuild() {
    // Verbatim shape of the Pre-Dev 2026-08-18 outage blob: an alien policy entry plus an unknown
    // field inside a nested generated type. Root-level @JsonIgnoreProperties does not shield
    // nested types, so the read itself must be lenient at every depth.
    givenStoredControlsJson(
        "{\"permissionsPageEnabled\":true,"
            + "\"permissionPolicies\":{\"featureVideoCall\":{\"value\":true}},"
            + "\"caseHandoverPolicies\":{\"requireConsent\":true}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().isPermissionsPageEnabled()).isTrue();
  }

  @Test
  void getControls_Should_substituteCaseHandoverDefaults_When_storedSectionHasNoReasons() {
    // A case-handover section whose reasons are unintelligible to this build behaves as if the
    // section were absent: the platform defaults apply instead of a null map that would NPE the
    // resolver downstream.
    givenStoredControlsJson("{\"caseHandoverPolicies\":{\"requireConsent\":true}}");

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getCaseHandoverPolicies().getReasons())
        .containsOnlyKeys(CaseHandoverPolicyDefaults.create().getReasons().keySet());
  }

  @Test
  void getControls_Should_handOverFullyHydratedDefaults_When_nothingIsStored() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
    when(tenantConverter.toTenantAdminControlsSettings(any(TenantAdminControls.class)))
        .thenReturn(TenantAdminControlsSettings.builder().build());

    tenantAdminControlsService.getControls();

    TenantAdminControlsSettings parsed = parsedSettingsHandedToConverter();
    assertThat(parsed.getPermissionPolicies()).isNotEmpty();
    assertThat(parsed.getCaseHandoverPolicies().getReasons())
        .containsOnlyKeys(CaseHandoverPolicyDefaults.create().getReasons().keySet());
  }

  @Test
  void getControls_Should_fallBackToDefaults_When_theStoredBlobIsBlank() {
    givenStoredControlsJson("   ");
    when(tenantConverter.toTenantAdminControlsSettings(any(TenantAdminControls.class)))
        .thenReturn(TenantAdminControlsSettings.builder().build());

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies()).isNotEmpty();
  }

  @Test
  void getControls_Should_fallBackToDefaults_When_theStoredBlobIsTheNullLiteral() {
    givenStoredControlsJson("null");
    when(tenantConverter.toTenantAdminControlsSettings(any(TenantAdminControls.class)))
        .thenReturn(TenantAdminControlsSettings.builder().build());

    tenantAdminControlsService.getControls();

    assertThat(parsedSettingsHandedToConverter().getPermissionPolicies()).isNotEmpty();
  }

  @Test
  void updateControls_Should_completeTheReasonRegistry_When_theRequestCarriesAPartialSection() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(true);
    TenantAdminControlsSettings partial =
        TenantAdminControlsSettings.builder()
            .permissionsPageEnabled(true)
            .permissionPolicies(
                java.util.Map.of(
                    "featureCallsEnabled", new PolicyValue<>(false, PermissionPolicyMode.ENFORCED)))
            .caseHandoverPolicies(new com.vi.tenantservice.api.model.CaseHandoverPolicies())
            .build();
    when(tenantConverter.toTenantAdminControlsSettings(request)).thenReturn(partial);
    when(tenantConverter.toTenantAdminControls(any(TenantAdminControlsSettings.class)))
        .thenReturn(request);

    TenantAdminControls result = tenantAdminControlsService.updateControls(request);

    assertThat(result).isSameAs(request);
    assertThat(capturedSavedControls())
        .contains("\"COUNSELLOR_LEFT\"")
        .contains("\"COUNSELLOR_ASKED_FOR_ADVICE\"")
        .contains("\"COUNSELLOR_ON_HOLIDAY\"")
        .contains("\"COUNSELLOR_IS_ILL\"");
  }

  @Test
  void updateControls_Should_treatAnEmptyRequestPolicyMapAsOmitted() {
    givenStoredControlsJson(
        "{\"permissionPolicies\":{\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}}");
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(false);
    when(tenantConverter.toTenantAdminControlsSettings(request))
        .thenReturn(
            TenantAdminControlsSettings.builder()
                .permissionsPageEnabled(false)
                .permissionPolicies(java.util.Map.of())
                .build());
    when(tenantConverter.toTenantAdminControls(any(TenantAdminControlsSettings.class)))
        .thenReturn(request);

    tenantAdminControlsService.updateControls(request);

    assertThat(capturedSavedControls())
        .contains("\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}");
  }

  @Test
  void updateControls_Should_notResurrectADroppedPolicyEntry_When_rewritingTheBlob() {
    // Dropped means dropped: the rewrite must not fabricate a defaulted entry out of a stored
    // fragment this build could not understand.
    givenStoredControlsJson(
        "{\"permissionPolicies\":{"
            + "\"featureVideoCallsEnabled\":{\"value\":true},"
            + "\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}}");
    TenantAdminControls request = new TenantAdminControls().permissionsPageEnabled(false);
    when(tenantConverter.toTenantAdminControlsSettings(request))
        .thenReturn(TenantAdminControlsSettings.builder().permissionsPageEnabled(false).build());
    when(tenantConverter.toTenantAdminControls(any(TenantAdminControlsSettings.class)))
        .thenReturn(request);

    tenantAdminControlsService.updateControls(request);

    assertThat(capturedSavedControls())
        .contains("\"featureCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}")
        .doesNotContain("featureVideoCallsEnabled");
  }
}
