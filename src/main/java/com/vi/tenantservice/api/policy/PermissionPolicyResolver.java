package com.vi.tenantservice.api.policy;

import java.util.Objects;

public final class PermissionPolicyResolver {

  private PermissionPolicyResolver() {}

  public static <T> PolicyValue<T> resolve(
      PolicyValue<T> inheritedPolicy, PolicyValue<T> localOverride) {
    Objects.requireNonNull(inheritedPolicy, "inheritedPolicy must not be null");
    if (inheritedPolicy.mode() == PermissionPolicyMode.ENFORCED || localOverride == null) {
      return inheritedPolicy;
    }
    return localOverride;
  }

  public static <T> ResolvedPolicyValue<T> resolveWithOrigin(
      PolicyValue<T> inheritedPolicy, PolicyValue<T> localOverride) {
    PolicyValue<T> resolved = resolve(inheritedPolicy, localOverride);
    return new ResolvedPolicyValue<>(
        resolved.value(),
        resolved.mode(),
        inheritedPolicy.mode() == PermissionPolicyMode.ENFORCED || localOverride == null);
  }
}
