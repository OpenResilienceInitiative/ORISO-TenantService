package com.vi.tenantservice.api.service.translation;

import lombok.Getter;

/**
 * Signals a machine-translation failure with a typed {@link TranslationErrorCode} so the controller
 * can map it to a structured 409/502 response the Admin UI understands.
 */
@Getter
public class TranslationException extends RuntimeException {

  private final TranslationErrorCode errorCode;
  private final transient String provider;

  public TranslationException(TranslationErrorCode errorCode, String provider, String message) {
    super(message);
    this.errorCode = errorCode;
    this.provider = provider;
  }
}
