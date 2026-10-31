package com.vi.tenantservice.config.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;

/** The C wire format; domain owners must independently authorize every use. */
public final class TaskContextProof {
  private static final ObjectMapper JSON =
      new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

  private TaskContextProof() {}

  public static boolean grouped(Environment environment) {
    return "FIVE_ACTORS".equals(environment.getProperty("TASK_IDENTITY_MODEL"));
  }

  public static byte[] key(Environment environment) {
    byte[] key = Base64.getDecoder().decode(environment.getProperty("ORISO_TASK_CONTEXT_KEY", ""));
    if (key.length < 32) throw new IllegalArgumentException("Task context key is not configured");
    return key;
  }

  public static String sign(byte[] key, Object claims) throws Exception {
    String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(canonical(claims));
    return payload
        + "."
        + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mac(key, "proof\n", payload.getBytes(StandardCharsets.US_ASCII)));
  }

  public static JsonNode verify(byte[] key, String proof) throws Exception {
    if (proof == null || proof.length() > 8192) throw new IllegalArgumentException();
    String[] parts = proof.split("\\.", -1);
    if (parts.length != 2
        || !parts[0].matches("[A-Za-z0-9_-]+")
        || !parts[1].matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException();
    byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
    if (!MessageDigest.isEqual(
        mac(key, "proof\n", parts[0].getBytes(StandardCharsets.US_ASCII)),
        Base64.getUrlDecoder().decode(parts[1]))) throw new IllegalArgumentException();
    JsonNode claims = JSON.readTree(payload);
    if (!claims.isObject() || !MessageDigest.isEqual(payload, canonical(claims)))
      throw new IllegalArgumentException();
    return claims;
  }

  public static String digest(byte[] key, Object payload) throws Exception {
    return HexFormat.of().formatHex(mac(key, "payload\n", canonical(payload)));
  }

  public static long integer(JsonNode value) {
    if (!value.isIntegralNumber() || !value.canConvertToLong())
      throw new IllegalArgumentException();
    return value.longValue();
  }

  public static byte[] canonical(Object value) throws Exception {
    var output = new StringBuilder();
    append(value instanceof JsonNode node ? node : JSON.valueToTree(value), output);
    return output.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void append(JsonNode value, StringBuilder output) throws Exception {
    if (value.isObject()) {
      var names = new ArrayList<String>();
      value.fieldNames().forEachRemaining(names::add);
      names.sort(String::compareTo);
      output.append('{');
      boolean comma = false;
      for (String name : names) {
        if (value.get(name).isNull()) continue;
        if (comma) output.append(',');
        output.append(JSON.writeValueAsString(name)).append(':');
        append(value.get(name), output);
        comma = true;
      }
      output.append('}');
    } else if (value.isArray()) {
      output.append('[');
      for (int index = 0; index < value.size(); index++) {
        if (index > 0) output.append(',');
        append(value.get(index), output);
      }
      output.append(']');
    } else if (value.isNumber()) {
      output.append(value.decimalValue().stripTrailingZeros().toPlainString());
    } else if (value.isTextual() || value.isBoolean() || value.isNull()) {
      output.append(JSON.writeValueAsString(value));
    } else throw new IllegalArgumentException("Unsupported semantic JSON value");
  }

  private static byte[] mac(byte[] key, String prefix, byte[] payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    mac.update(prefix.getBytes(StandardCharsets.US_ASCII));
    return mac.doFinal(payload);
  }
}
