package com.vi.tenantservice.config.security;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Exact task bindings supplement legacy service access during the reviewed migration. */
@Component("taskServiceIdentity")
public class TaskServiceIdentity {
  private static final Map<String, String> ROLES =
      Map.of(
          "CONFIG_WIZARD",
          "config-wizard",
          "INVITE_RESERVATIONS",
          "invitation-reservations",
          "NOTIFICATION_DISPATCH",
          "notification-dispatch",
          "SYSTEM_EMAIL_DELIVERY",
          "system-email-delivery",
          "MATRIX_AGENCY",
          "matrix-agency",
          "SMTP_SYNC",
          "smtp-sync",
          "RUNTIME_POLICY",
          "runtime-policy");
  private static final Set<String> FORBIDDEN =
      Set.of(
          "technical",
          "tenant-admin",
          "single-tenant-admin",
          "realm-admin",
          "manage-users",
          "impersonation",
          "cluster-admin",
          "agency-admin",
          "restricted-agency-admin",
          "user-admin",
          "topic-admin",
          "*",
          "wildcard");
  private final Environment environment;

  public TaskServiceIdentity(Environment environment) {
    this.environment = environment;
    String required = environment.getProperty("TASK_IDENTITY_REQUIRED_TASKS", "");
    for (String name : required.split(",")) {
      String task = name.trim();
      if (!task.isEmpty()
          && (!ROLES.containsKey(task)
              || configured(task, "CLIENT_ID").isBlank()
              || configured(task, "SERVICE_SUBJECT").isBlank()
              || environment.getProperty("TASK_IDENTITY_AUDIENCE", "").isBlank())) {
        throw new IllegalStateException("Required task identity is not configured: " + task);
      }
    }
    Set<String> clients = new HashSet<>();
    Set<String> subjects = new HashSet<>();
    for (String task : ROLES.keySet()) {
      String client = configured(task, "CLIENT_ID");
      String subject = configured(task, "SERVICE_SUBJECT");
      if (client.isBlank() != subject.isBlank())
        throw new IllegalStateException("Incomplete task identity binding: " + task);
      if (!client.isBlank() && (!clients.add(client) || !subjects.add(subject)))
        throw new IllegalStateException("Task identity bindings must be distinct");
    }
  }

  public boolean current(String task) {
    return allows(SecurityContextHolder.getContext().getAuthentication(), task);
  }

  public boolean allows(Authentication authentication, String task) {
    String role = ROLES.get(task);
    if (role == null
        || authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt jwt)) return false;
    String client = configured(task, "CLIENT_ID");
    String subject = configured(task, "SERVICE_SUBJECT");
    String audience = environment.getProperty("TASK_IDENTITY_AUDIENCE", "").trim();
    if (client.isBlank()
        || subject.isBlank()
        || audience.isBlank()
        || !subject.equals(jwt.getSubject())
        || !client.equals(jwt.getClaimAsString("azp"))
        || !jwt.getAudience().contains(audience)
        || jwt.getExpiresAt() == null
        || !jwt.getExpiresAt().isAfter(Instant.now())) return false;
    Object realm = jwt.getClaims().get("realm_access");
    if (!(realm instanceof Map<?, ?> access)
        || !(access.get("roles") instanceof Collection<?> roles)
        || !roles.contains(role)
        || roles.stream().anyMatch(FORBIDDEN::contains)) return false;
    Set<String> allowedRoles =
        switch (task) {
          case "NOTIFICATION_DISPATCH" ->
              Set.of("notification-dispatch", "notifications-technical");
          case "MATRIX_AGENCY" -> Set.of("matrix-agency", "matrix-agency-provision");
          default -> Set.of(role);
        };
    if (roles.stream().anyMatch(grant -> !allowedRoles.contains(grant))) return false;
    Object resources = jwt.getClaims().get("resource_access");
    return !(resources instanceof Map<?, ?> resourceAccess)
        || !resourceAccess.containsKey("realm-management");
  }

  public boolean allowsOperatorDpa(Authentication authentication, Long tenantId) {
    return allows(authentication, "CONFIG_WIZARD")
        && tenantId != null
        && tenantId
            .toString()
            .equals(environment.getProperty("IDENTITY_CONFIG_WIZARD_OPERATOR_DPA_TENANT_ID", "1"));
  }

  public static boolean hasTaskRole(Jwt jwt) {
    Object realm = jwt.getClaims().get("realm_access");
    if (!(realm instanceof Map)) {
      return false;
    }
    Object roles = ((Map<?, ?>) realm).get("roles");
    return roles instanceof Collection
        && ((Collection<?>) roles).stream().anyMatch(ROLES::containsValue);
  }

  public boolean isTaskToken(Jwt jwt) {
    if (hasTaskRole(jwt)) {
      return true;
    }
    return ROLES.keySet().stream()
        .anyMatch(
            task -> {
              String client = configured(task, "CLIENT_ID");
              String subject = configured(task, "SERVICE_SUBJECT");
              return (!client.isBlank() && client.equals(jwt.getClaimAsString("azp")))
                  || (!subject.isBlank() && subject.equals(jwt.getSubject()));
            });
  }

  private String configured(String task, String suffix) {
    return environment.getProperty("IDENTITY_" + task + "_" + suffix, "").trim();
  }
}
