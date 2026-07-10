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
}
