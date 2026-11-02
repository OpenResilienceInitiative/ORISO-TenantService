package com.vi.tenantservice.api.service;

import static com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED;
import static com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverConsentValue;
import com.vi.tenantservice.api.model.ConsentPermissionPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.policy.ResolvedPolicyValue;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class TenantPermissionPolicyServiceTest {

  @Mock private TenantPermissionPolicyRepository repository;
  @Mock private TenantAdminControlsService platformControls;
  @Mock private com.vi.tenantservice.api.repository.TenantRepository tenants;
  @InjectMocks private TenantPermissionPolicyService service;

  @Test
  void collectionOptOutUsesOneCanonicalMapWithoutOverridingExplicitPoliciesOrRequirements() {
    var keys =
        java.util.List.of(
            "featureAskerEmailCollectionAgencyCounsellingEnabled",
            "featureAskerEmailCollectionLiveChatEnabled",
            "featureAskerEmailCollectionSelfHelpEnabled");
    var raw = new java.util.LinkedHashMap<String, Boolean>();
    keys.forEach(key -> raw.put(key, false));
    String json;
    try {
      json = new ObjectMapper().writeValueAsString(raw);
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
    when(tenants.findAllById(Set.of(42L)))
        .thenReturn(
            java.util.List.of(
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(42L)
                    .settings(json)
                    .build()));
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    var inherited = service.getResolvedPolicies(Set.of(42L)).get(42L);
    keys.forEach(
        key ->
            assertThat(inherited.get(key))
                .as("stored opt-out: %s", key)
                .isEqualTo(new ResolvedPolicyValue<>(false, SUGGESTED, false)));

    var explicit = new java.util.LinkedHashMap<String, PolicyValue<Boolean>>();
    keys.forEach(key -> explicit.put(key, new PolicyValue<>(true, SUGGESTED)));
    try {
      when(repository.findByTenantIdIn(Set.of(42L)))
          .thenReturn(
              java.util.List.of(
                  TenantPermissionPolicyEntity.builder()
                      .tenantId(42L)
                      .policies(new ObjectMapper().writeValueAsString(explicit))
                      .build()));
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
    var chosen = service.getResolvedPolicies(Set.of(42L)).get(42L);
    keys.forEach(
        key ->
            assertThat(chosen.get(key))
                .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, false)));

    var enforced = new java.util.LinkedHashMap<String, BooleanPermissionPolicy>();
    keys.forEach(
        key -> enforced.put(key, new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED)));
    when(platformControls.getControls())
        .thenReturn(new TenantAdminControls().permissionPolicies(enforced));
    var required = service.getResolvedPolicies(Set.of(42L)).get(42L);
    keys.forEach(
        key ->
            assertThat(required.get(key))
                .isEqualTo(new ResolvedPolicyValue<>(true, ENFORCED, true)));
  }

  @Test
  void resolve_shouldKeepPlatformEnforcementAndApplyTenantSuggestionOverride() {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureSupervisionEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED),
                        "featureVideoCallsEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies(
                        "{\"featureSupervisionEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"},"
                            + "\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"ENFORCED\"}}")
                    .build()));

    Map<String, ResolvedPolicyValue<Boolean>> resolved = service.getResolvedPolicies(42L);

    assertThat(resolved.get("featureSupervisionEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, ENFORCED, true));
    assertThat(resolved.get("featureVideoCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(false, ENFORCED, false));
  }

  @Test
  void resolve_shouldApplyASplitFormatOverride_whenThePlatformMapPredatesTheSplit() {
    // the platform map lists only the master switch, never the split-out format keys (#250).
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureGroupChatV2Enabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies(
                        "{\"featureSelfHelpGroupsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}")
                    .build()));

    Map<String, ResolvedPolicyValue<Boolean>> resolved = service.getResolvedPolicies(42L);

    // the override is honoured against the master inherited as the split format's parent,
    // not dropped because the platform map lacks the key.
    assertThat(resolved.get("featureSelfHelpGroupsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(false, SUGGESTED, false));
    // the sibling internal format inherits the master untouched.
    assertThat(resolved.get("featureInternalGroupChatEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
  }

  @Test
  void save_shouldRemainTenantScoped() {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureVideoCallsEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantId(42L)).thenReturn(Optional.empty());

    service.saveOverrides(
        42L, Map.of("featureVideoCallsEnabled", new PolicyValue<>(false, SUGGESTED)));

    org.mockito.ArgumentCaptor<TenantPermissionPolicyEntity> saved =
        org.mockito.ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    org.mockito.Mockito.verify(repository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getTenantId()).isEqualTo(42L);
    assertThat(saved.getValue().getPolicies()).contains("featureVideoCallsEnabled");
  }

  @Test
  void save_shouldRejectAChangedValueUnderAnEnforcedParentBeforePersistence() {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureVideoCallsEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED))));

    assertThatThrownBy(
            () ->
                service.saveOverrides(
                    42L, Map.of("featureVideoCallsEnabled", new PolicyValue<>(false, SUGGESTED))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("featureVideoCallsEnabled");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void save_shouldDiscardAnUnchangedReadOnlyEchoUnderAnEnforcedParent() {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureSupervisionEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED))));
    when(repository.findByTenantId(42L)).thenReturn(Optional.empty());

    service.saveOverrides(
        42L, Map.of("featureSupervisionEnabled", new PolicyValue<>(true, ENFORCED)));

    org.mockito.ArgumentCaptor<TenantPermissionPolicyEntity> saved =
        org.mockito.ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    verify(repository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getPolicies()).isEqualTo("{}");
  }

  @Test
  void save_shouldRejectAStaleTenantPolicyWriter() {
    TenantPermissionPolicyEntity existing =
        TenantPermissionPolicyEntity.builder()
            .id(7L)
            .tenantId(42L)
            .version(4L)
            .policies("{}")
            .build();
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureVideoCallsEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantId(42L)).thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(existing))
        .thenThrow(new OptimisticLockingFailureException("stale tenant policy"));

    assertThatThrownBy(
            () ->
                service.saveOverrides(
                    42L, Map.of("featureVideoCallsEnabled", new PolicyValue<>(false, SUGGESTED))))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("changed while saving");
  }

  @Test
  void resolveMany_shouldLoadPlatformAndTenantOverridesInBulk() {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureVideoCallsEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantIdIn(Set.of(1L, 2L)))
        .thenReturn(
            java.util.List.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(1L)
                    .policies(
                        "{\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}")
                    .build()));

    var resolved = service.getResolvedPolicies(Set.of(1L, 2L));

    assertThat(resolved.get(1L).get("featureVideoCallsEnabled").value()).isFalse();
    assertThat(resolved.get(2L).get("featureVideoCallsEnabled").value()).isTrue();
    verify(repository).findByTenantIdIn(Set.of(1L, 2L));
    verify(repository, never()).findByTenantId(any());
    verify(platformControls).getControls();
  }

  // --- #254: the platform list on dev holds a single entry (caseHandoverEnabled) ---

  private static TenantAdminControls platformWithOnlyCaseHandover() {
    return new TenantAdminControls()
        .permissionPolicies(
            Map.of(
                "caseHandoverEnabled",
                new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED)));
  }

  private static String[] everyRegistryFeature() {
    return Arrays.stream(PermissionFeature.values())
        .map(PermissionFeature::apiKey)
        .toArray(String[]::new);
  }

  @Test
  void resolve_shouldKeepATenantOverrideForAFeatureWithoutAPlatformEntry_acrossSaveAndRead() {
    when(platformControls.getControls()).thenReturn(platformWithOnlyCaseHandover());
    TenantPermissionPolicyEntity[] stored = new TenantPermissionPolicyEntity[1];
    when(repository.findByTenantId(42L)).thenAnswer(invocation -> Optional.ofNullable(stored[0]));
    when(repository.saveAndFlush(any(TenantPermissionPolicyEntity.class)))
        .thenAnswer(
            invocation -> {
              stored[0] = invocation.getArgument(0);
              return stored[0];
            });

    service.saveOverrides(
        42L, Map.of("featureAudioCallsEnabled", new PolicyValue<>(true, ENFORCED)));
    Map<String, ResolvedPolicyValue<Boolean>> resolved = service.getResolvedPolicies(42L);

    assertThat(resolved.get("featureAudioCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, ENFORCED, false));
    assertThat(resolved.get("caseHandoverEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
  }

  @Test
  void resolve_shouldListEveryRegistryFeature_whenThePlatformListHoldsASingleEntry() {
    when(platformControls.getControls()).thenReturn(platformWithOnlyCaseHandover());
    when(repository.findByTenantId(42L)).thenReturn(Optional.empty());

    Map<String, ResolvedPolicyValue<Boolean>> resolved = service.getResolvedPolicies(42L);

    assertThat(resolved).containsOnlyKeys(everyRegistryFeature());
    assertThat(resolved.get("featureVideoCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
    assertThat(resolved.get("featureAudioCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
  }

  @Test
  void resolve_shouldTakeTheMissingPlatformEntryFromTheLegacyToggles_notFromAFixedDefault() {
    when(platformControls.getControls())
        .thenReturn(
            platformWithOnlyCaseHandover()
                .allowedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().videoCalls(false))
                .enforcedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().supervision(true)));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies(
                        "{\"featureVideoCallsEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"}}")
                    .build()));

    Map<String, ResolvedPolicyValue<Boolean>> resolved = service.getResolvedPolicies(42L);

    // platform forbade video calls through the legacy toggle: the tenant override cannot win
    assertThat(resolved.get("featureVideoCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(false, ENFORCED, true));
    assertThat(resolved.get("featureSupervisionEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, ENFORCED, true));
  }

  @Test
  void resolveMany_shouldListEveryRegistryFeature_whenThePlatformListHoldsASingleEntry() {
    when(platformControls.getControls()).thenReturn(platformWithOnlyCaseHandover());
    when(repository.findByTenantIdIn(Set.of(1L)))
        .thenReturn(
            java.util.List.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(1L)
                    .policies(
                        "{\"featureAudioCallsEnabled\":{\"value\":true,\"mode\":\"ENFORCED\"}}")
                    .build()));

    var resolved = service.getResolvedPolicies(Set.of(1L));

    assertThat(resolved.get(1L)).containsOnlyKeys(everyRegistryFeature());
    assertThat(resolved.get(1L).get("featureAudioCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, ENFORCED, false));
  }

  @Test
  void resolveCaseHandover_shouldKeepEnforcedParentAndApplySuggestedDurationOverride()
      throws Exception {
    var parent = CaseHandoverPolicyDefaults.create();
    var parentAdvice = parent.getReasons().get(CaseHandoverPolicyDefaults.ADVICE_NEEDED);
    parentAdvice.setEnabled(new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED));
    var local = CaseHandoverPolicyDefaults.create();
    var localAdvice = local.getReasons().get(CaseHandoverPolicyDefaults.ADVICE_NEEDED);
    localAdvice.setEnabled(new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED));
    localAdvice.setMaxAccessDurationMinutes(
        new IntegerPermissionPolicy(1500, PermissionPolicyMode.ENFORCED));
    when(platformControls.getControls())
        .thenReturn(new TenantAdminControls().caseHandoverPolicies(parent));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies("{}")
                    .caseHandoverPolicies(new ObjectMapper().writeValueAsString(local))
                    .build()));

    var resolved = service.getResolvedCaseHandoverPolicies(42L);
    var advice = resolved.getReasons().get(CaseHandoverPolicyDefaults.ADVICE_NEEDED);

    assertThat(advice.getEnabled().getValue()).isTrue();
    assertThat(advice.getEnabled().getInherited()).isTrue();
    assertThat(advice.getMaxAccessDurationMinutes().getValue()).isEqualTo(1500);
    assertThat(advice.getMaxAccessDurationMinutes().getInherited()).isFalse();
  }

  @Test
  void resolveCaseHandover_shouldPreferLegacyEnforcedOptInDuringDualRead() throws Exception {
    var local = CaseHandoverPolicyDefaults.create();
    var localAdvice = local.getReasons().get(CaseHandoverPolicyDefaults.ADVICE_NEEDED);
    localAdvice.setClientConsent(
        new ConsentPermissionPolicy(
            CaseHandoverConsentValue.OPT_OUT, PermissionPolicyMode.SUGGESTED));
    localAdvice.setClientConsentRequired(
        new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED));
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls().caseHandoverPolicies(CaseHandoverPolicyDefaults.create()));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies("{}")
                    .caseHandoverPolicies(new ObjectMapper().writeValueAsString(local))
                    .build()));

    var consent =
        service
            .getResolvedCaseHandoverPolicies(42L)
            .getReasons()
            .get(CaseHandoverPolicyDefaults.ADVICE_NEEDED)
            .getClientConsent();

    assertThat(consent.getValue()).isEqualTo(CaseHandoverConsentValue.OPT_IN);
    assertThat(consent.getMode()).isEqualTo(PermissionPolicyMode.ENFORCED);
    assertThat(consent.getInherited()).isFalse();
  }

  @Test
  void saveCaseHandover_shouldRejectEnforcedNoConsentBecauseItHasNoUiState() {
    var local = CaseHandoverPolicyDefaults.create();
    local
        .getReasons()
        .get(CaseHandoverPolicyDefaults.ADVICE_NEEDED)
        .setClientConsent(
            new ConsentPermissionPolicy(
                CaseHandoverConsentValue.NONE, PermissionPolicyMode.ENFORCED));

    assertThatThrownBy(() -> service.saveOverrides(42L, Map.of(), local))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("NONE consent");

    verify(repository, never()).saveAndFlush(any());
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments>
      collectionPolicyStates() {
    return java.util.stream.Stream.of(
            "featureAskerEmailCollectionAgencyCounsellingEnabled",
            "featureAskerEmailCollectionLiveChatEnabled",
            "featureAskerEmailCollectionSelfHelpEnabled")
        .flatMap(
            key ->
                java.util.stream.Stream.of(true, false)
                    .flatMap(
                        value ->
                            java.util.stream.Stream.of(
                                    PermissionPolicyMode.ENFORCED, PermissionPolicyMode.SUGGESTED)
                                .map(
                                    mode ->
                                        org.junit.jupiter.params.provider.Arguments.of(
                                            key, value, mode))));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("collectionPolicyStates")
  void collectionPoliciesHonorAllFourStatesAndKeepTenantOverridesIsolated(
      String key, boolean parentValue, PermissionPolicyMode parentMode) throws Exception {
    when(tenants.findAllById(Set.of(42L)))
        .thenReturn(
            java.util.List.of(
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(42L)
                    .settings("{\"" + key + "\":false}")
                    .build()));
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureAskerEmailEnabled",
                        new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
                        key,
                        new BooleanPermissionPolicy(parentValue, parentMode))));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies(
                        new ObjectMapper()
                            .writeValueAsString(
                                Map.of(key, new PolicyValue<>(!parentValue, SUGGESTED))))
                    .build()));
    when(repository.findByTenantId(84L)).thenReturn(Optional.empty());
    var locked = parentMode == PermissionPolicyMode.ENFORCED;
    var resolved = service.getResolvedPolicies(42L);
    assertThat(resolved.get(key))
        .isEqualTo(
            new ResolvedPolicyValue<>(
                locked ? parentValue : !parentValue, locked ? ENFORCED : SUGGESTED, locked));
    assertThat(resolved.get("featureAskerEmailEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(false, ENFORCED, true));
    assertThat(service.getResolvedPolicies(84L).get(key))
        .isEqualTo(new ResolvedPolicyValue<>(parentValue, locked ? ENFORCED : SUGGESTED, true));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "featureAskerEmailCollectionAgencyCounsellingEnabled",
        "featureAskerEmailCollectionLiveChatEnabled",
        "featureAskerEmailCollectionSelfHelpEnabled"
      })
  void collectionPreferencesRemainPersistableWhileTheGlobalMasterIsOff(String key)
      throws Exception {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureAskerEmailEnabled",
                        new BooleanPermissionPolicy(false, PermissionPolicyMode.ENFORCED),
                        key,
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantId(42L)).thenReturn(Optional.empty());
    service.saveOverrides(42L, Map.of(key, new PolicyValue<>(false, SUGGESTED)));
    var saved = org.mockito.ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    verify(repository).saveAndFlush(saved.capture());
    var json = new ObjectMapper().readTree(saved.getValue().getPolicies());
    assertThat(saved.getValue().getTenantId()).isEqualTo(42L);
    assertThat(json.path(key).path("value").asBoolean(true)).isFalse();
    assertThat(json.path(key).path("mode").asText()).isEqualTo("SUGGESTED");
    assertThat(json.has("featureAskerEmailEnabled")).isFalse();
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments>
      rawCollectionInputs() {
    return java.util.stream.Stream.of(
            "featureAskerEmailCollectionAgencyCounsellingEnabled",
            "featureAskerEmailCollectionLiveChatEnabled",
            "featureAskerEmailCollectionSelfHelpEnabled")
        .flatMap(
            key ->
                java.util.stream.Stream.of("absent", "null", "false", "true")
                    .flatMap(
                        raw ->
                            java.util.stream.Stream.of(true, false)
                                .map(
                                    parent ->
                                        org.junit.jupiter.params.provider.Arguments.of(
                                            key, raw, parent))));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("rawCollectionInputs")
  void rawFalseIsRestrictiveButNullAbsentAndTrueCannotGrantOverTheParentSuggestion(
      String key, String raw, boolean parent) {
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        key, new BooleanPermissionPolicy(parent, PermissionPolicyMode.SUGGESTED))));
    String json = "absent".equals(raw) ? "{}" : "{\"" + key + "\":" + raw + "}";
    when(tenants.findAllById(Set.of(42L)))
        .thenReturn(
            java.util.List.of(
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(42L)
                    .settings(json)
                    .build()));
    boolean optOut = "false".equals(raw);
    assertThat(service.getResolvedPolicies(42L).get(key))
        .isEqualTo(new ResolvedPolicyValue<>(optOut ? false : parent, SUGGESTED, !optOut));
    verify(tenants).findAllById(Set.of(42L));
  }

  @Test
  void batchedRawPreferencesStayIsolatedAndUseOneSettingsQueryForTheWholePage() {
    String key = "featureAskerEmailCollectionLiveChatEnabled";
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    Set<Long> ids = Set.of(42L, 84L);
    when(tenants.findAllById(ids))
        .thenReturn(
            java.util.List.of(
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(42L)
                    .settings("{\"" + key + "\":false}")
                    .build(),
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(84L)
                    .settings("{}")
                    .build(),
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(999L)
                    .settings("{\"" + key + "\":false}")
                    .build()));
    var result = service.getResolvedPolicies(ids);
    assertThat(result).containsOnlyKeys(42L, 84L);
    assertThat(result.get(42L).get(key))
        .isEqualTo(new ResolvedPolicyValue<>(false, SUGGESTED, false));
    assertThat(result.get(84L).get(key))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
    verify(tenants).findAllById(ids);
    verify(repository).findByTenantIdIn(ids);
    org.mockito.Mockito.verifyNoMoreInteractions(tenants, repository);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{\"smtpPassword\":\"must-never-leak\",",
        "[]",
        "null",
        "{\"featureAskerEmailCollectionLiveChatEnabled\":\"false\"}"
      })
  void malformedPrivateCollectionSettingsRefuseWithoutParserCauseOrRawData(String json) {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(tenants.findAllById(Set.of(42L)))
        .thenReturn(
            java.util.List.of(
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(42L)
                    .settings(json)
                    .build()));
    assertThatThrownBy(() -> service.getResolvedPolicies(42L))
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("Current tenant collection preferences are unavailable");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.NullAndEmptySource
  @org.junit.jupiter.params.provider.ValueSource(strings = {" "})
  void absentStoredSettingsRetainTheInheritedDefault(String json) {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(tenants.findAllById(Set.of(42L)))
        .thenReturn(
            java.util.List.of(
                com.vi.tenantservice.api.model.TenantEntity.builder()
                    .id(42L)
                    .settings(json)
                    .build()));
    assertThat(service.getResolvedPolicies(42L).get("featureAskerEmailCollectionLiveChatEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
  }
}
