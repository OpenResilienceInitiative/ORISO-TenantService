package com.vi.tenantservice.api.service.translation;

import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Encrypts the platform-global machine-translation provider API keys at rest.
 *
 * <p>These keys sit in the same {@code tenant_admin_controls.controls} JSON blob as the other
 * platform-global admin settings, and they are protected with the same key and the same {@code
 * ENC:} AES-256-GCM format as the tenant SMTP password (#183). One blob, one secret — a second
 * encryption secret for the same document would only add an operational failure mode.
 *
 * <p>This type exists so that call sites read as what they are. Delegating to {@link
 * SmtpPasswordEncryptionService} would otherwise put "smtp password" in the middle of translation
 * code and invite the next reader to conclude that the wrong thing is being stored.
 */
@Service
@RequiredArgsConstructor
public class TranslationApiKeyEncryptionService {

  private final @NonNull SmtpPasswordEncryptionService settingsSecretCipher;

  /** True when an encryption secret is configured; otherwise every method is a pass-through. */
  public boolean isEnabled() {
    return settingsSecretCipher.isEnabled();
  }

  /**
   * Encrypts a key that arrived through the API. The input is always treated as plaintext, even
   * when it carries the reserved {@code ENC:} prefix, so a crafted value cannot bypass encryption
   * and linger unencrypted in the settings JSON.
   */
  public String encryptNewApiKey(String apiKey) {
    return settingsSecretCipher.encryptNewPassword(apiKey);
  }

  /** Encrypts a stored value unless it already is our ciphertext. Used by the migration. */
  public String encryptStoredApiKey(String apiKey) {
    return settingsSecretCipher.encrypt(apiKey);
  }

  /** Returns the usable key. Values without the {@code ENC:} prefix are passed through. */
  public String decrypt(String storedValue) {
    return settingsSecretCipher.decrypt(storedValue);
  }

  /** True only for values that authenticate under the configured key. */
  public boolean isEncrypted(String storedValue) {
    return settingsSecretCipher.isEncrypted(storedValue);
  }
}
