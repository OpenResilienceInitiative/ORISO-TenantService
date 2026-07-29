package com.vi.tenantservice.api.model;

/** Lifecycle state of a row in the tenant ID allocation ledger. */
public enum TenantIdReservationStatus {
  /** The ID is held by an open invite and must not be handed out again. */
  RESERVED,
  /** The ID has been consumed by a real tenant. */
  ASSIGNED
}
