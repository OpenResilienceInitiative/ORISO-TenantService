package com.vi.tenantservice.api.exception;

/**
 * Thrown when a tenant ID cannot be allocated because it is already assigned to an existing tenant
 * or reserved by an open invite (TEN-INV-U1). Mapped to HTTP 409 CONFLICT.
 */
public class TenantIdAllocationConflictException extends RuntimeException {

  public TenantIdAllocationConflictException(String message) {
    super(message);
  }

  public TenantIdAllocationConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
