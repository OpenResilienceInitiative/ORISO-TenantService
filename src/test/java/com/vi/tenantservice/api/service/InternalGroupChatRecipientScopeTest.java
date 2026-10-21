package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.exception.TenantBadRequestException;
import com.vi.tenantservice.api.model.InternalGroupChatPolicies;
import com.vi.tenantservice.api.model.InternalGroupChatRecipientScope;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.RecipientScopePermissionPolicy;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * #319 - the recipient scope of internal group chats follows the Functionality Access policy model:
 * platform preset, Traeger override, ENFORCED (requirement) versus SUGGESTED (recommendation).
 */
@ExtendWith(MockitoExtension.class)
class InternalGroupChatRecipientScopeTest {

  private static final long TENANT = 42L;

  @Mock private TenantPermissionPolicyRepository repository;
  @Mock private TenantAdminControlsService platformControls;
  @InjectMocks private TenantPermissionPolicyService service;

  @Test
  void resolve_shouldDefaultToCounsellingCentreOnly_whenNeitherPlatformNorTraegerSetIt() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    RecipientScopePermissionPolicy scope = resolvedScope(TENANT);

    assertThat(scope.getValue()).isEqualTo(InternalGroupChatRecipientScope.AGENCY);
    assertThat(scope.getMode()).isEqualTo(PermissionPolicyMode.SUGGESTED);
    assertThat(scope.getInherited()).isTrue();
  }

  @Test
  void resolve_shouldInheritThePlatformPreset_whenTheTraegerSetNothing() {
    when(platformControls.getControls())
        .thenReturn(
            platformScope(
                InternalGroupChatRecipientScope.PLATFORM, PermissionPolicyMode.SUGGESTED));
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    RecipientScopePermissionPolicy scope = resolvedScope(TENANT);

    assertThat(scope.getValue()).isEqualTo(InternalGroupChatRecipientScope.PLATFORM);
    assertThat(scope.getInherited()).isTrue();
  }

  @Test
  void resolve_shouldApplyTheTraegerOverride_whenThePlatformOnlyRecommends() {
    when(platformControls.getControls())
        .thenReturn(
            platformScope(
                InternalGroupChatRecipientScope.PLATFORM, PermissionPolicyMode.SUGGESTED));
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                storedScope("{\"recipientScope\":{\"value\":\"TENANT\",\"mode\":\"SUGGESTED\"}}")));

    RecipientScopePermissionPolicy scope = resolvedScope(TENANT);

    assertThat(scope.getValue()).isEqualTo(InternalGroupChatRecipientScope.TENANT);
    assertThat(scope.getMode()).isEqualTo(PermissionPolicyMode.SUGGESTED);
    assertThat(scope.getInherited()).isFalse();
  }

  @Test
  void resolve_shouldKeepThePlatformRequirement_overAStoredTraegerOverride() {
    when(platformControls.getControls())
        .thenReturn(
            platformScope(InternalGroupChatRecipientScope.TENANT, PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                storedScope(
                    "{\"recipientScope\":{\"value\":\"PLATFORM\",\"mode\":\"SUGGESTED\"}}")));

    RecipientScopePermissionPolicy scope = resolvedScope(TENANT);

    assertThat(scope.getValue()).isEqualTo(InternalGroupChatRecipientScope.TENANT);
    assertThat(scope.getMode()).isEqualTo(PermissionPolicyMode.ENFORCED);
    assertThat(scope.getInherited()).isTrue();
  }

  @Test
  void resolve_shouldLeaveOtherTraegerUnaffected_whenOneTraegerWidensItsScope() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantIdIn(Set.of(1L, 2L)))
        .thenReturn(
            java.util.List.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(1L)
                    .policies("{}")
                    .internalGroupChatPolicies(
                        "{\"recipientScope\":{\"value\":\"PLATFORM\",\"mode\":\"SUGGESTED\"}}")
                    .build()));

    Map<Long, InternalGroupChatPolicies> resolved =
        service.getResolvedInternalGroupChatPolicies(Set.of(1L, 2L));

    assertThat(resolved.get(1L).getRecipientScope().getValue())
        .isEqualTo(InternalGroupChatRecipientScope.PLATFORM);
    assertThat(resolved.get(2L).getRecipientScope().getValue())
        .isEqualTo(InternalGroupChatRecipientScope.AGENCY);
    assertThat(resolved.get(2L).getRecipientScope().getInherited()).isTrue();
  }

  @Test
  void save_shouldStoreTheTraegerOverride_whenThePlatformOnlyRecommends() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        scopePolicies(InternalGroupChatRecipientScope.TENANT, PermissionPolicyMode.SUGGESTED));

    TenantPermissionPolicyEntity saved = savedEntity();
    assertThat(saved.getTenantId()).isEqualTo(TENANT);
    assertThat(saved.getInternalGroupChatPolicies()).contains("\"TENANT\"");
  }

  @Test
  void save_shouldRejectAChangeToAPlatformRequirement_beforePersistence() {
    when(platformControls.getControls())
        .thenReturn(
            platformScope(InternalGroupChatRecipientScope.TENANT, PermissionPolicyMode.ENFORCED));

    assertThatThrownBy(
            () ->
                service.saveOverrides(
                    TENANT,
                    Map.of(),
                    null,
                    scopePolicies(
                        InternalGroupChatRecipientScope.PLATFORM, PermissionPolicyMode.SUGGESTED)))
        .isInstanceOf(TenantBadRequestException.class)
        .hasMessageContaining("internalGroupChat.recipientScope");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void save_shouldDiscardAnUnchangedEchoOfAPlatformRequirement() {
    when(platformControls.getControls())
        .thenReturn(
            platformScope(InternalGroupChatRecipientScope.TENANT, PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        scopePolicies(InternalGroupChatRecipientScope.TENANT, PermissionPolicyMode.ENFORCED));

    assertThat(savedEntity().getInternalGroupChatPolicies()).doesNotContain("\"TENANT\"");
  }

  @Test
  void save_shouldKeepTheStoredScope_whenTheRequestDoesNotMentionIt() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    String stored = "{\"recipientScope\":{\"value\":\"TENANT\",\"mode\":\"SUGGESTED\"}}";
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.of(storedScope(stored)));

    service.saveOverrides(TENANT, Map.of(), null, null);

    assertThat(savedEntity().getInternalGroupChatPolicies()).isEqualTo(stored);
  }

  @Test
  void save_shouldNotStoreAScopeWithoutAMode() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT, Map.of(), null, scopePolicies(InternalGroupChatRecipientScope.TENANT, null));

    assertThat(savedEntity().getInternalGroupChatPolicies()).doesNotContain("\"TENANT\"");
  }

  private RecipientScopePermissionPolicy resolvedScope(long tenantId) {
    return service.getResolvedInternalGroupChatPolicies(tenantId).getRecipientScope();
  }

  private TenantPermissionPolicyEntity savedEntity() {
    ArgumentCaptor<TenantPermissionPolicyEntity> saved =
        ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    verify(repository).saveAndFlush(saved.capture());
    return saved.getValue();
  }

  private static TenantPermissionPolicyEntity storedScope(String json) {
    return TenantPermissionPolicyEntity.builder()
        .tenantId(TENANT)
        .policies("{}")
        .internalGroupChatPolicies(json)
        .build();
  }

  private static TenantAdminControls platformScope(
      InternalGroupChatRecipientScope value, PermissionPolicyMode mode) {
    return new TenantAdminControls().internalGroupChat(scopePolicies(value, mode));
  }

  private static InternalGroupChatPolicies scopePolicies(
      InternalGroupChatRecipientScope value, PermissionPolicyMode mode) {
    return new InternalGroupChatPolicies()
        .recipientScope(new RecipientScopePermissionPolicy(value, mode));
  }
}
