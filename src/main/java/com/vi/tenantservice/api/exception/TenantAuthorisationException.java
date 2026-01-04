package com.vi.tenantservice.api.exception;

import com.vi.tenantservice.api.exception.httpresponse.CustomHttpHeader;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Getter
@Slf4j
public class TenantAuthorisationException extends RuntimeException {

  private final HttpHeaders customHttpHeaders;
  private final HttpStatus httpStatus;

  public TenantAuthorisationException(String message, HttpStatusExceptionReason exceptionReason) {
    super(message);
    this.customHttpHeaders = new CustomHttpHeader(exceptionReason).buildHeader();
    this.httpStatus = HttpStatus.FORBIDDEN;
  }
}
