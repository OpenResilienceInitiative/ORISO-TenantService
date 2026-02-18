package com.vi.tenantservice.api.exception.httpresponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

/*
 * Custom http header with X-Reason header.
 */
@RequiredArgsConstructor
public class CustomHttpHeader {

  private final @NonNull HttpStatusExceptionReason httpStatusExceptionReason;

  public HttpHeaders buildHeader() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Reason", this.httpStatusExceptionReason.name());
    return headers;
  }
}
