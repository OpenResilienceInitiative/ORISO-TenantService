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
 * Tells the UserService deletion log (Admin &gt; Logs) that a Traeger was marked or deleted.
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

  /** The Träger was marked for deletion; the nightly jobs remove it (ORISO-Admin#1525). */
  public void reportTenantScheduled(Long tenantId) {
    report(tenantId, OutcomeEnum.SCHEDULED);
  }

  public void reportTenantExecuted(Long tenantId) {
    report(tenantId, OutcomeEnum.EXECUTED);
  }

  private void report(Long tenantId, OutcomeEnum outcome) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              send(tenantId, outcome);
            }
          });
    } else {
      send(tenantId, outcome);
    }
  }

  private void send(Long tenantId, OutcomeEnum outcome) {
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
              .outcome(outcome));
    } catch (RuntimeException e) {
      log.warn("Could not write deletion log entry for tenant {}: {}", tenantId, e.toString());
    }
  }
}
