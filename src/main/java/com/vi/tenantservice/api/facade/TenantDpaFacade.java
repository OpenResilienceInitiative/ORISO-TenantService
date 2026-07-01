package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.model.DpaGateStatusDTO;
import com.vi.tenantservice.api.model.DpaSignInviteDTO;
import com.vi.tenantservice.api.model.DpaSignatureDTO;
import com.vi.tenantservice.api.model.DpaVersionDTO;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.DpaNotPublishedException;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.util.JsonConverter;
import com.vi.tenantservice.api.validation.InputSanitizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

  private final @NonNull TenantDpaService tenantDpaService;
  private final @NonNull TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  private final @NonNull TenantService tenantService;
  private final @NonNull InputSanitizer inputSanitizer;

  @Value("${app.base.url:}")
  private String appBaseUrl;

  /**
   * Creates a single-use sign invite for the tenant's currently published DPA and returns the raw
   * token plus a user-facing sign link. Requires a published DPA (an activation date).
   *
   * @throws DpaNotPublishedException if the tenant has not published a DPA yet.
   */
  public DpaSignInviteDTO createSignInvite(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var version =
        tenantService
            .findTenantById(tenantId)
            .map(TenantEntity::getContentDataProcessingAgreementActivationDate)
            .orElse(null);
    if (version == null) {
      throw new DpaNotPublishedException(
          "Tenant " + tenantId + " has no published DPA to sign yet");
    }
    var rawToken = tenantDpaService.createSignInvite(tenantId, version, INVITE_TTL);
    return new DpaSignInviteDTO()
        .token(rawToken)
        .signLink(buildSignLink(rawToken))
        .expiresAt(LocalDateTime.now().plus(INVITE_TTL).toString());
  }

  private String buildSignLink(String rawToken) {
    return (appBaseUrl == null ? "" : appBaseUrl) + "/dpa-sign/" + rawToken;
  }

  /** The tenant's confirmed-DPA audit list (the platform-admin "list of confirmed AVVs"). */
  public List<DpaSignatureDTO> getSignatures(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    return tenantDpaService.getSignatures(tenantId).stream().map(TenantDpaFacade::toDto).toList();
  }

  /** Whether the tenant's DPA is published and signed (the consultation gate). */
  public DpaGateStatusDTO getGateStatus(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var version =
        tenantService
            .findTenantById(tenantId)
            .map(TenantEntity::getContentDataProcessingAgreementActivationDate)
            .orElse(null);
    boolean published = version != null;
    boolean signed = published && tenantDpaService.isSignedForVersion(tenantId, version);
    return new DpaGateStatusDTO().dpaPublished(published).dpaSigned(signed);
  }

  /**
   * Publishes the tenant's DPA: sanitises each per-language HTML translation (OWASP allowlist),
   * stores it as the multilingual JSON content, and stamps a fresh activation date (= new contract
   * version). Returns the resulting gate status (published; signed is false until the new version
   * is confirmed).
   */
  public DpaGateStatusDTO publishDpa(Long tenantId, Map<String, String> contentByLanguage) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    var tenant =
        tenantService
            .findTenantById(tenantId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    var sanitized = new LinkedHashMap<String, String>();
    if (contentByLanguage != null) {
      contentByLanguage.forEach(
          (lang, html) ->
              sanitized.put(lang, inputSanitizer.sanitizeAllowingFormattingAndLinks(html)));
    }
    var version = LocalDateTime.now();
    var json = JsonConverter.convertToJson(sanitized);
    tenant.setContentDataProcessingAgreement(json);
    tenant.setContentDataProcessingAgreementActivationDate(version);
    tenantService.update(tenant);
    tenantDpaService.recordPublishedVersion(tenantId, json, version);
    boolean signed = tenantDpaService.isSignedForVersion(tenantId, version);
    return new DpaGateStatusDTO().dpaPublished(true).dpaSigned(signed);
  }

  /** The tenant's published DPA versions (newest first) for the read-only "look back" viewer. */
  public List<DpaVersionDTO> getVersions(Long tenantId) {
    tenantFacadeAuthorisationService.assertUserIsAuthorizedToAccessTenant(tenantId);
    return tenantDpaService.getVersions(tenantId).stream()
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
        .signerName(entity.getSignerName())
        .signedAt(entity.getSignedAt() == null ? null : entity.getSignedAt().toString());
  }
}
