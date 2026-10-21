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
          "CONSULTANT_IMPORT",
          "consultant-import",
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
  private final boolean grouped;
  private final Set<String> requiredTasks = new HashSet<>();

  public TaskServiceIdentity(Environment environment) {
    this.environment = environment;
    String model = environment.getProperty("TASK_IDENTITY_MODEL");
    if (model != null && !"FIVE_ACTORS".equals(model)) {
      throw new IllegalStateException("Unknown task identity model: " + model);
    }
    this.grouped = model != null;
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
      if (!task.isEmpty()) {
        requiredTasks.add(task);
      }
    }
    Set<String> clients = new HashSet<>();
    Set<String> subjects = new HashSet<>();
    Set<String> bindingNames = new HashSet<>();
    if (grouped) {
      for (TaskServiceActor actor : TaskServiceActor.values()) {
        bindingNames.add(actor.name());
      }
    } else {
      bindingNames.addAll(ROLES.keySet());
    }
    for (String binding : bindingNames) {
      String client = bindingValue(binding, "CLIENT_ID");
      String subject = bindingValue(binding, "SERVICE_SUBJECT");
      if (client.isBlank() != subject.isBlank())
        throw new IllegalStateException("Incomplete task identity binding: " + binding);
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
        || (grouped && !requiredTasks.contains(task))
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
    Set<String> allowedRoles = allowedRoles(task, role);
    if (roles.stream().anyMatch(grant -> !allowedRoles.contains(grant))) return false;
    if (grouped && !new HashSet<>(roles).equals(allowedRoles)) return false;
    Object resources = jwt.getClaims().get("resource_access");
    if (grouped) {
      return resources == null
          || (resources instanceof Map<?, ?> resourceAccess && resourceAccess.isEmpty());
    }
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
    if (grouped) {
      for (TaskServiceActor actor : TaskServiceActor.values()) {
        String client = bindingValue(actor.name(), "CLIENT_ID");
        String subject = bindingValue(actor.name(), "SERVICE_SUBJECT");
        if ((!client.isBlank() && client.equals(jwt.getClaimAsString("azp")))
            || (!subject.isBlank() && subject.equals(jwt.getSubject()))) {
          return true;
        }
      }
      return false;
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

  private Set<String> allowedRoles(String task, String role) {
    if (grouped) {
      return TaskServiceActor.forPurpose(task).roles();
    }
    if ("NOTIFICATION_DISPATCH".equals(task)) {
      return Set.of("notification-dispatch", "notifications-technical");
    }
    if ("MATRIX_AGENCY".equals(task)) {
      return Set.of("matrix-agency", "matrix-agency-provision");
    }
    return Set.of(role);
  }

  private String configured(String task, String suffix) {
    return bindingValue(grouped ? TaskServiceActor.forPurpose(task).name() : task, suffix);
  }

  private String bindingValue(String binding, String suffix) {
    return environment.getProperty("IDENTITY_" + binding + "_" + suffix, "").trim();
  }
}
