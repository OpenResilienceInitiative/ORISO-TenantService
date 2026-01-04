package com.vi.tenantservice.api.facade;

import static com.vi.tenantservice.api.util.JsonConverter.convertToJson;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.authorisation.Authority.AuthorityValue;
import com.vi.tenantservice.api.converter.ConsultingTypePatchDTOConverter;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.ConsultingTypeCreationException;
import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantEntity.TenantBase;
import com.vi.tenantservice.api.service.SingleDomainTenantOverrideService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
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
  private final @NonNull TenantService tenantService;
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

  private final @NonNull TenantResolverService tenantResolverService;

  private final @NonNull SingleDomainTenantOverrideService singleDomainTenantOverrideService;

  @Value("${feature.multitenancy.with.single.domain.enabled}")
  private boolean multitenancyWithSingleDomain;

  public MultilingualTenantDTO createTenant(MultilingualTenantDTO tenantDTO) {
    log.info("Creating new tenant");
    MultilingualTenantDTO sanitizedTenantDTO = tenantInputSanitizer.sanitize(tenantDTO);
    validateCreateTenantInput(tenantDTO);
    tenantFacadeDependentSettingsOverrideService.overrideDependentSettingsOnCreate(
        sanitizedTenantDTO);
    var entity = tenantConverter.toEntity(sanitizedTenantDTO);
    populateTenantSettingsAndActivationDates(entity, tenantDTO);
    TenantEntity createdTenant = tenantService.create(entity);
    try {
      createDefaultConsultingTypeSettings(createdTenant);
    } catch (ConsultingTypeCreationException ex) {
      performRollback(createdTenant);
      log.error(
          "Error while creating consulting types for tenant with id {}", createdTenant.getId(), ex);
      throw new BadRequestException(
          "Error while creating consulting types for tenant with id " + createdTenant.getId());
    }
    return tenantConverter.toMultilingualDTO(createdTenant);
  }

  private void performRollback(TenantEntity createdTenant) {
    tenantService.delete(createdTenant);
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

  private boolean isAttemptToCreateFirstNonTechnicalTenant(Long tenantId) {
    return tenantId != 0L && multitenancyWithSingleDomain && onlyTechnicalTenantExists();
  }

  private boolean onlyTechnicalTenantExists() {
    List<TenantEntity> tenants = tenantService.getAllTenants();
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
    validateTenantInput(tenantDTO);
    validateId(tenantDTO);
  }

  private void validateId(MultilingualTenantDTO tenantDTO) {
    if (nonNull(tenantDTO.getId())) {
      throw new TenantValidationException(
          HttpStatusExceptionReason.ID_MUST_BE_NULL_WHEN_CREATING_TENANT);
    }
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
    if (!keys.isEmpty() && !isoCountries.containsAll(keys)) {
      throw new TenantValidationException(HttpStatusExceptionReason.LANGUAGE_KEY_NOT_VALID);
    }
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
    var updatedEntity = tenantConverter.toEntity(existingTenantEntity, sanitizedTenantDTO);
    setContentActivationDates(updatedEntity, sanitizedTenantDTO);
    updatedEntity = tenantService.update(updatedEntity);
    updateExtendedSettingsAsConsultingType(sanitizedTenantDTO, existingTenantEntity.getId());
    log.info("Tenant with id {} updated", existingTenantEntity.getId());
    return getConvertedAndEnrichedTenant(updatedEntity);
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
    var tenantById = tenantService.findTenantById(id);
    return tenantById.isEmpty()
        ? Optional.empty()
        : Optional.of(
            tenantConverter.toDTO(
                tenantById.get(), translationService.getCurrentLanguageContext()));
  }

  private MultilingualTenantDTO getConvertedAndEnrichedTenant(TenantEntity tenantEntity) {
    var multilingualTenantDTO = tenantConverter.toMultilingualDTO(tenantEntity);
    enrichWithAdminDataIfSuperadmin(multilingualTenantDTO);
    enrichWithConsultingTypeSettings(multilingualTenantDTO, tenantEntity.getId());
    return multilingualTenantDTO;
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
    List<AdminResponseDTO> tenantAdmins = userAdminService.getTenantAdmins(tenantId);
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
    var tenantById = tenantService.findTenantById(id);
    return tenantById.isEmpty()
        ? Optional.empty()
        : Optional.of(getConvertedAndEnrichedTenant(tenantById.get()));
  }

  public Optional<RestrictedTenantDTO> findRestrictedTenantById(Long id) {
    var tenantById = tenantService.findTenantById(id);

    String lang = translationService.getCurrentLanguageContext();
    return tenantById.isEmpty()
        ? Optional.empty()
        : Optional.of(tenantConverter.toRestrictedTenantDTO(tenantById.get(), lang));
  }

  public List<BasicTenantLicensingDTO> getAllTenants() {
    var tenantEntities = tenantService.getAllTenants();
    return tenantEntities.stream().map(tenantConverter::toBasicLicensingTenantDTO).toList();
  }

  public Optional<RestrictedTenantDTO> findTenantBySubdomain(
      String subdomain, Long optionalTenantIdOverride) {
    var tenantBySubdomain = tenantService.findTenantBySubdomain(subdomain);
    Optional<Long> tenantIdFromRequestOrCookie =
        resolveFromRequestOrCookie(optionalTenantIdOverride);

    if (multitenancyWithSingleDomain && tenantIdFromRequestOrCookie.isPresent()) {
      return getTenantDataWithOverride(tenantBySubdomain, tenantIdFromRequestOrCookie.get());
    }

    String lang = translationService.getCurrentLanguageContext();
    return tenantBySubdomain.isEmpty()
        ? Optional.empty()
        : Optional.of(tenantConverter.toRestrictedTenantDTO(tenantBySubdomain.get(), lang));
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
    var mainTenant = tenantService.findTenantBySubdomain(mainTenantSubdomain).orElseThrow();
    Long actualTenantId = tenantResolverService.tryResolve().orElseThrow();
    TenantEntity actualTenant = tenantService.findTenantById(actualTenantId).orElseThrow();
    return singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
        mainTenant, actualTenant);
  }

  public Optional<RestrictedTenantDTO> getTenantDataWithOverride(
      Optional<TenantEntity> mainTenantForSingleDomainMultitenancy, Long resolvedTenantId) {

    Optional<TenantEntity> tenantToOverridePrivacy = tenantService.findTenantById(resolvedTenantId);
    if (tenantToOverridePrivacy.isEmpty()) {
      throw new BadRequestException("Tenant not found for id " + resolvedTenantId);
    }
    return Optional.of(
        singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            mainTenantForSingleDomainMultitenancy.orElseThrow(),
            tenantToOverridePrivacy.orElseThrow()));
  }

  public Optional<RestrictedTenantDTO> getSingleTenant() {
    var tenantEntities = tenantService.getAllTenants();
    if (tenantEntities != null && tenantEntities.size() == 1) {
      var tenantEntity = tenantEntities.get(0);
      String lang = translationService.getCurrentLanguageContext();
      return Optional.of(tenantConverter.toRestrictedTenantDTO(tenantEntity, lang));
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

    var tenantBySubdomain = tenantService.findTenantBySubdomain(subdomain.get());
    return tenantFacadeAuthorisationService.canAccessTenant(tenantBySubdomain);
  }

  public Map<String, Object> findTenantsExceptTechnicalByInfix(
      String infix, int pageNumber, Integer pageSize, String fieldName, boolean isAscending) {
    var direction = isAscending ? Direction.ASC : Direction.DESC;
    var pageRequest = PageRequest.of(pageNumber, pageSize, direction, fieldName);
    Page<TenantBase> tenantPage = tenantService.findAllExceptTechnicalByInfix(infix, pageRequest);
    var tenantIds = tenantPage.stream().map(TenantBase::getId).toList();
    var fullTenants = tenantService.findAllByIds(tenantIds);
    return mapOf(tenantPage, fullTenants);
  }

  public List<AdminTenantDTO> getAllAdminTenantsExceptTechnical() {
    var tenantEntities = tenantService.getAllTenants();
    excludeTechnicalTenantFrom(tenantEntities);
    List<AdminTenantDTO> adminTenantDTOS =
        tenantEntities.stream().map(tenantConverter::toAdminTenantDTO).toList();
    adminTenantDTOS.forEach(
        adminTenantDTO ->
            enrichWithAdminData(adminTenantDTO.getId().intValue(), adminTenantDTO::setAdminEmails));
    return adminTenantDTOS;
  }

  private void excludeTechnicalTenantFrom(List<TenantEntity> tenants) {
    emptyIfNull(tenants).removeIf(tenant -> tenant.getId() == TECHNICAL_TENANT_ID);
  }

  private Map<String, Object> mapOf(Page<TenantBase> tenantPage, List<TenantEntity> fullTenants) {
    var fullTenantsLookupMap =
        fullTenants.stream().collect(Collectors.toMap(TenantEntity::getId, Function.identity()));

    var tenants = new ArrayList<Map<String, Object>>();
    tenantPage.forEach(
        tenantBase -> {
          var fullTenant = fullTenantsLookupMap.get(tenantBase.getId());
          var tenantMap = mapOf(tenantBase, fullTenant);
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

  private Map<String, Object> mapOf(TenantBase tenantBase, TenantEntity fullTenant) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", tenantBase.getId());
    map.put("name", tenantBase.getName());
    map.put("subdomain", fullTenant.getSubdomain());
    map.put("beraterCount", fullTenant.getLicensingAllowedNumberOfUsers());
    List<AdminResponseDTO> tenantAdmins =
        userAdminService.getTenantAdmins(tenantBase.getId().intValue());
    map.put("adminEmails", getAdminEmails(tenantAdmins));
    map.put(
        "createDate",
        nonNull(fullTenant.getCreateDate()) ? fullTenant.getCreateDate().toString() : null);
    map.put(
        "updateDate",
        nonNull(fullTenant.getUpdateDate()) ? fullTenant.getUpdateDate().toString() : null);
    return map;
  }
}
