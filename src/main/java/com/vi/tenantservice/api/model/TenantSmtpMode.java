package com.vi.tenantservice.api.model;

/** Explicit tenant mail transport. A missing value means legacy settings require review. */
public enum TenantSmtpMode {
  PLATFORM,
  OWN
}
