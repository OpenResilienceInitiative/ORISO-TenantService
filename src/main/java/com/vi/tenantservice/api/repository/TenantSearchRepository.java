package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.TenantDataView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantSearchRepository {
  Page<TenantDataView> searchTenantData(
      List<String> tokens, Map<String, Set<Long>> emailMatches, Pageable pageable);
}
