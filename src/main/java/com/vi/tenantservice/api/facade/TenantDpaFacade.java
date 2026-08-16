package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.model.DpaAdminSignRequestDTO;
import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaStatusDTO;
import com.vi.tenantservice.api.model.DpaVersionDTO;
import com.vi.tenantservice.api.model.PublicDpaForwardRequestDTO;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaStatus;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.GoverningDpaResolver;
import com.vi.tenantservice.api.service.InvalidDpaSignTokenException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.api.service.TenantDpaStatusService;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.util.JsonConverter;
import com.vi.tenantservice.api.util.TranslationMetaUtil;
import com.vi.tenantservice.api.validation.InputSanitizer;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tenant-scoped, authorisation-guarded entry point for the admin-facing DPA queries. Every method
 * runs {@link TenantFacadeAuthorisationService#assertUserIsAuthorizedToAccessTenant} first, so a
 * single-tenant admin can only ever see their own tenant's data — this is the IDOR guard the
 * security review required (the underlying {@link TenantDpaService} takes a raw tenant id and must
 * never be exposed without it).
 */
@Service
@RequiredArgsConstructor
public class TenantDpaFacade {

  private static final Duration INVITE_TTL = Duration.ofDays(14);

  /**
   * How many unexpired sign links one onboarding may have outstanding at once. The public forward
   * needs no session, so this is what stops a leaked onboarding token from minting links (and
   * signature rows) without limit; a real forward needs one or two.
   */
  static final int MAX_OUTSTANDING_INVITES = 5;

  private final @NonNull TenantDpaService tenantDpaService;
  private final @NonNull TenantDpaStatusService tenantDpaStatusService;
  private final @NonNull GoverningDpaResolver governingDpaResolver;
  private final @NonNull TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  private final @NonNull TenantService tenantService;
  private final @NonNull TenantIdReservationRepository tenantIdReservationRepository;
  private final @NonNull InputSanitizer inputSanitizer;
  private final @NonNull AuthorisationService authorisationService;

  @Value("${app.base.url:}")
  private String appBaseUrl;

  /**
   * Creates a single-use sign invite for the DPA version currently in force for the tenant and
   * returns the raw token plus a user-facing sign link.
   *
   * <p>The invite is the forwarding path of the SAME single agreement: the natural person
   * authorised to sign for the tenant organisation need not be an app user. It therefore covers the
   * governing document — the tenant's own published DPA if it authored one, otherwise the operator
   * DPA — exactly like the in-app signing path, so a tenant without a DPA of its own can forward
   * too. The PENDING row is always written under the TENANT's id, so the resulting signature counts
   * for the tenant's gate.
   *
   * @throws DpaNotPublishedException if no DPA governs the tenant yet.
   */
  public DpaSignInviteDTO createSignInvite(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var governing = governingDpaResolver.resolve(tenantId);
    if (governing == null) {
      throw new DpaNotPublishedException(
          "Tenant " + tenantId + " has no published DPA to sign yet");
    }
    var rawToken =
        tenantDpaService.createSignInvite(
            tenantId, governing.version(), INVITE_TTL, authorisationService.getUserId());
    return new DpaSignInviteDTO()
        .token(rawToken)
        .signLink(buildSignLink(rawToken))
        .expiresAt(LocalDateTime.now().plus(INVITE_TTL).toString());
  }

  /**
   * Creates a single-use DPA sign link from the PUBLIC tenant onboarding context
   * (ORISO-TenantService#179). No session exists — the caller is authorised by the tenant-ID
   * reservation pair its onboarding invite carries: the reservation must exist, the presented token
   * must match the ledger row (constant-time compare), and the reservation must still be RESERVED,
   * i.e. the onboarding it belongs to is still open. Every failure mode answers the same "invalid"
   * state via {@link InvalidDpaSignTokenException} (410), so nothing about the reservation ledger
   * leaks.
   *
   * <p>The PENDING signature row is bound to the RESERVED tenant id: the link works immediately —
   * before and after the registration completes — and the signature lands on the tenant the moment
   * it exists. Note the asymmetry: an already-minted link keeps working across registration (that
   * is the whole point of the forward), but MINTING is limited to the open onboarding window;
   * afterwards the authenticated admin endpoint owns it. {@code forwardedByUserId} stays null —
   * there is no account yet.
   *
   * @throws InvalidDpaSignTokenException unknown reservation, token mismatch, or a reservation that
   *     is no longer open (410)
   * @throws DpaNotPublishedException nothing is published to sign (409)
   */
  @Transactional
  public DpaSignInviteDTO createPublicForwardSignInvite(PublicDpaForwardRequestDTO request) {
    if (request == null
        || request.getReservedTenantId() == null
        || request.getTenantIdReservationToken() == null
        || request.getTenantIdReservationToken().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "reservedTenantId and tenantIdReservationToken are required");
    }
    var tenantId = request.getReservedTenantId();
    var reservation =
        tenantIdReservationRepository
            .findByTenantIdForUpdate(tenantId)
            .orElseThrow(() -> new InvalidDpaSignTokenException("Unknown tenant-ID reservation"));
    if (!constantTimeEquals(reservation.getToken(), request.getTenantIdReservationToken())) {
      throw new InvalidDpaSignTokenException("Tenant-ID reservation token mismatch");
    }
    // The reservation pair is a credential for the OPEN onboarding only. Registration consumes the
    // reservation by flipping the ledger row to ASSIGNED while keeping the same token, and a
    // tenant deletion leaves that ASSIGNED row behind — so without this check the onboarding token
    // would keep minting sign links (and re-creating signer PII) for the whole life of the id, and
    // even after the tenant it belonged to was deleted. Once the tenant exists, forwarding is the
    // authenticated admin's endpoint. The answer is the same opaque one an unknown reservation
    // gets, so nothing about the ledger's state leaks.
    if (reservation.getStatus() != TenantIdReservationStatus.RESERVED) {
      throw new InvalidDpaSignTokenException(
          "Tenant-ID reservation is no longer open for onboarding");
    }
    // Anyone holding the onboarding token can call this, and every call mints a link that stays
    // valid for 14 days, so the endpoint is throttled by how many links are already outstanding.
    // Capping instead of replacing is deliberate: the product rule is that every issued link keeps
    // working until a signature lands, so an admin who forwarded to two people does not silently
    // break the first one.
    if (tenantDpaService.countOutstandingSignInvites(
            tenantId, request.getTenantIdReservationToken())
        >= MAX_OUTSTANDING_INVITES) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Too many outstanding sign links for this onboarding; wait for one to be used or to"
              + " expire");
    }
    // A RESERVED id has no tenant row yet by definition, so the governing document is the
    // operator's — the same one the regular resolution returns the moment registration completes.
    var governing = governingDpaResolver.resolveForUnregisteredTenant();
    if (governing == null) {
      throw new DpaNotPublishedException(
          "No data processing agreement is published for reserved tenant " + tenantId + " yet");
    }
    var rawToken =
        tenantDpaService.createSignInvite(
            tenantId, governing.version(), INVITE_TTL, null, request.getTenantIdReservationToken());
    return new DpaSignInviteDTO()
        .token(rawToken)
        .signLink(buildSignLink(rawToken))
        .expiresAt(LocalDateTime.now().plus(INVITE_TTL).toString());
  }

  private static boolean constantTimeEquals(String expected, String presented) {
    if (expected == null || presented == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
  }

  private String buildSignLink(String rawToken) {
    return (appBaseUrl == null ? "" : appBaseUrl) + "/dpa-sign/" + rawToken;
  }

  /** The tenant's confirmed-DPA audit list (the platform-admin "list of confirmed AVVs"). */
  public List<DpaSignatureDTO> getSignatures(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    return tenantDpaService.getSignatures(tenantId).stream().map(TenantDpaFacade::toDto).toList();
  }

  /**
   * Whether a DPA is in force for the tenant and signed (the consultation gate).
   *
   * <p>Derived from the SAME authoritative status as the U9 gate (#569): it therefore covers the
   * governing operator document and counts admin signatures as well as forwarded ones. Anything
   * else would leave a tenant that just resolved the U10 blocker blocked from agency work with
   * nothing left to sign.
   */
  public DpaGateStatusDTO getGateStatus(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var status = tenantDpaStatusService.getStatus(tenantId);
    return new DpaGateStatusDTO()
        .dpaPublished(status.currentVersion() != null)
        .dpaSigned(status.status() == TenantDpaStatus.VALID)
        .dpaForwardPending(status.forwardPending());
  }

  /**
   * The authoritative DPA state of the tenant for its authenticated tenant admins (TEN-INV-U9).
   * Guarded like every other method here: a single-tenant admin can only read their own tenant.
   */
  public DpaStatusDTO getDpaStatus(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    if (tenantService.findTenantById(tenantId).isEmpty()) {
      // a platform admin can query any id; an absent tenant must not read as 200 MISSING
      throw new TenantNotFoundException("Tenant with id " + tenantId + " not found");
    }
    return toStatusDto(tenantDpaStatusService.getStatus(tenantId));
  }

  /**
   * Signs the tenant's currently published DPA version as the authenticated tenant admin. The
   * signer identity is taken from the access token — never from the request — and the submitted
   * form is persisted verbatim as an append-only, revision-safe audit row. Signing an already-VALID
   * tenant has no duplicate effect and simply returns the current status.
   */
  public DpaStatusDTO signDpa(Long tenantId, DpaAdminSignRequestDTO request) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var form =
        new TenantDpaStatusService.AdminSignatureForm(
            request.getSignerName(),
            request.getSignerPosition(),
            request.getSignerEmail(),
            request.getSignerOrganisation(),
            request.getLanguage(),
            buildFormDataJson(request));
    return toStatusDto(
        tenantDpaStatusService.sign(
            tenantId, authorisationService.getUserId(), authorisationService.getUsername(), form));
  }

  /** Verbatim JSON snapshot of the submitted sign form for the audit row. */
  private static String buildFormDataJson(DpaAdminSignRequestDTO request) {
    var formData = new LinkedHashMap<String, Object>();
    formData.put("signerName", request.getSignerName());
    formData.put("signerPosition", request.getSignerPosition());
    formData.put("signerEmail", request.getSignerEmail());
    formData.put("signerOrganisation", request.getSignerOrganisation());
    formData.put("language", request.getLanguage());
    formData.put("accepted", request.getAccepted());
    return JsonConverter.convertToJson(formData);
  }

  private static DpaStatusDTO toStatusDto(TenantDpaStatusService.DpaStatusView view) {
    return new DpaStatusDTO()
        .tenantId(view.tenantId())
        .status(DpaStatusDTO.StatusEnum.fromValue(view.status().name()))
        .currentDpaVersion(view.currentVersion() == null ? null : view.currentVersion().toString())
        .signedDpaVersion(view.signedVersion() == null ? null : view.signedVersion().toString())
        .signedAt(view.signedAt() == null ? null : view.signedAt().toString())
        .signedBy(view.signedBy())
        .forwardPending(view.forwardPending());
  }

  /**
   * Publishes the tenant's DPA: sanitises each per-language HTML translation (OWASP allowlist),
   * stores it as the multilingual JSON content, and stamps a fresh activation date (= new contract
   * version). Returns the resulting gate status (published; signed is false until the new version
   * is confirmed).
   *
   * <p>Machine-translation metadata convention (see documentation/translation-meta.md): the map may
   * carry parallel {@code <lang>__meta} keys marking a language as machine translated. Meta values
   * are NOT html-sanitized but validated as strict JSON with only the known fields ({@code mt},
   * {@code src}, {@code at}) and stored alongside the content. When the HTML of a language is
   * changed while its previously stored {@code mt:true} meta is merely resent unchanged, that meta
   * is removed - a manual edit clears the machine-translated tag.
   */
  @Transactional
  public DpaGateStatusDTO publishDpa(Long tenantId, Map<String, String> contentByLanguage) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var tenant =
        tenantService
            .findTenantById(tenantId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    var previous = JsonConverter.convertMapFromJson(tenant.getContentDataProcessingAgreement());
    var sanitized = new LinkedHashMap<String, String>();
    var metaByLanguage = new LinkedHashMap<String, String>();
    if (contentByLanguage != null) {
      contentByLanguage.forEach(
          (key, value) -> {
            if (TranslationMetaUtil.isMetaKey(key)) {
              if (!TranslationMetaUtil.isValidMeta(value)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid translation metadata for key: " + key);
              }
              metaByLanguage.put(TranslationMetaUtil.languageOf(key), value);
            } else {
              sanitized.put(key, inputSanitizer.sanitizeAllowingFormattingAndLinks(value));
            }
          });
    }
    metaByLanguage.forEach(
        (lang, meta) -> {
          if (shouldKeepMeta(lang, meta, sanitized, previous)) {
            sanitized.put(TranslationMetaUtil.metaKeyFor(lang), meta);
          }
        });
    // Truncate to seconds so the in-memory version key matches the MariaDB DATETIME(0) column
    // after a round-trip — otherwise the signature gate (equals on dpa_version) could never match.
    var version = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    var json = JsonConverter.convertToJson(sanitized);
    tenant.setContentDataProcessingAgreement(json);
    tenant.setContentDataProcessingAgreementActivationDate(version);
    tenantService.update(tenant);
    tenantDpaService.recordPublishedVersion(tenantId, json, version);
    boolean signed = tenantDpaService.isSignedForVersion(tenantId, version);
    return new DpaGateStatusDTO().dpaPublished(true).dpaSigned(signed);
  }

  /**
   * Decides whether a submitted {@code <lang>__meta} entry is stored. A meta without content for
   * its language is dropped (orphan). If the language's HTML is unchanged, the meta is kept
   * (republish). If the HTML changed but the submitted meta equals the previously stored one, the
   * meta is a stale round-trip of an {@code mt:true} tag over a manual edit - it is removed
   * (Frank's rule: a manual edit clears the machine-translated tag). A changed/new meta together
   * with changed content is a fresh machine translation and is stored.
   */
  private boolean shouldKeepMeta(
      String lang, String meta, Map<String, String> sanitized, Map<String, String> previous) {
    if (!sanitized.containsKey(lang)) {
      return false;
    }
    var newHtml = sanitized.get(lang);
    var previousHtml = previous.get(lang);
    if (Objects.equals(newHtml, previousHtml)) {
      return true;
    }
    var previousMeta = previous.get(TranslationMetaUtil.metaKeyFor(lang));
    return !Objects.equals(meta, previousMeta);
  }

  /**
   * The published versions (newest first) of the DPA document in force for the tenant — its own
   * publish history, or the governing operator document when it never published one (#569).
   *
   * <p>This is the document the tenant admin reads before signing: measuring a tenant against the
   * operator DPA while serving it nothing to read is the non-actionable dead end #569 removes. The
   * multilingual content map is returned unchanged, so the caller still picks the signer's
   * language. Signatures stay strictly per-tenant — only the CONTENT is shared.
   */
  public List<DpaVersionDTO> getVersions(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var documentTenantId = governingDpaResolver.documentTenantIdFor(tenantId);
    return tenantDpaService.getVersions(documentTenantId).stream()
        .map(TenantDpaFacade::toVersionDto)
        .toList();
  }

  private static DpaVersionDTO toVersionDto(TenantDpaVersionEntity entity) {
    return new DpaVersionDTO()
        .activationDate(
            entity.getActivationDate() == null ? null : entity.getActivationDate().toString())
        .content(entity.getContent());
  }

  private static DpaSignatureDTO toDto(TenantDpaSignatureEntity entity) {
    return new DpaSignatureDTO()
        .tenantId(entity.getTenantId())
        .status(entity.getStatus() == null ? null : entity.getStatus().name())
        .dpaVersion(entity.getDpaVersion() == null ? null : entity.getDpaVersion().toString())
        .signerName(entity.getSignerName())
        .signerPosition(entity.getSignerPosition())
        .signerEmail(entity.getSignerEmail())
        .signerOrganisation(entity.getSignerOrganisation())
        .forwardedByUserId(entity.getForwardedByUserId())
        .source(entity.getSource())
        .signedAt(entity.getSignedAt() == null ? null : entity.getSignedAt().toString());
  }
}
