package com.vi.tenantservice.api.exception;

/** A persisted tenant has unreadable settings, without exposing their contents. */
public class TenantSettingsUnavailableException extends RuntimeException {
  public TenantSettingsUnavailableException() {
    super("Current tenant account preferences are unavailable");
  }
}
