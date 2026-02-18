package com.vi.tenantservice.api.tenant;

import static java.util.Optional.of;

import com.vi.tenantservice.api.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SubdomainTenantResolver implements TenantResolver {

  private final @NonNull SubdomainExtractor subdomainExtractor;

  private final @NonNull TenantRepository tenantRepository;

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return resolveTenantFromSubdomain();
  }

  private Optional<Long> resolveTenantFromSubdomain() {
    Optional<String> currentSubdomain = subdomainExtractor.getCurrentSubdomain();
    if (currentSubdomain.isPresent()) {
      var tenant = tenantRepository.findBySubdomain(currentSubdomain.get());
      if (tenant != null) {
        return of(tenant.getId());
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
