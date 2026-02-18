package com.vi.tenantservice.config.security;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.authorisation.RoleAuthorizationAuthorityMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthorisationService {

  private final RoleAuthorizationAuthorityMapper roleAuthorizationAuthorityMapper =
      new RoleAuthorizationAuthorityMapper();

  public boolean hasAuthority(String authorityName) {
    return getAuthentication().getAuthorities().stream()
        .anyMatch(role -> authorityName.equals(role.getAuthority()));
  }

  public boolean hasRole(String roleName) {
    var roles = extractRealmRoles(getPrincipal());
    return roles != null && roles.contains(roleName);
  }

  public Optional<Long> findTenantIdInAccessToken() {
    Jwt principal = getPrincipal();
    Object tenantIdClaim = principal.getClaims().get("tenantId");
    if (tenantIdClaim == null) {
      throw new AccessDeniedException("tenantId attribute not found in the access token");
    }

    Long tenantId;
    if (tenantIdClaim instanceof Long) {
      tenantId = (Long) tenantIdClaim;
    } else if (tenantIdClaim instanceof Integer) {
      tenantId = Long.valueOf((Integer) tenantIdClaim);
    } else if (tenantIdClaim instanceof String) {
      tenantId = Long.parseLong((String) tenantIdClaim);
    } else {
      throw new AccessDeniedException(
          "Unsupported tenantId claim type: " + tenantIdClaim.getClass());
    }

    return Optional.of(tenantId);
  }

  public Object getUsername() {
    return getPrincipal().getClaims().get("username");
  }

  private Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private Jwt getPrincipal() {
    return (Jwt) getAuthentication().getPrincipal();
  }

  public Collection<GrantedAuthority> extractRealmAuthorities(Jwt jwt) {
    var roles = extractRealmRoles(jwt);
    return roleAuthorizationAuthorityMapper.mapAuthorities(
        roles.stream().collect(Collectors.toSet()));
  }

  public Collection<String> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
    if (realmAccess != null) {
      var roles = (List<String>) realmAccess.get("roles");
      if (roles != null) {
        return roles;
      }
    }
    return Lists.newArrayList();
  }
}
