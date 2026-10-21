package com.vi.tenantservice.config.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantBootstrapAuthorityTest {
  @AfterEach
  void clearIdentity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void aTenantIdAndMachineIdentityCannotMintFiveActorCreationAuthority() {
    var identity = mock(TaskServiceIdentity.class);
    when(identity.current("CONFIG_WIZARD")).thenReturn(true);
    var jwt = Jwt.withTokenValue("synthetic-no-wire-token").header("alg", "RS256")
        .issuer("https://synthetic.identity/realms/context").subject("setup-subject")
        .claim("azp", "backend-setup").expiresAt(Instant.now().plusSeconds(120)).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    var environment = grouped().withProperty("ORISO_TENANT_CREATION_CONTEXT_KEY", key());
    var signer = new TenantCreationContext(environment, identity);
    assertThrows(AccessDeniedException.class, () -> signer.issue(42));
  }

  @Test
  void fiveActorPreCreateValidationRequiresOnlyTheManagedCommonKey() {
    var signer = new TenantCreationContext(grouped(), mock(TaskServiceIdentity.class));
    assertDoesNotThrow(signer::validateConfiguration);
  }

  private static String key() {
    return Base64.getEncoder().encodeToString(new byte[32]);
  }

  private static MockEnvironment grouped() {
    return new MockEnvironment().withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD")
        .withProperty("ORISO_TASK_CONTEXT_KEY", key());
  }
}
