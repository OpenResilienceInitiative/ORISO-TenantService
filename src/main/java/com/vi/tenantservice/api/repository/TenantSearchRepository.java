package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDataView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantSearchRepository {
  default Page<TenantDataView> searchTenantData(
      List<String> tokens, Map<String, Set<Long>> emailMatches, Pageable pageable) {
    return searchTenantData(tokens, emailMatches, pageable, Optional.empty());
  }

  Page<TenantDataView> searchTenantData(
      List<String> tokens,
      Map<String, Set<Long>> emailMatches,
      Pageable pageable,
      Optional<Long> tenantRestriction);
}
