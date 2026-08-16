package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.util.JsonConverter.convertFromJson;
import static com.vi.tenantservice.api.util.JsonConverter.convertToJson;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.authorisation.Authority.AuthorityValue;
import com.vi.tenantservice.api.converter.ConsultingTypePatchDTOConverter;
import com.vi.tenantservice.api.converter.EffectivePermissionSettingsApplier;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.ConsultingTypeCreationException;
import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.exception.TenantIdAllocationExhaustedException;
import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.OnboardingDpaAcceptanceDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantData;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantPermissionPolicies;
import com.vi.tenantservice.api.model.TenantRestrictedData;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.service.SingleDomainTenantOverrideService;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.api.service.TenantDpaStatusService;
import com.vi.tenantservice.api.service.TenantDpaStatusService.AdminSignatureForm;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.TenantPermissionPolicyService;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.service.TranslationService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.validation.TenantInputSanitizer;
import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO;
import jakarta.ws.rs.BadRequestException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Facade to encapsulate services and logic needed to manage tenants */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceFacade {

  private static final int TECHNICAL_TENANT_ID = 0;

  /** AUTO create retries share the allocation service's retry budget (TEN-INV hardening). */
  private static final int MAX_AUTO_ID_CREATE_ATTEMPTS =
      TenantIdAllocationService.MAX_AUTO_ATTEMPTS;

  /** Column widths of tenant_dpa_admin_signature (changeset 0025) and the API contract. */
  private static final int MAX_SIGNER_FIELD_LENGTH = 255;

  private static final int MAX_LANGUAGE_LENGTH = 10;

  private final @NonNull TenantService tenantService;
  private final @NonNull TenantIdAllocationService tenantIdAllocationService;
  private final @NonNull TenantConverter tenantConverter;
  private final @NonNull TenantInputSanitizer tenantInputSanitizer;
  private final @NonNull TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  private final @NonNull AuthorisationService authorisationService;
  private final @NonNull TranslationService translationService;
  private final @NonNull ConsultingTypeService consultingTypeService;
  private final @NonNull SubdomainExtractor subdomainExtractor;
  private final @NonNull ApplicationSettingsService applicationSettingsService;

  private final @NonNull UserAdminService userAdminService;

  private final @NonNull ConsultingTypePatchDTOConverter consultingTypePatchDTOConverter;

  private final @NonNull TenantFacadeDependentSettingsOverrideService
      tenantFacadeDependentSettingsOverrideService;

  private final @NonNull TenantAdminControlsService tenantAdminControlsService;

  private final @NonNull TenantPermissionPolicyService tenantPermissionPolicyService;

  private final @NonNull EffectivePermissionSettingsApplier effectivePermissionSettingsApplier;

  private final @NonNull TenantResolverService tenantResolverService;

  private final @NonNull TenantDpaStatusService tenantDpaStatusService;

  private final @NonNull SingleDomainTenantOverrideService singleDomainTenantOverrideService;

  @Value("${feature.multitenancy.with.single.domain.enabled}")
  private boolean multitenancyWithSingleDomain;

  public MultilingualTenantDTO createTenant(MultilingualTenantDTO tenantDTO) {
    log.info("Creating new tenant");
    MultilingualTenantDTO sanitizedTenantDTO = tenantInputSanitizer.sanitize(tenantDTO);
    validateCreateTenantInput(tenantDTO);
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnCreate(
        sanitizedTenantDTO);
    tenantAdminControlsService.stripTenantAdminControlsFromTenantDto(sanitizedTenantDTO);
    var entity = tenantConverter.toEntity(sanitizedTenantDTO);
    populateTenantSettingsAndActivationDates(entity, tenantDTO);
    String reservationToken = tenantDTO.getTenantIdReservationToken();
    TenantEntity createdTenant = createWithIdAllocationRetry(entity, reservationToken);
    try {
      createDefaultConsultingTypeSettings(createdTenant);
    } catch (ConsultingTypeCreationException ex) {
      performRollback(createdTenant, reservationToken);
      log.error(
          "Error while creating consulting types for tenant with id {}", createdTenant.getId(), ex);
      throw new BadRequestException(
          "Error while creating consulting types for tenant with id " + createdTenant.getId());
    }
    recordOnboardingDpaAcceptance(
        createdTenant, tenantDTO.getOnboardingDpaAcceptance(), reservationToken);
    var createdTenantDto = tenantConverter.toMultilingualDTO(createdTenant);
    tenantAdminControlsService.enrichTenantDtoWithTenantAdminControls(createdTenantDto);
    return createdTenantDto;
  }

  /**
   * Persists the DPA acceptance a tenant admin gave while onboarding through a public invite link
   * (#569, TEN-INV-U9) as the tenant's append-only admin signature.
   *
   * <p>This runs INSIDE the tenant creation on purpose. The acceptance is the signature that makes
   * the new tenant's DPA status VALID, so a tenant must never exist without it — otherwise the
   * freshly onboarded admin logs in straight into the non-bypassable DPA blocker (U10) with nothing
   * to act on. Recording it from the caller after the creation returned could not be compensated:
   * the caller cannot restore a consumed ID reservation, whereas failing here reuses the very same
   * rollback the consulting-type provisioning uses — tenant row deleted, reservation restored to
   * RESERVED with its original token, so the invite link stays usable for a retry.
   */
  private void recordOnboardingDpaAcceptance(
      TenantEntity createdTenant, OnboardingDpaAcceptanceDTO acceptance, String reservationToken) {
    if (acceptance == null) {
      return;
    }
    try {
      if (!Boolean.TRUE.equals(acceptance.getAccepted())
          || isBlank(acceptance.getSignerUserId())
          || isBlank(acceptance.getSignerName())) {
        throw new BadRequestException(
            "onboardingDpaAcceptance requires accepted=true, signerUserId and signerName");
      }
      tenantDpaStatusService.signOnboarding(
          createdTenant.getId(),
          bounded(acceptance.getSignerUserId(), MAX_SIGNER_FIELD_LENGTH, "signerUserId"),
          bounded(acceptance.getSignerUsername(), MAX_SIGNER_FIELD_LENGTH, "signerUsername"),
          parseDpaVersion(acceptance.getDpaVersion()),
          new AdminSignatureForm(
              bounded(acceptance.getSignerName(), MAX_SIGNER_FIELD_LENGTH, "signerName"),
              bounded(acceptance.getSignerPosition(), MAX_SIGNER_FIELD_LENGTH, "signerPosition"),
              bounded(acceptance.getSignerEmail(), MAX_SIGNER_FIELD_LENGTH, "signerEmail"),
              bounded(
                  acceptance.getSignerOrganisation(),
                  MAX_SIGNER_FIELD_LENGTH,
                  "signerOrganisation"),
              bounded(acceptance.getLanguage(), MAX_LANGUAGE_LENGTH, "language"),
              buildOnboardingFormDataJson(acceptance)));
    } catch (RuntimeException ex) {
      performRollback(createdTenant, reservationToken);
      log.error(
          "Onboarding DPA acceptance could not be recorded for tenant {} — the tenant creation was"
              + " rolled back so the acceptance is never silently lost",
          createdTenant.getId(),
          ex);
      throw ex;
    }
  }

  /** Verbatim JSON snapshot of the submitted onboarding acceptance (audit evidence). */
  private String buildOnboardingFormDataJson(OnboardingDpaAcceptanceDTO acceptance) {
    var formData = new LinkedHashMap<String, Object>();
    formData.put("signerName", acceptance.getSignerName());
    formData.put("signerPosition", acceptance.getSignerPosition());
    formData.put("signerEmail", acceptance.getSignerEmail());
    formData.put("signerOrganisation", acceptance.getSignerOrganisation());
    formData.put("language", acceptance.getLanguage());
    formData.put("accepted", acceptance.getAccepted());
    formData.put("dpaVersion", acceptance.getDpaVersion());
    formData.put("source", "PUBLIC_TENANT_ADMIN_ONBOARDING");
    return convertToJson(formData);
  }

  /**
   * The signer fields are PLAIN TEXT of an append-only legal record, so they are stored exactly as
   * the signer submitted them (#569). Running them through an HTML sanitizer entity-encoded the
   * audit trail — a stored value that no longer matches what was signed. They are never interpreted
   * as markup here; consumers encode them at RENDER time (the admin UI sanitizes its own output).
   *
   * <p>Verbatim is not unbounded: a value longer than its column is rejected rather than silently
   * truncated by the database. The API contract bounds these fields too ({@code maxLength} in
   * tenantservice.yaml); this guard keeps the invariant when the facade is reached directly.
   */
  private static String bounded(String value, int maxLength, String field) {
    if (value != null && value.length() > maxLength) {
      throw new BadRequestException(
          "onboardingDpaAcceptance." + field + " exceeds " + maxLength + " characters");
    }
    return value;
  }

  private static LocalDateTime parseDpaVersion(String dpaVersion) {
    if (isBlank(dpaVersion)) {
      return null;
    }
    try {
      return LocalDateTime.parse(dpaVersion.trim());
    } catch (DateTimeParseException ex) {
      throw new BadRequestException("onboardingDpaAcceptance.dpaVersion is not a valid timestamp");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Creates the tenant with authoritative ID allocation (TEN-INV-U1). In AUTO mode (no ID in the
   * request) a lost race for the smallest free ID is retried with the next candidate; exhausting
   * the retry budget is a server-side contention condition and surfaces as HTTP 503. A manual ID
   * conflict is never retried and surfaces as HTTP 409.
   */
  private TenantEntity createWithIdAllocationRetry(TenantEntity entity, String reservationToken) {
    boolean autoMode = entity.getId() == null;
    int attempts = autoMode ? MAX_AUTO_ID_CREATE_ATTEMPTS : 1;
    TenantIdAllocationConflictException lastConflict = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return tenantService.create(entity, reservationToken);
      } catch (TenantIdAllocationConflictException ex) {
        lastConflict = ex;
        if (autoMode) {
          log.info("Lost the race for an AUTO tenant ID (attempt {}), retrying", attempt);
          entity.setId(null);
        }
      }
    }
    if (autoMode) {
      throw new TenantIdAllocationExhaustedException(
          "Could not allocate an AUTO tenant ID after " + attempts + " attempts", lastConflict);
    }
    throw lastConflict;
  }

  /**
   * Compensates a tenant creation whose consulting-type provisioning failed after commit. When the
   * creation consumed an open invite's reservation, the ledger row is restored to RESERVED with its
   * original token (the invite keeps its ID); a row created fresh in the failed creation is
   * deleted. If the tenant row itself cannot be deleted, the ASSIGNED ledger row is deliberately
   * kept: the ID is still occupied by the surviving tenant row, and deleting or restoring the
   * ledger row would let a second allocation hand out the same ID twice.
   */
  private void performRollback(TenantEntity createdTenant, String reservationToken) {
    try {
      tenantService.delete(createdTenant);
    } catch (RuntimeException ex) {
      log.error(
          "Rollback of tenant {} could not delete the tenant row; keeping its ASSIGNED ledger row"
              + " so the ledger stays consistent with the surviving tenant",
          createdTenant.getId(),
          ex);
      return;
    }
    tenantIdAllocationService.rollbackAssignment(createdTenant.getId(), reservationToken);
  }

  private void populateTenantSettingsAndActivationDates(
      TenantEntity entity, MultilingualTenantDTO tenantDTO) {
    setContentActivationDates(entity, tenantDTO);
    setDefaultTenantSettings(entity);
  }

  private void setDefaultTenantSettings(TenantEntity tenant) {
    var defaultTenantSettings = tenantService.getDefaultTenantSettings();
    tenant.setSettings(convertToJson(defaultTenantSettings));
  }

  private void createDefaultConsultingTypeSettings(TenantEntity createdTenant)
      throws ConsultingTypeCreationException {
    try {
      consultingTypeService.createDefaultConsultingTypes(createdTenant.getId());
    } catch (RestClientException ex) {
      throw new ConsultingTypeCreationException(
          "Consulting types could not be created for tenant with id " + createdTenant.getId(), ex);
    }
    if (isAttemptToCreateFirstNonTechnicalTenant(createdTenant.getId())) {
      validateSubDomain(createdTenant.getSubdomain());
      try {
        applicationSettingsService.saveMainTenantSubDomain(createdTenant.getSubdomain());
      } catch (RestClientException ex) {
        throw new ConsultingTypeCreationException(
            "Main tenant subdomain could not be saved for tenant with id " + createdTenant.getId(),
            ex);
      }
    }
  }

  private void validateSubDomain(String subdomain) {
    Optional<String> subDomainFromUrl = subdomainExtractor.getCurrentSubdomain();
    if (subDomainFromUrl.isPresent() && !subdomain.equals(subDomainFromUrl.get())) {
      throw new TenantValidationException(
          HttpStatusExceptionReason.SUBDOMAIN_IN_REQUEST_BODY_NOT_EQUAL_TO_SUBDOMAIN_IN_URL);
    }
  }

  public MultilingualTenantDTO updateTenant(Long id, MultilingualTenantDTO tenantDTO) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(id);
    validateTenantInput(tenantDTO);
    MultilingualTenantDTO sanitizedTenantDTO = tenantInputSanitizer.sanitize(tenantDTO);

    log.info("Attempting to update tenant with id {}", id);
    return updateWithSanitizedInput(id, sanitizedTenantDTO);
  }

  public void deleteTenant(Long id) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(id);

    if (id == TECHNICAL_TENANT_ID) {
      throw new BadRequestException("Technical tenant cannot be deleted.");
    }

    TenantEntity tenant =
        tenantService
            .findTenantById(id)
            .orElseThrow(() -> new TenantNotFoundException("Tenant with id " + id + " not found"));

    tenantService.delete(tenant);
  }

  private boolean isAttemptToCreateFirstNonTechnicalTenant(Long tenantId) {
    return tenantId != 0L && multitenancyWithSingleDomain && onlyTechnicalTenantExists();
  }

  private boolean onlyTechnicalTenantExists() {
    List<TenantData> tenants = tenantService.getAllTenantData();
    return tenants.size() == 1 && tenants.get(0).getId().equals(0L);
  }

  private void validateTenantInput(MultilingualTenantDTO tenantDTO) {
    var isoCountries = Arrays.stream(Locale.getISOLanguages()).toList();
    validateContent(tenantDTO, isoCountries);
    validateSettings(tenantDTO.getSettings());
  }

  private void validateSettings(Settings settings) {
    if (settings != null && settings.getActiveLanguages() != null) {
      validateEachLanguageHasCorrectFormat(settings.getActiveLanguages());
    }
  }

  private void validateEachLanguageHasCorrectFormat(List<String> activeLanguages) {
    List<String> invalidLanguages =
        activeLanguages.stream()
            .filter(language -> language == null || language.length() != 2)
            .toList();
    if (!invalidLanguages.isEmpty()) {
      throw new TenantValidationException(
          HttpStatusExceptionReason.ID_MUST_BE_NULL_WHEN_CREATING_TENANT);
    }
  }

  private void validateCreateTenantInput(MultilingualTenantDTO tenantDTO) {
    // A tenant ID in the request switches the creation to manual allocation mode; it is
    // re-validated against the allocation ledger inside the creating transaction (TEN-INV-U1),
    // so no upfront ID check happens here.
    validateTenantInput(tenantDTO);
  }

  private void validateContent(MultilingualTenantDTO tenantDTO, List<String> isoCountries) {
    if (tenantDTO.getContent() != null) {
      validateTranslationKeys(
          isoCountries, getLanguageLowercaseKeys(tenantDTO.getContent().getImpressum()));
      validateTranslationKeys(
          isoCountries, getLanguageLowercaseKeys(tenantDTO.getContent().getPrivacy()));
      validateTranslationKeys(
          isoCountries, getLanguageLowercaseKeys(tenantDTO.getContent().getTermsAndConditions()));
      validateTranslationKeys(
          isoCountries, getLanguageLowercaseKeys(tenantDTO.getContent().getClaim()));
    }
  }

  private void validateTranslationKeys(List<String> isoCountries, List<String> keys) {
    boolean hasInvalidKey =
        keys.stream().anyMatch(key -> !isValidTranslationKey(isoCountries, key));
    if (hasInvalidKey) {
      throw new TenantValidationException(HttpStatusExceptionReason.LANGUAGE_KEY_NOT_VALID);
    }
  }

  private boolean isValidTranslationKey(List<String> isoCountries, String key) {
    if (isoCountries.contains(key)) {
      return true;
    }

    String metadataSuffix = "__meta";
    return key.endsWith(metadataSuffix)
        && isoCountries.contains(key.substring(0, key.length() - metadataSuffix.length()));
  }

  private static List<String> getLanguageLowercaseKeys(Map<String, String> translatedMap) {
    if (translatedMap == null) {
      return Lists.newArrayList();
    }
    return translatedMap.keySet().stream().map(String::toLowerCase).toList();
  }

  private MultilingualTenantDTO updateWithSanitizedInput(
      Long id, MultilingualTenantDTO sanitizedTenantDTO) {
    var tenantById = tenantService.findTenantById(id);
    if (tenantById.isPresent()) {
      return updateExistingTenant(sanitizedTenantDTO, tenantById.get());
    } else {
      throw new TenantNotFoundException("Tenant with given id could not be found : " + id);
    }
  }

  private void updateExtendedSettingsAsConsultingType(
      MultilingualTenantDTO sanitizedTenantDTO, Long tenantId) {
    FullConsultingTypeResponseDTO consultingTypesByTenantId =
        consultingTypeService.getConsultingTypesByTenantId(tenantId.intValue());

    if (sanitizedTenantDTO.getSettings() != null
        && sanitizedTenantDTO.getSettings().getExtendedSettings() != null) {
      if (extendedTenantSettingsChanged(
          consultingTypesByTenantId, sanitizedTenantDTO.getSettings().getExtendedSettings())) {
        consultingTypeService.patchConsultingType(
            consultingTypesByTenantId.getId(),
            consultingTypePatchDTOConverter.convertToConsultingTypeServiceModel(
                sanitizedTenantDTO.getSettings().getExtendedSettings()));
      } else {
        log.debug(
            "Skipping consulting types update during tenant update, these settings did not change");
      }
    }
  }

  private boolean extendedTenantSettingsChanged(
      FullConsultingTypeResponseDTO consultingTypesByTenantId,
      ConsultingTypePatchDTO newExtendedTenantSettings) {
    ConsultingTypePatchDTO existingExtendedTenantSettings =
        consultingTypePatchDTOConverter.convertConsultingTypePatchDTO(consultingTypesByTenantId);
    return !nullSafeEquals(newExtendedTenantSettings, existingExtendedTenantSettings);
  }

  private MultilingualTenantDTO updateExistingTenant(
      MultilingualTenantDTO sanitizedTenantDTO, TenantEntity existingTenantEntity) {
    tenantFacadeAuthorisationService.assertUserHasSufficientPermissionsToChangeAttributes(
        sanitizedTenantDTO, existingTenantEntity);
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnUpdate(
        sanitizedTenantDTO, existingTenantEntity);
    tenantAdminControlsService.stripTenantAdminControlsFromTenantDto(sanitizedTenantDTO);
    // toEntity mutates existingTenantEntity, so capture the stored settings first
    var existingSettingsJson = existingTenantEntity.getSettings();
    var updatedEntity = tenantConverter.toEntity(existingTenantEntity, sanitizedTenantDTO);
    preserveStoredSmtpPassword(existingSettingsJson, updatedEntity);
    setContentActivationDates(updatedEntity, sanitizedTenantDTO);
    updatedEntity = tenantService.update(updatedEntity);
    updateExtendedSettingsAsConsultingType(sanitizedTenantDTO, existingTenantEntity.getId());
    log.info("Tenant with id {} updated", existingTenantEntity.getId());
    return getConvertedAndEnrichedTenant(updatedEntity);
  }

  /**
   * The API is write-only for the SMTP password (#182): clients send it blank, absent or masked
   * when it should stay unchanged, so the stored value must survive the settings full-replace.
   */
  private void preserveStoredSmtpPassword(String existingSettingsJson, TenantEntity updatedEntity) {
    if (existingSettingsJson == null || updatedEntity.getSettings() == null) {
      return;
    }
    TenantSettings updatedSettings = convertFromJson(updatedEntity.getSettings());
    if (updatedSettings.getSmtp() == null
        || nonNull(updatedSettings.getSmtp().getPassword())
            && !updatedSettings.getSmtp().getPassword().isBlank()) {
      return;
    }
    TenantSettings existingSettings = convertFromJson(existingSettingsJson);
    if (existingSettings.getSmtp() == null
        || existingSettings.getSmtp().getPassword() == null
        || existingSettings.getSmtp().getPassword().isBlank()) {
      return;
    }
    updatedSettings.getSmtp().setPassword(existingSettings.getSmtp().getPassword());
    updatedEntity.setSettings(convertToJson(updatedSettings));
  }

  private void setContentActivationDates(TenantEntity entity, MultilingualTenantDTO tenantDTO) {
    MultilingualContent content = tenantDTO.getContent();

    if (content == null) {
      return;
    }

    if (content.getConfirmPrivacy() != null && content.getConfirmPrivacy()) {
      entity.setContentPrivacyActivationDate(LocalDateTime.now());
    }

    if (content.getConfirmTermsAndConditions() != null && content.getConfirmTermsAndConditions()) {
      entity.setContentTermsAndConditionsActivationDate(LocalDateTime.now());
    }
  }

  public Optional<TenantDTO> findTenantById(Long id) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(id);
    var tenantById = tenantService.findTenantDataById(id);
    if (tenantById.isEmpty()) {
      return Optional.empty();
    }
    var tenantDTO =
        tenantConverter.toDTO(tenantById.get(), translationService.getCurrentLanguageContext());
    tenantAdminControlsService.enrichTenantDtoWithTenantAdminControls(tenantDTO);
    return Optional.of(tenantDTO);
  }

  private MultilingualTenantDTO getConvertedAndEnrichedTenant(TenantData tenantEntity) {
    var multilingualTenantDTO = tenantConverter.toMultilingualDTO(tenantEntity);
    tenantAdminControlsService.enrichTenantDtoWithTenantAdminControls(multilingualTenantDTO);
    enrichWithAdminDataIfSuperadmin(multilingualTenantDTO);
    enrichWithConsultingTypeSettings(multilingualTenantDTO, tenantEntity.getId());
    return multilingualTenantDTO;
  }

  public TenantAdminControls getTenantAdminControls() {
    assertSuperAdmin();
    return tenantAdminControlsService.getControls();
  }

  public TenantAdminControls updateTenantAdminControls(TenantAdminControls tenantAdminControls) {
    assertSuperAdmin();
    return tenantAdminControlsService.updateControls(tenantAdminControls);
  }

  public TenantPermissionPolicies getTenantPermissionPolicies(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    return toTenantPermissionPolicies(
        tenantId,
        tenantPermissionPolicyService.getResolvedPolicies(tenantId),
        tenantPermissionPolicyService.getResolvedCaseHandoverPolicies(tenantId));
  }

  public TenantPermissionPolicies updateTenantPermissionPolicies(
      Long tenantId, TenantPermissionPolicies request) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    if (request == null || !tenantId.equals(request.getTenantId())) {
      throw new BadRequestException("Path tenant id must match request tenant id");
    }
    tenantPermissionPolicyService.saveOverrides(
        tenantId,
        request.getPolicies().entrySet().stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    entry ->
                        new com.vi.tenantservice.api.policy.PolicyValue<>(
                            entry.getValue().getValue(),
                            com.vi.tenantservice.api.policy.PermissionPolicyMode.valueOf(
                                entry.getValue().getMode().name())))),
        request.getCaseHandoverPolicies());
    return getTenantPermissionPolicies(tenantId);
  }

  private TenantPermissionPolicies toTenantPermissionPolicies(
      Long tenantId,
      Map<String, com.vi.tenantservice.api.policy.ResolvedPolicyValue<Boolean>> policies,
      com.vi.tenantservice.api.model.CaseHandoverPolicies caseHandoverPolicies) {
    return new TenantPermissionPolicies(
            tenantId,
            policies.entrySet().stream()
                .collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry ->
                            new BooleanPermissionPolicy(
                                    entry.getValue().value(),
                                    com.vi.tenantservice.api.model.PermissionPolicyMode.valueOf(
                                        entry.getValue().mode().name()))
                                .inherited(entry.getValue().inherited()))))
        .caseHandoverPolicies(caseHandoverPolicies);
  }

  private void assertSuperAdmin() {
    if (!tenantFacadeAuthorisationService.isSuperAdmin()) {
      throw new AccessDeniedException("Only super admin can manage platform tenant admin controls");
    }
  }

  private void enrichWithConsultingTypeSettings(
      MultilingualTenantDTO multilingualTenantDTO, Long tenantId) {
    FullConsultingTypeResponseDTO consultingTypesByTenantId =
        consultingTypeService.getConsultingTypesByTenantId(tenantId.intValue());
    if (consultingTypesByTenantId != null) {
      multilingualTenantDTO
          .getSettings()
          .setExtendedSettings(
              consultingTypePatchDTOConverter.convertConsultingTypePatchDTO(
                  consultingTypesByTenantId));
    }
  }

  private void enrichWithAdminDataIfSuperadmin(MultilingualTenantDTO multilingualTenantDTO) {
    if (authorisationService.hasAuthority(AuthorityValue.GET_TENANT_ADMIN_DATA)) {
      enrichWithAdminData(
          multilingualTenantDTO.getId().intValue(), multilingualTenantDTO::setAdminEmails);
    }
  }

  private void enrichWithAdminData(
      final Integer tenantId, final Consumer<List<String>> setAdminEmailsConsumer) {
    List<AdminResponseDTO> tenantAdmins = new ArrayList<>();
    try {
      tenantAdmins = userAdminService.getTenantAdmins(tenantId);
    } catch (Exception ex) {
      log.warn(
          "Could not resolve tenant-admin emails for tenant {}. Returning tenant without adminEmails.",
          tenantId,
          ex);
    }
    if (tenantAdmins != null && !tenantAdmins.isEmpty()) {
      log.debug("Enriching tenant with admin email data");
      setAdminEmailsConsumer.accept(getAdminEmails(tenantAdmins));
    } else {
      log.debug("No tenant admins found for a given tenant {}", tenantId);
    }
  }

  private List<String> getAdminEmails(List<AdminResponseDTO> tenantAdmins) {
    return tenantAdmins.stream()
        .map(admin -> admin.getEmbedded() != null ? admin.getEmbedded().getEmail() : "")
        .toList();
  }

  public Optional<MultilingualTenantDTO> findMultilingualTenantById(Long id) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(id);
    var tenantById = tenantService.findTenantDataById(id);
    return tenantById.isEmpty()
        ? Optional.empty()
        : Optional.of(getConvertedAndEnrichedTenant(tenantById.get()));
  }

  public Optional<RestrictedTenantDTO> findRestrictedTenantById(Long id) {
    var tenantById = tenantService.findRestrictedTenantDataById(id);

    String lang = translationService.getCurrentLanguageContext();
    return tenantById.isEmpty()
        ? Optional.empty()
        : Optional.of(
            withEffectivePermissions(
                tenantConverter.toRestrictedTenantDTO(tenantById.get(), lang)));
  }

  public List<RestrictedTenantDTO> findRestrictedTenantsByIds(Set<Long> ids) {
    String lang = translationService.getCurrentLanguageContext();
    return tenantService.findRestrictedTenantDataByIds(ids).stream()
        .map(tenant -> tenantConverter.toRestrictedTenantDTO(tenant, lang))
        .map(this::withEffectivePermissions)
        .toList();
  }

  /**
   * Bakes the platform admin controls into the public settings so the counselling app receives
   * effective feature flags (forced-off disabled, enforced-on locked on) without seeing the
   * controls themselves. See ADR-013 P4.
   */
  private RestrictedTenantDTO withEffectivePermissions(RestrictedTenantDTO dto) {
    if (dto != null) {
      Map<String, BooleanPermissionPolicy> policies =
          tenantPermissionPolicyService.getResolvedPolicies(dto.getId()).entrySet().stream()
              .collect(
                  java.util.stream.Collectors.toUnmodifiableMap(
                      Map.Entry::getKey,
                      entry ->
                          new BooleanPermissionPolicy(
                                  entry.getValue().value(),
                                  com.vi.tenantservice.api.model.PermissionPolicyMode.valueOf(
                                      entry.getValue().mode().name()))
                              .inherited(entry.getValue().inherited())));
      dto.setPermissionPolicies(policies);
      effectivePermissionSettingsApplier.applyPolicies(dto.getSettings(), policies);
    }
    return dto;
  }

  public List<BasicTenantLicensingDTO> getAllTenants() {
    var tenantEntities = tenantService.getAllTenantData();
    return tenantEntities.stream().map(tenantConverter::toBasicLicensingTenantDTO).toList();
  }

  public Optional<RestrictedTenantDTO> findTenantBySubdomain(
      String subdomain, Long optionalTenantIdOverride) {
    var tenantBySubdomain = tenantService.findRestrictedTenantDataBySubdomain(subdomain);
    Optional<Long> tenantIdFromRequestOrCookie =
        resolveFromRequestOrCookie(optionalTenantIdOverride);

    if (multitenancyWithSingleDomain && tenantIdFromRequestOrCookie.isPresent()) {
      return getTenantDataWithOverride(tenantBySubdomain, tenantIdFromRequestOrCookie.get());
    }

    String lang = translationService.getCurrentLanguageContext();
    return tenantBySubdomain.isEmpty()
        ? Optional.empty()
        : Optional.of(
            withEffectivePermissions(
                tenantConverter.toRestrictedTenantDTO(tenantBySubdomain.get(), lang)));
  }

  private Optional<Long> resolveFromRequestOrCookie(Long optionalTenantIdOverride) {
    return optionalTenantIdOverride != null
        ? Optional.of(optionalTenantIdOverride)
        : tenantResolverService.tryResolveForNonAuthUsers();
  }

  public RestrictedTenantDTO getRestrictedTenantDataDeterminingTenantContext() {
    if (multitenancyWithSingleDomain) {
      return getRestrictedTenantDataWithOverrideForSingleDomainTenancy();
    } else {
      var tenantId = tenantResolverService.tryResolve().orElseThrow();
      return findRestrictedTenantById(tenantId).orElseThrow();
    }
  }

  private RestrictedTenantDTO getRestrictedTenantDataWithOverrideForSingleDomainTenancy() {
    String mainTenantSubdomain =
        applicationSettingsService
            .getApplicationSettings()
            .getMainTenantSubdomainForSingleDomainMultitenancy()
            .getValue();
    var mainTenant =
        tenantService.findRestrictedTenantDataBySubdomain(mainTenantSubdomain).orElseThrow();
    Long actualTenantId = tenantResolverService.tryResolve().orElseThrow();
    TenantRestrictedData actualTenant =
        tenantService.findRestrictedTenantDataById(actualTenantId).orElseThrow();
    return withEffectivePermissions(
        singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            mainTenant, actualTenant));
  }

  public Optional<RestrictedTenantDTO> getTenantDataWithOverride(
      Optional<TenantRestrictedData> mainTenantForSingleDomainMultitenancy, Long resolvedTenantId) {

    if (mainTenantForSingleDomainMultitenancy.isEmpty()) {
      return Optional.empty();
    }

    Optional<TenantRestrictedData> tenantToOverridePrivacy =
        tenantService.findRestrictedTenantDataById(resolvedTenantId);
    if (tenantToOverridePrivacy.isEmpty()) {
      throw new BadRequestException("Tenant not found for id " + resolvedTenantId);
    }
    return Optional.of(
        withEffectivePermissions(
            singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
                mainTenantForSingleDomainMultitenancy.get(), tenantToOverridePrivacy.get())));
  }

  public Optional<RestrictedTenantDTO> getSingleTenant() {
    var tenantEntities = tenantService.getAllTenantData();
    if (tenantEntities != null && tenantEntities.size() == 1) {
      var tenantEntity = tenantEntities.get(0);
      String lang = translationService.getCurrentLanguageContext();
      return Optional.of(
          withEffectivePermissions(tenantConverter.toRestrictedTenantDTO(tenantEntity, lang)));
    } else {
      throw new IllegalStateException("Not exactly one tenant was found.");
    }
  }

  public boolean canAccessTenant() {
    if (multitenancyWithSingleDomain) {
      return true;
    }

    Optional<String> subdomain = subdomainExtractor.getCurrentSubdomain();

    if (subdomain.isEmpty()) {
      // Special case: if subdomain is empty, try to get it from the request server name
      var request =
          ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
      String serverName = request.getServerName();

      if ("localhost".equals(serverName)) {
        subdomain = Optional.of("localhost");
      } else {
        return false;
      }
    }

    var tenantIdBySubdomain = tenantService.findTenantIdBySubdomain(subdomain.get());
    return tenantFacadeAuthorisationService.canAccessTenantById(tenantIdBySubdomain);
  }

  public Map<String, Object> findTenantsExceptTechnicalByInfix(
      String infix, int pageNumber, Integer pageSize, String fieldName, boolean isAscending) {
    var direction = isAscending ? Direction.ASC : Direction.DESC;
    var pageRequest = PageRequest.of(pageNumber, pageSize, direction, fieldName);
    Page<TenantData> tenantPage =
        tenantService.findAllTenantDataExceptTechnicalByInfix(infix, pageRequest);
    return mapOf(tenantPage);
  }

  public List<AdminTenantDTO> getAllAdminTenantsExceptTechnical() {
    var tenantEntities = new ArrayList<>(tenantService.getAllTenantData());
    excludeTechnicalTenantFrom(tenantEntities);
    List<AdminTenantDTO> adminTenantDTOS =
        tenantEntities.stream().map(tenantConverter::toAdminTenantDTO).toList();
    adminTenantDTOS.forEach(
        adminTenantDTO ->
            enrichWithAdminData(adminTenantDTO.getId().intValue(), adminTenantDTO::setAdminEmails));
    return adminTenantDTOS;
  }

  private void excludeTechnicalTenantFrom(List<? extends TenantData> tenants) {
    emptyIfNull(tenants).removeIf(tenant -> tenant.getId() == TECHNICAL_TENANT_ID);
  }

  private Map<String, Object> mapOf(Page<TenantData> tenantPage) {
    var tenants = new ArrayList<Map<String, Object>>();
    tenantPage.forEach(
        tenantData -> {
          var tenantMap = mapOf(tenantData);
          tenants.add(tenantMap);
        });

    return Map.of(
        "totalElements",
        (int) tenantPage.getTotalElements(),
        "isFirstPage",
        tenantPage.isFirst(),
        "isLastPage",
        tenantPage.isLast(),
        "tenants",
        tenants);
  }

  private Map<String, Object> mapOf(TenantData tenantData) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", tenantData.getId());
    map.put("name", tenantData.getName());
    map.put("subdomain", tenantData.getSubdomain());
    map.put("beraterCount", tenantData.getLicensingAllowedNumberOfUsers());
    List<AdminResponseDTO> tenantAdmins = new ArrayList<>();
    try {
      tenantAdmins = userAdminService.getTenantAdmins(tenantData.getId().intValue());
    } catch (Exception ex) {
      log.warn(
          "Could not resolve tenant-admin emails for tenant {}. Returning tenant without adminEmails.",
          tenantData.getId(),
          ex);
    }
    map.put("adminEmails", getAdminEmails(tenantAdmins));
    map.put(
        "createDate",
        nonNull(tenantData.getCreateDate()) ? tenantData.getCreateDate().toString() : null);
    map.put(
        "updateDate",
        nonNull(tenantData.getUpdateDate()) ? tenantData.getUpdateDate().toString() : null);
    return map;
  }
}
