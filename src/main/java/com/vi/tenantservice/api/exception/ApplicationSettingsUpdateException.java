package com.vi.tenantservice.api.exception;

import org.springframework.web.client.RestClientException;

public class ApplicationSettingsUpdateException extends ConsultingTypeCommunicationException {

  public ApplicationSettingsUpdateException(String message, RestClientException ex) {
    super(message, ex);
  }
}
