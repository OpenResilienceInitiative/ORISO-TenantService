package com.vi.tenantservice.api.service;

import static com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED;
import static com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  void save_shouldRemainTenantScoped() {
    when(repository.findByTenantId(42L)).thenReturn(Optional.empty());

    service.saveOverrides(
        42L, Map.of("featureVideoCallsEnabled", new PolicyValue<>(false, SUGGESTED)));

    org.mockito.ArgumentCaptor<TenantPermissionPolicyEntity> saved =
        org.mockito.ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    org.mockito.Mockito.verify(repository).save(saved.capture());
    assertThat(saved.getValue().getTenantId()).isEqualTo(42L);
    assertThat(saved.getValue().getPolicies()).contains("featureVideoCallsEnabled");
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
}
