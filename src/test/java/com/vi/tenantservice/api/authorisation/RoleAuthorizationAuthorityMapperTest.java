package com.vi.tenantservice.api.authorisation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleAuthorizationAuthorityMapperTest {

  @Test
  void consultantReceivesOnlyTheBoundedGroupChatTranslationAuthority() {
    var authorities = new RoleAuthorizationAuthorityMapper().mapAuthorities(Set.of("consultant"));

    assertThat(authorities)
        .extracting(authority -> authority.getAuthority())
        .containsExactly(Authority.AuthorityValue.TRANSLATE_GROUP_CHAT_CONTENT);
  }

  @Test
  void technicalServiceIdentityReceivesOnlyTheNarrowMachineAuthorities() {
    var authorities = new RoleAuthorizationAuthorityMapper().mapAuthorities(Set.of("technical"));

    assertThat(authorities)
        .extracting(authority -> authority.getAuthority())
        .containsExactlyInAnyOrder(
            Authority.AuthorityValue.TECHNICAL_READ_TENANT,
            Authority.AuthorityValue.TECHNICAL_CREATE_RESERVED_TENANT,
            Authority.AuthorityValue.TECHNICAL_RELEASE_TENANT_ID_RESERVATION);
  }

  @Test
  void technicalWithTenantAdminKeepsEveryTenantAdminAuthority() {
    var mapper = new RoleAuthorizationAuthorityMapper();
    var tenantAdminOnly = mapper.mapAuthorities(Set.of("tenant-admin"));

    assertThat(mapper.mapAuthorities(Set.of("technical", "tenant-admin")))
        .containsAll(tenantAdminOnly);
  }
}
