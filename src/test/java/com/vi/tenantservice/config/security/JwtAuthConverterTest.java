package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtAuthConverterTest {

  private final JwtAuthConverterProperties properties = new JwtAuthConverterProperties();
  private final JwtAuthConverter jwtAuthConverter =
      new JwtAuthConverter(properties, new AuthorisationService());

  @Test
  void convert_Should_NotThrow_WhenRealmAccessClaimHasUnexpectedType() {
    // given
    Jwt jwt = jwtWithClaims(Map.of("sub", "user-id", "realm_access", "invalid"));

    // when, then
    assertThatCode(() -> jwtAuthConverter.convert(jwt)).doesNotThrowAnyException();
  }

  @Test
  void convert_Should_IgnoreNonStringRoleValues_WhenRealmAccessRolesAreMixed() {
    // given
    Jwt jwt =
        jwtWithClaims(
            Map.of("sub", "user-id", "realm_access", Map.of("roles", List.of(42, "tenant-admin"))));

    // when
    JwtAuthenticationToken authenticationToken =
        (JwtAuthenticationToken) jwtAuthConverter.convert(jwt);

    // then
    assertThat(authenticationToken.getAuthorities())
        .extracting("authority")
        .contains("AUTHORIZATION_GET_ALL_TENANTS");
  }

  private Jwt jwtWithClaims(Map<String, Object> claims) {
    Map<String, Object> headers = new HashMap<>();
    headers.put("alg", "none");
    return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), headers, claims);
  }
}
