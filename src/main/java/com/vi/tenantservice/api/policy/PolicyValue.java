package com.vi.tenantservice.api.policy;

import java.util.Objects;

public record PolicyValue<T>(T value, PermissionPolicyMode mode) {

  public PolicyValue {
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(mode, "mode must not be null");
  }
}
