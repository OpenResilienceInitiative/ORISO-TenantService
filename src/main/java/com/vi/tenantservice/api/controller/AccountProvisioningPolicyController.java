package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.AccountProvisioningPolicy;
import com.vi.tenantservice.config.security.WizardPolicyContext;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountProvisioningPolicyController {
  private final TenantServiceFacade tenants;
  private final WizardPolicyContext context;

  public AccountProvisioningPolicyController(
      TenantServiceFacade tenants, WizardPolicyContext context) {
    this.tenants = tenants;
    this.context = context;
  }

  @GetMapping("/internal/tenants/{id}/account-provisioning-policy")
  @PreAuthorize("@taskServiceIdentity.allows(authentication, 'CONFIG_WIZARD')")
  public ResponseEntity<AccountProvisioningPolicy> get(
      @PathVariable Long id,
      @RequestHeader(value = WizardPolicyContext.HEADER, required = false) String proof) {
    context.require(id, proof);
    return tenants
        .findAccountProvisioningPolicy(id)
        .map(policy -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(policy))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
