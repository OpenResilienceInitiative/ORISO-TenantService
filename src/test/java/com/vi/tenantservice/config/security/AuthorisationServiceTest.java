package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.security.access.AccessDeniedException;
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
    // "testuser" Base32-encoded = ORSXG5A=, stored in JWT as enc.ORSXG5A. (= replaced by .)
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(jwt.getClaims()).thenReturn(new HashMap<>(Map.of("username", "enc.ORSXG5A.")));
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
  void getUsername_Should_ThrowAccessDeniedException_WhenBase32IsInvalid() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(jwt.getClaims()).thenReturn(new HashMap<>(Map.of("username", "enc.!!!NOT_BASE32!!!")));
    when(authentication.getPrincipal()).thenReturn(jwt);

    assertThatThrownBy(() -> authorisationService.getUsername())
        .isInstanceOf(AccessDeniedException.class);
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
