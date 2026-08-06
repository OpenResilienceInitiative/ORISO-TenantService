package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.exception.TenantAuthorisationException;
import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = {TenantValidationException.class})
  protected ResponseEntity<Object> handle(RuntimeException ex, WebRequest request) {

    var customHttpHeader = ((TenantValidationException) ex).getCustomHttpHeaders();
    HttpStatusExceptionReason statusExceptionReason =
        ((TenantValidationException) ex).getStatusExceptionReason();
    if (HttpStatusExceptionReason.LANGUAGE_KEY_NOT_VALID.equals(statusExceptionReason)) {
      return handleExceptionInternal(ex, "", customHttpHeader, HttpStatus.BAD_REQUEST, request);
    }
    return handleExceptionInternal(ex, "", customHttpHeader, HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(value = {TenantIdAllocationConflictException.class})
  @ResponseStatus(HttpStatus.CONFLICT)
  protected void handleTenantIdAllocationConflict(TenantIdAllocationConflictException e) {
    logger.info("Returning HTTP 409 Conflict for tenant ID allocation: " + e.getMessage());
  }

  @ExceptionHandler(value = {TenantAuthorisationException.class})
  protected ResponseEntity<Object> handle(TenantAuthorisationException ex, WebRequest request) {
    return handleExceptionInternal(
        ex, "", ex.getCustomHttpHeaders(), HttpStatus.FORBIDDEN, request);
  }

  @ExceptionHandler(value = {IllegalStateException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  protected void handleIllegalStateException(IllegalStateException e) {
    logger.warn("Returning HTTP 400 Bad Request for IllegalStateException", e);
  }

  @ExceptionHandler(value = {BadRequestException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public void handle(BadRequestException e) {
    logger.warn("Returning HTTP 400 Bad Request", e);
  }

  @ExceptionHandler(value = {ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public void handle(ConstraintViolationException e) {
    logger.warn(
        "Returning HTTP 400 Bad Request for invalid request constraints: " + e.getMessage());
  }

  /**
   * Catch-all for any exception that isn't handled above. Logs the full stack trace before
   * returning a generic 500 - previously such exceptions fell through to Spring's default handling
   * with no application-level logging at all (a silent 500).
   *
   * <p>Exceptions that already have a correct, framework-driven status translation are deliberately
   * rethrown instead of being logged-and-500'd here: {@link AccessDeniedException} needs to keep
   * propagating so Spring Security's {@code ExceptionTranslationFilter} can convert it to 403, and
   * any exception carrying {@link ResponseStatus} (or a {@link ResponseStatusException}) needs to
   * keep propagating so Spring's {@code ResponseStatusExceptionResolver} can apply its annotated
   * status (e.g. 404). Rethrowing from an {@code @ExceptionHandler} method is safe: Spring treats
   * that as "unresolved" and falls through to the next resolver in the chain instead of surfacing
   * the rethrown exception directly.
   */
  @ExceptionHandler(value = {Exception.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  protected void handleException(Exception e) throws Exception {
    if (isFrameworkHandledElsewhere(e)) {
      throw e;
    }
    logger.error("Returning HTTP 500 Internal Server Error", e);
  }

  private boolean isFrameworkHandledElsewhere(Exception e) {
    return e instanceof AccessDeniedException
        || e instanceof ResponseStatusException
        || AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class) != null;
  }
}
