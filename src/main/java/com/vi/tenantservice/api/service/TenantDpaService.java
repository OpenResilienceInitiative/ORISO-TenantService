package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
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

  private final TenantDpaSignatureRepository signatureRepository;
  private final TenantDpaVersionRepository versionRepository;

  /** Persists a SIGNED confirmation of the given DPA version for a tenant. */
  public TenantDpaSignatureEntity recordSignature(
      Long tenantId,
      LocalDateTime dpaVersion,
      String signerName,
      String signerPosition,
      boolean signerIsMember,
      String language) {
    var now = LocalDateTime.now();
    return signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(dpaVersion)
            .signerName(signerName)
            .signerPosition(signerPosition)
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
   */
  public String createSignInvite(Long tenantId, LocalDateTime dpaVersion, Duration ttl) {
    var rawToken = DpaSignToken.generate();
    var now = LocalDateTime.now();
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(dpaVersion)
            .status(DpaSignatureStatus.PENDING)
            .tokenHash(DpaSignToken.hash(rawToken))
            .tokenExpiresAt(now.plus(ttl))
            .createDate(now)
            .build());
    return rawToken;
  }

  /**
   * Confirms a DPA via its single-use sign token: looks up the PENDING row by token hash, validates
   * it is not expired, records the signer, marks it SIGNED, and consumes the token.
   *
   * @throws InvalidDpaSignTokenException if the token is unknown, already used, or expired.
   */
  @Transactional
  public TenantDpaSignatureEntity confirmSignature(
      String rawToken,
      String signerName,
      String signerPosition,
      boolean signerIsMember,
      String language) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new InvalidDpaSignTokenException("Missing sign token");
    }
    var tokenHash = DpaSignToken.hash(rawToken);
    var pending =
        signatureRepository
            .findByTokenHashAndStatus(tokenHash, DpaSignatureStatus.PENDING)
            .orElseThrow(
                () -> new InvalidDpaSignTokenException("Unknown or already-used sign token"));
    if (pending.getTokenExpiresAt() == null
        || pending.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidDpaSignTokenException("Sign token has expired");
    }
    var now = LocalDateTime.now();
    // Atomic single-use: only the still-PENDING row is updated, so a concurrent double-submit
    // produces exactly one winner (1 row) and the rest see 0.
    int consumed =
        signatureRepository.consumeSignToken(
            tokenHash, signerName, signerPosition, signerIsMember, language, now);
    if (consumed == 0) {
      throw new InvalidDpaSignTokenException("Sign token has already been used");
    }
    // The bulk update detached the context (clearAutomatically); build the confirmed view to
    // return.
    pending.setSignerName(signerName);
    pending.setSignerPosition(signerPosition);
    pending.setSignerIsMember(signerIsMember);
    pending.setLanguage(language);
    pending.setStatus(DpaSignatureStatus.SIGNED);
    pending.setSignedAt(now);
    pending.setTokenHash(null);
    return pending;
  }
}
