package com.vi.tenantservice.api.service;

/** The uploaded file exceeds the media size limit. */
public class MediaSizeLimitExceededException extends RuntimeException {
  public MediaSizeLimitExceededException(String message) {
    super(message);
  }
}
