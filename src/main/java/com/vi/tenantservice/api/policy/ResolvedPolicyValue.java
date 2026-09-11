package com.vi.tenantservice.api.policy;

import java.util.Objects;

/** A resolved policy plus the information whether the value came from the parent level. */
public record ResolvedPolicyValue<T>(T value, PermissionPolicyMode mode, boolean inherited) {

  public ResolvedPolicyValue {
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(mode, "mode must not be null");
  }

  public PolicyValue<T> asPolicyValue() {
    return new PolicyValue<>(value, mode);
  }
}
