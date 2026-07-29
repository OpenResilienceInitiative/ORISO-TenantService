package com.vi.tenantservice.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * AUTO tenant ID allocation gave up after the maximum number of lost races (TEN-INV-U1). This is a
 * server-side contention condition, not a client error: the request named no ID, so neither 400 nor
 * 409 would be honest. 503 tells the client to retry later.
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "Tenant ID allocation exhausted")
public class TenantIdAllocationExhaustedException extends RuntimeException {

  public TenantIdAllocationExhaustedException(String message) {
    super(message);
  }

  public TenantIdAllocationExhaustedException(String message, Throwable cause) {
    super(message, cause);
  }
}
