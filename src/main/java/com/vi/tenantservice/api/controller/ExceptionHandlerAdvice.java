package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.exception.TenantAuthorisationException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import javax.ws.rs.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
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

  @ExceptionHandler(value = {TenantAuthorisationException.class})
  protected ResponseEntity<Object> handle(TenantAuthorisationException ex, WebRequest request) {
    return handleExceptionInternal(
        ex, "", ex.getCustomHttpHeaders(), HttpStatus.FORBIDDEN, request);
  }

  @ExceptionHandler(value = {IllegalStateException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  protected void handleIllegalStateException() {
    // status code is set with ResponseStatus
  }

  @ExceptionHandler(value = {BadRequestException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public void handle(HttpMessageNotReadableException e) {
    logger.warn("Returning HTTP 400 Bad Request", e);
  }
}
