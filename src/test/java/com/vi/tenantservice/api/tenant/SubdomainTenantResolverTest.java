package com.vi.tenantservice.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubdomainTenantResolverTest {

  @Mock SubdomainExtractor subdomainExtractor;

  @Mock TenantRepository tenantRepository;

  @Mock HttpServletRequest httpServletRequest;

  @InjectMocks SubdomainTenantResolver subdomainTenantResolver;

  @Test
  void resolve_should_resolveTenantId_When_SubdomainCouldBeDetermined() {
    // given
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("mucoviscidose"));
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(1L);
    when(tenantRepository.findBySubdomain("mucoviscidose")).thenReturn(tenantEntity);

    // when
    Optional<Long> resolve = subdomainTenantResolver.resolve(httpServletRequest);

    // then
    assertThat(resolve).contains(1L);
  }

  @Test
  void resolve_should_NotResolve_When_SubdomainIsEmpty() {
    // given
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.empty());

    // when
    Optional<Long> resolve = subdomainTenantResolver.resolve(httpServletRequest);

    // then
    assertThat(resolve).isEmpty();
  }
}
