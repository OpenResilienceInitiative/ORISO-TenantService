package com.vi.tenantservice.api.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    if (PermissionFeature.byApiKey(feature).isEmpty()) {
      return drop(feature, "it is not a known feature in this build");
    }
    if (node == null || !node.isObject()) {
      return drop(feature, "its policy is not a JSON object");
    }

    JsonNode valueNode = node.get("value");
    if (valueNode == null || !valueNode.isBoolean()) {
      return drop(feature, "its value is not a boolean");
    }

    JsonNode modeNode = node.get("mode");
    if (modeNode == null || !modeNode.isTextual()) {
      return drop(feature, "its mode is missing");
    }
    PermissionPolicyMode mode;
    try {
      mode = PermissionPolicyMode.valueOf(modeNode.asText());
    } catch (IllegalArgumentException unknownMode) {
      return drop(feature, "its mode '" + modeNode.asText() + "' is unknown to this build");
    }

    return Optional.of(new PolicyValue<>(valueNode.booleanValue(), mode));
  }

  private Optional<PolicyValue<Boolean>> drop(String feature, String reason) {
    log.warn(
        "Dropping stored permission policy for '{}' because {}. The platform default applies.",
        feature,
        reason);
    return Optional.empty();
  }
}
