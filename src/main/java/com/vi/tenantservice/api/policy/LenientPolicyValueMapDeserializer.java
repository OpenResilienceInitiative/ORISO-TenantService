package com.vi.tenantservice.api.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a {@code feature -> PolicyValue<Boolean>} map one entry at a time, dropping entries this
 * build cannot understand instead of failing the whole document.
 *
 * <p>The map lives inside the shared {@code tenant_admin_controls.controls} blob, which is read on
 * the bootstrap path. A blob written by a newer build — an unknown feature key, a policy mode that
 * did not exist yet, a value whose type changed — would otherwise abort the entire parse, and the
 * caller turns that into HTTP 500 on the first request of every session. Pre-Dev lost bootstrap
 * this way on 2026-08-18.
 *
 * <p>The tolerance is deliberately one-way and entry-scoped. An unreadable entry costs that one
 * feature its stored policy, which then falls back to the platform default; it never costs the
 * tenant its other settings. A syntactically broken document is a different failure and still
 * throws, because there is no partial result to salvage.
 */
public class LenientPolicyValueMapDeserializer
    extends JsonDeserializer<Map<String, PolicyValue<Boolean>>> {

  private static final Logger log =
      LoggerFactory.getLogger(LenientPolicyValueMapDeserializer.class);

  private static final List<String> MODES =
      Arrays.stream(PermissionPolicyMode.values()).map(Enum::name).toList();

  /** What an unreadable entry of a known feature reads as until it is saved again. */
  public static final PolicyValue<Boolean> UNREADABLE_ENTRY_POLICY =
      new PolicyValue<>(false, PermissionPolicyMode.ENFORCED);

  /**
   * The stored entries of a {@code permissionPolicies} object that this build does not read as
   * stored — unknown features and entries it replaces with {@link #UNREADABLE_ENTRY_POLICY} — keyed
   * by feature, as raw JSON. A writer puts them back unchanged so that an unrelated save does not
   * destroy what a newer build (or a later repair) can still read (#386). Never serve these to a
   * client.
   */
  public static Map<String, JsonNode> unreadableEntries(JsonNode policies) {
    Map<String, JsonNode> unreadable = new LinkedHashMap<>();
    if (policies == null || !policies.isObject()) {
      return unreadable;
    }
    Iterator<Map.Entry<String, JsonNode>> entries = policies.fields();
    while (entries.hasNext()) {
      Map.Entry<String, JsonNode> entry = entries.next();
      if (problemWith(entry.getKey(), entry.getValue()) != null) {
        unreadable.put(entry.getKey(), entry.getValue().deepCopy());
      }
    }
    return unreadable;
  }

  @Override
  public Map<String, PolicyValue<Boolean>> deserialize(
      JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode root = parser.getCodec().readTree(parser);
    if (root == null || root.isNull()) {
      return null;
    }
    if (!root.isObject()) {
      log.warn(
          "Ignoring stored permission policies: expected a JSON object but found {}.",
          root.getNodeType());
      return new LinkedHashMap<>();
    }

    Map<String, PolicyValue<Boolean>> policies = new LinkedHashMap<>();
    Iterator<Map.Entry<String, JsonNode>> entries = root.fields();
    while (entries.hasNext()) {
      Map.Entry<String, JsonNode> entry = entries.next();
      readEntry(entry.getKey(), entry.getValue())
          .ifPresent(policy -> policies.put(entry.getKey(), policy));
    }
    return policies;
  }

  private Optional<PolicyValue<Boolean>> readEntry(String feature, JsonNode node) {
    String problem = problemWith(feature, node);
    if (problem == null) {
      return Optional.of(
          new PolicyValue<>(
              node.get("value").booleanValue(),
              PermissionPolicyMode.valueOf(node.get("mode").asText())));
    }
    return PermissionFeature.byApiKey(feature).isEmpty()
        ? drop(feature, problem)
        : restrict(feature, problem);
  }

  /** Why this build cannot read the entry as stored, or {@code null} when it can. */
  private static String problemWith(String feature, JsonNode node) {
    if (PermissionFeature.byApiKey(feature).isEmpty()) {
      return "it is not a known feature in this build";
    }
    if (node == null || !node.isObject()) {
      return "its policy is not a JSON object";
    }
    JsonNode valueNode = node.get("value");
    if (valueNode == null || !valueNode.isBoolean()) {
      return "its value is not a boolean";
    }
    JsonNode modeNode = node.get("mode");
    if (modeNode == null || !modeNode.isTextual()) {
      return "its mode is missing or not a string";
    }
    if (!MODES.contains(modeNode.asText())) {
      return "its mode '" + modeNode.asText() + "' is unknown to this build";
    }
    return null;
  }

  private Optional<PolicyValue<Boolean>> drop(String feature, String reason) {
    log.warn(
        "Dropping stored permission policy for '{}' because {}. The platform default applies.",
        feature,
        reason);
    return Optional.empty();
  }

  /**
   * A known feature whose stored policy cannot be read gets the most restrictive policy (off,
   * enforced) instead of vanishing: a missing entry would be refilled from the legacy toggles
   * (everything on, suggested) and could silently lift a platform restriction that was stored as
   * ENFORCED. The tenant keeps its bootstrap; the feature stays off until the policy is saved
   * again.
   */
  private Optional<PolicyValue<Boolean>> restrict(String feature, String reason) {
    log.warn(
        "Stored permission policy for '{}' is unreadable because {}. Applying the restrictive"
            + " default (off, enforced) until it is saved again.",
        feature,
        reason);
    return Optional.of(UNREADABLE_ENTRY_POLICY);
  }
}
