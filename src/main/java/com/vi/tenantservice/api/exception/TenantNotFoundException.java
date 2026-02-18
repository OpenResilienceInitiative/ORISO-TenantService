package com.vi.tenantservice.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such tenant")
public class TenantNotFoundException extends RuntimeException {
  public TenantNotFoundException(String exception) {
    super(exception);
  }
}
