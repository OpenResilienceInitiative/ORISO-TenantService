package com.vi.tenantservice.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vi.tenantservice.api.policy.LenientPolicyValueMapDeserializer;
import com.vi.tenantservice.api.policy.PolicyValue;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deserialized from the single {@code tenant_admin_controls.controls} JSON blob.
 *
 * <p>Unknown fields are ignored on purpose. The blob is shared: a newer build may add keys this
 * version has never heard of, and rolling that build back must not take the service down. This type
 * is read on the bootstrap path — {@code getControls()} -> {@code withEffectivePermissions()} ->
 * {@code GET /tenant/public/id/{id}} — so a parse failure is not a degraded feature, it is HTTP 500
 * on the first request of every session. That happened on Pre-Dev on 2026-08-18, when a build
 * without {@code permissionPolicies} and {@code caseHandoverPolicies} met a blob written by a build
 * that had them.
 *
 * <p>Tolerance here is deliberate and one-way: reading leniently costs nothing, while a strict read
 * turns any version skew into an outage. {@code JsonConverter} and {@code CookieTenantResolver}
 * take the same position for the same reason.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
  @JsonDeserialize(using = LenientPolicyValueMapDeserializer.class)
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
