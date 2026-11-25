package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.exception.TenantBadRequestException;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseSharingMode;
import com.vi.tenantservice.api.model.CaseSharingModePermissionPolicy;
import com.vi.tenantservice.api.model.CaseSharingPolicies;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ORISO-TenantService#374: case handover, team counselling or neither as one cascading choice;
 * {@code caseHandoverEnabled} is served derived from it.
 */
@ExtendWith(MockitoExtension.class)
class CaseSharingPolicyTest {

  private static final long TENANT = 42L;
  private static final String CASE_HANDOVER = "caseHandoverEnabled";

  @Mock private TenantPermissionPolicyRepository repository;
  @Mock private TenantAdminControlsService platformControls;
  @Mock private TenantRepository tenants;
  @InjectMocks private TenantPermissionPolicyService service;

  @BeforeEach
  void noAccountSettings() {
    lenient().when(tenants.findSettingsByIdIn(any())).thenReturn(List.of());
  }

  @Test
  void resolve_shouldSuggestCaseHandover_whenNoLevelChose() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    var mode = service.getResolvedCaseSharingPolicies(TENANT).getMode();

    assertThat(mode.getValue()).isEqualTo(CaseSharingMode.CASE_HANDOVER);
    assertThat(mode.getMode()).isEqualTo(PermissionPolicyMode.SUGGESTED);
    assertThat(mode.getInherited()).isTrue();
  }

  @Test
  void resolve_shouldReadTheTraegersLegacyCaseHandoverOff_asNone() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                entity(
                    "{\"caseHandoverEnabled\":{\"value\":false,\"mode\":\"SUGGESTED\"}}", null)));

    var mode = service.getResolvedCaseSharingPolicies(TENANT).getMode();

    assertThat(mode.getValue()).isEqualTo(CaseSharingMode.NONE);
    assertThat(mode.getInherited()).isFalse();
  }

  @Test
  void resolve_shouldDeriveCaseHandoverEnabled_fromTheTraegersMode() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                entity(
                    "{\"caseHandoverEnabled\":{\"value\":true,\"mode\":\"SUGGESTED\"}}",
                    "{\"mode\":{\"value\":\"TEAM_COUNSELLING\",\"mode\":\"ENFORCED\"}}")));

    var derived = service.getResolvedPolicies(TENANT).get(CASE_HANDOVER);

    assertThat(derived.value()).isFalse();
    assertThat(derived.mode().name()).isEqualTo("ENFORCED");
    assertThat(derived.inherited()).isFalse();
  }

  @Test
  void resolve_shouldKeepThePlatformRequirement_overTheTraegersChoice() {
    when(platformControls.getControls())
        .thenReturn(platform(CaseSharingMode.NONE, PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                entity("{}", "{\"mode\":{\"value\":\"CASE_HANDOVER\",\"mode\":\"SUGGESTED\"}}")));

    var mode = service.getResolvedCaseSharingPolicies(TENANT).getMode();

    assertThat(mode.getValue()).isEqualTo(CaseSharingMode.NONE);
    assertThat(mode.getMode()).isEqualTo(PermissionPolicyMode.ENFORCED);
    assertThat(service.getResolvedPolicies(TENANT).get(CASE_HANDOVER).value()).isFalse();
  }

  @Test
  void resolveBatch_shouldDeriveCaseHandoverPerTraeger() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantIdIn(Set.of(1L, 2L)))
        .thenReturn(
            List.of(
                TenantPermissionPolicyEntity.builder()
                    .tenantId(1L)
                    .policies("{}")
                    .caseSharingPolicies(
                        "{\"mode\":{\"value\":\"TEAM_COUNSELLING\",\"mode\":\"SUGGESTED\"}}")
                    .build()));

    var resolved = service.getResolvedPolicies(Set.of(1L, 2L));

    assertThat(resolved.get(1L).get(CASE_HANDOVER).value()).isFalse();
    assertThat(resolved.get(2L).get(CASE_HANDOVER).value()).isTrue();
  }

  @Test
  void save_shouldStoreTheModeAndTheDerivedCaseHandover() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        null,
        null,
        caseSharing(CaseSharingMode.TEAM_COUNSELLING, PermissionPolicyMode.SUGGESTED));

    var saved = captureSaved();
    assertThat(saved.getCaseSharingPolicies()).contains("TEAM_COUNSELLING");
    assertThat(saved.getPolicies()).contains("\"caseHandoverEnabled\":{\"value\":false");
  }

  @Test
  void save_shouldIgnoreAStaleCaseHandoverEntry_whenTheModeIsWritten() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(
            CASE_HANDOVER,
            new PolicyValue<>(
                true, com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED)),
        null,
        null,
        null,
        caseSharing(CaseSharingMode.NONE, PermissionPolicyMode.SUGGESTED));

    assertThat(captureSaved().getPolicies()).contains("\"caseHandoverEnabled\":{\"value\":false");
  }

  @Test
  void save_shouldTurnALegacyCaseHandoverChange_intoTheMode() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    when(repository.findByTenantId(TENANT))
        .thenReturn(
            Optional.of(
                entity("{}", "{\"mode\":{\"value\":\"CASE_HANDOVER\",\"mode\":\"SUGGESTED\"}}")));

    service.saveOverrides(
        TENANT,
        Map.of(
            CASE_HANDOVER,
            new PolicyValue<>(
                false, com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED)),
        null,
        null,
        null);

    assertThat(captureSaved().getCaseSharingPolicies()).contains("\"NONE\"");
  }

  @Test
  void save_shouldKeepTeamCounselling_whenALegacyBuildEchoesTheDerivedValue() {
    when(platformControls.getControls()).thenReturn(new TenantAdminControls());
    String stored = "{\"mode\":{\"value\":\"TEAM_COUNSELLING\",\"mode\":\"SUGGESTED\"}}";
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.of(entity("{}", stored)));

    service.saveOverrides(
        TENANT,
        Map.of(
            CASE_HANDOVER,
            new PolicyValue<>(
                false, com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED)),
        null,
        null,
        null);

    assertThat(captureSaved().getCaseSharingPolicies()).isEqualTo(stored);
  }

  @Test
  void save_shouldRefuseToChangeAPlatformRequirement() {
    when(platformControls.getControls())
        .thenReturn(platform(CaseSharingMode.CASE_HANDOVER, PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.saveOverrides(
                    TENANT,
                    Map.of(),
                    null,
                    null,
                    null,
                    caseSharing(CaseSharingMode.TEAM_COUNSELLING, PermissionPolicyMode.SUGGESTED)))
        .isInstanceOf(TenantBadRequestException.class)
        .hasMessageContaining("caseSharing.mode");
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void save_shouldDiscardAnUnchangedEchoOfAPlatformRequirement() {
    when(platformControls.getControls())
        .thenReturn(platform(CaseSharingMode.CASE_HANDOVER, PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(),
        null,
        null,
        null,
        caseSharing(CaseSharingMode.CASE_HANDOVER, PermissionPolicyMode.ENFORCED));

    assertThat(captureSaved().getCaseSharingPolicies()).isNull();
  }

  @Test
  void save_shouldIgnoreAStaleCaseHandoverEntry_underAPlatformRequirement() {
    when(platformControls.getControls())
        .thenReturn(platform(CaseSharingMode.NONE, PermissionPolicyMode.ENFORCED));
    when(repository.findByTenantId(TENANT)).thenReturn(Optional.empty());

    service.saveOverrides(
        TENANT,
        Map.of(
            CASE_HANDOVER,
            new PolicyValue<>(
                true, com.vi.tenantservice.api.policy.PermissionPolicyMode.SUGGESTED)),
        null,
        null,
        null,
        caseSharing(CaseSharingMode.NONE, PermissionPolicyMode.ENFORCED));

    TenantPermissionPolicyEntity saved = captureSaved();
    assertThat(saved.getCaseSharingPolicies()).isNull();
    assertThat(saved.getPolicies()).doesNotContain(CASE_HANDOVER);
  }

  private TenantPermissionPolicyEntity captureSaved() {
    var captor = ArgumentCaptor.forClass(TenantPermissionPolicyEntity.class);
    verify(repository).saveAndFlush(captor.capture());
    return captor.getValue();
  }

  private static TenantPermissionPolicyEntity entity(String policies, String caseSharing) {
    return TenantPermissionPolicyEntity.builder()
        .tenantId(TENANT)
        .policies(policies)
        .caseSharingPolicies(caseSharing)
        .build();
  }

  private static CaseSharingPolicies caseSharing(CaseSharingMode value, PermissionPolicyMode mode) {
    return new CaseSharingPolicies().mode(new CaseSharingModePermissionPolicy(value, mode));
  }

  private static TenantAdminControls platform(CaseSharingMode value, PermissionPolicyMode mode) {
    return new TenantAdminControls()
        .caseSharing(caseSharing(value, mode))
        .permissionPolicies(
            Map.of(
                CASE_HANDOVER,
                new BooleanPermissionPolicy(value == CaseSharingMode.CASE_HANDOVER, mode)));
  }
}
