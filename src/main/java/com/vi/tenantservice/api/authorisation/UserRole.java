package com.vi.tenantservice.api.authorisation;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRole {
  TENANT_ADMIN("tenant-admin"),
  SINGLE_TENANT_ADMIN("single-tenant-admin"),

  RESTRICTED_AGENCY_ADMIN("restricted-agency-admin"),

  RESTRICTED_CONSULTANT_ADMIN("restricted-consultant-admin");

  private final String value;

  public static Optional<UserRole> getRoleByValue(String value) {
    return Arrays.stream(values()).filter(userRole -> userRole.value.equals(value)).findFirst();
  }
}
