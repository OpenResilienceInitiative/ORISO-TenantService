package com.vi.tenantservice.api.facade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.service.TenantLegalProposalService;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.*;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantLegalProposalFacade {
  private final @NonNull TenantFacadeAuthorisationService authorisation;
  private final @NonNull AuthorisationService principal;
  private final @NonNull TenantLegalProposalService proposals;
  private final @NonNull TenantLegalDraftFacade drafts;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DistributionResult distribute(TenantLegalProposalDistributionRequest request) {
    authorisation.assertCanDistributeLegalProposals();
    TenantLegalProposalService.DeliveryResult result =
        proposals.deliverWithOutcome(
            request.getRequestKey(),
            kind(request.getKind().getValue()),
            request.getSourceRevision(),
            TenantLegalProposalAudience.valueOf(request.getAudience().getValue()),
            request.getTenantIds(),
            principal.getUserId());
    TenantLegalProposalDistributionDTO dto =
        new TenantLegalProposalDistributionDTO()
            .requestKey(request.getRequestKey())
            .recipientTenantIds(new ArrayList<>(proposals.fixedRecipients(result.distribution())))
            .proposals(result.proposals().stream().map(this::dto).toList());
    return new DistributionResult(dto, result.created());
  }

  public List<TenantLegalTemplateVersionDTO> templateHistory(String kind) {
    authorisation.assertCanDistributeLegalProposals();
    return proposals.templateHistory(kind(kind)).stream().map(this::templateDto).toList();
  }

  private TenantLegalTemplateVersionDTO templateDto(
      TenantLegalProposalService.TemplateVersion version) {
    TenantLegalProposalDistributionEntity distribution = version.distribution();
    TenantLegalTemplateVersionDTO dto =
        new TenantLegalTemplateVersionDTO()
            .distributionId(UUID.fromString(distribution.getId()))
            .sourceRevision(
                distribution.getSourceDraftId() + ":" + distribution.getSourceDraftVersion())
            .createdAt(distribution.getCreatedAt())
            .recipientCount(proposals.fixedRecipients(distribution).size());
    // The distribution's own copy survives deleted recipients; older rows fall back to a proposal.
    String content =
        distribution.getContent() != null
            ? distribution.getContent()
            : version.snapshot().map(TenantLegalProposalEntity::getContent).orElse(null);
    String consent =
        distribution.getContent() != null
            ? distribution.getPrivacyConsent()
            : version.snapshot().map(TenantLegalProposalEntity::getPrivacyConsent).orElse(null);
    // Unknown stays absent rather than an empty map, which would claim an empty text was sent.
    if (content != null) dto.setContent(read(content));
    if (consent != null) dto.setPrivacyConsent(read(consent));
    return dto;
  }

  public List<TenantLegalProposalDTO> list(Long tenantId, String kind) {
    authorisation.assertCanReadLegalProposal(tenantId);
    TenantLegalDraftKind parsed = kind == null ? null : kind(kind);
    return proposals.list(tenantId, parsed).stream().map(this::dto).toList();
  }

  public TenantLegalProposalDTO get(Long tenantId, Long proposalId) {
    authorisation.assertCanReadLegalProposal(tenantId);
    return dto(proposals.get(tenantId, proposalId));
  }

  public TenantLegalProposalDTO dismiss(
      Long tenantId, Long proposalId, TenantLegalProposalDismissRequest request) {
    authorisation.assertCanManageOwnLegalProposal(tenantId);
    return dto(
        proposals.dismiss(
            tenantId, proposalId, request.getExpectedProposalRevision(), principal.getUserId()));
  }

  public TenantLegalDraftDTO adopt(
      Long tenantId, Long proposalId, TenantLegalProposalAdoptRequest request) {
    authorisation.assertCanManageOwnLegalProposal(tenantId);
    TenantLegalProposalAdoptionMode mode =
        TenantLegalProposalAdoptionMode.valueOf(request.getMode().getValue());
    return drafts.dto(
        proposals.adopt(
            tenantId,
            proposalId,
            mode,
            request.getExpectedProposalRevision(),
            request.getExpectedDraftRevision(),
            principal.getUserId()));
  }

  public List<TenantLegalDraftArchiveDTO> archives(Long tenantId, String kind) {
    authorisation.assertCanReadLegalProposal(tenantId);
    TenantLegalDraftKind parsed = kind == null ? null : kind(kind);
    return proposals.archives(tenantId, parsed).stream().map(this::archiveDto).toList();
  }

  public TenantLegalDraftArchiveDTO archive(Long tenantId, Long archiveId) {
    authorisation.assertCanReadLegalProposal(tenantId);
    return archiveDto(proposals.archive(tenantId, archiveId));
  }

  private TenantLegalProposalDTO dto(TenantLegalProposalEntity entity) {
    TenantLegalProposalDTO dto =
        new TenantLegalProposalDTO()
            .id(entity.getId())
            .recipientTenantId(entity.getRecipientTenantId())
            .kind(TenantLegalProposalDTO.KindEnum.fromValue(entity.getKind().name()))
            .content(read(entity.getContent()))
            .status(TenantLegalProposalDTO.StatusEnum.fromValue(entity.getStatus().name()))
            .revision(proposals.revision(entity))
            .sourceRevision(entity.getSourceDraftId() + ":" + entity.getSourceDraftVersion())
            .sourceUpdatedAt(entity.getSourceUpdatedAt())
            .distributionId(UUID.fromString(entity.getDistributionId()))
            .audience(TenantLegalProposalDTO.AudienceEnum.fromValue(entity.getAudience().name()))
            .createdBy(entity.getCreatedBy())
            .createdAt(entity.getCreatedAt())
            .decidedBy(entity.getDecidedBy())
            .decidedAt(entity.getDecidedAt())
            .supersededByProposalId(entity.getSupersededByProposalId())
            .supersededAt(entity.getSupersededAt());
    return entity.getPrivacyConsent() == null
        ? dto
        : dto.privacyConsent(read(entity.getPrivacyConsent()));
  }

  private TenantLegalDraftArchiveDTO archiveDto(TenantLegalDraftArchiveEntity entity) {
    TenantLegalDraftArchiveDTO dto =
        new TenantLegalDraftArchiveDTO()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .kind(TenantLegalDraftArchiveDTO.KindEnum.fromValue(entity.getKind().name()))
            .draftRowId(entity.getDraftRowId())
            .draftRevision(entity.getDraftRevision())
            .content(read(entity.getContent()))
            .draftSavedAt(entity.getDraftSavedAt())
            .originProposalId(entity.getOriginProposalId())
            .originSourceRevision(entity.getOriginSourceRevision())
            .originSourceUpdatedAt(entity.getOriginSourceUpdatedAt())
            .originSharedBy(entity.getOriginSharedBy())
            .archivedBy(entity.getArchivedBy())
            .archivedAt(entity.getArchivedAt());
    if (entity.getPrivacyConsent() != null) dto.setPrivacyConsent(read(entity.getPrivacyConsent()));
    if (entity.getOriginDistributionId() != null) {
      dto.setOriginDistributionId(UUID.fromString(entity.getOriginDistributionId()));
    }
    return dto;
  }

  private Map<String, String> read(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
    } catch (Exception exception) {
      throw new IllegalStateException("Could not read legal proposal snapshot", exception);
    }
  }

  private TenantLegalDraftKind kind(String value) {
    try {
      return TenantLegalDraftKind.valueOf(value);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid legal draft kind");
    }
  }

  public record DistributionResult(TenantLegalProposalDistributionDTO body, boolean created) {}
}
