package com.vi.tenantservice.api.service.translation;

/**
 * Typed machine-translation error codes exposed to the Admin UI so it can show a clear, actionable
 * message instead of a generic failure.
 */
public enum TranslationErrorCode {

  /** No API key is configured for the requested (or any) provider. Mapped to HTTP 409. */
  TRANSLATION_NOT_CONFIGURED,

  /** The provider rejected the configured API key (401/403). Mapped to HTTP 502. */
  TRANSLATION_KEY_INVALID,

  /** The provider account has no credit left (402). Mapped to HTTP 502. */
  TRANSLATION_NO_CREDIT,

  /** The provider rate limit was hit (429). Mapped to HTTP 502. */
  TRANSLATION_RATE_LIMITED,

  /** Provider timeout, connection failure or 5xx. Mapped to HTTP 502. */
  TRANSLATION_PROVIDER_UNAVAILABLE,

  /**
   * The key was not saved because the server has no encryption secret configured, and keys are
   * never stored in plain text (#346). Nothing the admin can fix in the UI; the operator has to set
   * {@code SETTINGS_SMTP_PASSWORD_ENCRYPTION_SECRET}. Mapped to HTTP 409.
   */
  TRANSLATION_ENCRYPTION_NOT_CONFIGURED
}
