package com.vi.tenantservice.api.policy;

import static com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED;
import static com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LegacyPermissionPolicyMapperTest {

  @org.junit.jupiter.api.Test
  void fromLegacyMaps_shouldCreateCanonicalPoliciesAndEnforceTeamAccessDefault() {
    TenantAdminAllowedPermissionTogglesSettings allowed =
        TenantAdminAllowedPermissionTogglesSettings.builder()
            .supervision(true)
            .videoCalls(false)
            .build();
    TenantAdminAllowedPermissionTogglesSettings enforced =
        TenantAdminAllowedPermissionTogglesSettings.builder().supervision(true).build();

    Map<String, PolicyValue<Boolean>> policies =
        LegacyPermissionPolicyMapper.fromLegacyMaps(allowed, enforced);

    assertThat(policies)
        .containsEntry("featureSupervisionEnabled", new PolicyValue<>(true, ENFORCED))
        .containsEntry("featureVideoCallsEnabled", new PolicyValue<>(false, ENFORCED))
        .containsEntry("caseHandoverTeamAccessOptOut", new PolicyValue<>(true, ENFORCED));
  }

  @ParameterizedTest
  @MethodSource("legacyStates")
  void fromLegacy_shouldPreserveTheDocumentedPrecedence(
      Boolean allowed,
      Boolean enforced,
      boolean currentValue,
      PolicyValue<Boolean> expectedPolicy) {
    assertThat(LegacyPermissionPolicyMapper.fromLegacy(allowed, enforced, currentValue))
        .isEqualTo(expectedPolicy);
  }

  private static Stream<Arguments> legacyStates() {
    return Stream.of(
        Arguments.of(false, true, false, new PolicyValue<>(true, ENFORCED)),
        Arguments.of(false, false, true, new PolicyValue<>(false, ENFORCED)),
        Arguments.of(false, null, true, new PolicyValue<>(false, ENFORCED)),
        Arguments.of(true, false, true, new PolicyValue<>(true, SUGGESTED)),
        Arguments.of(true, false, false, new PolicyValue<>(false, SUGGESTED)),
        Arguments.of(null, true, false, new PolicyValue<>(true, ENFORCED)),
        Arguments.of(null, false, false, new PolicyValue<>(false, SUGGESTED)),
        Arguments.of(null, null, true, new PolicyValue<>(true, SUGGESTED)),
        Arguments.of(null, null, false, new PolicyValue<>(false, SUGGESTED)));
  }
}
