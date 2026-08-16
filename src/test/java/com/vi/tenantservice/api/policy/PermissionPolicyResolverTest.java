package com.vi.tenantservice.api.policy;

import static com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED;
import static com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PermissionPolicyResolverTest {

  @ParameterizedTest
  @MethodSource("fourBooleanPolicyStates")
  void resolve_shouldRepresentEveryBooleanPolicyState(boolean value, PermissionPolicyMode mode) {
    PolicyValue<Boolean> policy = new PolicyValue<>(value, mode);

    assertThat(PermissionPolicyResolver.resolve(policy, null)).isEqualTo(policy);
  }

  private static Stream<Arguments> fourBooleanPolicyStates() {
    return Stream.of(
        Arguments.of(true, ENFORCED),
        Arguments.of(false, ENFORCED),
        Arguments.of(true, SUGGESTED),
        Arguments.of(false, SUGGESTED));
  }
}
