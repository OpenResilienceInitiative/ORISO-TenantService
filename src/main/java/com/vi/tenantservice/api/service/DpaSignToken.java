package com.vi.tenantservice.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Secure single-use token for the public DPA sign link. The raw 256-bit token is what travels in
 * the link and is returned to the caller exactly once; only its SHA-256 hash is persisted, so a
 * database read cannot replay the link.
 */
final class DpaSignToken {

  private static final SecureRandom RANDOM = new SecureRandom();

  private DpaSignToken() {}

  /** A 256-bit URL-safe random token. Only this raw value goes in the link; it is never stored. */
  static String generate() {
    var bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** SHA-256 hex of the raw token — what is persisted and looked up by. */
  static String hash(String rawToken) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
