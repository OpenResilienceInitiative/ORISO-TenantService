package com.vi.tenantservice.api.service;

import static com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason.SUBDOMAIN_NOT_UNIQUE;
import static com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason.TENANT_MARKED_FOR_DELETION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.model.TenantData;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantRestrictedData;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.TenantStatisticsNameDTO;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

  @Value("${feature.multitenancy.with.single.domain.enabled}")
  private boolean multitenancyWithSingleDomain;

  @Value("${default.tenant.settings.json.path}")
  private String defaultTenantSettingsFilePath;

  private final @NonNull TenantRepository tenantRepository;

  private final @NonNull ApplicationSettingsService applicationSettingsService;

  private final @NonNull ConfigurationFileLoader configurationFileLoader;

  private final @NonNull TenantIdAllocationService tenantIdAllocationService;

  @Transactional
  public TenantEntity create(TenantEntity tenantEntity) {
    return create(tenantEntity, null);
  }

  /**
   * Creates a tenant with authoritative ID allocation (TEN-INV-U1): without a pre-set ID the
   * smallest currently free tenant ID is assigned atomically; with a pre-set ID the ID is
   * re-validated against the allocation ledger inside this transaction (a reserved ID is consumed
   * only with the matching reservation token, a taken ID is rejected with a conflict).
   */
  @Transactional
  public TenantEntity create(TenantEntity tenantEntity, String tenantIdReservationToken) {
    validateTenant(tenantEntity);
    overrideSubdomainIfNeededForSingleDomainMultitenancy(tenantEntity);
    setCreateAndUpdateDate(tenantEntity);
    tenantIdAllocationService.assignIdForNewTenant(tenantEntity, tenantIdReservationToken);
    return tenantRepository.save(tenantEntity);
  }

  private boolean shouldOverrideSubdomain(TenantEntity tenantEntity, String mainTenantSubdomain) {
    return tenantEntity.getSubdomain() == null
        || !tenantEntity.getSubdomain().equals(mainTenantSubdomain);
  }

  private void overrideSubdomainIfNeededForSingleDomainMultitenancy(TenantEntity tenantEntity) {
    if (multitenancyWithSingleDomain
        && shouldOverrideSubdomain(tenantEntity, getMainTenantSubdomain())) {
      tenantEntity.setSubdomain(StringUtils.EMPTY);
    }
  }

  private String getMainTenantSubdomain() {
    return applicationSettingsService
        .getApplicationSettings()
        .getMainTenantSubdomainForSingleDomainMultitenancy()
        .getValue();
  }

  private void setCreateAndUpdateDate(TenantEntity tenantEntity) {
    tenantEntity.setCreateDate(LocalDateTime.now(ZoneOffset.UTC));
    tenantEntity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
  }

  private void validateTenantSubdomainDoesNotExist(TenantEntity tenantEntity) {
    var existingTenantId = tenantRepository.findIdBySubdomain(tenantEntity.getSubdomain());
    if (tenantWithSuchSubdomainAlreadyExists(tenantEntity, existingTenantId)) {
      throw new TenantValidationException(SUBDOMAIN_NOT_UNIQUE);
    }
  }

  private boolean tenantWithSuchSubdomainAlreadyExists(
      TenantEntity tenantEntity, Long existingTenantId) {
    return existingTenantId != null && !existingTenantId.equals(tenantEntity.getId());
  }

  /**
   * Switches a Träger on or off. A Träger marked for deletion stays off (ORISO-Admin#1525): its
   * accounts are already being deleted, so switching it on again is refused with 409.
   */
  @Transactional
  public TenantEntity updateOperationalStatus(Long id, boolean active) {
    if (tenantRepository.updateOperationalStatus(id, active, LocalDateTime.now(ZoneOffset.UTC))
        == 0) {
      var tenant =
          tenantRepository
              .findById(id)
              .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
      if (tenant.getDeleteDate() != null) {
        throw new TenantValidationException(TENANT_MARKED_FOR_DELETION);
      }
      throw new TenantNotFoundException("Tenant not found: " + id);
    }
    return tenantRepository
        .findById(id)
        .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
  }

  /**
   * Marks the Träger for deletion and switches it off (ORISO-Admin#1525). Idempotent and safe
   * against overlapping requests: one conditional update sets the date, so a Träger already in
   * deletion keeps its first date and is only switched off again.
   *
   * @return true when the Träger was marked by this call
   */
  @Transactional
  public boolean markForDeletion(Long tenantId) {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    if (tenantRepository.markForDeletion(tenantId, now) == 1) {
      return true;
    }
    tenantRepository.deactivateMarkedForDeletion(tenantId, now);
    return false;
  }

  public List<Long> findTenantIdsMarkedForDeletion() {
    return tenantRepository.findIdsMarkedForDeletion();
  }

  public TenantEntity update(TenantEntity tenantEntity) {
    // The edit form can carry "active" as well: a Träger in deletion must stay off (#1525).
    if (tenantEntity.getDeleteDate() != null && Boolean.TRUE.equals(tenantEntity.getActive())) {
      throw new TenantValidationException(TENANT_MARKED_FOR_DELETION);
    }
    validateTenant(tenantEntity);
    overrideSubdomainIfNeededForSingleDomainMultitenancy(tenantEntity);
    tenantEntity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    return tenantRepository.save(tenantEntity);
  }

  // Keep loaded entities managed through the save: detached merge can copy a stale own status.
  @Transactional
  public void updateTenantSettingsBatch(Map<Long, String> settingsByTenantId) {
    if (settingsByTenantId == null || settingsByTenantId.isEmpty()) {
      return;
    }
    LocalDateTime updateDate = LocalDateTime.now(ZoneOffset.UTC);
    List<TenantEntity> tenants =
        tenantRepository.findAllByIdIn(new ArrayList<>(settingsByTenantId.keySet()));
    tenants.forEach(
        tenant -> {
          tenant.setSettings(settingsByTenantId.get(tenant.getId()));
          tenant.setUpdateDate(updateDate);
        });
    tenantRepository.saveAll(tenants);
  }

  private void validateTenant(TenantEntity tenantEntity) {
    if (!multitenancyWithSingleDomain
        || getMainTenantSubdomain().equals(tenantEntity.getSubdomain())) {
      validateTenantSubdomainDoesNotExist(tenantEntity);
    }
  }

  public Optional<TenantEntity> findTenantById(Long id) {
    return tenantRepository.findById(id);
  }

  /** Serializes contract versions for one document owner inside the publication transaction. */
  public Optional<TenantEntity> findTenantForDpaPublication(Long id) {
    return tenantRepository.findForDpaPublication(id);
  }

  public Optional<TenantData> findTenantDataById(Long id) {
    return Optional.ofNullable(tenantRepository.findTenantDataById(id));
  }

  public Optional<TenantEntity> findTenantBySubdomain(String subdomain) {
    var bySubdomain = tenantRepository.findBySubdomain(subdomain);
    return bySubdomain != null ? Optional.of(bySubdomain) : Optional.empty();
  }

  public Optional<Long> findTenantIdBySubdomain(String subdomain) {
    return Optional.ofNullable(tenantRepository.findIdBySubdomain(subdomain));
  }

  public Optional<TenantRestrictedData> findRestrictedTenantDataBySubdomain(String subdomain) {
    return Optional.ofNullable(tenantRepository.findRestrictedDataBySubdomain(subdomain));
  }

  public Optional<TenantRestrictedData> findRestrictedTenantDataById(Long id) {
    return Optional.ofNullable(tenantRepository.findRestrictedDataById(id));
  }

  public List<TenantRestrictedData> findRestrictedTenantDataByIds(Set<Long> ids) {
    return List.copyOf(tenantRepository.findRestrictedDataByIdIn(ids));
  }

  public List<TenantEntity> getAllTenants() {
    return tenantRepository.findAll();
  }

  public List<TenantData> getAllTenantData() {
    return List.copyOf(tenantRepository.findAllTenantData());
  }

  public List<Long> searchableTenantIds(Optional<Long> tenantRestriction) {
    if (tenantRestriction.isPresent()) {
      var id = tenantRestriction.get();
      return tenantRepository.existsById(id) ? List.of(id) : List.of();
    }
    return tenantRepository.findAllIds().stream().filter(id -> id != 0L).toList();
  }

  public Page<TenantData> searchTenantData(
      List<String> tokens,
      Map<String, Set<Long>> emailMatches,
      PageRequest pageRequest,
      Optional<Long> tenantRestriction) {
    return tenantRepository
        .searchTenantData(tokens, emailMatches, pageRequest, tenantRestriction)
        .map(data -> data);
  }

  public List<TenantEntity> findAllByIds(List<Long> tenantIds) {
    return tenantRepository.findAllByIdIn(tenantIds);
  }

  public TenantSettings getDefaultTenantSettings() {
    if (StringUtils.isBlank(defaultTenantSettingsFilePath)) {
      log.warn("default.tenant.settings.json.path is empty - using empty default tenant settings");
      return TenantSettings.builder().build();
    }

    final File file = configurationFileLoader.loadFrom(defaultTenantSettingsFilePath);
    if (!file.exists() || file.isDirectory()) {
      log.warn(
          "Default tenant settings file does not exist or is not a file (path: {}) - using empty defaults",
          defaultTenantSettingsFilePath);
      return TenantSettings.builder().build();
    }

    try {
      return new ObjectMapper().readValue(file, TenantSettings.class);
    } catch (IOException ioException) {
      log.error("Error while reading default tenant settings configuration file", ioException);
      return TenantSettings.builder().build();
    }
  }

  public void delete(TenantEntity createdTenant) {
    tenantRepository.delete(createdTenant);
  }

  @Transactional(readOnly = true)
  public List<TenantStatisticsNameDTO> findStatisticsNamesExceptTechnical() {
    return tenantRepository.findStatisticsNamesExceptTechnical();
  }
}
