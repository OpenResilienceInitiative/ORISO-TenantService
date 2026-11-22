package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.SystemEmailContext;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemEmailContextController {
  private final TenantServiceFacade tenants;

  public SystemEmailContextController(TenantServiceFacade tenants) {
    this.tenants = tenants;
  }

  @GetMapping("/internal/tenants/{id}/system-email-context")
  @PreAuthorize("@taskServiceIdentity.allows(authentication, 'NOTIFICATION_DISPATCH')")
  public ResponseEntity<SystemEmailContext> get(@PathVariable Long id) {
    return tenants
        .findSystemEmailContext(id)
        .map(context -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(context))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
