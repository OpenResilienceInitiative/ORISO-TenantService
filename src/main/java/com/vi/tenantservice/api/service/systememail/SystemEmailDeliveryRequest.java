package com.vi.tenantservice.api.service.systememail;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SystemEmailDeliveryRequest(
    @NotNull Purpose purpose,
    @NotBlank @Email @Size(max = 254) @Pattern(regexp = "[^\\r\\n,;]+") String recipient,
    @NotBlank @Size(max = 256) @Pattern(regexp = "[^\\r\\n]+") String subject,
    @NotBlank @Size(max = 262144) String html,
    @NotBlank @Size(max = 131072) String text,
    @NotNull java.util.UUID correlationId) {
  public enum Purpose {
    EMAIL_ADDRESS_CHANGED,
    SUPERVISOR_ADDED,
    SUPERVISOR_REMOVED
  }

  @JsonAnySetter
  public void rejectUnknownField(String field, Object value) {
    throw new IllegalArgumentException("Unsupported delivery field");
  }

  @Override
  public String toString() {
    return "SystemEmailDeliveryRequest[redacted]";
  }
}
