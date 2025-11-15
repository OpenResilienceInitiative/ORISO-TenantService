package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.exception.TenantAuthorisationException;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSetting;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.Theming;
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
}
