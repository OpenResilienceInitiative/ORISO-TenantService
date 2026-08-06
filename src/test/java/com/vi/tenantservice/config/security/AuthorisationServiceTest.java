package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AuthorisationServiceTest {

  @Mock SecurityContext securityContext;

  @Mock Authentication authentication;

  @Mock Jwt jwt;

  @InjectMocks AuthorisationService authorisationService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getUsername_Should_ReturnUsernameFromJwtPrincipal() {
    // given
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(jwt.getClaims()).thenReturn(new HashMap<>(Map.of("username", "testUsername")));

    when(authentication.getPrincipal()).thenReturn(jwt);

    // when, then
    assertThat(authorisationService.getUsername()).isEqualTo("testUsername");
  }

  @Test
  void getUsername_Should_DecodeBase32EncodedUsername() {
    // "testuser" Base32-encoded = ORSXG5DVONSXE===, stored in JWT as enc.ORSXG5DVONSXE...
    // (padding '=' is replaced by '.' in the JWT claim)
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(jwt.getClaims()).thenReturn(new HashMap<>(Map.of("username", "enc.ORSXG5DVONSXE...")));
    when(authentication.getPrincipal()).thenReturn(jwt);

    assertThat(authorisationService.getUsername()).isEqualTo("testuser");
  }

  @Test
  void getUsername_Should_ReturnNull_WhenUsernameClaim_IsMissing() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(jwt.getClaims()).thenReturn(new HashMap<>());
    when(authentication.getPrincipal()).thenReturn(jwt);

    assertThat(authorisationService.getUsername()).isNull();
  }

  @Test
  void getUsername_Should_NotThrow_WhenBase32ContainsOnlyInvalidChars() {
    // Apache Commons Codec Base32 is lenient by default: non-alphabet characters are silently
    // skipped rather than raising an exception. The try-catch for IllegalArgumentException in
    // getUsername() acts as a forward-compatibility safety net.
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(jwt.getClaims()).thenReturn(new HashMap<>(Map.of("username", "enc.!!!NOT_BASE32!!!")));
    when(authentication.getPrincipal()).thenReturn(jwt);

    assertThatCode(() -> authorisationService.getUsername()).doesNotThrowAnyException();
  }

  @Test
  void extractRealmRoles_Should_ExtractRolesFromJwt() {
    // given
    when(jwt.getClaims())
        .thenReturn(
            new HashMap<>(
                Map.of(
                    "realm_access", Map.of("roles", Lists.newArrayList("single-tenant-admin")))));

    // when
    Collection<String> roles = authorisationService.extractRealmRoles(jwt);

    // then
    assertThat(roles).contains("single-tenant-admin");
  }

  @Test
  void extractRealmRoles_Should_ReturnNoRoles_WhenRealmAccessClaimHasUnexpectedType() {
    // given
    when(jwt.getClaims()).thenReturn(new HashMap<>(Map.of("realm_access", "invalid")));

    // when
    Collection<String> roles = authorisationService.extractRealmRoles(jwt);

    // then
    assertThat(roles).isEmpty();
  }

  @Test
  void extractRealmRoles_Should_IgnoreNonStringRoleValues() {
    // given
    when(jwt.getClaims())
        .thenReturn(
            new HashMap<>(
                Map.of("realm_access", Map.of("roles", Lists.newArrayList(1, "tenant-admin")))));

    // when
    Collection<String> roles = authorisationService.extractRealmRoles(jwt);

    // then
    assertThat(roles).containsExactly("tenant-admin");
  }

  @Test
  void extractRealmRoles_Should_ExtractAuthoritiesFromJwt() {
    // given
    when(jwt.getClaims())
        .thenReturn(
            new HashMap<>(
                Map.of(
                    "realm_access", Map.of("roles", Lists.newArrayList("single-tenant-admin")))));

    // when
    Collection<? extends GrantedAuthority> grantedAuthorities =
        authorisationService.extractRealmAuthorities(jwt);

    // then
    assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
        .contains("AUTHORIZATION_GET_TENANT", "AUTHORIZATION_UPDATE_TENANT");
  }
}
