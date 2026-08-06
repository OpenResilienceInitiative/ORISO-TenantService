package com.vi.tenantservice.api.model;

/**
 * Authoritative Data Processing Agreement (Auftragsverarbeitungsvertrag) state of a tenant, as
 * exposed to its authenticated tenant admins (TEN-INV-U9, ORISO-TenantService#144).
 *
 * <ul>
 *   <li>{@code MISSING} — no DPA has ever been published for the tenant and nothing is signed.
 *   <li>{@code UNSIGNED} — a DPA is published but no version has ever been signed.
 *   <li>{@code OUTDATED} — only an older published version is signed; the current one is not.
 *   <li>{@code VALID} — the currently published version is signed.
 *   <li>{@code INCONSISTENT} — the stored data contradicts itself (e.g. a signature without a
 *       version, a signature newer than the currently published version, or signatures for a tenant
 *       that has no published DPA at all).
 * </ul>
 */
public enum TenantDpaStatus {
  MISSING,
  UNSIGNED,
  OUTDATED,
  VALID,
  INCONSISTENT
}
