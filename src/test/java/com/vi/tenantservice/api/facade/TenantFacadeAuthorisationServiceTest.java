package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.exception.TenantAuthorisationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
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
import com.vi.tenantservice.api.util.JsonConverter;
import com.vi.tenantservice.config.security.AuthorisationService;
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

  @Mock TenantFacadeChangeDetectionService tenantFacadeChangeDetectionService;

  @Mock ApplicationSettingsService applicationSettingsService;

  @Mock TenantAdminControlsService tenantAdminControlsService;

  @Mock TenantPermissionPolicyService tenantPermissionPolicyService;

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
    when(authorisationService.getUsername())
        .thenThrow(new AccessDeniedException("Invalid encoded username claim in JWT token"));

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
    when(authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS))
        .thenReturn(true);

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
}
