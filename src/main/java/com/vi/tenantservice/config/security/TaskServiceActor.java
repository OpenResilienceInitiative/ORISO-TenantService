package com.vi.tenantservice.config.security;

import java.util.Set;

/** One credential per actor; purposes remain separate receiver operation contracts. */
enum TaskServiceActor {
  SETUP(
      Set.of("CONFIG_WIZARD", "INVITE_RESERVATIONS", "CONSULTANT_IMPORT"),
      Set.of("config-wizard", "invitation-reservations", "consultant-import")),
  ACCOUNT_CREATE(Set.of("ACCOUNT_PROVISIONING"), Set.of("account-provisioning", "account-read")),
  ACCOUNT_ADMIN(
      Set.of("ACCOUNT_MAINTENANCE", "OTP", "SESSION_EXCHANGE"),
      Set.of("account-maintenance", "account-read", "otp-config-admin", "session-exchange")),
  MAIL(
      Set.of("SMTP_SYNC", "SYSTEM_EMAIL_DELIVERY", "NOTIFICATION_DISPATCH"),
      Set.of(
          "smtp-sync",
          "system-email-delivery",
          "notification-dispatch",
          "notifications-technical")),
  RUNTIME(
      Set.of("RUNTIME_POLICY", "MATRIX_AGENCY", "APPOINTMENT_SYNC", "APPOINTMENT_CLEANUP"),
      Set.of(
          "runtime-policy",
          "matrix-agency",
          "matrix-agency-provision",
          "appointment-sync",
          "appointment-participant-cleanup"));

  private final Set<String> purposes;
  private final Set<String> roles;

  TaskServiceActor(Set<String> purposes, Set<String> roles) {
    this.purposes = purposes;
    this.roles = roles;
  }

  Set<String> roles() {
    return roles;
  }

  static TaskServiceActor forPurpose(String purpose) {
    for (TaskServiceActor actor : values()) {
      if (actor.purposes.contains(purpose)) {
        return actor;
      }
    }
    return null;
  }
}
