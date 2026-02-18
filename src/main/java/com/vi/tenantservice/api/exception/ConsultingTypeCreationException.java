package com.vi.tenantservice.api.exception;

import org.springframework.web.client.RestClientException;

public class ConsultingTypeCreationException extends Exception {

  public ConsultingTypeCreationException(String message, RestClientException ex) {
    super(message, ex);
  }
}
