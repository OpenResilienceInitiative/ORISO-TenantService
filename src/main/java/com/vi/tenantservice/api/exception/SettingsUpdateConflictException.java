package com.vi.tenantservice.api.exception;

public class SettingsUpdateConflictException extends RuntimeException {

  public SettingsUpdateConflictException(long currentRevision, long submittedRevision) {
    super(
        "Stale chat recovery settings revision: currentRevision="
            + currentRevision
            + ", submittedRevision="
            + submittedRevision
            + ". Reload the current values and try again.");
  }

  public SettingsUpdateConflictException(Throwable cause) {
    super("Settings changed while saving. Reload the current values and try again.", cause);
  }
}
