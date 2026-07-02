package com.vi.tenantservice.api.service;

/** Thrown when a DPA sign token is unknown, already used (consumed), or expired. */
public class InvalidDpaSignTokenException extends RuntimeException {

  public InvalidDpaSignTokenException(String message) {
    super(message);
  }
}
