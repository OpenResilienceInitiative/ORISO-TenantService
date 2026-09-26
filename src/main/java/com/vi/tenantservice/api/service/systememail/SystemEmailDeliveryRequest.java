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
    SUPERVISOR_REMOVED,
    NEW_ENQUIRY,
    DIRECT_ENQUIRY,
    ENQUIRY_ASSIGNED,
    DAILY_ENQUIRY_DIGEST,
    NEW_MESSAGE,
    CONTACT_SHEET,
    SELF_HELP_APPOINTMENT_CONFIRMED,
    SELF_HELP_APPOINTMENT_RESCHEDULED,
    SELF_HELP_APPOINTMENT_CANCELLED,
    SELF_HELP_APPOINTMENT_REMINDER,
    HANDOVER_REQUESTED,
    HANDOVER_CONFIRMED,
    FREE_TEXT_NOTICE
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
