package com.vi.tenantservice.config.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CreationContextKeyTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void fiveActorsStartsWithOnlyTheManagedTaskContextKey() {
    var environment = grouped().withProperty("ORISO_TASK_CONTEXT_KEY", KEY);
    assertDoesNotThrow(
        () -> new TenantCreationContext(environment, mock(TaskServiceIdentity.class)));
  }

  @Test
  void legacyCreationKeyCannotSubstituteForTheManagedFiveActorKey() {
    var environment = grouped().withProperty("ORISO_TENANT_CREATION_CONTEXT_KEY", KEY);
    assertThrows(
        IllegalStateException.class,
        () -> new TenantCreationContext(environment, mock(TaskServiceIdentity.class)));
  }

  private static MockEnvironment grouped() {
    return new MockEnvironment()
        .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD");
  }
}
