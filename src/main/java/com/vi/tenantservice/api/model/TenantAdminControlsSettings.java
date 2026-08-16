package com.vi.tenantservice.api.model;

import com.vi.tenantservice.api.policy.PolicyValue;
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
   * Per-feature flags an upper role locks <em>on</em> for every lower role (Platform -> Träger ->
   * Beratungsstelle). {@code true} = enforced-on (lower roles cannot hide it); absent/{@code false}
   * = not enforced. Same shape as {@link #allowedPermissionToggles}. Stored in the same JSON blob;
   * absent on legacy rows (backward compatible).
   */
  TenantAdminAllowedPermissionTogglesSettings enforcedPermissionToggles;

  /** Canonical four-state policies. Legacy boolean maps remain readable during migration. */
  Map<String, PolicyValue<Boolean>> permissionPolicies;

  /** Platform defaults for reason-specific Case Handover policies. */
  CaseHandoverPolicies caseHandoverPolicies;

  /**
   * Platform-global machine-translation provider API keys (provider id -> raw key), e.g.
   * "openrouter"/"mistral". Stored in the same tenant_admin_controls JSON blob as the other
   * platform-global admin settings. Never exposed through the TenantAdminControls DTO - dedicated
   * endpoints return them masked only.
   */
  Map<String, String> translationApiKeys;
}
