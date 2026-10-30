package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.config.apiclient.UserAdminServiceApiControllerFactory;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.useradminservice.generated.web.model.DeletionAuditEntryRequestDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.DeletionAuditEntryRequestDTO.OutcomeEnum;
import com.vi.tenantservice.useradminservice.generated.web.model.DeletionAuditEntryRequestDTO.TargetTypeEnum;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tells the UserService deletion log (Admin &gt; Logs) that a Traeger was deleted.
 *
 * <p>Best effort by contract. Inside a transaction the call is made only after it commits, so a
 * rolled-back deletion leaves no entry; a failing log call is logged and swallowed and can never
 * fail the deletion. The caller's own token is forwarded, so the UserService records who acted;
 * only the opaque id, the tenant id and the outcome are sent - no name, no address.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeletionAuditReporter {

  private final @NonNull UserAdminServiceApiControllerFactory userAdminServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;

  public void reportTenantExecuted(Long tenantId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              send(tenantId);
            }
          });
    } else {
      send(tenantId);
    }
  }

  private void send(Long tenantId) {
    try {
      var api = userAdminServiceApiControllerFactory.createDeletionAuditApi();
      var headers = securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();
      headers.forEach(
          (key, value) -> api.getApiClient().addDefaultHeader(key, value.iterator().next()));
      api.recordDeletion(
          new DeletionAuditEntryRequestDTO()
              .targetType(TargetTypeEnum.TENANT)
              .targetId(String.valueOf(tenantId))
              .tenantId(tenantId)
              .outcome(OutcomeEnum.EXECUTED));
    } catch (RuntimeException e) {
      log.warn("Could not write deletion log entry for tenant {}: {}", tenantId, e.toString());
    }
  }
}
