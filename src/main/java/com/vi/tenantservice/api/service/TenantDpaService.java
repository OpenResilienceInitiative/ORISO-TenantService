package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and queries tenant confirmations ("signatures") of the Data Processing Agreement
 * (Auftragsverarbeitungsvertrag). Design-stable core: it deals only with the signature record and
 * the gate derived from it, independent of where the DPA <em>content</em> ultimately lives.
 *
 * <p>NOTE (from security review): {@link #getSignatures} and {@link #isSignedForVersion} must never
 * be exposed via an endpoint that takes a caller-supplied tenant id — the calling layer must derive
 * the tenant from the authenticated principal and guard it with admin authorisation, otherwise the
 * signer PII becomes an enumerable IDOR. The public sign flow is token-based (hashed, single-use,
 * expiring) and consumed atomically in {@link #confirmSignature}.
 */
@Service
@RequiredArgsConstructor
public class TenantDpaService {

  /** Public, read-only view of the exact published contract referenced by a valid sign token. */
  public record DpaSignPreview(
      Long tenantId,
      String tenantName,
      LocalDateTime dpaVersion,
      String content,
      LocalDateTime expiresAt) {}

  /** Signature source of every token-based sign link (the forwarded path of the agreement). */
  public static final String SOURCE_FORWARDED_EXTERNAL = "FORWARDED_EXTERNAL";

  /**
   * Placeholder organisation name for a sign link issued before the tenant registration finished.
   * Same wording the DPA forward e-mail uses, so the signer sees one consistent phrasing.
   */
  static final String UNREGISTERED_TENANT_NAME = "Ihrer Organisation";

  private final TenantDpaSignatureRepository signatureRepository;
  private final TenantDpaVersionRepository versionRepository;
  private final TenantRepository tenantRepository;
  private final TenantIdReservationRepository tenantIdReservationRepository;
  private final GoverningDpaResolver governingDpaResolver;

  /** Persists a SIGNED confirmation of the given DPA version for a tenant. */
  public TenantDpaSignatureEntity recordSignature(
      Long tenantId,
      LocalDateTime dpaVersion,
      String signerName,
      String signerPosition,
      boolean signerIsMember,
      String language) {
    return recordSignature(
        tenantId,
        dpaVersion,
        signerName,
        signerPosition,
        null,
        null,
        null,
        null,
        signerIsMember,
        language);
  }

  /** Persists a SIGNED confirmation of the given DPA version for a tenant. */
  public TenantDpaSignatureEntity recordSignature(
      Long tenantId,
      LocalDateTime dpaVersion,
      String signerName,
      String signerPosition,
      String signerEmail,
      String signerOrganisation,
      String forwardedByUserId,
      String source,
      boolean signerIsMember,
      String language) {
    var now = LocalDateTime.now();
    return signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(dpaVersion)
            .signerName(signerName)
            .signerPosition(signerPosition)
            .signerEmail(signerEmail)
            .signerOrganisation(signerOrganisation)
            .forwardedByUserId(forwardedByUserId)
            .source(normalizeSource(source))
            .signerIsMember(signerIsMember)
            .language(language)
            .status(DpaSignatureStatus.SIGNED)
            .signedAt(now)
            .createDate(now)
            .build());
  }

  /** Whether the tenant has a SIGNED confirmation for exactly the given DPA version (the gate). */
  public boolean isSignedForVersion(Long tenantId, LocalDateTime dpaVersion) {
    if (dpaVersion == null) {
      return false;
    }
    return signatureRepository.findByTenantIdAndStatus(tenantId, DpaSignatureStatus.SIGNED).stream()
        .anyMatch(signature -> dpaVersion.equals(signature.getDpaVersion()));
  }

  /** All confirmations for a tenant (for the platform-admin list of confirmed AVVs). */
  public List<TenantDpaSignatureEntity> getSignatures(Long tenantId) {
    return signatureRepository.findByTenantId(tenantId);
  }

  /** Appends a published-version snapshot (called on every publish) for the "look back" history. */
  public void recordPublishedVersion(Long tenantId, String content, LocalDateTime activationDate) {
    versionRepository.save(
        TenantDpaVersionEntity.builder()
            .tenantId(tenantId)
            .content(content)
            .activationDate(activationDate)
            .createDate(LocalDateTime.now())
            .build());
  }

  /** Published DPA versions for a tenant, newest first. */
  public List<TenantDpaVersionEntity> getVersions(Long tenantId) {
    return versionRepository.findByTenantIdOrderByActivationDateDesc(tenantId);
  }

  /**
   * Creates a PENDING confirmation carrying a single-use, expiring sign token and returns the RAW
   * token (caller builds the public sign link from it). Only the token's hash is persisted.
   *
   * <p>The forwarder identity is stamped HERE (ORISO-TenantService#179), not on confirmation: the
   * signing client must never be able to claim who created the link. {@code forwardedByUserId} is
   * the authenticated admin who forwarded, or {@code null} when the link was created from the
   * pre-account onboarding wizard.
   */
  public String createSignInvite(
      Long tenantId, LocalDateTime dpaVersion, Duration ttl, String forwardedByUserId) {
    var rawToken = DpaSignToken.generate();
    var now = LocalDateTime.now();
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(dpaVersion)
            .status(DpaSignatureStatus.PENDING)
            .forwardedByUserId(forwardedByUserId)
            .source(SOURCE_FORWARDED_EXTERNAL)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(now.plus(ttl))
            .createDate(now)
            .build());
    return rawToken;
  }

  /**
   * Resolves the exact immutable DPA snapshot bound to a still-valid invitation. The token is only
   * read here; previewing the contract never consumes it.
   *
   * <p>The snapshot is looked up through the governing document (#569): a tenant without a DPA of
   * its own forwards the operator document under its own tenant id, so the published version lives
   * on the operator while the signature belongs to the tenant.
   */
  @Transactional(readOnly = true)
  public DpaSignPreview getSignPreview(String rawToken) {
    var pending = requireValidPendingSignature(rawToken);
    var publishedVersion =
        governingDpaResolver
            .findPublishedVersion(pending.getTenantId(), pending.getDpaVersion())
            .orElseThrow(
                () ->
                    new InvalidDpaSignTokenException(
                        "Published DPA version for sign token was not found"));
    var tenantName =
        tenantRepository
            .findById(pending.getTenantId())
            .map(tenant -> tenant.getName())
            .orElseGet(() -> tenantNameFallbackForReservedTenant(pending.getTenantId()));
    return new DpaSignPreview(
        pending.getTenantId(),
        tenantName,
        pending.getDpaVersion(),
        publishedVersion.getContent(),
        pending.getTokenExpiresAt());
  }

  /**
   * A sign link created from the public onboarding wizard is bound to a RESERVED tenant id whose
   * tenant row does not exist yet (ORISO-TenantService#179). The organisation has no stored name
   * until the registration completes, so the preview carries the same neutral placeholder the
   * forward e-mail already uses — {@code tenantName} stays a non-null part of the contract, and
   * showing the real organisation on the signing page is tracked in ORISO-Frontend#879.
   *
   * <p>A pending row whose tenant neither exists nor is reserved is treated as an invalid token —
   * fail closed, nothing about internal state leaks.
   */
  private String tenantNameFallbackForReservedTenant(Long tenantId) {
    if (tenantIdReservationRepository.existsById(tenantId)) {
      return UNREGISTERED_TENANT_NAME;
    }
    throw new InvalidDpaSignTokenException("Tenant for sign token was not found");
  }

  /**
   * Confirms a DPA via its single-use sign token: looks up the PENDING row by token hash, validates
   * it is not expired, records the signer, marks it SIGNED, and consumes the token. The forwarder
   * identity ({@code forwardedByUserId}, {@code source}) is NOT taken from the caller — it was
   * stamped when the link was created and stays untouched (ORISO-TenantService#179).
   *
   * <p>The moment the signature is recorded, every OTHER outstanding sign link of the tenant is
   * invalidated in the same transaction — a signed tenant has no live links left.
   *
   * @throws InvalidDpaSignTokenException if the token is unknown, already used, or expired.
   */
  @Transactional
  public TenantDpaSignatureEntity confirmSignature(
      String rawToken,
      String signerName,
      String signerPosition,
      String signerEmail,
      String signerOrganisation,
      boolean signerIsMember,
      String language) {
    var pending = requireValidPendingSignature(rawToken);
    var tokenHash = DpaSignToken.hash(rawToken);
    var now = LocalDateTime.now();
    // Atomic single-use: only the still-PENDING row is updated, so a concurrent double-submit
    // produces exactly one winner (1 row) and the rest see 0.
    int consumed =
        signatureRepository.consumeSignToken(
            tokenHash,
            signerName,
            signerPosition,
            signerEmail,
            signerOrganisation,
            signerIsMember,
            language,
            now);
    if (consumed == 0) {
      throw new InvalidDpaSignTokenException("Sign token has already been used");
    }
    // ORISO-TenantService#179: kill every other outstanding link of this tenant (the consumed row
    // is already SIGNED, so the conditional bulk update cannot touch it).
    signatureRepository.invalidateOutstandingByTenantId(pending.getTenantId());
    // The bulk update detached the context (clearAutomatically); build the confirmed view to
    // return. Forwarder identity and source keep the values stamped at link creation; legacy
    // pending rows without a stamped source read as the forwarded path they are.
    pending.setSignerName(signerName);
    pending.setSignerPosition(signerPosition);
    pending.setSignerEmail(signerEmail);
    pending.setSignerOrganisation(signerOrganisation);
    pending.setSource(normalizeSource(pending.getSource()));
    pending.setSignerIsMember(signerIsMember);
    pending.setLanguage(language);
    pending.setStatus(DpaSignatureStatus.SIGNED);
    pending.setSignedAt(now);
    pending.setTokenHash(null);
    return pending;
  }

  /**
   * Whether an unexpired sign link is still outstanding for the tenant — the "contract on hold"
   * predicate behind {@code PENDING_FORWARDED} (ORISO-TenantService#179).
   */
  public boolean hasOutstandingSignInvite(Long tenantId) {
    return signatureRepository.existsByTenantIdAndStatusAndTokenExpiresAtAfter(
        tenantId, DpaSignatureStatus.PENDING, LocalDateTime.now());
  }

  /**
   * Invalidates every outstanding sign link of the tenant. Called whenever a signature is recorded
   * through a NON-token path (in-app admin signing, onboarding acceptance) so that no live link
   * survives any successful signature (ORISO-TenantService#179).
   */
  @Transactional
  public void invalidateOutstandingSignInvites(Long tenantId) {
    signatureRepository.invalidateOutstandingByTenantId(tenantId);
  }

  /**
   * Removes every signature row of a deleted tenant — the application-level replacement for the
   * database cascade that had to go when sign links for still-unregistered (reserved) tenants were
   * introduced (#179, changeset 0028).
   */
  @Transactional
  public void deleteSignaturesForTenant(Long tenantId) {
    signatureRepository.deleteByTenantId(tenantId);
  }

  private TenantDpaSignatureEntity requireValidPendingSignature(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new InvalidDpaSignTokenException("Missing sign token");
    }
    var pending =
        signatureRepository
            .findByTokenHashAndStatus(DpaSignToken.hash(rawToken), DpaSignatureStatus.PENDING)
            .orElseThrow(
                () -> new InvalidDpaSignTokenException("Unknown or already-used sign token"));
    if (pending.getTokenExpiresAt() == null
        || pending.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidDpaSignTokenException("Sign token has expired");
    }
    return pending;
  }

  private static String normalizeSource(String source) {
    return source == null || source.isBlank() ? SOURCE_FORWARDED_EXTERNAL : source;
  }
}
