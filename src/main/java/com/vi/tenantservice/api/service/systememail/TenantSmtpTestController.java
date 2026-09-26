package com.vi.tenantservice.api.service.systememail;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TenantSmtpTestController {
  private final TenantSmtpTestService test;

  public TenantSmtpTestController(TenantSmtpTestService test) {
    this.test = test;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Void> safeError(ResponseStatusException error) {
    var response =
        ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore());
    if (error.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) response.header("Retry-After", "60");
    return response.build();
  }

  @PostMapping("/tenant/{tenantId}/smtp-test-deliveries")
  @PreAuthorize("@tenantSmtpTestIdentity.allows(authentication, #tenantId)")
  public ResponseEntity<Void> send(
      @PathVariable long tenantId, @RequestBody(required = false) String body) {
    if (body != null && !body.isBlank()) {
      return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();
    }
    test.send(tenantId);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
