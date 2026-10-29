package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDataView;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantRestrictedDataView;
import com.vi.tenantservice.api.service.search.TenantSearchText;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository
    extends JpaRepository<TenantEntity, Long>, TenantSearchRepository {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE TenantEntity t SET t.active = :active, t.updateDate = :updatedAt WHERE t.id = :id")
  int updateOperationalStatus(
      @Param("id") Long id,
      @Param("active") Boolean active,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM TenantEntity t WHERE t.id = :id")
  Optional<TenantEntity> findByIdForSmtpTest(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM TenantEntity t WHERE t.id = :id")
  Optional<TenantEntity> findForDpaPublication(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM TenantEntity t WHERE t.id IN :ids ORDER BY t.id")
  List<TenantEntity> findForDpaSigning(@Param("ids") Set<Long> ids);

  TenantEntity findBySubdomain(String subdomain);

  @Query("SELECT t.id FROM TenantEntity t WHERE t.subdomain = :subdomain")
  Long findIdBySubdomain(@Param("subdomain") String subdomain);

  @Query("SELECT t.id FROM TenantEntity t")
  List<Long> findAllIds();

  @Query(
      value =
          "SELECT new com.vi.tenantservice.api.model.TenantDataView("
              + "t.id, "
              + "t.name, "
              + "t.subdomain, "
              + "t.address, "
              + "t.description, "
              + "t.legalName, "
              + "t.contactEmail, "
              + "t.contactPhone, "
              + "t.licensingAllowedNumberOfUsers, "
              + "t.themingLogo, "
              + "t.themingAssociationLogo, "
              + "t.themingFavicon, "
              + "t.themingAssistantName, "
              + "t.themingAssistantIcon, "
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.themingAccent, "
              + "t.themingSignal, "
              + "t.themingLoginEffect, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.contentErstantwortGreeting, "
              + "t.contentErstantwortWhoReadsAlong, "
              + "t.contentErstantwortEmergencyAddition, "
              + "t.contentErstantwortFreeNotice, "
              + "t.contentErstantwortClosing, "
              + "t.erstantwortResponseDeadlineDays, "
              + "t.settings, "
              + "t.createDate, "
              + "t.updateDate, "
              + "t.dataProtectionOfficer, t.active) "
              + "FROM TenantEntity t WHERE t.id = :id")
  TenantDataView findTenantDataById(@Param("id") Long id);

  @Query(
      value =
          "SELECT new com.vi.tenantservice.api.model.TenantDataView("
              + "t.id, "
              + "t.name, "
              + "t.subdomain, "
              + "t.address, "
              + "t.description, "
              + "t.legalName, "
              + "t.contactEmail, "
              + "t.contactPhone, "
              + "t.licensingAllowedNumberOfUsers, "
              + "t.themingLogo, "
              + "t.themingAssociationLogo, "
              + "t.themingFavicon, "
              + "t.themingAssistantName, "
              + "t.themingAssistantIcon, "
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.themingAccent, "
              + "t.themingSignal, "
              + "t.themingLoginEffect, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.contentErstantwortGreeting, "
              + "t.contentErstantwortWhoReadsAlong, "
              + "t.contentErstantwortEmergencyAddition, "
              + "t.contentErstantwortFreeNotice, "
              + "t.contentErstantwortClosing, "
              + "t.erstantwortResponseDeadlineDays, "
              + "t.settings, "
              + "t.createDate, "
              + "t.updateDate, "
              + "t.dataProtectionOfficer, t.active) "
              + "FROM TenantEntity t")
  List<TenantDataView> findAllTenantData();

  default Page<TenantDataView> findAllTenantDataExceptTechnicalByInfix(
      String infix, Pageable pageable) {
    return searchTenantData(TenantSearchText.tokens(infix), java.util.Map.of(), pageable);
  }

  @Query(
      value =
          "SELECT new com.vi.tenantservice.api.model.TenantRestrictedDataView("
              + "t.id, "
              + "t.name, "
              + "t.subdomain, "
              + "t.themingLogo, "
              + "t.themingAssociationLogo, "
              + "t.themingFavicon, "
              + "t.themingAssistantName, "
              + "t.themingAssistantIcon, "
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.themingAccent, "
              + "t.themingSignal, "
              + "t.themingLoginEffect, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.settings, "
              + "t.dataProtectionOfficer, t.active) "
              + "FROM TenantEntity t WHERE t.subdomain = :subdomain")
  TenantRestrictedDataView findRestrictedDataBySubdomain(@Param("subdomain") String subdomain);

  @Query(
      value =
          "SELECT new com.vi.tenantservice.api.model.TenantRestrictedDataView("
              + "t.id, "
              + "t.name, "
              + "t.subdomain, "
              + "t.themingLogo, "
              + "t.themingAssociationLogo, "
              + "t.themingFavicon, "
              + "t.themingAssistantName, "
              + "t.themingAssistantIcon, "
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.themingAccent, "
              + "t.themingSignal, "
              + "t.themingLoginEffect, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.settings, "
              + "t.dataProtectionOfficer, t.active) "
              + "FROM TenantEntity t WHERE t.id = :id")
  TenantRestrictedDataView findRestrictedDataById(@Param("id") Long id);

  @Query(
      value =
          "SELECT new com.vi.tenantservice.api.model.TenantRestrictedDataView("
              + "t.id, "
              + "t.name, "
              + "t.subdomain, "
              + "t.themingLogo, "
              + "t.themingAssociationLogo, "
              + "t.themingFavicon, "
              + "t.themingAssistantName, "
              + "t.themingAssistantIcon, "
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.themingAccent, "
              + "t.themingSignal, "
              + "t.themingLoginEffect, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.settings, "
              + "t.dataProtectionOfficer, t.active) "
              + "FROM TenantEntity t WHERE t.id IN :ids")
  List<TenantRestrictedDataView> findRestrictedDataByIdIn(@Param("ids") Set<Long> ids);

  List<TenantEntity> findAllByIdIn(List<Long> tenantIds);
}
