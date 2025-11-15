package com.vi.tenantservice.api.tenant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface TenantResolver {

  Optional<Long> resolve(HttpServletRequest request);

  boolean canResolve(HttpServletRequest request);
}
