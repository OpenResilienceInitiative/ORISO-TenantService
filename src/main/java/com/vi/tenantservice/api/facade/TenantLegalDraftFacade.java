package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.service.*;
import java.util.*;
import lombok.*;
import org.springframework.http.HttpStatus;
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

  public TenantLegalDraftDTO save(Long tenantId, String kind, TenantLegalDraftDTO request) {
    authorize(tenantId);
    return dto(drafts.save(tenantId, parse(kind), request.getContent(), request.getRevision()));
  }

  public void delete(Long tenantId, String kind, String revision) {
    authorize(tenantId);
    drafts.delete(tenantId, parse(kind), revision);
  }

  private void authorize(Long tenantId) {
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

  private TenantLegalDraftDTO dto(TenantLegalDraftEntity e) {
    return new TenantLegalDraftDTO()
        .kind(TenantLegalDraftDTO.KindEnum.fromValue(e.getKind().name()))
        .content(drafts.content(e))
        .revision(drafts.revision(e))
        .updatedAt(e.getUpdateDate());
  }
}
