package com.vi.tenantservice.api.service.consultingtype;

import com.vi.tenantservice.api.config.apiclient.UserAdminServiceApiControllerFactory;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** Service class to communicate with the ConsultingTypeService. */
@Component
@RequiredArgsConstructor
public class UserAdminService {

  private final @NonNull TenantResolverService tenantResolverService;
  private final @NonNull UserAdminServiceApiControllerFactory userAdminServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;

  public List<AdminResponseDTO> getTenantAdmins(Integer tenantId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeadersWithKeycloak(controllerApi.getApiClient());
    return controllerApi.getTenantAdmins(tenantId);
  }

  private void addDefaultHeadersWithKeycloak(ApiClient apiClient) {
    var headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();
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
