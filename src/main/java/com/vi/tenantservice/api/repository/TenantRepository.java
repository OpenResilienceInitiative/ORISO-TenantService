package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantEntity.TenantBase;
import com.vi.tenantservice.api.model.TenantRestrictedDataView;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<TenantEntity, Long> {

  TenantEntity findBySubdomain(String subdomain);

  @Query("SELECT t.id FROM TenantEntity t WHERE t.subdomain = :subdomain")
  Long findIdBySubdomain(@Param("subdomain") String subdomain);

  @Query(
      value =
          "SELECT new com.vi.tenantservice.api.model.TenantRestrictedDataView("
              + "t.id, "
              + "t.name, "
              + "t.subdomain, "
              + "t.themingLogo, "
              + "t.themingAssociationLogo, "
              + "t.themingFavicon, "
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.settings) "
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
              + "t.themingPrimaryColor, "
              + "t.themingSecondaryColor, "
              + "t.contentImpressum, "
              + "t.contentClaim, "
              + "t.contentPrivacy, "
              + "t.contentPrivacyActivationDate, "
              + "t.contentTermsAndConditions, "
              + "t.contentTermsAndConditionsActivationDate, "
              + "t.settings) "
              + "FROM TenantEntity t WHERE t.id = :id")
  TenantRestrictedDataView findRestrictedDataById(@Param("id") Long id);

  @Query(
      value =
          "SELECT t.id as id, t.name as name "
              + "FROM TenantEntity t "
              + "WHERE"
              + "  id != 0L "
              + "  AND ( ?1 = '*' "
              + "  OR cast(t.id as string) LIKE CONCAT('%', UPPER(?1), '%') "
              + "  OR UPPER(t.name) LIKE CONCAT('%', UPPER(?1), '%')"
              + "  )")
  Page<TenantBase> findAllExceptTechnicalByInfix(String infix, Pageable pageable);

  List<TenantEntity> findAllByIdIn(List<Long> tenantIds);
}
