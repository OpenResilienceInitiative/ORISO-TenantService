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
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.policy.ResolvedPolicyValue;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.api.service.TenantPermissionPolicyService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.FeatureToggleDTO;
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

  private final @NonNull TenantAdminControlsService tenantAdminControlsService;

  private final @NonNull TenantPermissionPolicyService tenantPermissionPolicyService;

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

    FeatureToggleDTO legalContentChangesBySingleTenantAdminsAllowed =
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
    assertSingleTenantAdminMayChangeAppearance(sanitizedTenantDTO, existingTenant);
  }

  /**
   * The platform admin can take branding away from Träger admins via {@code
   * allowedPermissionToggles.appearance}. Until now that switch only greyed out the admin panel's
   * "Individuelle Bilder" card, so the same change still went through over the API. See
   * TenantService#174.
   *
   * <p>Only an explicit {@code false} restricts anything: legacy rows carry no controls at all, and
   * an absent toggle must keep meaning "allowed" rather than silently locking every existing Träger
   * out of its own branding.
   */
  private void assertSingleTenantAdminMayChangeAppearance(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenant) {
    if (appearanceChangesAllowedForTenantAdmins(existingTenant.getId())
        || !themingChanged(sanitizedTenantDTO, existingTenant)) {
      return;
    }

    logAndThrowTenantAuthorisationException(
        "Single tenant admin cannot change appearance for tenant with id: "
            + existingTenant.getId(),
        HttpStatusExceptionReason.NOT_ALLOWED_TO_CHANGE_APPEARANCE);
  }

  /**
   * Resolves the appearance permission <em>for this tenant</em>, not from the platform row alone.
   *
   * <p>{@code tenant_admin_controls} is a single platform-wide row on purpose - it has no {@code
   * tenant_id} - so reading it directly would apply one global appearance toggle to every tenant
   * and silently ignore a tenant-level override. Tenant scope lives in {@code
   * tenant_permission_policy} (ADR-013 platform -> tenant -> agency), which {@link
   * TenantPermissionPolicyService#getResolvedPolicies(Long)} resolves.
   *
   * <p>The legacy toggle map stays as the fallback for one transition release: a tenant whose
   * canonical policy has not been written yet still gets the platform answer it got before. Unknown
   * tenant or missing policy means allowed, matching the previous fail-open behaviour - this check
   * restricts a Träger admin, it does not grant anything.
   */
  private boolean appearanceChangesAllowedForTenantAdmins(Long tenantId) {
    if (tenantId != null) {
      ResolvedPolicyValue<Boolean> resolved =
          tenantPermissionPolicyService
              .getResolvedPolicies(tenantId)
              .get(PermissionFeature.APPEARANCE.apiKey());
      if (resolved != null) {
        return !Boolean.FALSE.equals(resolved.value());
      }
    }

    var controls = tenantAdminControlsService.getControls();
    if (controls == null || controls.getAllowedPermissionToggles() == null) {
      return true;
    }

    return !Boolean.FALSE.equals(controls.getAllowedPermissionToggles().getAppearance());
  }

  /**
   * Compares the submitted branding against what is stored. The admin panel sends whole tenant
   * payloads, so every other card's save carries the theming block too - rejecting on its mere
   * presence would lock a restricted Träger admin out of settings that have nothing to do with
   * branding. Only a real difference counts.
   */
  private boolean themingChanged(MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existing) {
    var theming = sanitizedTenantDTO.getTheming();
    if (theming == null) {
      return false;
    }

    return !Objects.equals(theming.getLogo(), existing.getThemingLogo())
        || !Objects.equals(theming.getFavicon(), existing.getThemingFavicon())
        || !Objects.equals(theming.getAssociationLogo(), existing.getThemingAssociationLogo())
        || !Objects.equals(theming.getPrimaryColor(), existing.getThemingPrimaryColor())
        || !Objects.equals(theming.getSecondaryColor(), existing.getThemingSecondaryColor())
        || !Objects.equals(theming.getAccent(), existing.getThemingAccent())
        || !Objects.equals(theming.getSignal(), existing.getThemingSignal())
        || !Objects.equals(loginEffectValue(theming), existing.getThemingLoginEffect());
  }

  /** Mirrors TenantConverter: the effect is stored as the enum name, null stays null. */
  private String loginEffectValue(Theming theming) {
    return theming.getLoginEffect() == null ? null : theming.getLoginEffect().getValue();
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
    return canAccessTenantById(tenant.map(TenantEntity::getId));
  }

  public boolean canAccessTenantById(Optional<Long> tenantId) {
    if (multitenancyWithSingleDomain || isSuperAdmin()) {
      return true;
    }

    if (tenantId.isEmpty()) {
      return false;
    }

    try {
      var tenantIdInAccessToken = authorisationService.findTenantIdInAccessToken();
      boolean result = tenantMatching(tenantId.get(), tenantIdInAccessToken);

      // Temporary workaround: always return true for technical user
      if (isTechnicalUser()) {
        return true;
      }

      return result;
    } catch (Exception e) {
      log.debug("Could not determine tenant access from access token", e);
      // Temporary workaround: always return true for technical user
      return isTechnicalUser();
    }
  }

  public boolean isSuperAdmin() {
    try {
      Optional<Long> tenantId = authorisationService.findTenantIdInAccessToken();
      return tenantId.filter(id -> id.equals(0L)).isPresent()
          && authorisationService.hasRole("tenant-admin");
    } catch (Exception e) {
      log.debug("Could not determine tenant id from access token while checking super admin", e);
      return false;
    }
  }

  private boolean isTechnicalUser() {
    try {
      return "technical".equals(authorisationService.getUsername());
    } catch (Exception e) {
      log.debug("Could not determine username from access token while checking technical user", e);
      return false;
    }
  }
}
