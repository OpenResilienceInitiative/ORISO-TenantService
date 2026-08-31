package com.vi.tenantservice.api.exception;

public class SettingsUpdateConflictException extends RuntimeException {

  public SettingsUpdateConflictException(Throwable cause) {
    super("Settings changed while saving. Reload the current values and try again.", cause);
  }
}
