package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDataView;
import com.vi.tenantservice.api.model.TenantEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class TenantSearchRepositoryImpl implements TenantSearchRepository {
  @PersistenceContext private EntityManager entityManager;
  private static final List<String> FIELDS =
      List.of(
          "id",
          "name",
          "subdomain",
          "address",
          "description",
          "legalName",
          "contactEmail",
          "contactPhone",
          "licensingAllowedNumberOfUsers",
          "themingLogo",
          "themingAssociationLogo",
          "themingFavicon",
          "themingAssistantName",
          "themingAssistantIcon",
          "themingPrimaryColor",
          "themingSecondaryColor",
          "themingAccent",
          "themingSignal",
          "themingLoginEffect",
          "contentImpressum",
          "contentClaim",
          "contentPrivacy",
          "contentPrivacyActivationDate",
          "contentTermsAndConditions",
          "contentTermsAndConditionsActivationDate",
          "contentErstantwortGreeting",
          "contentErstantwortWhoReadsAlong",
          "contentErstantwortEmergencyAddition",
          "contentErstantwortFreeNotice",
          "contentErstantwortClosing",
          "erstantwortResponseDeadlineDays",
          "settings",
          "createDate",
          "updateDate",
          "dataProtectionOfficer",
          "active");

  public Page<TenantDataView> searchTenantData(
      List<String> tokens, Map<String, Set<Long>> emailMatches, Pageable pageable) {
    var cb = entityManager.getCriteriaBuilder();
    var query = cb.createQuery(TenantDataView.class);
    var root = query.from(TenantEntity.class);
    query.select(
        cb.construct(
            TenantDataView.class, FIELDS.stream().map(root::get).toArray(Selection[]::new)));
    query.where(predicate(cb, root, tokens, emailMatches));
    query.orderBy(
        pageable.getSort().stream()
            .map(
                o ->
                    o.isAscending()
                        ? cb.asc(root.get(o.getProperty()))
                        : cb.desc(root.get(o.getProperty())))
            .toList());
    var count = cb.createQuery(Long.class);
    var countRoot = count.from(TenantEntity.class);
    count.select(cb.count(countRoot)).where(predicate(cb, countRoot, tokens, emailMatches));
    var rows =
        entityManager
            .createQuery(query)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    return new PageImpl<>(rows, pageable, entityManager.createQuery(count).getSingleResult());
  }

  private Predicate predicate(
      CriteriaBuilder cb,
      Root<TenantEntity> root,
      List<String> tokens,
      Map<String, Set<Long>> emailMatches) {
    var predicates = new ArrayList<Predicate>();
    predicates.add(cb.notEqual(root.get("id"), 0L));
    for (var token : tokens) {
      String pattern = "%" + token.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
      var matches = new ArrayList<Predicate>();
      for (String field : List.of("name", "subdomain"))
        matches.add(
            cb.like(cb.function("admin_search_text", String.class, root.get(field)), pattern, '!'));
      matches.add(
          cb.like(
              ((org.hibernate.query.criteria.JpaExpression<Long>) root.<Long>get("id"))
                  .cast(String.class),
              pattern,
              '!'));
      var ids = emailMatches.getOrDefault(token, Set.of());
      if (!ids.isEmpty()) matches.add(root.get("id").in(ids));
      predicates.add(cb.or(matches.toArray(Predicate[]::new)));
    }
    return cb.and(predicates.toArray(Predicate[]::new));
  }
}
