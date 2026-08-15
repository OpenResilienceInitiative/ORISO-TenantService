package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for "which data processing agreement document is in force for this tenant,
 * and which version of it" (#569, Frank's domain rule of 2026-07-29).
 *
 * <p>There is exactly ONE data processing agreement relationship — platform operator &lt;-&gt;
 * tenant — and one governing, versioned operator DPA document (held by tenant {@code
 * app.dpa.operator-tenant-id}). A tenant does not author a DPA of its own, so a tenant WITHOUT its
 * own published DPA is governed by the operator document. Tenants that DO carry their own published
 * DPA keep being measured against it: that legacy per-tenant authoring predates the rule and
 * retiring it is a separate product decision, so the fallback is deliberately additive — no tenant
 * that is VALID today can be turned into a blocked one by it.
 *
 * <p>Every DPA path resolves the governing document through this component, so the status the
 * tenant is measured against, the document it can read, the version it signs in-app and the version
 * it forwards for signature can never diverge. A divergence is exactly the non-actionable dead end
 * (blocked with nothing to sign) that #569 exists to remove.
 *
 * <p>NOTE (IDOR guard): like the DPA services this component takes a raw tenant id and must only be
 * reached through a layer that validates the tenant against the authenticated principal.
 */
@Component
@RequiredArgsConstructor
public class GoverningDpaResolver {

  /**
   * The DPA document in force for a tenant: the tenant that OWNS the published document (the tenant
   * itself, or the operator) and the version currently in force.
   */
  public record GoverningDpa(long documentTenantId, LocalDateTime version) {}

  private final TenantRepository tenantRepository;
  private final TenantDpaVersionRepository versionRepository;

  /**
   * Tenant holding the governing operator DPA document; {@code 0} or negative disables the fallback
   * (then a tenant without its own published DPA has no DPA at all, the pre-#569 behaviour).
   */
  @Value("${app.dpa.operator-tenant-id:1}")
  private long operatorTenantId;

  /**
   * The DPA document in force for the tenant, or {@code null} when none is: the tenant does not
   * exist, or neither it nor the operator has published anything. A missing tenant never resolves a
   * document — an absent tenant must never read as "has a contract to sign".
   */
  public GoverningDpa resolve(Long tenantId) {
    var tenant = tenantRepository.findById(tenantId);
    if (tenant.isEmpty()) {
      return null;
    }
    var own =
        publishedVersionOf(
            tenantId, tenant.get().getContentDataProcessingAgreementActivationDate());
    if (own != null) {
      return new GoverningDpa(tenantId, own);
    }
    if (!operatorFallbackApplies(tenantId)) {
      return null;
    }
    var operatorVersion =
        tenantRepository
            .findById(operatorTenantId)
            .map(
                entity ->
                    publishedVersionOf(
                        operatorTenantId, entity.getContentDataProcessingAgreementActivationDate()))
            .orElse(null);
    return operatorVersion == null ? null : new GoverningDpa(operatorTenantId, operatorVersion);
  }

  /**
   * The DPA document in force for a tenant that does NOT exist yet (ORISO-TenantService#179): a
   * sign link created from the public onboarding wizard is bound to a RESERVED tenant id. Such a
   * tenant cannot have authored an own DPA, so the governing document is always the operator's —
   * exactly what {@link #resolve} would answer the moment the registration completes. {@code null}
   * when the operator fallback is disabled or nothing is published.
   */
  public GoverningDpa resolveForUnregisteredTenant() {
    if (operatorTenantId <= 0) {
      return null;
    }
    var operatorVersion =
        tenantRepository
            .findById(operatorTenantId)
            .map(
                entity ->
                    publishedVersionOf(
                        operatorTenantId, entity.getContentDataProcessingAgreementActivationDate()))
            .orElse(null);
    return operatorVersion == null ? null : new GoverningDpa(operatorTenantId, operatorVersion);
  }

  /**
   * The tenant that owns the DPA document readable by the given tenant. Falls back to the tenant
   * itself when nothing is published anywhere, so a caller listing versions gets that tenant's
   * (empty) history rather than an unrelated one.
   */
  public long documentTenantIdFor(Long tenantId) {
    var governing = resolve(tenantId);
    return governing == null ? tenantId : governing.documentTenantId();
  }

  /**
   * The published snapshot of the given version as it applies to the tenant: its own publish
   * history first, then the governing operator history (a tenant signs and forwards the operator
   * document under its OWN tenant id, so the snapshot lives on the operator).
   */
  public Optional<TenantDpaVersionEntity> findPublishedVersion(
      Long tenantId, LocalDateTime version) {
    if (version == null) {
      return Optional.empty();
    }
    var own = versionRepository.findFirstByTenantIdAndActivationDate(tenantId, version);
    if (own.isPresent() || !operatorFallbackApplies(tenantId)) {
      return own;
    }
    return versionRepository.findFirstByTenantIdAndActivationDate(operatorTenantId, version);
  }

  /** Whether the version exists in the publish history governing this tenant. */
  public boolean isPublishedVersion(Long tenantId, LocalDateTime version) {
    return findPublishedVersion(tenantId, version).isPresent();
  }

  /**
   * A tenant's own published DPA version: the passed embedded activation date, falling back to the
   * newest entry of the append-only publish history when a legacy narrow tenant update cleared it.
   */
  private LocalDateTime publishedVersionOf(Long tenantId, LocalDateTime embeddedVersion) {
    if (embeddedVersion != null) {
      return embeddedVersion;
    }
    return versionRepository.findByTenantIdOrderByActivationDateDesc(tenantId).stream()
        .map(TenantDpaVersionEntity::getActivationDate)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private boolean operatorFallbackApplies(Long tenantId) {
    return operatorTenantId > 0 && !Long.valueOf(operatorTenantId).equals(tenantId);
  }
}
