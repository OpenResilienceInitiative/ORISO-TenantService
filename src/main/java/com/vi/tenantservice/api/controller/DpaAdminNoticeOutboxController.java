package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.service.DpaAdminNoticeOutboxService;
import com.vi.tenantservice.api.service.DpaAdminNoticeOutboxService.ExactSignature;
import com.vi.tenantservice.api.service.DpaAdminNoticeOutboxService.PendingPage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tenant.dpa.admin-notice-outbox.pull-enabled", havingValue = "true")
class DpaAdminNoticeOutboxController {

  private final DpaAdminNoticeOutboxService outbox;

  @GetMapping("/internal/dpa-admin-notice-outbox")
  @PreAuthorize("@taskServiceIdentity.allows(authentication, 'NOTIFICATION_DISPATCH')")
  public ResponseEntity<PendingPage> readPending(
      @RequestParam(defaultValue = "0") long afterId,
      @RequestParam(defaultValue = "50") int limit) {
    if (afterId < 0) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(outbox.readPending(afterId, limit));
  }

  @GetMapping("/internal/dpa-admin-notice-outbox/{eventId:[0-9]+}")
  @PreAuthorize("@taskServiceIdentity.allows(authentication, 'NOTIFICATION_DISPATCH')")
  public ResponseEntity<ExactSignature> readExact(@PathVariable long eventId) {
    return outbox
        .readExact(eventId)
        .map(signature -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(signature))
        .orElseGet(
            () ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.noStore())
                    .build());
  }
}
