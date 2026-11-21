package com.vi.tenantservice.api.exception;

public class ConsultingSettingsOutcomeUnknownException extends RuntimeException {
  public ConsultingSettingsOutcomeUnknownException(Throwable cause) {
    super("ConsultingTypeService did not confirm the consulting settings update", cause);
  }
}
