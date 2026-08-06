package com.vi.tenantservice.api.model;

/**
 * Allocation state of a single tenant ID as exposed by the availability check (TEN-INV-U1). FREE
 * means assignable, RESERVED means held by an open invite, ASSIGNED means consumed by an existing
 * tenant.
 */
public enum TenantIdAllocationStatus {
  FREE,
  RESERVED,
  ASSIGNED
}
