package com.vi.tenantservice.api.service.systememail;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
public class SystemEmailDeliveryController {
  private final SystemEmailDeliveryService delivery;

  public SystemEmailDeliveryController(SystemEmailDeliveryService delivery) {
    this.delivery = delivery;
  }

  // Default MVC validation logging includes rejected values, potentially the entire mail body.
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<Void> invalidDelivery() {
    return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();
  }

  @PostMapping("/tenant/{tenantId}/internal/system-email-deliveries")
  @PreAuthorize("@systemEmailServiceIdentity.allows(authentication)")
  public ResponseEntity<Void> deliver(
      @PathVariable long tenantId, @Valid @RequestBody SystemEmailDeliveryRequest request) {
    boolean sent = delivery.deliver(tenantId, request);
    return ResponseEntity.status(sent ? 200 : 204).cacheControl(CacheControl.noStore()).build();
  }
}
