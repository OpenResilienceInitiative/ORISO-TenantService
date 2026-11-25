package com.vi.tenantservice.api.service.consultingtype;

import com.vi.tenantservice.api.config.apiclient.UserAdminServiceApiControllerFactory;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.TenantAdminLoginEmailsDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.TenantDeletionBlockersDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.TenantDeletionResultDTO;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Service class to communicate with the ConsultingTypeService. */
@Component
@RequiredArgsConstructor
public class UserAdminService {

  private static final int LOGIN_EMAIL_BATCH_LIMIT = 100;

  private final @NonNull TenantResolverService tenantResolverService;
  private final @NonNull UserAdminServiceApiControllerFactory userAdminServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;

  public List<AdminResponseDTO> getTenantAdmins(Integer tenantId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeadersWithKeycloak(controllerApi.getApiClient());
    return controllerApi.getTenantAdmins(tenantId);
  }

  public Map<Long, List<String>> getTenantAdminLoginEmails(List<Long> tenantIds) {
    if (tenantIds.isEmpty()) {
      return Map.of();
    }

    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeadersWithKeycloak(controllerApi.getApiClient());
    var emailsByTenant = new HashMap<Long, List<String>>();
    for (int start = 0; start < tenantIds.size(); start += LOGIN_EMAIL_BATCH_LIMIT) {
      int end = Math.min(start + LOGIN_EMAIL_BATCH_LIMIT, tenantIds.size());
      var batchIds = tenantIds.subList(start, end);
      var batch = requestLoginEmails(controllerApi, batchIds);
      if (batch == null || batch.size() != batchIds.size()) {
        throw invalidLoginEmailResponse();
      }

      var remainingIds = new HashSet<>(batchIds);
      for (var entry : batch) {
        if (entry == null
            || !remainingIds.remove(entry.getTenantId())
            || entry.getEmails() == null
            || entry.getEmails().stream().anyMatch(Objects::isNull)) {
          throw invalidLoginEmailResponse();
        }
        emailsByTenant.put(entry.getTenantId(), List.copyOf(entry.getEmails()));
      }
    }
    return emailsByTenant;
  }

  private static InvalidLoginEmailResponseException invalidLoginEmailResponse() {
    return new InvalidLoginEmailResponseException();
  }

  /**
   * Status and transport failures pass through: the caller may fall back to per-tenant lookups. A
   * response UserService did send but that does not decode is a broken contract instead.
   */
  private static List<TenantAdminLoginEmailsDTO> requestLoginEmails(
      AdminUserControllerApi controllerApi, List<Long> batchIds) {
    try {
      return controllerApi.getTenantAdminLoginEmails(batchIds);
    } catch (ResourceAccessException | HttpStatusCodeException upstreamFailure) {
      throw upstreamFailure;
    } catch (RestClientException undecodableResponse) {
      throw new InvalidLoginEmailResponseException(undecodableResponse);
    }
  }

  /**
   * UserService answered, but the answer breaks the batch contract. Unlike an unreachable or
   * missing endpoint this is not something a per-tenant retry should paper over.
   */
  public static class InvalidLoginEmailResponseException extends ResponseStatusException {
    InvalidLoginEmailResponseException() {
      this(null);
    }

    InvalidLoginEmailResponseException(Throwable cause) {
      super(HttpStatus.BAD_GATEWAY, "Invalid tenant-admin email response", cause);
    }
  }

  /**
   * Asks UserService what still belongs to the Träger, with the deleting admin's own token, so the
   * answer stays within that admin's reach (#1466).
   */
  public TenantDeletionBlockersDTO getTenantDeletionBlockers(Long tenantId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeadersWithKeycloak(controllerApi.getApiClient());
    return controllerApi.getTenantDeletionBlockers(tenantId);
  }

  /**
   * Marks everything that belongs to the Träger for deletion, as the deleting platform
   * administrator (ORISO-Admin#1525). UserService skips what is already marked, so this can be
   * repeated.
   */
  public TenantDeletionResultDTO markTenantContentsForDeletion(Long tenantId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeadersWithKeycloak(controllerApi.getApiClient());
    return controllerApi.markTenantContentsForDeletion(tenantId);
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
