package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SystemEmailServiceIdentityTest {
  private JwtAuthenticationToken token(String subject, String client, String role) {
    return new JwtAuthenticationToken(
        Jwt.withTokenValue("synthetic")
            .header("alg", "RS256")
            .subject(subject)
            .claim("azp", client)
            .claim("realm_access", Map.of("roles", List.of(role)))
            .build(),
        List.of());
  }

  @Test
  void acceptsOnlyPinnedTechnicalIdentity() {
    assertThat(
            new SystemEmailServiceIdentity("service-id", "app")
                .allows(token("service-id", "app", "technical")))
        .isTrue();
  }

  @Test
  void rejectsOrdinaryAndAdminRolesEvenWithCorrectPins() {
    var guard = new SystemEmailServiceIdentity("service-id", "app");
    for (String role : List.of("consultant", "tenant-admin", "single-tenant-admin"))
      assertThat(guard.allows(token("service-id", "app", role))).isFalse();
  }

  @Test
  void rejectsUnauthenticatedTokenEvenWithCorrectClaims() {
    var valid = token("service-id", "app", "technical");
    valid.setAuthenticated(false);
    assertThat(new SystemEmailServiceIdentity("service-id", "app").allows(valid)).isFalse();
  }

  @Test
  void rejectsWrongSubjectOrClientAndMissingPins() {
    var valid = token("service-id", "app", "technical");
    assertThat(new SystemEmailServiceIdentity("other", "app").allows(valid)).isFalse();
    assertThat(new SystemEmailServiceIdentity("service-id", "other").allows(valid)).isFalse();
    assertThat(new SystemEmailServiceIdentity("", "app").allows(valid)).isFalse();
    assertThat(new SystemEmailServiceIdentity("service-id", "").allows(valid)).isFalse();
    assertThat(new SystemEmailServiceIdentity("service-id", "app").allows(null)).isFalse();
  }
}
