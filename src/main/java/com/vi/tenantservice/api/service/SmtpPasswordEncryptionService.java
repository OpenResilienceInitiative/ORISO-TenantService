package com.vi.tenantservice.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encrypts tenant SMTP passwords at rest using AES-256-GCM (#183). Port of the
 * ConsultingTypeService service of the same name so both services share the {@code ENC:} wire
 * format and the {@code SETTINGS_SMTP_PASSWORD_ENCRYPTION_SECRET} configuration convention.
 */
@Service
@Slf4j
public class SmtpPasswordEncryptionService {

  static final String ENCRYPTED_PREFIX = "ENC:";
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final SecretKey secretKey;

  public SmtpPasswordEncryptionService(
      @Value("${settings.smtp.password.encryption.secret:}") String encryptionSecret) {
    this.secretKey = deriveKey(encryptionSecret);
    if (secretKey == null) {
      log.warn(
          "SMTP password encryption is disabled: settings.smtp.password.encryption.secret is not"
              + " configured");
    }
  }

  public boolean isEnabled() {
    return secretKey != null;
  }

  public String encrypt(String plaintext) {
    if (StringUtils.isEmpty(plaintext) || secretKey == null || isEncrypted(plaintext)) {
      return plaintext;
    }
    return doEncrypt(plaintext);
  }

  /**
   * Encrypts a password supplied through the API. Unlike {@link #encrypt(String)}, the input is
   * always treated as plaintext — even when it carries the reserved {@code ENC:} prefix — so a
   * crafted value cannot bypass encryption and linger unencrypted in the settings JSON.
   */
  public String encryptNewPassword(String plaintext) {
    if (StringUtils.isEmpty(plaintext) || secretKey == null) {
      return plaintext;
    }
    return doEncrypt(plaintext);
  }

  private String doEncrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      byte[] payload = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);

      return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encrypt SMTP password", exception);
    }
  }

  public String decrypt(String value) {
    if (StringUtils.isEmpty(value) || secretKey == null || !isEncrypted(value)) {
      return value;
    }
    try {
      byte[] payload = Base64.getDecoder().decode(value.substring(ENCRYPTED_PREFIX.length()));
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] ciphertext = new byte[payload.length - GCM_IV_LENGTH];
      System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
      System.arraycopy(payload, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to decrypt SMTP password", exception);
    }
  }

  /**
   * True only for values that structurally are our ciphertext: {@code ENC:} prefix plus a Base64
   * payload long enough to hold IV and tag. A bare prefix with junk behind it is NOT considered
   * encrypted, so the migration re-encrypts it instead of skipping it.
   */
  public boolean isEncrypted(String value) {
    if (value == null || !value.startsWith(ENCRYPTED_PREFIX)) {
      return false;
    }
    try {
      byte[] payload = Base64.getDecoder().decode(value.substring(ENCRYPTED_PREFIX.length()));
      return payload.length > GCM_IV_LENGTH;
    } catch (IllegalArgumentException invalidBase64) {
      return false;
    }
  }

  private static SecretKey deriveKey(String encryptionSecret) {
    if (StringUtils.isBlank(encryptionSecret)) {
      return null;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptionSecret);
      if (decoded.length == 32) {
        return new SecretKeySpec(decoded, "AES");
      }
    } catch (IllegalArgumentException ignored) {
      // fall through to SHA-256 derivation
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return new SecretKeySpec(
          digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8)), "AES");
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to derive SMTP password encryption key", exception);
    }
  }
}
