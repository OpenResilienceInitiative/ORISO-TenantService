package com.vi.tenantservice.api.model;

/**
 * State of a tenant's Data Processing Agreement (Auftragsverarbeitungsvertrag) confirmation.
 *
 * <ul>
 *   <li>{@code PENDING} — invite sent, not yet confirmed (blocks platform work for the tenant).
 *   <li>{@code SIGNED} — confirmed; unlocks tenant functionality.
 *   <li>{@code DENIED} — explicitly refused; the tenant/unit is set inactive and auto-deleted after
 *       365 days.
 * </ul>
 */
public enum DpaSignatureStatus {
  PENDING,
  SIGNED,
  DENIED
}
