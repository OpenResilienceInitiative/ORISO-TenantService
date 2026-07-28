package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaAdminSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.repository.TenantDpaAdminSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative DPA status derivation and audit-proof in-app signing for authenticated tenant
 * admins (TEN-INV-U9, ORISO-TenantService#144).
 *
 * <p>Status is derived from three sources: the tenant's currently published DPA version (embedded
 * activation date with a fallback to the append-only publish history), the append-only tenant admin
 * signature audit trail, and the legacy token-based public-link signatures — a tenant that validly
 * confirmed via either channel counts as signed.
 *
 * <p>NOTE (IDOR guard, same rule as {@link TenantDpaService}): this service takes a raw tenant id
 * and must only ever be called through a layer that derives/validates the tenant against the
 * authenticated principal (see {@code TenantDpaFacade}).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantDpaStatusService {

  /** Authoritative DPA state of a tenant plus the facts it was derived from. */
  public record DpaStatusView(
      Long tenantId,
      TenantDpaStatus status,
      LocalDateTime currentVersion,
      LocalDateTime signedVersion,
      LocalDateTime signedAt,
      String signedBy) {}

  /** The submitted sign form: structured signer fields plus the verbatim JSON snapshot. */
  public record AdminSignatureForm(
      String signerName,
      String signerPosition,
      String signerEmail,
      String signerOrganisation,
      String language,
      String formDataJson) {}

  private record SignedEntry(LocalDateTime version, LocalDateTime signedAt, String signedBy) {}

  private final TenantDpaAdminSignatureRepository adminSignatureRepository;
  private final TenantDpaSignatureRepository signatureRepository;
  private final TenantDpaVersionRepository versionRepository;
  private final TenantRepository tenantRepository;

  /** Computes the authoritative DPA state for the tenant. */
  @Transactional(readOnly = true)
  public DpaStatusView getStatus(Long tenantId) {
    var currentVersion = resolveCurrentVersion(tenantId);
    var signedEntries = collectSignedEntries(tenantId);
    var status = deriveStatus(currentVersion, signedEntries);
    var latestSigned = latestSignedEntry(signedEntries);
    return new DpaStatusView(
        tenantId,
        status,
        currentVersion,
        latestSigned == null ? null : latestSigned.version(),
        latestSigned == null ? null : latestSigned.signedAt(),
        latestSigned == null ? null : latestSigned.signedBy());
  }

  /**
   * Signs the tenant's currently published DPA version as the given authenticated tenant admin.
   * Append-only and idempotent-safe: an already-VALID tenant is returned unchanged without writing
   * a second row, and a concurrent duplicate insert (unique constraint on tenant + version) is
   * absorbed by re-reading the authoritative status.
   *
   * @throws DpaNotPublishedException if the tenant has no published DPA to sign.
   */
  @Transactional
  public DpaStatusView sign(
      Long tenantId, String signerUserId, String signerUsername, AdminSignatureForm form) {
    var status = getStatus(tenantId);
    if (status.currentVersion() == null) {
      throw new DpaNotPublishedException(
          "Tenant " + tenantId + " has no published DPA to sign yet");
    }
    if (status.status() == TenantDpaStatus.VALID) {
      return status;
    }
    var now = LocalDateTime.now();
    try {
      adminSignatureRepository.save(
          TenantDpaAdminSignatureEntity.builder()
              .tenantId(tenantId)
              .dpaVersion(status.currentVersion())
              .signerUserId(signerUserId)
              .signerUsername(signerUsername)
              .signerName(form.signerName())
              .signerPosition(form.signerPosition())
              .signerEmail(form.signerEmail())
              .signerOrganisation(form.signerOrganisation())
              .language(form.language())
              .formData(form.formDataJson())
              .signedAt(now)
              .createDate(now)
              .build());
    } catch (DataIntegrityViolationException e) {
      log.info(
          "Concurrent DPA signature for tenant {} version {} was absorbed (unique constraint)",
          tenantId,
          status.currentVersion());
    }
    return getStatus(tenantId);
  }

  /**
   * The currently published DPA version: the tenant row's embedded activation date, falling back to
   * the newest entry of the append-only publish history when a legacy narrow tenant update cleared
   * the embedded date. A missing tenant never falls back.
   */
  private LocalDateTime resolveCurrentVersion(Long tenantId) {
    var tenant = tenantRepository.findById(tenantId);
    if (tenant.isEmpty()) {
      return null;
    }
    var embedded = tenant.get().getContentDataProcessingAgreementActivationDate();
    if (embedded != null) {
      return embedded;
    }
    return versionRepository.findByTenantIdOrderByActivationDateDesc(tenantId).stream()
        .map(TenantDpaVersionEntity::getActivationDate)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private List<SignedEntry> collectSignedEntries(Long tenantId) {
    var entries = new ArrayList<SignedEntry>();
    adminSignatureRepository
        .findByTenantIdOrderBySignedAtDescIdDesc(tenantId)
        .forEach(
            row ->
                entries.add(
                    new SignedEntry(
                        row.getDpaVersion(),
                        row.getSignedAt(),
                        row.getSignerName() != null
                            ? row.getSignerName()
                            : row.getSignerUsername())));
    signatureRepository
        .findByTenantIdAndStatus(tenantId, DpaSignatureStatus.SIGNED)
        .forEach(
            row ->
                entries.add(
                    new SignedEntry(row.getDpaVersion(), row.getSignedAt(), row.getSignerName())));
    return entries;
  }

  private TenantDpaStatus deriveStatus(LocalDateTime currentVersion, List<SignedEntry> signed) {
    if (currentVersion == null) {
      return signed.isEmpty() ? TenantDpaStatus.MISSING : TenantDpaStatus.INCONSISTENT;
    }
    boolean inconsistent =
        signed.stream()
            .anyMatch(entry -> entry.version() == null || entry.version().isAfter(currentVersion));
    if (inconsistent) {
      return TenantDpaStatus.INCONSISTENT;
    }
    boolean currentSigned =
        signed.stream().anyMatch(entry -> currentVersion.equals(entry.version()));
    if (currentSigned) {
      return TenantDpaStatus.VALID;
    }
    return signed.isEmpty() ? TenantDpaStatus.UNSIGNED : TenantDpaStatus.OUTDATED;
  }

  /** The signature covering the newest version (signing time breaks ties). */
  private SignedEntry latestSignedEntry(List<SignedEntry> signed) {
    return signed.stream()
        .max(
            Comparator.comparing(
                    SignedEntry::version, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(
                    SignedEntry::signedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
  }
}
