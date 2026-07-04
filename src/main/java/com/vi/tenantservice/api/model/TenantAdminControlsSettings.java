package com.vi.tenantservice.api.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAdminControlsSettings {
  boolean permissionsPageEnabled;
  TenantAdminAllowedPermissionTogglesSettings allowedPermissionToggles;

  /**
   * Platform-global machine-translation provider API keys (provider id -> raw key), e.g.
   * "openrouter"/"mistral". Stored in the same tenant_admin_controls JSON blob as the other
   * platform-global admin settings. Never exposed through the TenantAdminControls DTO - dedicated
   * endpoints return them masked only.
   */
  Map<String, String> translationApiKeys;
}
