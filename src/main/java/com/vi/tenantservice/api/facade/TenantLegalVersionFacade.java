package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalTextVersionDTO;
import com.vi.tenantservice.api.model.TenantLegalTextVersionEntity;
import com.vi.tenantservice.api.service.TenantLegalVersionService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Read side of the Träger/platform legal-text history (ORISO-Admin#270). */
@Service
@RequiredArgsConstructor
public class TenantLegalVersionFacade {

  /** Fixed 19 characters, as AgencyService serves it: a strict parser and string compare hold. */
  private static final DateTimeFormatter WIRE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final @NonNull TenantFacadeAuthorisationService authorisation;
  private final @NonNull TenantLegalVersionService versions;

  public List<TenantLegalTextVersionDTO> list(Long tenantId, String kind) {
    authorisation.assertCanReadLegalVersions(tenantId);
    TenantLegalDraftKind parsed = parse(kind);
    return versions.list(tenantId, parsed).stream().map(this::dto).toList();
  }

  private TenantLegalTextVersionDTO dto(TenantLegalTextVersionEntity entity) {
    return new TenantLegalTextVersionDTO()
        .id(entity.getId())
        .kind(
            entity.getKind() == TenantLegalDraftKind.PRIVACY
                ? TenantLegalTextVersionDTO.KindEnum.DPP
                : TenantLegalTextVersionDTO.KindEnum.IMPRINT)
        .ownerLevel(
            entity.getTenantId() == 0L
                ? TenantLegalTextVersionDTO.OwnerLevelEnum.PLATFORM
                : TenantLegalTextVersionDTO.OwnerLevelEnum.TENANT)
        .ownerId(entity.getTenantId())
        .content(entity.getContent())
        .publishedAt(format(entity.getPublishedAt()))
        .publishedBy(entity.getPublishedBy())
        .supersededAt(format(entity.getSupersededAt()));
  }

  private static String format(LocalDateTime value) {
    return value == null ? null : WIRE.format(value);
  }

  /** DPP is the wire spelling shared with AgencyService; internally the tenant calls it PRIVACY. */
  private static TenantLegalDraftKind parse(String kind) {
    if ("DPP".equals(kind)) return TenantLegalDraftKind.PRIVACY;
    if ("IMPRINT".equals(kind)) return TenantLegalDraftKind.IMPRINT;
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid legal text kind");
  }
}
