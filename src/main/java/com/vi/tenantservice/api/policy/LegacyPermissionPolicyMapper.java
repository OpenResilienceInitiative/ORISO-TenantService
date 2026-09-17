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
    return complete(Map.of(), allowed, enforced);
  }

  /**
   * Returns a policy for every {@link PermissionFeature}: the stored entry where one exists, else
   * the split-out feature's transition fallback (#250), else the legacy toggle mapping (feature
   * default SUGGESTED/on). A stored list is never taken at face value (#254): a list that carries
   * only some features must still yield a platform rule for all of them on every read.
   *
   * <p>{@code allowed}/{@code enforced} accept the settings shape or the API toggle DTO; both carry
   * the same legacy toggle keys.
   */
  public static Map<String, PolicyValue<Boolean>> complete(
      Map<String, PolicyValue<Boolean>> stored, Object allowed, Object enforced) {
    Map<String, Boolean> allowedValues = asMap(allowed);
    Map<String, Boolean> enforcedValues = asMap(enforced);
    Map<String, PolicyValue<Boolean>> policies = new LinkedHashMap<>();
    if (stored != null && !stored.isEmpty()) {
      // stored entries win, but only for registry features: a non-registry key (e.g. one left by an
      // older schema) is never a canonical policy and must not be served (#231)
      PermissionFeature.withTransitionFallbacks(stored)
          .forEach(
              (key, policy) -> {
                if (policy != null && PermissionFeature.byApiKey(key).isPresent()) {
                  policies.put(key, policy);
                }
              });
    }
    for (PermissionFeature feature : PermissionFeature.values()) {
      if (policies.get(feature.apiKey()) != null) {
        continue;
      }
      Boolean legacyAllowed = legacyValue(allowedValues, feature);
      Boolean legacyEnforced = legacyValue(enforcedValues, feature);
      policies.put(
          feature.apiKey(),
          fromLegacy(legacyAllowed, legacyEnforced, !Boolean.FALSE.equals(legacyAllowed)));
    }
    return Map.copyOf(policies);
  }

  /** A toggle never stored for a split-out feature (#250) reads the toggle it was split from. */
  private static Boolean legacyValue(Map<String, Boolean> values, PermissionFeature feature) {
    String legacyKey = feature.legacyToggleKey();
    if (legacyKey == null) {
      return null;
    }
    Boolean value = values.get(legacyKey);
    if (value != null) {
      return value;
    }
    return feature
        .transitionFallback()
        .map(PermissionFeature::legacyToggleKey)
        .map(values::get)
        .orElse(null);
  }

  private static Map<String, Boolean> asMap(Object toggles) {
    if (toggles == null) {
      return Map.of();
    }
    return new ObjectMapper().convertValue(toggles, new TypeReference<Map<String, Boolean>>() {});
  }
}
