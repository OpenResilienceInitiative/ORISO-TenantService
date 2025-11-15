package com.vi.tenantservice.api.service;

public class TemplateDescriptionServiceException extends Exception {

  public TemplateDescriptionServiceException(String format, Exception ex) {
    super(format, ex);
  }
}
