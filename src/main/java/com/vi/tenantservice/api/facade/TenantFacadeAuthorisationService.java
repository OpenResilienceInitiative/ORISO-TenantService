package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_LICENSING;
import static com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_SUBDOMAIN;

import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.exception.TenantAuthorisationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.TenantContent;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSetting;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantFacadeAuthorisationService {

  private final @NonNull AuthorisationService authorisationService;
  private final @NonNull TenantFacadeChangeDetectionService tenantFacadeChangeDetectionService;

  private final @NonNull ApplicationSettingsService applicationSettingsService;

  @Value("${feature.multitenancy.with.single.domain.enabled}")
  private boolean multitenancyWithSingleDomain;

  private boolean userHasAnyRoleOf(List<UserRole> roles) {
    return roles.stream().anyMatch(userRole -> authorisationService.hasRole(userRole.getValue()));
  }

  private boolean tenantMatching(Long id, Optional<Long> tenantId) {
    return tenantId.isPresent() && tenantId.get().equals(id);
  }

  void assertUserIsAuthorizedToAccessTenant(Long tenantId) {
    log.info("Asserting user is authorized to update tenant with id " + tenantId);
    if (hasSingleTenantAccessAuthority()) {
      log.info(
          "User has single tenant permission. Checking if he has authority to access tenant with id "
              + tenantId);
      var tenantIdFromAccessToken = authorisationService.findTenantIdInAccessToken();
      if (!tenantMatching(tenantId, tenantIdFromAccessToken)) {
        throw new AccessDeniedException(
            "User "
                + authorisationService.getUsername()
                + " not authorized to edit tenant with id: "
                + tenantId);
      }
    }
  }

  private boolean hasSingleTenantAccessAuthority() {
    return !authorisationService.hasAuthority(Authority.AuthorityValue.GET_ALL_TENANTS);
  }

  void assertUserHasSufficientPermissionsToChangeAttributes(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    if (hasSingleTenantAccessAuthority()) {
      assertSingleTenantAdminHasPermissionsToChangeAttributes(sanitizedTenantDTO, existingTenant);
    }
    List<TenantSetting> changedSettingTypes =
        tenantFacadeChangeDetectionService.determineChangedSettings(
            sanitizedTenantDTO, existingTenant);
    log.info("Detected the following changes in setting attributes: " + changedSettingTypes);
    assertUserHasPermissionsToChangeSettings(changedSettingTypes);
    List<TenantContent> changedContentTypes =
        tenantFacadeChangeDetectionService.determineChangedContent(
            sanitizedTenantDTO, existingTenant);
    log.info("Detected the following changes in content attributes: " + changedContentTypes);
    assertUserHasPermissionsToChangeContent(changedContentTypes);
  }

  private void assertUserHasPermissionsToChangeContent(List<TenantContent> changedContentTypes) {
    if (!changedContentTypes.isEmpty()) {
      changedContentTypes.forEach(this::assertUserHasPermissionsToChangeContent);
    }
  }

  private void assertUserHasPermissionsToChangeSettings(List<TenantSetting> changedSettings) {
    if (!changedSettings.isEmpty()) {
      changedSettings.forEach(this::assertUserHasPermissionsToChangeContent);
    }
  }

  private void assertUserHasPermissionsToChangeContent(TenantContent tenantContent) {
    if (!isAllowedToEditLegalContent()) {
      String msg = "User does not have permissions to change content: " + tenantContent.name();
      logAndThrowTenantAuthorisationException(
          msg, HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_LEGAL_CONTENT);
    }
  }

  private boolean isAllowedToEditLegalContent() {
    return (authorisationService.hasRole("single-tenant-admin")
            && singleTenantAdminCanEditLegalTexts())
        || authorisationService.hasAuthority(Authority.AuthorityValue.CHANGE_LEGAL_CONTENT);
  }

  private boolean singleTenantAdminCanEditLegalTexts() {
    var applicationSettings = applicationSettingsService.getApplicationSettings();

    ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled
        legalContentChangesBySingleTenantAdminsAllowed =
            applicationSettings.getLegalContentChangesBySingleTenantAdminsAllowed();

    if (legalContentChangesBySingleTenantAdminsAllowed != null) {
      return legalContentChangesBySingleTenantAdminsAllowed.getValue();
    } else {
      log.warn(
          "No value for setting legalContentChangesBySingleTenantAdminsAllowed found. Setting it to false.");
      return false;
    }
  }

  private void assertUserHasPermissionsToChangeContent(TenantSetting tenantSetting) {
    if (!userHasAnyRoleOf(tenantSetting.getRolesAuthorisedToChange())) {
      String msg = "User does not have permissions to change setting: " + tenantSetting.name();
      logAndThrowTenantAuthorisationException(
          msg, HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_SETTING);
    }
  }

  private void assertSingleTenantAdminHasPermissionsToChangeAttributes(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {

    if (!Objects.equals(sanitizedTenantDTO.getSubdomain(), existingTenant.getSubdomain())) {
      logAndThrowTenantAuthorisationException(
          "Single tenant admin cannot change subdomain", NOT_ALLOWED_TO_CHANGE_SUBDOMAIN);
    }
    assertSingleTenantAdminDoesNotTryToChangeLicensingInformation(
        sanitizedTenantDTO, existingTenant);
  }

  private void assertSingleTenantAdminDoesNotTryToChangeLicensingInformation(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    if (isAttemptToDeleteExistingLicensingInformation(sanitizedTenantDTO, existingTenant)) {
      logAndThrowTenantAuthorisationException(
          "Single tenant admin cannot delete licensing", NOT_ALLOWED_TO_CHANGE_LICENSING);
    }

    if (sanitizedTenantDTO.getLicensing() != null
        && licensingChanged(sanitizedTenantDTO, existingTenant)) {
      logAndThrowTenantAuthorisationException(
          "Single tenant admin cannot change allowed number of users",
          NOT_ALLOWED_TO_CHANGE_LICENSING);
    }
  }

  private boolean licensingChanged(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    return !Objects.equals(
        sanitizedTenantDTO.getLicensing().getAllowedNumberOfUsers(),
        existingTenant.getLicensingAllowedNumberOfUsers());
  }

  private boolean isAttemptToDeleteExistingLicensingInformation(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    return sanitizedTenantDTO.getLicensing() == null
        && existingTenant.getLicensingAllowedNumberOfUsers() != null;
  }

  private void logAndThrowTenantAuthorisationException(
      String msg, HttpStatusExceptionReason reason) {
    log.warn(msg);
    throw new TenantAuthorisationException(msg, reason);
  }

  public boolean canAccessTenant(Optional<TenantEntity> tenant) {
    if (isSuperAdmin() || multitenancyWithSingleDomain) {
      return true;
    }

    if (tenant.isEmpty()) {
      return false;
    }

    try {
      var tenantIdInAccessToken = authorisationService.findTenantIdInAccessToken();
      boolean result = tenantMatching(tenant.get().getId(), tenantIdInAccessToken);

      // Temporary workaround: always return true for technical user
      if (authorisationService.getUsername().equals("technical")) {
        return true;
      }

      return result;
    } catch (Exception e) {
      // Temporary workaround: always return true for technical user
      if (authorisationService.getUsername().equals("technical")) {
        return true;
      }
      return false;
    }
  }

  public boolean isSuperAdmin() {
    var tenantId = authorisationService.findTenantIdInAccessToken();
    if (tenantId.isEmpty() || !tenantId.get().equals(0L)) {
      return false;
    }
    return authorisationService.hasRole("tenant-admin");
  }
}
