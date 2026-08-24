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
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.policy.ResolvedPolicyValue;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
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
  @InjectMocks private TenantPermissionPolicyService service;

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
  void resolve_shouldIgnoreAStoredOverrideEntryItCannotParse_InsteadOfFailingTheBootstrapRead() {
    // The tenant_permission_policy blob is version-shared exactly like the platform controls
    // blob: an entry written by another build (here: no mode) must not take down
    // getResolvedPolicies, which feeds GET /tenant/public/... on app bootstrap. The
    // unintelligible override is ignored - the inherited platform policy stands - while a valid
    // sibling override keeps working.
    when(platformControls.getControls())
        .thenReturn(
            new TenantAdminControls()
                .permissionPolicies(
                    Map.of(
                        "featureSupervisionEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED),
                        "featureVideoCallsEnabled",
                        new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))));
    when(repository.findByTenantId(42L))
        .thenReturn(
            Optional.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(42L)
                    .policies(
                        "{\"featureSupervisionEnabled\":{\"value\":false},"
                            + "\"featureVideoCallsEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}")
                    .build()));

    Map<String, ResolvedPolicyValue<Boolean>> resolved = service.getResolvedPolicies(42L);

    assertThat(resolved.get("featureSupervisionEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(true, SUGGESTED, true));
    assertThat(resolved.get("featureVideoCallsEnabled"))
        .isEqualTo(new ResolvedPolicyValue<>(false, SUGGESTED, false));
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
}
