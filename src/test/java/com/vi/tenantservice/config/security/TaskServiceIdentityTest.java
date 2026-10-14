package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TaskServiceIdentityTest {
  private MockEnvironment bindings() {
    return new MockEnvironment()
        .withProperty("TASK_IDENTITY_AUDIENCE", "receiver")
        .withProperty("IDENTITY_CONFIG_WIZARD_CLIENT_ID", "backend-config-wizard")
        .withProperty("IDENTITY_CONFIG_WIZARD_SERVICE_SUBJECT", "wizard-subject");
  }

  @Test
  void requiredMissingAndDuplicatedBindingsFailStartup() {
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    new MockEnvironment()
                        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    bindings()
                        .withProperty(
                            "IDENTITY_NOTIFICATION_DISPATCH_CLIENT_ID", "backend-config-wizard")
                        .withProperty(
                            "IDENTITY_NOTIFICATION_DISPATCH_SERVICE_SUBJECT", "dispatch-subject")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    bindings()
                        .withProperty(
                            "IDENTITY_NOTIFICATION_DISPATCH_CLIENT_ID",
                            "backend-notification-dispatch")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void boundTaskCannotInheritHumanOrNativeAdministration() {
    var identity = new TaskServiceIdentity(bindings());
    for (String forbidden :
        List.of(
            "technical",
            "tenant-admin",
            "agency-admin",
            "realm-admin",
            "manage-users",
            "impersonation",
            "*")) {
      var jwt = token(List.of("config-wizard", forbidden));
      assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
          .isFalse();
      assertThat(identity.isTaskToken(jwt)).isTrue();
    }
    var jwt = token(List.of("config-wizard"));
    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
        .isTrue();
    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "INVITE_RESERVATIONS"))
        .isFalse();
  }

  private Jwt token(List<String> roles) {
    return Jwt.withTokenValue("fixture")
        .header("alg", "none")
        .subject("wizard-subject")
        .claim("azp", "backend-config-wizard")
        .audience(List.of("receiver"))
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("realm_access", Map.of("roles", roles))
        .build();
  }
}
