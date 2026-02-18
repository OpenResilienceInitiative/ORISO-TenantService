package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantEntity.TenantBase;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TenantRepository extends JpaRepository<TenantEntity, Long> {

  TenantEntity findBySubdomain(String subdomain);

  @Query(
      value =
          "SELECT t.id as id, t.name as name "
              + "FROM TenantEntity t "
              + "WHERE"
              + "  id != 0L "
              + "  AND ( ?1 = '*' "
              + "  OR t.id LIKE CONCAT('%', UPPER(?1), '%') "
              + "  OR UPPER(t.name) LIKE CONCAT('%', UPPER(?1), '%')"
              + "  )")
  Page<TenantBase> findAllExceptTechnicalByInfix(String infix, Pageable pageable);

  List<TenantEntity> findAllByIdIn(List<Long> tenantIds);
}
