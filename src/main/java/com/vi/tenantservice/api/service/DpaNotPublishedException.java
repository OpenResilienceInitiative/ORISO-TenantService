package com.vi.tenantservice.api.service;

/** Thrown when a DPA sign invite is requested for a tenant that has not published its DPA yet. */
public class DpaNotPublishedException extends RuntimeException {

  public DpaNotPublishedException(String message) {
    super(message);
  }
}
