package com.vi.tenantservice.api.authorisation;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRole {
  CONSULTANT("consultant"),
  GROUP_CHAT_CONSULTANT("group-chat-consultant"),
  TENANT_ADMIN("tenant-admin"),
  SINGLE_TENANT_ADMIN("single-tenant-admin"),

  RESTRICTED_AGENCY_ADMIN("restricted-agency-admin"),

  RESTRICTED_CONSULTANT_ADMIN("restricted-consultant-admin"),

  /**
   * The Keycloak service identity the other services authenticate as when they act on behalf of
   * nobody. Not an admin role — see {@code Authority.TECHNICAL_USER} for what it may actually do
   * here (reading a tenant, nothing else).
   */
  TECHNICAL("technical");

  private final String value;

  public static Optional<UserRole> getRoleByValue(String value) {
    return Arrays.stream(values()).filter(userRole -> userRole.value.equals(value)).findFirst();
  }
}
