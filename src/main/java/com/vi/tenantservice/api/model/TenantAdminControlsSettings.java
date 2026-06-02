package com.vi.tenantservice.api.model;

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
}
