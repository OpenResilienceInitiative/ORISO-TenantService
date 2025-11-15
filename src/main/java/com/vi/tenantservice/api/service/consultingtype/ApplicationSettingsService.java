package com.vi.tenantservice.api.service.consultingtype;

import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.applicationsettingsservice.generated.ApiClient;
import com.vi.tenantservice.applicationsettingsservice.generated.web.ApplicationsettingsControllerApi;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTO;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTOMainTenantSubdomainForSingleDomainMultitenancy;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsPatchDTO;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** Service class to communicate with the ConsultingTypeService. */
@Component
@RequiredArgsConstructor
public class ApplicationSettingsService {

  private final @NonNull TenantResolverService tenantResolverService;
  private final @NonNull ApplicationSettingsApiControllerFactory
      applicationSettingsApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;

  public ApplicationSettingsDTO getApplicationSettings() {
    ApplicationsettingsControllerApi controllerApi =
        applicationSettingsApiControllerFactory.createControllerApi();
    addDefaultHeaders(controllerApi.getApiClient());
    return controllerApi.getApplicationSettings();
  }

  public void saveMainTenantSubDomain(final String subdomain) {
    ApplicationsettingsControllerApi controllerApi =
        applicationSettingsApiControllerFactory.createControllerApi();
    addDefaultHeadersWithKeycloak(controllerApi.getApiClient());
    ApplicationSettingsPatchDTO applicationSettingsPatchDTO = new ApplicationSettingsPatchDTO();
    applicationSettingsPatchDTO.setMainTenantSubdomainForSingleDomainMultitenancy(
        new ApplicationSettingsDTOMainTenantSubdomainForSingleDomainMultitenancy()
            .value(subdomain));
    controllerApi.patchApplicationSettings(applicationSettingsPatchDTO);
  }

  private void addDefaultHeadersWithKeycloak(ApiClient apiClient) {
    var headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();
    addHeaders(apiClient, headers);
  }

  private void addDefaultHeaders(ApiClient apiClient) {
    var headers = this.securityHeaderSupplier.getCsrfHttpHeaders();
    addHeaders(apiClient, headers);
  }

  private void addHeaders(ApiClient apiClient, HttpHeaders headers) {
    Optional<Long> optionalTenant = tenantResolverService.tryResolve();
    if (optionalTenant.isPresent()) {
      headers.add("tenantId", optionalTenant.get().toString());
    }
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }
}
