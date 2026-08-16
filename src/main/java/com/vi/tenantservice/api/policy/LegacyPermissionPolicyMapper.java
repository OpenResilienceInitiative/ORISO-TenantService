package com.vi.tenantservice.api.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegacyPermissionPolicyMapper {

  private LegacyPermissionPolicyMapper() {}

  public static PolicyValue<Boolean> fromLegacy(
      Boolean allowed, Boolean enforced, boolean currentValue) {
    if (Boolean.TRUE.equals(enforced)) {
      return new PolicyValue<>(true, PermissionPolicyMode.ENFORCED);
    }
    if (Boolean.FALSE.equals(allowed)) {
      return new PolicyValue<>(false, PermissionPolicyMode.ENFORCED);
    }
    return new PolicyValue<>(currentValue, PermissionPolicyMode.SUGGESTED);
  }

  public static Map<String, PolicyValue<Boolean>> fromLegacyMaps(
      TenantAdminAllowedPermissionTogglesSettings allowed,
      TenantAdminAllowedPermissionTogglesSettings enforced) {
    Map<String, Boolean> allowedValues = asMap(allowed);
    Map<String, Boolean> enforcedValues = asMap(enforced);
    Map<String, PolicyValue<Boolean>> policies = new LinkedHashMap<>();

    for (PermissionFeature feature : PermissionFeature.values()) {
      String legacyKey = feature.legacyToggleKey();
      Boolean legacyAllowed = legacyKey == null ? null : allowedValues.get(legacyKey);
      Boolean legacyEnforced = legacyKey == null ? null : enforcedValues.get(legacyKey);
      policies.put(
          feature.apiKey(),
          fromLegacy(legacyAllowed, legacyEnforced, !Boolean.FALSE.equals(legacyAllowed)));
    }
    return Map.copyOf(policies);
  }

  private static Map<String, Boolean> asMap(TenantAdminAllowedPermissionTogglesSettings settings) {
    if (settings == null) {
      return Map.of();
    }
    return new ObjectMapper().convertValue(settings, new TypeReference<Map<String, Boolean>>() {});
  }
}
