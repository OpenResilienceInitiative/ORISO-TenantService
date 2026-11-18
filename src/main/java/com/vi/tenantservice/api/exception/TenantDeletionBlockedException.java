package com.vi.tenantservice.api.exception;

import com.vi.tenantservice.useradminservice.generated.web.model.TenantDeletionBlockersDTO;
import lombok.Getter;

/**
 * Thrown when a Träger still has agencies, accounts or open invitations and therefore must not be
 * deleted yet (ORISO-UserService#1466). Mapped to HTTP 409 with {@code X-Reason:
 * TENANT_HAS_DEPENDENTS} and the counts in the body, so the admin interface can say what has to be
 * deleted first.
 */
@Getter
public class TenantDeletionBlockedException extends RuntimeException {

  private final transient TenantDeletionBlockersDTO blockers;

  public TenantDeletionBlockedException(Long tenantId, TenantDeletionBlockersDTO blockers) {
    super("Tenant " + tenantId + " still has agencies, accounts or open invitations");
    this.blockers = blockers;
  }
}
