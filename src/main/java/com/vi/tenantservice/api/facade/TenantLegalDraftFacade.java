package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.service.*;
import java.util.*;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantLegalDraftFacade {
  private final @NonNull TenantFacadeAuthorisationService authorisation;
  private final @NonNull TenantService tenantService;
  private final @NonNull TenantLegalDraftService drafts;

  public Optional<TenantLegalDraftDTO> get(Long tenantId, String kind) {
    authorize(tenantId);
    return drafts.get(tenantId, parse(kind)).map(this::dto);
  }

  public TenantLegalDraftDTO save(
      Long tenantId, String kind, TenantLegalDraftUpdateRequest request) {
    authorize(tenantId);
    authorisation.assertCanWriteLegalDraft();
    return dto(
        drafts.save(
            tenantId,
            parse(kind),
            request.getContent(),
            privacyConsent(request.getPrivacyConsent()),
            request.getRevision()));
  }

  public void delete(Long tenantId, String kind, String revision) {
    authorize(tenantId);
    authorisation.assertCanWriteLegalDraft();
    drafts.delete(tenantId, parse(kind), revision);
  }

  private void authorize(Long tenantId) {
    if (tenantId == null || tenantId < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant id");
    }
    if (tenantId == 0L) {
      if (!authorisation.isPlatformAdministrator()) {
        throw new AccessDeniedException("Only the platform administrator may edit platform drafts");
      }
      return;
    }
    authorisation.assertUserIsAuthorizedToAccessTenant(tenantId);
    if (tenantService.findTenantById(tenantId).isEmpty())
      throw new TenantNotFoundException("Tenant with id " + tenantId + " not found");
  }

  private TenantLegalDraftKind parse(String kind) {
    try {
      return TenantLegalDraftKind.valueOf(kind);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid legal draft kind");
    }
  }

  private Map<String, String> privacyConsent(Object value) {
    if (value == null) return null;
    if (!(value instanceof Map<?, ?> values)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Privacy consent must be a language-to-text map");
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (var entry : values.entrySet()) {
      if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String text)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Privacy consent must contain plain-text values");
      }
      result.put(key, text);
    }
    return result;
  }

  TenantLegalDraftDTO dto(TenantLegalDraftEntity e) {
    TenantLegalDraftDTO dto =
        new TenantLegalDraftDTO()
            .kind(TenantLegalDraftDTO.KindEnum.fromValue(e.getKind().name()))
            .content(drafts.content(e))
            .revision(drafts.revision(e))
            .updatedAt(e.getUpdateDate())
            .originProposalId(e.getOriginProposalId())
            .originSourceRevision(e.getOriginSourceRevision())
            .originSourceUpdatedAt(e.getOriginSourceUpdatedAt())
            .originSharedBy(e.getOriginSharedBy());
    if (e.getOriginDistributionId() != null) {
      dto.setOriginDistributionId(UUID.fromString(e.getOriginDistributionId()));
    }
    Map<String, String> privacyConsent = drafts.privacyConsent(e);
    return privacyConsent == null ? dto : dto.privacyConsent(privacyConsent);
  }
}
