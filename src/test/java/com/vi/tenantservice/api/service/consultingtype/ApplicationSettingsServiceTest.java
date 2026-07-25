package com.vi.tenantservice.api.service.consultingtype;

import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.ApplicationsettingsControllerApi;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsPatchDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class ApplicationSettingsServiceTest {

  @InjectMocks ApplicationSettingsService applicationSettingsService;

  @Mock ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

  @Mock TenantResolverService tenantResolverService;

  @Mock SecurityHeaderSupplier securityHeaderSupplier;

  @Mock ApplicationsettingsControllerApi applicationsettingsControllerApi;

  @Mock com.vi.tenantservice.applicationsettingsservice.generated.ApiClient apiClient;

  MockHttpServletRequest httpServletRequest;

  @BeforeEach
  public void setUp() {
    httpServletRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
  }

  @Test
  void getApplicationSettings_Should_TryResolveTenantAndCallApplicationSettingsService() {
    // when
    Mockito.when(applicationSettingsApiControllerFactory.createControllerApi())
        .thenReturn(applicationsettingsControllerApi);
    Mockito.when(applicationsettingsControllerApi.getApiClient()).thenReturn(apiClient);
    Mockito.when(this.securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(new HttpHeaders());
    // given
    applicationSettingsService.getApplicationSettings();

    // then
    Mockito.verify(tenantResolverService).tryResolve();
    Mockito.verify(applicationsettingsControllerApi).getApplicationSettings();
  }

  @Test
  void saveMainTenantSubDomain_Should_SendProviderContractValue() {
    Mockito.when(applicationSettingsApiControllerFactory.createControllerApi())
        .thenReturn(applicationsettingsControllerApi);
    Mockito.when(applicationsettingsControllerApi.getApiClient()).thenReturn(apiClient);
    Mockito.when(this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(new HttpHeaders());

    applicationSettingsService.saveMainTenantSubDomain("main");

    ArgumentCaptor<ApplicationSettingsPatchDTO> patch =
        ArgumentCaptor.forClass(ApplicationSettingsPatchDTO.class);
    Mockito.verify(applicationsettingsControllerApi).patchApplicationSettings(patch.capture());
    Assertions.assertEquals(
        "main", patch.getValue().getMainTenantSubdomainForSingleDomainMultitenancy());
  }

  @AfterEach
  public void tearDown() {
    RequestContextHolder.setRequestAttributes(null);
  }
}
