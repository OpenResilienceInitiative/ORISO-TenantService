package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Only the current platform account creation defaults, without administrative controls. */
@RestController
public class PlatformAccountInactivityPolicyController {
  public static final String HEADER = "X-ORISO-Platform-Inactivity-Context";
  private final Environment environment;
  private final TaskServiceIdentity identity;
  private final TenantAdminControlsService controls;

  public PlatformAccountInactivityPolicyController(
      Environment environment, TaskServiceIdentity identity, TenantAdminControlsService controls) {
    this.environment = environment;
    this.identity = identity;
    this.controls = controls;
  }

  @GetMapping("/internal/platform/account-inactivity-policy")
  @org.springframework.security.access.prepost.PreAuthorize(
      "@taskServiceIdentity.allows(authentication, 'RUNTIME_POLICY')")
  public ResponseEntity<Policy> get(@RequestHeader(value = HEADER, required = false) String proof) {
    throw new org.springframework.security.access.AccessDeniedException(
        "Checked platform policy required");
  }

  public record Policy(int otherMonths, long revision) {}
}
