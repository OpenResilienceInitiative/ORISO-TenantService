package com.vi.tenantservice.api.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Lenient storage-read boundary for a stored {@code Map<String, PolicyValue<Boolean>>}.
 *
 * <p>The permission-policy maps live inside version-shared JSON blobs ({@code
 * tenant_admin_controls.controls}, {@code tenant_permission_policy.policies}) that are read on the
 * public bootstrap path. A blob written by a newer or older build may contain entries this build
 * cannot fully understand; failing the whole read would turn version skew into an outage (observed
 * on Pre-Dev 2026-08-18).
 *
 * <p>Deserialization is therefore separated from the domain invariant: {@link PolicyValue} stays
 * strict for deliberate construction, while this reader keeps only entries that are fully
 * intelligible to this build and drops everything else with a WARN log. An entry survives iff
 *
 * <ul>
 *   <li>its key is a {@link PermissionFeature} known to this build (an alien key would 500 the
 *       bootstrap later, in {@code TenantPermissionPolicyService.toDomain}),
 *   <li>it is a JSON object whose {@code value} is a JSON boolean,
 *   <li>its {@code mode} is a known {@link PermissionPolicyMode} constant.
 * </ul>
 *
 * <p>Unknown extra fields inside an entry (e.g. the API's read-only {@code inherited}) are ignored,
 * mirroring the one-way tolerance for unknown fields at the blob root. A dropped entry is never
 * defaulted: the service behaves as if the alien build's entry were invisible - no enforcement is
 * fabricated in either direction, and the legacy toggle clamps in the same blob keep applying.
 * Dropped entries do not survive the next rewrite of the blob, exactly like unknown root fields.
 * Syntactically broken JSON is NOT tolerated here - corruption should stay loud; this boundary is
 * about version skew only.
 */
@Slf4j
public class LenientPolicyValueMapDeserializer
    extends JsonDeserializer<Map<String, PolicyValue<Boolean>>> {

  @Override
  public Map<String, PolicyValue<Boolean>> deserialize(
      JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode node = parser.readValueAsTree();
    return readIntelligibleEntries(node);
  }

  /**
   * Reads the intelligible entries of a stored policy map. Never returns {@code null}; an
   * unintelligible whole map yields an empty map, which downstream code treats like an absent one.
   */
  public static Map<String, PolicyValue<Boolean>> readIntelligibleEntries(JsonNode mapNode) {
    if (mapNode == null || !mapNode.isObject()) {
      if (mapNode != null && !mapNode.isNull()) {
        log.warn(
            "Dropping stored permission-policy map: expected a JSON object but found {}",
            mapNode.getNodeType());
      }
      return Map.of();
    }
    Map<String, PolicyValue<Boolean>> policies = new LinkedHashMap<>();
    mapNode
        .fields()
        .forEachRemaining(
            entry ->
                readEntry(entry.getKey(), entry.getValue())
                    .ifPresent(policy -> policies.put(entry.getKey(), policy)));
    return Map.copyOf(policies);
  }

  private static Optional<PolicyValue<Boolean>> readEntry(String feature, JsonNode entry) {
    if (PermissionFeature.byApiKey(feature).isEmpty()) {
      return drop(feature, "the feature is unknown to this build");
    }
    if (entry == null || !entry.isObject()) {
      return drop(feature, "the entry is not a JSON object");
    }
    JsonNode value = entry.get("value");
    if (value == null || !value.isBoolean()) {
      return drop(feature, "'value' is missing or not a boolean");
    }
    JsonNode mode = entry.get("mode");
    if (mode == null || !mode.isTextual()) {
      return drop(feature, "'mode' is missing or not textual");
    }
    PermissionPolicyMode parsedMode;
    try {
      parsedMode = PermissionPolicyMode.valueOf(mode.asText());
    } catch (IllegalArgumentException unknownMode) {
      return drop(feature, "'mode' is not a known policy mode");
    }
    return Optional.of(new PolicyValue<>(value.booleanValue(), parsedMode));
  }

  private static Optional<PolicyValue<Boolean>> drop(String feature, String reason) {
    log.warn(
        "Dropping stored permission-policy entry '{}' written by another build: {}. "
            + "The entry enforces nothing; legacy toggle clamps still apply.",
        feature,
        reason);
    return Optional.empty();
  }
}
