package com.vi.tenantservice.api.tenant;

import static org.mockito.Mockito.when;

import java.nio.file.attribute.UserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class TenantResolverServiceTest {

  @Mock AccessTokenTenantResolver accessTokenTenantResolver;

  @Mock CookieTenantResolver cookieTenantResolver;

  @Mock SubdomainTenantResolver subdomainTenantResolver;

  @InjectMocks TenantResolverService tenantResolverService;

  MockHttpServletRequest httpServletRequest;

  @BeforeEach
  public void setUp() {
    httpServletRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
  }

  @Test
  void tryResolve_Should_Call_AccessTokenResolver_ForAuthUsers() {
    // given
    httpServletRequest.setUserPrincipal((UserPrincipal) () -> "user");
    // when
    tenantResolverService.tryResolve();
    // then
    Mockito.verify(accessTokenTenantResolver).resolve(httpServletRequest);
  }

  @Test
  void tryResolve_Should_Call_CookieTokenResolver_ForNonAuthUsers() {
    // given
    httpServletRequest.setUserPrincipal(null);
    when(cookieTenantResolver.resolveTenantFromRequest(httpServletRequest))
        .thenReturn(Optional.of(1L));
    // when
    tenantResolverService.tryResolve();
    // then
    Mockito.verify(cookieTenantResolver).resolveTenantFromRequest(httpServletRequest);
  }

  @Test
  void tryResolve_Should_Call_SubdomainResolver_ForNonAuthUsersIfCookieNotResolved() {
    // given
    httpServletRequest.setUserPrincipal(null);
    when(cookieTenantResolver.resolveTenantFromRequest(httpServletRequest))
        .thenReturn(Optional.empty());
    // when
    tenantResolverService.tryResolve();
    // then
    Mockito.verify(subdomainTenantResolver).resolve(httpServletRequest);
  }
}
