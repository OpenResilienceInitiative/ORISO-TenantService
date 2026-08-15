package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaAdminSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.repository.TenantDpaAdminSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Authoritative DPA status derivation and audit-proof in-app signing for authenticated tenant
 * admins (TEN-INV-U9, ORISO-TenantService#144).
 *
 * <p>Status is derived from three sources: the DPA version currently in force for the tenant
 * (embedded activation date with a fallback to the append-only publish history), the append-only
 * tenant admin signature audit trail, and the legacy token-based public-link signatures — a tenant
 * that validly confirmed via either channel counts as signed.
 *
 * <p>Governing document (#569, Frank's domain rule of 2026-07-29): the version a tenant is measured
 * against is resolved by {@link GoverningDpaResolver} — its own published DPA if it authored one,
 * otherwise the operator's. The same resolver serves the reading, signing and forwarding paths, so
 * a tenant is never measured against a document it cannot obtain.
 *
 * <p>NOTE (IDOR guard, same rule as {@link TenantDpaService}): this service takes a raw tenant id
 * and must only ever be called through a layer that derives/validates the tenant against the
 * authenticated principal (see {@code TenantDpaFacade}).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantDpaStatusService {

  /**
   * Authoritative DPA state of a tenant plus the facts it was derived from.
   *
   * <p>{@code forwardPending} is the orthogonal "contract on hold" fact (ORISO-TenantService#179):
   * the tenant is not validly signed AND at least one unexpired sign link is outstanding, i.e. the
   * agreement was forwarded and is awaiting the authorised signer. It is deliberately a separate
   * flag rather than a status value, so consumers keep receiving the UNSIGNED/OUTDATED they already
   * handle and can opt into the waiting state.
   */
  public record DpaStatusView(
      Long tenantId,
      TenantDpaStatus status,
      LocalDateTime currentVersion,
      LocalDateTime signedVersion,
      LocalDateTime signedAt,
      String signedBy,
      boolean forwardPending) {}

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
  private final GoverningDpaResolver governingDpaResolver;
  private final PlatformTransactionManager transactionManager;

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
        latestSigned == null ? null : latestSigned.signedBy(),
        isForwardPending(tenantId, status));
  }

  /**
   * "Contract on hold" (ORISO-TenantService#179): a not-yet-validly-signed tenant with an unexpired
   * outstanding sign link is waiting for the authorised signer, which the wizard, the legal gate
   * and the invite progress board need to tell apart from "nobody acted yet". A VALID, MISSING or
   * INCONSISTENT tenant is never reported as waiting, and the ledger is only queried when the
   * answer can be true — the common signed case costs no extra read.
   */
  private boolean isForwardPending(Long tenantId, TenantDpaStatus derived) {
    if (derived != TenantDpaStatus.UNSIGNED && derived != TenantDpaStatus.OUTDATED) {
      return false;
    }
    return signatureRepository.existsByTenantIdAndStatusAndTokenExpiresAtAfter(
        tenantId, DpaSignatureStatus.PENDING, LocalDateTime.now());
  }

  /**
   * Signs the tenant's currently published DPA version as the given authenticated tenant admin.
   * Append-only and idempotent-safe: an already-VALID tenant is returned unchanged without writing
   * a second row, and a concurrent duplicate insert (unique constraint on tenant + version) is
   * absorbed by re-reading the authoritative status.
   *
   * <p>Deliberately NOT {@code @Transactional}: the INSERT of the audit row runs in its own
   * REQUIRES_NEW transaction so that a unique-constraint violation from a concurrent double-submit
   * surfaces at THAT transaction's flush/commit — inside the catch below — instead of at an outer
   * flush outside any catch. With a surrounding transaction the losing request would (a) see the
   * violation escape this method (HTTP 500) or (b) after catching it, sit on a rollback-only
   * transaction that dies at commit with {@code UnexpectedRollbackException}. Keeping the method
   * non-transactional also guarantees the final re-read observes the winning, committed row
   * regardless of the database's isolation level. Real-flush-semantics proof:
   * TenantDpaStatusServiceConcurrencyIT.
   *
   * @throws DpaNotPublishedException if the tenant has no published DPA to sign.
   */
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
    persistSignature(tenantId, status.currentVersion(), signerUserId, signerUsername, form);
    return getStatus(tenantId);
  }

  /**
   * Records the DPA acceptance a tenant admin gave while onboarding through a public invite link
   * (#569). Same audit contract as {@link #sign}, with two deliberate differences:
   *
   * <ul>
   *   <li>the signer identity is passed in explicitly — the acceptance belongs to the freshly
   *       created tenant admin, not to the technical service account that carries the call. Only
   *       the tenant-creating authority may reach this path (see {@code TenantServiceFacade}).
   *   <li>the version signed is the one that was actually SHOWN to the signer. Recording the
   *       version currently in force instead would claim a signature on a text the signer never
   *       saw. If the operator republished in between, the resulting status is OUTDATED — blocked
   *       but signable in-app, never a dead end.
   * </ul>
   *
   * <p>{@code shownVersion} must be a genuinely published version of the governing document;
   * anything else is rejected so a caller cannot invent a contract version. {@code null} means "the
   * version currently in force".
   *
   * @throws DpaNotPublishedException if nothing is published to sign, or the shown version was
   *     never published.
   */
  public DpaStatusView signOnboarding(
      Long tenantId,
      String signerUserId,
      String signerUsername,
      LocalDateTime shownVersion,
      AdminSignatureForm form) {
    var status = getStatus(tenantId);
    var version = shownVersion == null ? status.currentVersion() : shownVersion;
    if (version == null) {
      throw new DpaNotPublishedException(
          "No data processing agreement is published for tenant " + tenantId + " to accept");
    }
    if (!version.equals(status.currentVersion()) && !isPublishedVersion(tenantId, version)) {
      throw new DpaNotPublishedException(
          "DPA version " + version + " was never published for tenant " + tenantId);
    }
    if (status.status() == TenantDpaStatus.VALID) {
      return status;
    }
    persistSignature(tenantId, version, signerUserId, signerUsername, form);
    return getStatus(tenantId);
  }

  private void persistSignature(
      Long tenantId,
      LocalDateTime version,
      String signerUserId,
      String signerUsername,
      AdminSignatureForm form) {
    var now = LocalDateTime.now();
    var entity =
        TenantDpaAdminSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(version)
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
            .build();
    var insertTransaction = new TransactionTemplate(transactionManager);
    insertTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      // the catch must wrap the whole transaction boundary: with a SEQUENCE-generated id the
      // INSERT is deferred until the flush at this template's commit, not save() itself
      insertTransaction.executeWithoutResult(tx -> adminSignatureRepository.save(entity));
    } catch (DataIntegrityViolationException e) {
      log.info(
          "Concurrent DPA signature for tenant {} version {} was absorbed (unique constraint)",
          tenantId,
          version);
    }
    invalidateOutstandingSignInvites(tenantId);
  }

  /**
   * ORISO-TenantService#179: the moment ANY signature is recorded — in-app or during onboarding —
   * every outstanding sign link of the tenant is invalidated. Runs in its own transaction because
   * {@link #sign} is deliberately non-transactional; idempotent (an absorbed concurrent duplicate
   * invalidates the same already-empty set again).
   */
  private void invalidateOutstandingSignInvites(Long tenantId) {
    var invalidateTransaction = new TransactionTemplate(transactionManager);
    invalidateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    invalidateTransaction.executeWithoutResult(
        tx -> signatureRepository.invalidateOutstandingByTenantId(tenantId));
  }

  /**
   * The DPA version currently in force for the tenant — its own published version if it has one,
   * otherwise the governing operator DPA version (see {@link GoverningDpaResolver}).
   */
  private LocalDateTime resolveCurrentVersion(Long tenantId) {
    var governing = governingDpaResolver.resolve(tenantId);
    return governing == null ? null : governing.version();
  }

  private boolean isPublishedVersion(Long tenantId, LocalDateTime version) {
    return governingDpaResolver.isPublishedVersion(tenantId, version);
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
