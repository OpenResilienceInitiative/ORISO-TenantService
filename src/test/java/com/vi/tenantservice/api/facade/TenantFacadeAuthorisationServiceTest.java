package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.exception.TenantAuthorisationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSetting;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.policy.PermissionPolicyMode;
import com.vi.tenantservice.api.policy.ResolvedPolicyValue;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.api.service.TenantPermissionPolicyService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.util.JsonConverter;
import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TenantFacadeAuthorisationServiceTest {
  private static final long ID = 1L;

  @InjectMocks TenantFacadeAuthorisationService tenantFacadeAuthorisationService;

  @Mock AuthorisationService authorisationService;

  @Mock com.vi.tenantservice.config.security.TechnicalServiceIdentity technicalServiceIdentity;
  @Mock com.vi.tenantservice.config.security.TaskServiceIdentity taskServiceIdentity;

  @Mock TenantFacadeChangeDetectionService tenantFacadeChangeDetectionService;

  @Mock ApplicationSettingsService applicationSettingsService;

  @Mock ConsultingTypeService consultingTypeService;

  @Mock TenantAdminControlsService tenantAdminControlsService;

  @Mock TenantPermissionPolicyService tenantPermissionPolicyService;

  @Test
  void platformTenantAdminMayReadButCannotActForARecipient() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);

    assertThatCode(() -> tenantFacadeAuthorisationService.assertCanReadLegalProposal(7L))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> tenantFacadeAuthorisationService.assertCanManageOwnLegalProposal(7L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void exactRecipientWithLegalAuthorityMayReadAndAct() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(7L));
    when(authorisationService.hasAuthority(Authority.AuthorityValue.CHANGE_LEGAL_CONTENT))
        .thenReturn(true);

    assertThatCode(() -> tenantFacadeAuthorisationService.assertCanReadLegalProposal(7L))
        .doesNotThrowAnyException();
    assertThatCode(() -> tenantFacadeAuthorisationService.assertCanManageOwnLegalProposal(7L))
        .doesNotThrowAnyException();
  }

  @Test
  void exactRecipientWithoutLegalAuthorityMayReadButNotAct() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(7L));
    when(authorisationService.hasRole("single-tenant-admin")).thenReturn(false);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.CHANGE_LEGAL_CONTENT))
        .thenReturn(false);

    assertThatCode(() -> tenantFacadeAuthorisationService.assertCanReadLegalProposal(7L))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> tenantFacadeAuthorisationService.assertCanManageOwnLegalProposal(7L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void onlyThePlatformAdministratorMaySharePlatformLegalDrafts() {
    // Sharing writes a proposal into every selected Träger, so the guard on it is the most
    // privileged rule here. A Träger admin of tenant 7 must not reach it.
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(7L));

    assertThatThrownBy(() -> tenantFacadeAuthorisationService.assertCanDistributeLegalProposals())
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void theTechnicalPrincipalMayNotSharePlatformLegalDrafts() {
    when(technicalServiceIdentity.isCurrentCaller()).thenReturn(true);

    assertThatThrownBy(() -> tenantFacadeAuthorisationService.assertCanDistributeLegalProposals())
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void theTechnicalPrincipalWithTenantZeroMayNotListEveryTenantButKeepsSingleTenantAccess() {
    when(technicalServiceIdentity.isCurrentCaller()).thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(true);

    assertThat(tenantFacadeAuthorisationService.mayListEveryTenant()).isFalse();
    // it still mints sign invites for reserved Träger ids during onboarding
    assertThat(tenantFacadeAuthorisationService.mayAccessEveryTenant()).isTrue();
  }

  @Test
  void thePlatformAdministratorMayListEveryTenant() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(true);

    assertThat(tenantFacadeAuthorisationService.mayListEveryTenant()).isTrue();
  }

  @Test
  void thePlatformAdministratorMaySharePlatformLegalDrafts() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);

    assertThatCode(() -> tenantFacadeAuthorisationService.assertCanDistributeLegalProposals())
        .doesNotThrowAnyException();
  }

  @Test
  void foreignRecipientAndTechnicalPrincipalAreDeniedBeforeProposalLookup() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(8L));
    assertThatThrownBy(() -> tenantFacadeAuthorisationService.assertCanReadLegalProposal(7L))
        .isInstanceOf(AccessDeniedException.class);

    when(technicalServiceIdentity.isCurrentCaller()).thenReturn(true);
    assertThatThrownBy(() -> tenantFacadeAuthorisationService.assertCanManageOwnLegalProposal(7L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void
      assertUserIsAuthorizedToAccessTenant_Should_AllowOperation_When_tenantIsFoundAndUserIsSingleTenantAdminForThatTenant() {
    // given
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(ID));

    // when
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(ID);

    // then
    verify(authorisationService).findTenantIdInAccessToken();
  }

  @Test
  void
      assertUserIsAuthorizedToAccessTenant_Should_ThrowAccessDeniedException_When_UserIsSingleTenantAdminAndDoesNotHaveAnyTokenIdKeycloakAttribute() {
    // given
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.empty());
    // then
    assertThrows(
        AccessDeniedException.class,
        () -> {
          // when
          tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(ID);
        });
  }

  @Test
  void canAccessTenant_Should_ReturnFalse_When_TenantIdCannotBeReadFromAccessToken() {
    // given
    when(authorisationService.findTenantIdInAccessToken())
        .thenThrow(new AccessDeniedException("tenantId attribute not found in the access token"));
    // when
    boolean canAccessTenant =
        tenantFacadeAuthorisationService.canAccessTenant(
            Optional.of(TenantEntity.builder().id(ID).build()));

    // then
    assertThat(canAccessTenant).isFalse();
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_ThrowException_When_UserIsSingleTenantAdminAndTriesToChangeLicencedNumberOfUsers() {
    // given
    TenantEntity tenantEntity = TenantEntity.builder().licensingAllowedNumberOfUsers(1).build();
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO().licensing(new Licensing().allowedNumberOfUsers(2));
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);

    // then
    assertThrows(
        TenantAuthorisationException.class,
        () -> {
          // when
          tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
              tenantDTO, tenantEntity);
        });
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_ThrowException_When_UserIsSingleTenantAdminAndTriesToChangeLicencedNumberOfUsersFromNull() {
    // given
    TenantEntity tenantEntity = TenantEntity.builder().build();
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO().licensing(new Licensing().allowedNumberOfUsers(2));
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);

    // then
    assertThrows(
        TenantAuthorisationException.class,
        () -> {
          // when
          tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
              tenantDTO, tenantEntity);
        });
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_ThrowException_When_UserIsSingleTenantAdminAndTriesToChangeSubdomain() {
    // given
    TenantEntity tenantEntity = TenantEntity.builder().subdomain("old subdomain").build();
    MultilingualTenantDTO tenantDTO = new MultilingualTenantDTO().subdomain("new subdomain");
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);

    // then
    assertThrows(
        TenantAuthorisationException.class,
        () -> {
          // when
          tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
              tenantDTO, tenantEntity);
        });
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_ThrowException_When_UserIsSingleTenantAdminAndTriesToChangeSubdomainFromNull() {
    // given
    TenantEntity tenantEntity = TenantEntity.builder().build();
    MultilingualTenantDTO tenantDTO = new MultilingualTenantDTO().subdomain("new subdomain");
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
    // then
    assertThrows(
        TenantAuthorisationException.class,
        () -> {
          // when
          tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
              tenantDTO, tenantEntity);
        });
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowOperation_When_UserIsSingleTenantAdminAndDoesNotChangeSubdomainNorLicensing() {
    // given
    TenantEntity tenantEntity =
        TenantEntity.builder().subdomain("old subdomain").licensingAllowedNumberOfUsers(1).build();
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO()
            .subdomain("old subdomain")
            .licensing(new Licensing().allowedNumberOfUsers(1))
            .theming(new Theming().logo("logo"));
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);

    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        tenantDTO, tenantEntity);
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowOperation_When_UserIsSingleTenantAdmin() {
    // given
    TenantEntity tenantEntity =
        TenantEntity.builder().subdomain("old subdomain").licensingAllowedNumberOfUsers(1).build();
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO()
            .subdomain("old subdomain")
            .licensing(new Licensing().allowedNumberOfUsers(1))
            .theming(new Theming().logo("logo"));
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);

    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        tenantDTO, tenantEntity);
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowOperation_When_NoChangesInSettingsDetected() {
    // given
    TenantSettings tenantSettings = new TenantSettings();
    String settings = JsonConverter.convertToJson(tenantSettings);
    TenantEntity tenantEntity = TenantEntity.builder().settings(settings).build();

    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO().theming(new Theming().logo("logo"));
    //    when(authorisationService.hasRole(SINGLE_TENANT_ADMIN.getValue())).thenReturn(true);

    when(tenantFacadeChangeDetectionService.determineChangedSettings(tenantDTO, tenantEntity))
        .thenReturn(Lists.newArrayList());
    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        tenantDTO, tenantEntity);
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowOperation_When_ChangesInSettingsDetectedForWhichSingleTenantAdminHavePermissions() {
    // given
    TenantSettings tenantSettings = new TenantSettings();
    String settings = JsonConverter.convertToJson(tenantSettings);
    TenantEntity tenantEntity = TenantEntity.builder().settings(settings).build();

    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO().theming(new Theming().logo("logo"));
    when(authorisationService.hasRole(SINGLE_TENANT_ADMIN.getValue())).thenReturn(true);
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(false);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
    when(tenantFacadeChangeDetectionService.determineChangedSettings(tenantDTO, tenantEntity))
        .thenReturn(Lists.newArrayList());
    when(tenantFacadeChangeDetectionService.determineChangedSettings(tenantDTO, tenantEntity))
        .thenReturn(Lists.newArrayList(TenantSetting.ENABLE_TOPICS_IN_REGISTRATION));
    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        tenantDTO, tenantEntity);
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_NotAllowOperation_When_ChangesInSettingsDetectedForWhichSingleTenantAdminDoesNotPermissions() {
    // given
    TenantSettings tenantSettings = new TenantSettings();
    String settings = JsonConverter.convertToJson(tenantSettings);
    TenantEntity tenantEntity = TenantEntity.builder().settings(settings).build();

    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantDTO().theming(new Theming().logo("logo"));
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(false);
    when(tenantFacadeChangeDetectionService.determineChangedSettings(tenantDTO, tenantEntity))
        .thenReturn(Lists.newArrayList(TenantSetting.FEATURE_DEMOGRAPHICS_ENABLED));
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
    // when
    assertThrows(
        TenantAuthorisationException.class,
        () -> {
          tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
              tenantDTO, tenantEntity);
        });
  }

  // --- Appearance permission (TenantService#174) ------------------------------------------
  // The platform admin can take branding away from Träger admins. Until now that switch only
  // greyed out the admin panel's card, so the same change still went through over the API.

  private void givenSingleTenantAdmin() {
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
  }

  private void givenAppearanceToggle(Boolean appearance) {
    when(tenantAdminControlsService.getControls())
        .thenReturn(
            new TenantAdminControls()
                .allowedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().appearance(appearance)));
  }

  /** tenant-admin from tenant 0, as the platform admin's token carries it. */
  private void givenPlatformAdmin() {
    givenTenantAdminOf(0L);
  }

  /**
   * Every Träger admin holds tenant-admin, and with it GET_ALL_TENANTS, like the platform admin.
   */
  private void givenTenantAdminOf(long tokenTenantId) {
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(true);
    // isSuperAdmin() only asks for the role once the token names tenant 0.
    org.mockito.Mockito.lenient()
        .when(authorisationService.hasRole("tenant-admin"))
        .thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(tokenTenantId));
  }

  @Test
  void assertUserIsAuthorizedToAccessTenant_Should_rejectATraegerAdminOnAnotherTraeger() {
    givenTenantAdminOf(8L);

    assertThatThrownBy(
            () -> tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(7L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () -> tenantFacadeAuthorisationService.assertUserIsAuthorizedToReadTenant(7L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void assertUserIsAuthorizedToAccessTenant_Should_letATraegerAdminReachItsOwnTraeger() {
    givenTenantAdminOf(7L);

    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(7L);
  }

  @Test
  void assertUserIsAuthorizedToAccessTenant_Should_letThePlatformAdminReachEveryTraeger() {
    givenPlatformAdmin();

    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(7L);
    assertThat(tenantFacadeAuthorisationService.mayAccessEveryTenant()).isTrue();
  }

  @Test
  void mayAccessEveryTenant_Should_beFalse_When_theTokenCarriesNoTenant() {
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken())
        .thenThrow(new AccessDeniedException("tenantId attribute not found in the access token"));

    assertThat(tenantFacadeAuthorisationService.mayAccessEveryTenant()).isFalse();
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_rejectASubdomainChange_When_aTraegerAdminHoldsTenantAdmin() {
    givenTenantAdminOf(ID);
    TenantEntity existing = tenantWithLogo("same-logo");
    existing.setSubdomain("own");

    assertThatThrownBy(
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(
                        new MultilingualTenantDTO().subdomain("other"), existing))
        .isInstanceOf(TenantAuthorisationException.class);
  }

  private TenantEntity tenantWithLogo(String logo) {
    return TenantEntity.builder()
        .id(ID)
        .settings(JsonConverter.convertToJson(new TenantSettings()))
        .themingLogo(logo)
        .build();
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_RejectBrandingChange_When_AppearanceToggleIsOffForTenantAdmin() {
    // given
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    givenAppearanceToggle(false);

    // when / then
    TenantAuthorisationException thrown =
        assertThrows(
            TenantAuthorisationException.class,
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(changed, existing));

    // the reason matters: a regression that throws NOT_ALLOWED_TO_CHANGE_SUBDOMAIN, or any other
    // reason, would satisfy the exception type alone and still tell the caller the wrong thing
    assertThat(thrown.getCustomHttpHeaders().getFirst("X-Reason"))
        .isEqualTo(HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_APPEARANCE.name());
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowUnrelatedSave_When_AppearanceToggleIsOffButBrandingIsUnchanged() {
    // The admin panel submits whole tenant payloads, so every other card's save carries the
    // theming block too. Rejecting on its mere presence would lock a restricted Träger admin
    // out of settings that have nothing to do with branding.
    // given
    TenantEntity existing = tenantWithLogo("same-logo");
    MultilingualTenantDTO unchanged =
        new MultilingualTenantDTO().theming(new Theming().logo("same-logo"));
    givenSingleTenantAdmin();
    givenAppearanceToggle(false);

    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        unchanged, existing);

    // then no exception
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowBrandingChange_When_AppearanceToggleIsOn() {
    // given
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    givenAppearanceToggle(true);

    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        changed, existing);

    // then no exception
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowBrandingChange_When_ToggleIsAbsent() {
    // Legacy rows carry no controls at all. An absent toggle must keep meaning "allowed",
    // otherwise every existing Träger is silently locked out of its own branding.
    // given
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    givenAppearanceToggle(null);

    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        changed, existing);

    // then no exception
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowBrandingChange_When_UserIsSuperAdmin() {
    // given
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenPlatformAdmin();

    // when
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        changed, existing);

    // then no exception - the controls are never consulted for a super admin
    verify(tenantAdminControlsService, org.mockito.Mockito.never()).getControls();
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowBrandingChange_When_ControlsAreAbsentEntirely() {
    // The other absent branch: not a null toggle, but no controls row at all.
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    when(tenantAdminControlsService.getControls()).thenReturn(null);

    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        changed, existing);
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowBrandingChange_When_ToggleMapIsAbsent() {
    // Third absent branch: a controls row whose allowedPermissionToggles map is null.
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    when(tenantAdminControlsService.getControls()).thenReturn(new TenantAdminControls());

    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        changed, existing);
  }

  /**
   * The point of resolving per tenant. tenant_admin_controls is one platform-wide row, so reading
   * it alone applies a single global appearance toggle to every tenant. A tenant that overrode the
   * platform default must get its own answer.
   */
  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_AllowBrandingChange_When_TenantOverridesAPlatformDenial() {
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(true);

    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        changed, existing);

    // the platform row is not consulted once a resolved policy exists for this tenant
    verify(tenantAdminControlsService, org.mockito.Mockito.never()).getControls();
  }

  @Test
  void
      assertUserHasSufficientPermissionsToChangeAttributes_Should_RejectBrandingChange_When_TheTenantsResolvedPolicyDeniesIt() {
    TenantEntity existing = tenantWithLogo("old-logo");
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO().theming(new Theming().logo("new-logo"));
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);

    TenantAuthorisationException thrown =
        assertThrows(
            TenantAuthorisationException.class,
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(changed, existing));

    assertThat(thrown.getCustomHttpHeaders().getFirst("X-Reason"))
        .isEqualTo(HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_APPEARANCE.name());
  }

  private void givenResolvedAppearancePolicy(boolean allowed) {
    when(tenantPermissionPolicyService.getResolvedPolicies(ID))
        .thenReturn(
            java.util.Map.of(
                PermissionFeature.APPEARANCE.apiKey(),
                new ResolvedPolicyValue<>(allowed, PermissionPolicyMode.ENFORCED, false)));
  }

  private void platformAllowsTraegerLegalChanges(boolean allowed) {
    var settings =
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model
            .ApplicationSettingsDTO();
    settings.setLegalContentChangesBySingleTenantAdminsAllowed(
        new com.vi.tenantservice.applicationsettingsservice.generated.web.model.FeatureToggleDTO()
            .value(allowed));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(settings);
  }

  @Test
  void assertCanWriteLegalDraft_Should_refuseATraegerAdminWhileThePlatformForbidsLegalChanges() {
    when(authorisationService.hasRole("single-tenant-admin")).thenReturn(true);
    platformAllowsTraegerLegalChanges(false);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.CHANGE_LEGAL_CONTENT))
        .thenReturn(false);

    assertThrows(
        TenantAuthorisationException.class,
        () -> tenantFacadeAuthorisationService.assertCanWriteLegalDraft());
  }

  @Test
  void assertCanWriteLegalDraft_Should_allowATraegerAdminWhenThePlatformAllowsLegalChanges() {
    when(authorisationService.hasRole("single-tenant-admin")).thenReturn(true);
    platformAllowsTraegerLegalChanges(true);

    tenantFacadeAuthorisationService.assertCanWriteLegalDraft();
  }

  @Test
  void assertCanWriteLegalDraft_Should_allowAnAdminWhoMayChangeLegalContent() {
    when(authorisationService.hasRole("single-tenant-admin")).thenReturn(false);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.CHANGE_LEGAL_CONTENT))
        .thenReturn(true);

    tenantFacadeAuthorisationService.assertCanWriteLegalDraft();
  }

  @Test
  void isPlatformAdministrator_Should_rejectTheTechnicalUserEvenWithAPlatformShapedToken() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    when(technicalServiceIdentity.isCurrentCaller()).thenReturn(true);

    assertThat(tenantFacadeAuthorisationService.isPlatformAdministrator()).isFalse();
  }

  @Test
  void isPlatformAdministrator_Should_acceptThePlatformAdministrator() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);

    assertThat(tenantFacadeAuthorisationService.isPlatformAdministrator()).isTrue();
  }

  @Test
  void canAccessTenantById_Should_notGrantTheTechnicalIdentityForeignTenants() {
    org.mockito.Mockito.lenient().when(authorisationService.getUsername()).thenReturn("technical");
    org.mockito.Mockito.lenient().when(technicalServiceIdentity.isCurrentCaller()).thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));

    assertThat(tenantFacadeAuthorisationService.canAccessTenantById(Optional.of(7L))).isFalse();
  }

  @Test
  void canAccessTenantById_Should_notGrantAnythingWhenTheTokenHasNoTenant() {
    org.mockito.Mockito.lenient().when(authorisationService.getUsername()).thenReturn("technical");
    org.mockito.Mockito.lenient().when(technicalServiceIdentity.isCurrentCaller()).thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken())
        .thenThrow(new AccessDeniedException("no tenantId"));

    assertThat(tenantFacadeAuthorisationService.canAccessTenantById(Optional.of(7L))).isFalse();
  }

  @Test
  void isPlatformAdministrator_Should_neverDecideByUsername() {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    org.mockito.Mockito.lenient().when(authorisationService.getUsername()).thenReturn("technical");

    assertThat(tenantFacadeAuthorisationService.isPlatformAdministrator()).isTrue();
  }

  @Test
  void assertUserIsAuthorizedToReadTenant_Should_letTheTechnicalReadAuthorityReadAnyTenant() {
    when(authorisationService.hasAuthority(Authority.AuthorityValue.TECHNICAL_READ_TENANT))
        .thenReturn(true);

    assertThatCode(() -> tenantFacadeAuthorisationService.assertUserIsAuthorizedToReadTenant(7L))
        .doesNotThrowAnyException();
    verify(authorisationService, org.mockito.Mockito.never()).findTenantIdInAccessToken();
  }

  @Test
  void assertUserIsAuthorizedToReadTenant_Should_keepTheSingleTenantCheckForEveryoneElse() {
    when(authorisationService.hasAuthority(Authority.AuthorityValue.TECHNICAL_READ_TENANT))
        .thenReturn(false);
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(false);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(8L));

    assertThatThrownBy(
            () -> tenantFacadeAuthorisationService.assertUserIsAuthorizedToReadTenant(7L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void mayOnlyCreateReservedTenants_Should_dependOnTheFullCreateAuthority() {
    when(authorisationService.hasAuthority(Authority.AuthorityValue.CREATE_TENANT))
        .thenReturn(true, false);

    assertThat(tenantFacadeAuthorisationService.mayOnlyCreateReservedTenants()).isFalse();
    assertThat(tenantFacadeAuthorisationService.mayOnlyCreateReservedTenants()).isTrue();
  }

  @Test
  void restrictedTenantAdministratorCannotChangeAssistantIdentity() {
    TenantEntity existing = tenantWithLogo(null);
    MultilingualTenantDTO changed =
        new MultilingualTenantDTO()
            .theming(new Theming().assistantName("New helper").assistantIcon("robot-7341990"));
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
    var denied =
        assertThrows(
            TenantAuthorisationException.class,
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(changed, existing));
    assertThat(denied.getCustomHttpHeaders().getFirst("X-Reason"))
        .isEqualTo(HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_APPEARANCE.name());
  }

  @Test
  void restrictedAdministratorCanRoundTripBlankUnsetIdentity() {
    TenantEntity existing = tenantWithLogo(null);
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
    assertThatCode(
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(
                        new MultilingualTenantDTO()
                            .theming(new Theming().assistantName(" ").assistantIcon("")),
                        existing))
        .doesNotThrowAnyException();
  }

  @Test
  void restrictedAdministratorCanKeepOwnIdentity() {
    TenantEntity existing = tenantWithLogo(null);
    existing.setThemingAssistantName("Helper");
    existing.setThemingAssistantIcon("robot-7341990");
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
    assertThatCode(
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(
                        new MultilingualTenantDTO()
                            .theming(
                                new Theming()
                                    .assistantName("Helper")
                                    .assistantIcon("robot-7341990")),
                        existing))
        .doesNotThrowAnyException();
  }

  @Test
  void administratorCanChangeIdentityWhenAppearanceAllowed() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(true);
    assertThatCode(
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(
                        new MultilingualTenantDTO()
                            .theming(
                                new Theming()
                                    .assistantName("Helper")
                                    .assistantIcon("robot-7341990")),
                        tenantWithLogo(null)))
        .doesNotThrowAnyException();
  }

  // --- Everything the switch locks, not only branding (TenantService#345) ---------------------

  private void assertRefusedAsAppearance(MultilingualTenantDTO changed, TenantEntity existing) {
    var denied =
        assertThrows(
            TenantAuthorisationException.class,
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(changed, existing));
    assertThat(denied.getCustomHttpHeaders().getFirst("X-Reason"))
        .isEqualTo(HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_APPEARANCE.name());
  }

  private void assertAccepted(MultilingualTenantDTO submitted, TenantEntity existing) {
    assertThatCode(
            () ->
                tenantFacadeAuthorisationService
                    .assertUserHasSufficientPermissionsToChangeAttributes(submitted, existing))
        .doesNotThrowAnyException();
  }

  private TenantEntity storedTenant() {
    var tenant = tenantWithLogo(null);
    tenant.setName("Happylife");
    tenant.setContentClaim("{\"de\":\"Old slogan\"}");
    return tenant;
  }

  private MultilingualTenantDTO wholeObject() {
    return new MultilingualTenantDTO("Happylife")
        .content(new MultilingualContent().claim(Map.of("de", "Old slogan")))
        .theming(new Theming())
        .settings(new Settings());
  }

  @Test
  void restrictedAdministratorCannotChangeNameSloganOrLanguages() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);

    assertRefusedAsAppearance(wholeObject().name("Renamed"), storedTenant());
    assertRefusedAsAppearance(
        wholeObject().content(new MultilingualContent().claim(Map.of("de", "New slogan"))),
        storedTenant());
    assertRefusedAsAppearance(
        wholeObject().settings(new Settings().activeLanguages(List.of("de", "en"))),
        storedTenant());
  }

  @Test
  void restrictedAdministratorCannotChangeFormOfAddress() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
    when(consultingTypeService.getConsultingTypesByTenantId((int) ID))
        .thenReturn(new FullConsultingTypeResponseDTO().languageFormal(false));

    assertRefusedAsAppearance(
        wholeObject()
            .settings(
                new Settings().extendedSettings(new ConsultingTypePatchDTO().languageFormal(true))),
        storedTenant());
  }

  @Test
  void restrictedAdministratorCanSendTheWholeObjectUnchanged() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
    when(consultingTypeService.getConsultingTypesByTenantId((int) ID))
        .thenReturn(new FullConsultingTypeResponseDTO().languageFormal(false));
    var echoed =
        wholeObject()
            .theming(new Theming().loginEffect(Theming.LoginEffectEnum.NONE))
            .settings(
                new Settings()
                    // a tenant that never stored languages reads as German only
                    .activeLanguages(List.of("de"))
                    .extendedSettings(new ConsultingTypePatchDTO().languageFormal(false)));

    assertAccepted(echoed, storedTenant());
  }

  @Test
  void restrictedAdministratorCanReorderTheSameLanguages() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
    var stored = storedTenant();
    stored.setSettings(
        JsonConverter.convertToJson(
            TenantSettings.builder().activeLanguages(List.of("de", "en")).build()));

    assertAccepted(
        wholeObject().settings(new Settings().activeLanguages(List.of("en", "de"))), stored);
  }

  @Test
  void restrictedAdministratorCannotClearTheSloganByLeavingItOut() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);

    assertRefusedAsAppearance(wholeObject().content(new MultilingualContent()), storedTenant());
  }

  @Test
  void platformAdministratorChangesEverythingWithoutAnyLookup() {
    givenPlatformAdmin();

    assertAccepted(
        wholeObject()
            .name("Renamed")
            .settings(
                new Settings()
                    .activeLanguages(List.of("de", "en"))
                    .extendedSettings(new ConsultingTypePatchDTO().languageFormal(true))),
        storedTenant());
    org.mockito.Mockito.verifyNoInteractions(consultingTypeService, tenantPermissionPolicyService);
  }

  @Test
  void administratorChangesEverythingWhileEditingIsAllowed() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(true);

    assertAccepted(
        wholeObject()
            .name("Renamed")
            .settings(
                new Settings()
                    .activeLanguages(List.of("de", "en"))
                    .extendedSettings(new ConsultingTypePatchDTO().languageFormal(true))),
        storedTenant());
    org.mockito.Mockito.verifyNoInteractions(consultingTypeService);
  }

  // --- Review round: what the lock compares must be what gets stored ---------------------------

  private void givenRestrictedAdministrator() {
    givenSingleTenantAdmin();
    givenResolvedAppearancePolicy(false);
  }

  private MultilingualTenantDTO withClaim(Map<String, String> claim) {
    return wholeObject().content(new MultilingualContent().claim(claim));
  }

  @Test
  void restrictedAdministratorCannotAddABlankLanguageToTheSlogan() {
    // a blank entry is stored and shows blank instead of the German fallback
    givenRestrictedAdministrator();

    assertRefusedAsAppearance(withClaim(Map.of("de", "Old slogan", "en", "")), storedTenant());
  }

  @Test
  void restrictedAdministratorCannotDropAStoredLanguageOfTheSlogan() {
    // the Admin sends {de: x} only; saving that would silently lose the other languages
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setContentClaim("{\"de\":\"Old slogan\",\"en\":\"Old English slogan\"}");

    assertRefusedAsAppearance(withClaim(Map.of("de", "Old slogan")), stored);
  }

  @Test
  void restrictedAdministratorCanSendAMultiLanguageSloganUnchanged() {
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setContentClaim("{\"de\":\"Old slogan\",\"en\":\"Old English slogan\"}");

    assertAccepted(withClaim(Map.of("de", "Old slogan", "en", "Old English slogan")), stored);
  }

  @Test
  void restrictedAdministratorCannotClearAssistantIdentityWithBlanks() {
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setThemingAssistantName("Helper");
    stored.setThemingAssistantIcon("robot-7341990");

    assertRefusedAsAppearance(
        wholeObject().theming(new Theming().assistantName("").assistantIcon("robot-7341990")),
        stored);
    assertRefusedAsAppearance(
        wholeObject().theming(new Theming().assistantName("Helper").assistantIcon(" ")), stored);
  }

  @Test
  void restrictedAdministratorKeepsAssistantIdentityAndLoginEffectTheRequestLeavesOut() {
    // the signed-in Admin never sends them; the save must neither fail nor clear them
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setThemingAssistantName("Helper");
    stored.setThemingAssistantIcon("robot-7341990");
    stored.setThemingLoginEffect("CRACKS");
    var theming = new Theming();

    assertAccepted(wholeObject().theming(theming), stored);

    assertThat(theming.getAssistantName()).isEqualTo("Helper");
    assertThat(theming.getAssistantIcon()).isEqualTo("robot-7341990");
    assertThat(theming.getLoginEffect()).isEqualTo(Theming.LoginEffectEnum.CRACKS);
  }

  @Test
  void restrictedAdministratorCannotChangeTheLoginEffect() {
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setThemingLoginEffect("CRACKS");

    assertRefusedAsAppearance(
        wholeObject().theming(new Theming().loginEffect(Theming.LoginEffectEnum.NONE)), stored);
    assertRefusedAsAppearance(
        wholeObject().theming(new Theming().loginEffect(Theming.LoginEffectEnum.LINES)), stored);
  }

  @Test
  void restrictedAdministratorCanSendTheLoginEffectAsStored() {
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setThemingLoginEffect("CRACKS");

    assertAccepted(
        wholeObject().theming(new Theming().loginEffect(Theming.LoginEffectEnum.CRACKS)), stored);
  }

  @Test
  void restrictedAdministratorSeesAnUnknownStoredLoginEffectAsNone() {
    // TenantConverter#toLoginEffect serves an unknown stored value as NONE, so NONE is an echo
    givenRestrictedAdministrator();
    var stored = storedTenant();
    stored.setThemingLoginEffect("SPARKLES");

    assertAccepted(
        wholeObject().theming(new Theming().loginEffect(Theming.LoginEffectEnum.NONE)), stored);
    assertAccepted(wholeObject().theming(new Theming()), stored);
    assertRefusedAsAppearance(
        wholeObject().theming(new Theming().loginEffect(Theming.LoginEffectEnum.LINES)), stored);
  }

  @Test
  void anUnreadableFormOfAddressNeverLetsAChangeThrough() {
    givenRestrictedAdministrator();
    when(consultingTypeService.getConsultingTypesByTenantId((int) ID))
        .thenThrow(new org.springframework.web.client.RestClientException("consulting type down"));

    assertThrows(
        org.springframework.web.client.RestClientException.class,
        () ->
            tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
                wholeObject()
                    .settings(
                        new Settings()
                            .extendedSettings(new ConsultingTypePatchDTO().languageFormal(true))),
                storedTenant()));
  }

  @Test
  void restrictedAdministratorCanSaveWhatTheSignedInAdminSends() {
    // mapTenantDataToTenantAdminData: claim {de: x}, no extendedSettings, no login effect, no
    // assistant identity, empty logos instead of missing ones, null colours
    givenRestrictedAdministrator();
    var stored = storedTenant();
    var adminShape =
        new MultilingualTenantDTO("Happylife")
            .content(new MultilingualContent().claim(Map.of("de", "Old slogan")))
            .theming(new Theming().logo("").favicon("").primaryColor(null))
            .settings(new Settings().activeLanguages(List.of("de")));

    assertAccepted(adminShape, stored);
  }
}
