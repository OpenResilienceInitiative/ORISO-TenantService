package com.vi.tenantservice.api.authorisation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Definition of all authorities and of the role-authority-mapping. */
@AllArgsConstructor
@Getter
public enum Authority {
  TENANT_ADMIN(
      UserRole.TENANT_ADMIN,
      Lists.newArrayList(
          AuthorityValue.CREATE_TENANT,
          AuthorityValue.UPDATE_TENANT,
          AuthorityValue.GET_ALL_TENANTS,
          AuthorityValue.GET_TENANT,
          AuthorityValue.CHANGE_LEGAL_CONTENT,
          AuthorityValue.SEARCH_TENANTS,
          AuthorityValue.GET_TENANT_ADMIN_DATA,
          AuthorityValue.UPDATE_EXTENDED_TENANT_SETTINGS)),

  SINGLE_TENANT_ADMIN(
      UserRole.SINGLE_TENANT_ADMIN,
      Lists.newArrayList(AuthorityValue.UPDATE_TENANT, AuthorityValue.GET_TENANT)),
  READ_TENANT_AS_AGENCY_ADMIN(
      UserRole.RESTRICTED_AGENCY_ADMIN, singletonList(AuthorityValue.GET_TENANT)),

  READ_TENANT_AS_CONSULTANT_ADMIN(
      UserRole.RESTRICTED_CONSULTANT_ADMIN, singletonList(AuthorityValue.GET_TENANT));

  private final UserRole userRole;
  private final List<String> grantedAuthorities;

  public static List<String> getAuthoritiesByUserRole(UserRole userRole) {
    Optional<Authority> authorityByUserRole =
        Stream.of(values()).filter(authority -> authority.userRole.equals(userRole)).findFirst();

    return authorityByUserRole.isPresent()
        ? authorityByUserRole.get().getGrantedAuthorities()
        : emptyList();
  }

  public static class AuthorityValue {

    private AuthorityValue() {}

    public static final String PREFIX = "AUTHORIZATION_";
    public static final String CREATE_TENANT = PREFIX + "CREATE_TENANT";
    public static final String UPDATE_TENANT = PREFIX + "UPDATE_TENANT";
    public static final String GET_ALL_TENANTS = PREFIX + "GET_ALL_TENANTS";
    public static final String GET_TENANT = PREFIX + "GET_TENANT";
    public static final String CHANGE_LEGAL_CONTENT = PREFIX + "CHANGE_LEGAL_CONTENT";
    public static final String SEARCH_TENANTS = PREFIX + "SEARCH_TENANTS";
    public static final String GET_TENANT_ADMIN_DATA = PREFIX + "GET_TENANT_ADMIN_DATA";

    public static final String UPDATE_EXTENDED_TENANT_SETTINGS =
        PREFIX + "UPDATE_EXTENDED_TENANT_SETTINGS";
  }
}
