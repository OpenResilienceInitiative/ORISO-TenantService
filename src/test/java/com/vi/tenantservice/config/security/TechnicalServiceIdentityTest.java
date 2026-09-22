package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The technical service identity is recognised by its token claims - Keycloak subject plus realm
 * role {@code technical} - never by the username (ORISO-Helm#367).
 */
class TechnicalServiceIdentityTest {

  private static final String SERVICE_SUBJECT = "0b7c6d52-9f0e-4c1e-8a57-6d1f1e0f2a11";

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  void missingSubject_Should_failStartup_andNameTheVariable(String subject) {
    assertThatThrownBy(() -> new TechnicalServiceIdentity(subject))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TECHNICAL_SERVICE_SUBJECT");
  }

  @Test
  void serviceSubjectWithTechnicalRole_Should_beTheTechnicalIdentity() {
    authenticate(SERVICE_SUBJECT, "someone", List.of("technical"));

    assertThat(identity().isCurrentCaller()).isTrue();
  }

  @Test
  void serviceSubjectAlsoHoldingTenantAdmin_Should_stillBeTheTechnicalIdentity() {
    authenticate(SERVICE_SUBJECT, "technical", List.of("technical", "tenant-admin"));

    assertThat(identity().isCurrentCaller()).isTrue();
  }

  @Test
  void usernameTechnicalWithAnotherSubject_Should_notBeTheTechnicalIdentity() {
    authenticate("another-subject", "technical", List.of("technical", "tenant-admin"));

    assertThat(identity().isCurrentCaller()).isFalse();
  }

  @Test
  void aHumanAdminHoldingTheTechnicalRole_Should_notBeTheTechnicalIdentity() {
    // The Helm bootstrap gives the realm admin the technical role too; it stays a person.
    authenticate("realm-admin-subject", "realmadmin", List.of("technical", "tenant-admin"));

    assertThat(identity().isCurrentCaller()).isFalse();
  }

  @Test
  void serviceSubjectWithoutTechnicalRole_Should_notBeTheTechnicalIdentity() {
    authenticate(SERVICE_SUBJECT, "technical", List.of("tenant-admin"));

    assertThat(identity().isCurrentCaller()).isFalse();
  }

  @Test
  void noAuthentication_Should_notBeTheTechnicalIdentity() {
    assertThat(identity().isCurrentCaller()).isFalse();
  }

  private static TechnicalServiceIdentity identity() {
    return new TechnicalServiceIdentity(SERVICE_SUBJECT);
  }

  private static void authenticate(String subject, String username, List<String> roles) {
    Jwt jwt =
        new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", subject, "username", username, "realm_access", Map.of("roles", roles)));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
