package com.vi.tenantservice.api.exception;

/**
 * Thrown when a request is rejected because its input is invalid. Mapped to HTTP 400 BAD REQUEST by
 * {@code ExceptionHandlerAdvice}.
 *
 * <p>Replaces {@code jakarta.ws.rs.BadRequestException} (#197). This is a Spring MVC service; the
 * only JAX-RS artifact on the classpath was the API jar, with no implementation behind it, so
 * constructing the JAX-RS exception failed inside its own constructor ({@code
 * RuntimeDelegate.getInstance()} → {@code ClassNotFoundException}). Every intended 400 therefore
 * answered 500. A plain project exception has no such runtime dependency.
 *
 * <p>Deliberately NOT annotated with {@code @ResponseStatus}: the status mapping lives in {@code
 * ExceptionHandlerAdvice} alone, so there is exactly one place to look for it — the same shape the
 * other facade-level exceptions in this package use.
 */
public class TenantBadRequestException extends RuntimeException {

  public TenantBadRequestException(String message) {
    super(message);
  }

  public TenantBadRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
