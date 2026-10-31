package com.vi.tenantservice.config.security;

import java.util.Set;

/** One credential per actor; purposes remain separate receiver operation contracts. */
enum TaskServiceActor {
  SETUP(
      Set.of("CONFIG_WIZARD", "INVITE_RESERVATIONS", "CONSULTANT_IMPORT", "SMTP_SYNC"),
      Set.of("config-wizard", "invitation-reservations", "consultant-import", "smtp-sync"),
      Set.of(
          "tenantservice",
          "agencyservice",
          "consultingtypeservice",
          "userservice",
          "oriso-task-commands")),
  ACCOUNT_CREATE(
      Set.of("ACCOUNT_PROVISIONING"),
      Set.of("account-provisioning", "account-read"),
      Set.of("userservice", "oriso-account-lifecycle")),
  ACCOUNT_ADMIN(
      Set.of("ACCOUNT_MAINTENANCE", "OTP", "SESSION_EXCHANGE"),
      Set.of("account-maintenance", "account-read", "otp-config-admin", "session-exchange"),
      Set.of("userservice", "oriso-account-lifecycle", "oriso-otp")),
  MAIL(
      Set.of("SYSTEM_EMAIL_DELIVERY", "NOTIFICATION_DISPATCH"),
      Set.of("system-email-delivery", "notification-dispatch", "notifications-technical"),
      Set.of("tenantservice", "agencyservice", "consultingtypeservice", "userservice")),
  RUNTIME(
      Set.of("RUNTIME_POLICY", "MATRIX_AGENCY", "APPOINTMENT_SYNC", "APPOINTMENT_CLEANUP"),
      Set.of(
          "runtime-policy",
          "matrix-agency",
          "matrix-agency-provision",
          "appointment-sync",
          "appointment-participant-cleanup"),
      Set.of("tenantservice", "agencyservice", "appointmentservice"));

  private final Set<String> purposes;
  private final Set<String> roles;
  private final Set<String> audiences;

  TaskServiceActor(Set<String> purposes, Set<String> roles, Set<String> audiences) {
    this.purposes = purposes;
    this.roles = roles;
    this.audiences = audiences;
  }

  Set<String> roles() {
    return roles;
  }

  Set<String> audiences() {
    return audiences;
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
