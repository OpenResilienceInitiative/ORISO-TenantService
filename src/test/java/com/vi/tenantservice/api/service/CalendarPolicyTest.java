package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.exception.TenantBadRequestException;
import com.vi.tenantservice.api.exception.TenantSettingsUnavailableException;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CalendarPolicies;
import com.vi.tenantservice.api.model.CalendarVisibilityScope;
import com.vi.tenantservice.api.model.CalendarVisibilityScopePolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.policy.CalendarPolicyRules;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import java.util.List;
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
 * ADR-032 / #365 - the calendar visibility scope and the asker username visibility follow the
 * Functionality Access policy model: platform preset, Traeger override, ENFORCED (requirement)
 * versus SUGGESTED (recommendation). Same semantics as the internal group chat recipient scope.
 */
@ExtendWith(MockitoExtension.class)
class CalendarPolicyTest {

  private static final long TENANT = 42L;

  @Mock private TenantPermissionPolicyRepository repository;
  @Mock private TenantAdminControlsService platformControls;
  @Mock private com.vi.tenantservice.api.repository.TenantRepository tenants;
  @InjectMocks private TenantPermissionPolicyService service;

  // ---- defaults / normalize ----

  @Test
  void defaults_shouldBeCounsellingCentreOnlyAndHiddenUsernames_asRecommendation() {
    CalendarPolicies defaults = CalendarPolicyRules.defaults();

    assertThat(defaults.getVisibilityScope())
        .isEqualTo(
            new CalendarVisibilityScopePolicy(
                CalendarVisibilityScope.AGENCY, PermissionPolicyMode.SUGGESTED));
    assertThat(defaults.getShowAskerUsernames())
        .isEqualTo(new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED));
  }

  @Test
  void normalize_shouldFillEachMissingPolicyOnItsOwn_andDropTheInheritedMarker() {
    CalendarPolicies stored =
        new CalendarPolicies()
            .visibilityScope(
                new CalendarVisibilityScopePolicy(
                        CalendarVisibilityScope.PLATFORM, PermissionPolicyMode.ENFORCED)
                    .inherited(true));

    CalendarPolicies normalized = CalendarPolicyRules.normalize(stored);

    assertThat(normalized.getVisibilityScope())
        .isEqualTo(
            new CalendarVisibilityScopePolicy(
                CalendarVisibilityScope.PLATFORM, PermissionPolicyMode.ENFORCED));
    assertThat(normalized.getShowAskerUsernames())
        .isEqualTo(new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED));
    assertThat(CalendarPolicyRules.normalize(null)).isEqualTo(CalendarPolicyRules.defaults());
  }

  @Test
  void normalize_shouldFallBackToTheDefault_whenAStoredPolicyLacksItsMode() {
    CalendarPolicies stored =
        new CalendarPolicies()
            .visibilityScope(
                new CalendarVisibilityScopePolicy(CalendarVisibilityScope.TENANT, null))
            .showAskerUsernames(new BooleanPermissionPolicy(true, null));

    assertThat(CalendarPolicyRules.normalize(stored)).isEqualTo(CalendarPolicyRules.defaults());
  }

  // ---- resolve ----

  @Test
  void resolve_shouldDefaultToAgencyAndHiddenUsernames_whenNeitherPlatformNorTraegerSetIt() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    CalendarPolicies resolved = service.getResolvedCalendarPolicies(TENANT);

    assertThat(resolved.getVisibilityScope().getValue()).isEqualTo(CalendarVisibilityScope.AGENCY);
    assertThat(resolved.getVisibilityScope().getMode()).isEqualTo(PermissionPolicyMode.SUGGESTED);
    assertThat(resolved.getVisibilityScope().getInherited()).isTrue();
    assertThat(resolved.getShowAskerUsernames().getValue()).isFalse();
    assertThat(resolved.getShowAskerUsernames().getMode())
        .isEqualTo(PermissionPolicyMode.SUGGESTED);
    assertThat(resolved.getShowAskerUsernames().getInherited()).isTrue();
  }

  @Test
  void resolve_shouldApplyTheTraegerOverride_whenThePlatformOnlyRecommends() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                stored(
                    "{\"visibilityScope\":{\"value\":\"TENANT\",\"mode\":\"SUGGESTED\"},"
                        + "\"showAskerUsernames\":{\"value\":true,\"mode\":\"SUGGESTED\"}}")));

    CalendarPolicies resolved = service.getResolvedCalendarPolicies(TENANT);

    assertThat(resolved.getVisibilityScope().getValue()).isEqualTo(CalendarVisibilityScope.TENANT);
    assertThat(resolved.getVisibilityScope().getInherited()).isFalse();
    assertThat(resolved.getShowAskerUsernames().getValue()).isTrue();
    assertThat(resolved.getShowAskerUsernames().getInherited()).isFalse();
  }

  @Test
  void resolve_shouldKeepThePlatformRequirement_overAStoredTraegerOverride() {
    when(platformControls.getControls())
        .thenReturn(
            platform(
                CalendarVisibilityScope.AGENCY,
                PermissionPolicyMode.ENFORCED,
                false,
                PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                stored(
                    "{\"visibilityScope\":{\"value\":\"PLATFORM\",\"mode\":\"SUGGESTED\"},"
                        + "\"showAskerUsernames\":{\"value\":true,\"mode\":\"SUGGESTED\"}}")));

    CalendarPolicies resolved = service.getResolvedCalendarPolicies(TENANT);

    assertThat(resolved.getVisibilityScope().getValue()).isEqualTo(CalendarVisibilityScope.AGENCY);
    assertThat(resolved.getVisibilityScope().getMode()).isEqualTo(PermissionPolicyMode.ENFORCED);
    assertThat(resolved.getVisibilityScope().getInherited()).isTrue();
    assertThat(resolved.getShowAskerUsernames().getValue()).isFalse();
    assertThat(resolved.getShowAskerUsernames().getInherited()).isTrue();
  }

  @Test
  void resolve_shouldLeaveOtherTraegerUnaffected_andOmitAnUnreadableRow() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantIdIn(Set.of(1L, 2L, 3L)))
        .thenReturn(
            List.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(1L)
                    .policies("{}")
                    .calendarPolicies(
                        "{\"visibilityScope\":{\"value\":\"PLATFORM\",\"mode\":\"SUGGESTED\"}}")
                    .build(),
                TenantPermissionPolicyEntity.builder()
                    .tenantId(3L)
                    .policies("{}")
                    .calendarPolicies("not json")
                    .build()));

    Map<Long, CalendarPolicies> resolved = service.getResolvedCalendarPolicies(Set.of(1L, 2L, 3L));

    assertThat(resolved.get(1L).getVisibilityScope().getValue())
        .isEqualTo(CalendarVisibilityScope.PLATFORM);
    assertThat(resolved.get(2L).getVisibilityScope().getValue())
        .isEqualTo(CalendarVisibilityScope.AGENCY);
    assertThat(resolved.get(2L).getShowAskerUsernames().getValue()).isFalse();
    assertThat(resolved).doesNotContainKey(3L);
  }

  @Test
  void resolve_shouldRefuseAStoredOverrideWithoutAMode() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT))
        .thenReturn(Optional.of(stored("{\"showAskerUsernames\":{\"value\":true}}")));

    assertThatThrownBy(() -> service.getResolvedCalendarPolicies(TENANT))
        .isInstanceOf(TenantSettingsUnavailableException.class);
  }

  // ---- save / sanitize ----

  @Test
  void save_shouldStoreTheTraegerOverride_whenThePlatformOnlyRecommends() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        null,
        calendar(
            CalendarVisibilityScope.TENANT,
            PermissionPolicyMode.SUGGESTED,
            true,
            PermissionPolicyMode.SUGGESTED));

    TenantPermissionPolicyEntity saved = savedEntity();
    assertThat(saved.getTenantId()).isEqualTo(TENANT);
    assertThat(saved.getCalendarPolicies()).contains("\"TENANT\"").contains("true");
    assertThat(saved.getInternalGroupChatPolicies()).isNull();
  }

  @Test
  void save_shouldRejectAChangedVisibilityScope_whenThePlatformRequiresIt() {
    when(platformControls.getControls())
        .thenReturn(
            platform(
                CalendarVisibilityScope.AGENCY,
                PermissionPolicyMode.ENFORCED,
                false,
                PermissionPolicyMode.SUGGESTED));

    assertThatThrownBy(
            () ->
                service.saveOverrides(
                    TENANT,
                    Map.of(),
                    null,
                    null,
                    new CalendarPolicies()
                        .visibilityScope(
                            new CalendarVisibilityScopePolicy(
                                CalendarVisibilityScope.TENANT, PermissionPolicyMode.SUGGESTED))))
        .isInstanceOf(TenantBadRequestException.class)
        .hasMessageContaining("calendar.visibilityScope");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void save_shouldRejectAChangedUsernameVisibility_whenThePlatformRequiresIt() {
    when(platformControls.getControls())
        .thenReturn(
            platform(
                CalendarVisibilityScope.AGENCY,
                PermissionPolicyMode.SUGGESTED,
                false,
                PermissionPolicyMode.ENFORCED));

    assertThatThrownBy(
            () ->
                service.saveOverrides(
                    TENANT,
                    Map.of(),
                    null,
                    null,
                    new CalendarPolicies()
                        .showAskerUsernames(
                            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED))))
        .isInstanceOf(TenantBadRequestException.class)
        .hasMessageContaining("calendar.showAskerUsernames");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void save_shouldDiscardAnEnforcedEcho_butKeepTheOtherPolicy() {
    when(platformControls.getControls())
        .thenReturn(
            platform(
                CalendarVisibilityScope.TENANT,
                PermissionPolicyMode.ENFORCED,
                false,
                PermissionPolicyMode.SUGGESTED));
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        null,
        calendar(
            CalendarVisibilityScope.TENANT,
            PermissionPolicyMode.ENFORCED,
            true,
            PermissionPolicyMode.SUGGESTED));

    String saved = savedEntity().getCalendarPolicies();
    assertThat(saved).doesNotContain("\"TENANT\"").contains("true");
  }

  @Test
  void save_shouldKeepTheStoredCalendar_whenTheRequestDoesNotMentionIt() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    String stored = "{\"visibilityScope\":{\"value\":\"TENANT\",\"mode\":\"SUGGESTED\"}}";
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.of(stored(stored)));

    service.saveOverrides(TENANT, Map.of(), null, null, null);

    assertThat(savedEntity().getCalendarPolicies()).isEqualTo(stored);
  }

  @Test
  void save_shouldNotStoreAPolicyWithoutAMode() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        null,
        new CalendarPolicies()
            .visibilityScope(
                new CalendarVisibilityScopePolicy(CalendarVisibilityScope.TENANT, null)));

    assertThat(savedEntity().getCalendarPolicies()).doesNotContain("\"TENANT\"");
  }

  private TenantPermissionPolicyEntity savedEntity() {
    ArgumentCaptor<TenantPermissionPolicyEntity> saved =
        ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    verify(repository).saveAndFlush(saved.capture());
    return saved.getValue();
  }

  private static TenantPermissionPolicyEntity stored(String json) {
    return TenantPermissionPolicyEntity.builder()
        .tenantId(TENANT)
        .policies("{}")
        .calendarPolicies(json)
        .build();
  }

  private static TenantAdminControls platform(
      CalendarVisibilityScope scope,
      PermissionPolicyMode scopeMode,
      boolean usernames,
      PermissionPolicyMode usernamesMode) {
    return new TenantAdminControls().calendar(calendar(scope, scopeMode, usernames, usernamesMode));
  }

  private static CalendarPolicies calendar(
      CalendarVisibilityScope scope,
      PermissionPolicyMode scopeMode,
      boolean usernames,
      PermissionPolicyMode usernamesMode) {
    return new CalendarPolicies()
        .visibilityScope(new CalendarVisibilityScopePolicy(scope, scopeMode))
        .showAskerUsernames(new BooleanPermissionPolicy(usernames, usernamesMode));
  }
}
