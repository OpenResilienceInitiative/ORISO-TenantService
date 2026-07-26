package com.vi.tenantservice.api.service;

/** The uploaded bytes are not one of the supported raster image formats (PNG/JPEG/WebP). */
public class UnsupportedMediaContentException extends RuntimeException {
  public UnsupportedMediaContentException(String message) {
    super(message);
  }
}
