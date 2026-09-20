package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.*;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantLegalProposalService {
  private final @NonNull TenantLegalDraftRepository draftRepository;
  private final @NonNull TenantLegalProposalRepository proposalRepository;
  private final @NonNull TenantLegalDraftArchiveRepository archiveRepository;
  private final @NonNull TenantRepository tenantRepository;
  private final @NonNull TenantLegalProposalDistributionRepository distributionRepository;
  private final @NonNull TenantLegalProposalDeliveryRepository deliveryRepository;

  @Transactional
  public List<TenantLegalProposalEntity> deliver(
      String requestKey,
      TenantLegalDraftKind kind,
      String sourceRevision,
      TenantLegalProposalAudience audience,
      Set<Long> selectedTenantIds,
      String actorId) {
    return deliverWithOutcome(
            requestKey, kind, sourceRevision, audience, selectedTenantIds, actorId)
        .proposals();
  }

  @Transactional
  public DeliveryResult deliverWithOutcome(
      String requestKey,
      TenantLegalDraftKind kind,
      String sourceRevision,
      TenantLegalProposalAudience audience,
      Set<Long> selectedTenantIds,
      String actorId) {
    if (kind == null || audience == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kind and audience are required");
    }
    validateRequestKey(requestKey);
    Optional<TenantLegalProposalDistributionEntity> retry =
        distributionRepository.findByRequestKey(requestKey);
    if (retry.isPresent()) {
      return new DeliveryResult(
          retry.get(),
          validateAndReadRetry(retry.get(), kind, sourceRevision, audience, selectedTenantIds),
          false);
    }

    TenantLegalDraftEntity source =
        draftRepository
            .findLockedByOwnerKeyAndKind(0L, kind)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Platform legal draft not found"));

    // A locking read is a current read under MariaDB REPEATABLE READ. After waiting for the
    // source-draft lock it observes a concurrent identical delivery instead of attempting a
    // duplicate insert from this transaction's older snapshot.
    retry = distributionRepository.findLockedByRequestKey(requestKey);
    if (retry.isPresent()) {
      return new DeliveryResult(
          retry.get(),
          validateAndReadRetry(retry.get(), kind, sourceRevision, audience, selectedTenantIds),
          false);
    }
    requireRevision(source, sourceRevision);

    SortedSet<Long> recipients = resolveRecipients(audience, selectedTenantIds);
    validateRecipients(recipients);
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    String distributionId = UUID.randomUUID().toString();
    TenantLegalProposalDistributionEntity distribution =
        TenantLegalProposalDistributionEntity.builder()
            .id(distributionId)
            .requestKey(requestKey)
            .kind(kind)
            .audience(audience)
            .sourceDraftId(source.getId())
            .sourceDraftVersion(source.getVersion())
            .sourceUpdatedAt(source.getUpdateDate())
            .requestFingerprint(fingerprint(kind, sourceRevision, audience, selectedTenantIds))
            .recipientIds(encodeRecipients(recipients))
            .createdBy(actorId)
            .createdAt(now)
            .build();

    try {
      distributionRepository.saveAndFlush(distribution);
      Map<Long, TenantLegalProposalEntity> proposals =
          proposalRepository
              .findBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
                  source.getId(), source.getVersion(), recipients)
              .stream()
              .collect(
                  Collectors.toMap(
                      TenantLegalProposalEntity::getRecipientTenantId, Function.identity()));
      List<TenantLegalProposalEntity> missing =
          recipients.stream()
              .filter(recipient -> !proposals.containsKey(recipient))
              .map(recipient -> proposal(source, distribution, recipient, actorId, now))
              .toList();
      proposalRepository
          .saveAllAndFlush(missing)
          .forEach(p -> proposals.put(p.getRecipientTenantId(), p));

      supersedeOlderIncoming(recipients, kind, new HashSet<>(proposals.values()), now);

      List<TenantLegalProposalDeliveryEntity> deliveries =
          recipients.stream()
              .map(
                  recipient ->
                      TenantLegalProposalDeliveryEntity.builder()
                          .distributionId(distributionId)
                          .proposalId(proposals.get(recipient).getId())
                          .build())
              .toList();
      deliveryRepository.saveAllAndFlush(deliveries);
      return new DeliveryResult(
          distribution, recipients.stream().map(proposals::get).toList(), true);
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  public record DeliveryResult(
      TenantLegalProposalDistributionEntity distribution,
      List<TenantLegalProposalEntity> proposals,
      boolean created) {}

  @Transactional(readOnly = true)
  public List<TenantLegalProposalEntity> list(Long tenantId, TenantLegalDraftKind kind) {
    return kind == null
        ? proposalRepository.findByRecipientTenantIdOrderByCreatedAtDesc(tenantId)
        : proposalRepository.findByRecipientTenantIdAndKindOrderByCreatedAtDesc(tenantId, kind);
  }

  @Transactional(readOnly = true)
  public TenantLegalProposalEntity get(Long tenantId, Long proposalId) {
    return proposalRepository
        .findByIdAndRecipientTenantId(proposalId, tenantId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal proposal not found"));
  }

  @Transactional
  public TenantLegalProposalEntity dismiss(
      Long tenantId, Long proposalId, String expectedProposalRevision, String actorId) {
    TenantLegalProposalEntity proposal = lockedPending(tenantId, proposalId);
    requireRevision(proposal, expectedProposalRevision);
    proposal.setStatus(TenantLegalProposalStatus.DISMISSED);
    proposal.setDecidedBy(actorId);
    proposal.setDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    try {
      return proposalRepository.saveAndFlush(proposal);
    } catch (OptimisticLockingFailureException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  @Transactional
  public TenantLegalDraftEntity adopt(
      Long tenantId,
      Long proposalId,
      TenantLegalProposalAdoptionMode mode,
      String expectedProposalRevision,
      String expectedDraftRevision,
      String actorId) {
    if (mode == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adoption mode is required");
    }
    TenantLegalProposalEntity proposal = lockedAdoptable(tenantId, proposalId);
    requireRevision(proposal, expectedProposalRevision);
    Optional<TenantLegalDraftEntity> current =
        draftRepository.findLockedByOwnerKeyAndKind(tenantId, proposal.getKind());
    TenantLegalDraftEntity draft;
    if (mode == TenantLegalProposalAdoptionMode.CREATE_IF_EMPTY) {
      if (current.isPresent()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "An existing draft requires replacement confirmation");
      }
      if (expectedDraftRevision != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "CREATE_IF_EMPTY does not accept a draft revision");
      }
      draft =
          TenantLegalDraftEntity.builder()
              .ownerKey(tenantId)
              .tenantId(tenantId)
              .kind(proposal.getKind())
              .build();
    } else {
      draft =
          current.orElseThrow(
              () ->
                  new SettingsUpdateConflictException(
                      TenantLegalDraftService.NEW_REVISION, expectedDraftRevision));
      requireRevision(draft, expectedDraftRevision);
      try {
        archiveRepository.saveAndFlush(archive(draft, actorId));
      } catch (DataIntegrityViolationException | OptimisticLockingFailureException exception) {
        throw new SettingsUpdateConflictException(exception);
      }
    }

    applyProposal(draft, proposal);
    try {
      TenantLegalDraftEntity adopted = draftRepository.saveAndFlush(draft);
      proposal.setStatus(TenantLegalProposalStatus.ADOPTED);
      proposal.setDecidedBy(actorId);
      proposal.setDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
      proposalRepository.saveAndFlush(proposal);
      return adopted;
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  @Transactional(readOnly = true)
  public List<TenantLegalDraftArchiveEntity> archives(Long tenantId, TenantLegalDraftKind kind) {
    return kind == null
        ? archiveRepository.findByTenantIdOrderByArchivedAtDesc(tenantId)
        : archiveRepository.findByTenantIdAndKindOrderByArchivedAtDesc(tenantId, kind);
  }

  @Transactional(readOnly = true)
  public TenantLegalDraftArchiveEntity archive(Long tenantId, Long archiveId) {
    return archiveRepository
        .findByIdAndTenantId(archiveId, tenantId)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal draft archive not found"));
  }

  public String revision(TenantLegalProposalEntity proposal) {
    if (proposal.getId() == null || proposal.getVersion() == null) {
      throw new IllegalStateException("A persisted proposal is required for a revision token");
    }
    return proposal.getId() + ":" + proposal.getVersion();
  }

  private TenantLegalProposalEntity lockedPending(Long tenantId, Long proposalId) {
    TenantLegalProposalEntity proposal = locked(tenantId, proposalId);
    if (proposal.getStatus() != TenantLegalProposalStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Legal proposal is no longer pending");
    }
    return proposal;
  }

  private TenantLegalProposalEntity lockedAdoptable(Long tenantId, Long proposalId) {
    TenantLegalProposalEntity proposal = locked(tenantId, proposalId);
    if (proposal.getStatus() == TenantLegalProposalStatus.ADOPTED
        || proposal.getStatus() == TenantLegalProposalStatus.SUPERSEDED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Legal proposal is historical");
    }
    return proposal;
  }

  private TenantLegalProposalEntity locked(Long tenantId, Long proposalId) {
    TenantLegalProposalEntity proposal =
        proposalRepository
            .findLockedByIdAndRecipientTenantId(proposalId, tenantId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal proposal not found"));
    return proposal;
  }

  private void applyProposal(TenantLegalDraftEntity draft, TenantLegalProposalEntity proposal) {
    draft.setContent(proposal.getContent());
    draft.setPrivacyConsent(proposal.getPrivacyConsent());
    draft.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    draft.setOriginProposalId(proposal.getId());
    draft.setOriginDistributionId(proposal.getDistributionId());
    draft.setOriginSourceRevision(sourceRevision(proposal));
    draft.setOriginSourceUpdatedAt(proposal.getSourceUpdatedAt());
    draft.setOriginSharedBy(proposal.getCreatedBy());
  }

  private TenantLegalDraftArchiveEntity archive(TenantLegalDraftEntity draft, String actorId) {
    return TenantLegalDraftArchiveEntity.builder()
        .tenantId(draft.getOwnerKey())
        .kind(draft.getKind())
        .draftRowId(draft.getId())
        .draftRevision(draftRevision(draft))
        .content(draft.getContent())
        .privacyConsent(draft.getPrivacyConsent())
        .draftSavedAt(draft.getUpdateDate())
        .originProposalId(draft.getOriginProposalId())
        .originDistributionId(draft.getOriginDistributionId())
        .originSourceRevision(draft.getOriginSourceRevision())
        .originSourceUpdatedAt(draft.getOriginSourceUpdatedAt())
        .originSharedBy(draft.getOriginSharedBy())
        .archivedBy(actorId)
        .archivedAt(LocalDateTime.now(ZoneOffset.UTC))
        .build();
  }

  private TenantLegalProposalEntity proposal(
      TenantLegalDraftEntity source,
      TenantLegalProposalDistributionEntity distribution,
      Long recipient,
      String actorId,
      LocalDateTime now) {
    return TenantLegalProposalEntity.builder()
        .recipientTenantId(recipient)
        .kind(source.getKind())
        .distributionId(distribution.getId())
        .audience(distribution.getAudience())
        .sourceDraftId(source.getId())
        .sourceDraftVersion(source.getVersion())
        .sourceUpdatedAt(source.getUpdateDate())
        .content(source.getContent())
        .privacyConsent(source.getPrivacyConsent())
        .status(TenantLegalProposalStatus.PENDING)
        .createdBy(actorId)
        .createdAt(now)
        .build();
  }

  private void supersedeOlderIncoming(
      SortedSet<Long> recipients,
      TenantLegalDraftKind kind,
      Set<TenantLegalProposalEntity> newest,
      LocalDateTime now) {
    Map<Long, TenantLegalProposalEntity> newestByRecipient =
        newest.stream()
            .collect(
                Collectors.toMap(
                    TenantLegalProposalEntity::getRecipientTenantId, Function.identity()));
    List<TenantLegalProposalEntity> older =
        proposalRepository.findLockedByRecipientTenantIdInAndKindAndStatusIn(
            recipients,
            kind,
            List.of(TenantLegalProposalStatus.PENDING, TenantLegalProposalStatus.DISMISSED));
    List<TenantLegalProposalEntity> superseded =
        older.stream()
            .filter(p -> !newest.contains(p))
            .map(
                p -> {
                  TenantLegalProposalEntity replacement =
                      newestByRecipient.get(p.getRecipientTenantId());
                  p.setStatus(TenantLegalProposalStatus.SUPERSEDED);
                  p.setSupersededByProposalId(replacement.getId());
                  p.setSupersededAt(now);
                  return p;
                })
            .toList();
    if (!superseded.isEmpty()) proposalRepository.saveAllAndFlush(superseded);
  }

  private SortedSet<Long> resolveRecipients(
      TenantLegalProposalAudience audience, Set<Long> selectedTenantIds) {
    Set<Long> requested = selectedTenantIds == null ? Set.of() : selectedTenantIds;
    if (audience == TenantLegalProposalAudience.SELECTED) {
      if (requested.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SELECTED requires recipients");
      }
      return new TreeSet<>(requested);
    }
    if (!requested.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ALL does not accept recipients");
    }
    return tenantRepository.findAllIds().stream()
        .filter(Objects::nonNull)
        .filter(id -> id > 0)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private void validateRecipients(SortedSet<Long> recipients) {
    if (recipients.isEmpty() || recipients.contains(0L) || recipients.first() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid proposal recipients");
    }
    Set<Long> found =
        tenantRepository.findAllById(recipients).stream()
            .map(TenantEntity::getId)
            .collect(Collectors.toSet());
    if (!found.equals(recipients)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown proposal recipient");
    }
  }

  private List<TenantLegalProposalEntity> validateAndReadRetry(
      TenantLegalProposalDistributionEntity distribution,
      TenantLegalDraftKind kind,
      String sourceRevision,
      TenantLegalProposalAudience audience,
      Set<Long> selectedTenantIds) {
    if (!distribution
        .getRequestFingerprint()
        .equals(fingerprint(kind, sourceRevision, audience, selectedTenantIds))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Idempotency key was already used for a different delivery");
    }
    List<TenantLegalProposalDeliveryEntity> deliveries =
        deliveryRepository.findLockedByDistributionId(distribution.getId());
    if (deliveries.isEmpty()) return List.of();
    Map<Long, TenantLegalProposalEntity> proposals =
        proposalRepository
            .findLockedByIdIn(
                deliveries.stream().map(TenantLegalProposalDeliveryEntity::getProposalId).toList())
            .stream()
            .collect(Collectors.toMap(TenantLegalProposalEntity::getId, Function.identity()));
    return deliveries.stream()
        .map(delivery -> proposals.get(delivery.getProposalId()))
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(TenantLegalProposalEntity::getRecipientTenantId))
        .toList();
  }

  public SortedSet<Long> fixedRecipients(TenantLegalProposalDistributionEntity distribution) {
    SortedSet<Long> recipients = new TreeSet<>();
    if (distribution.getRecipientIds() == null || distribution.getRecipientIds().isBlank()) {
      return recipients;
    }
    Arrays.stream(distribution.getRecipientIds().split(","))
        .map(Long::valueOf)
        .forEach(recipients::add);
    return recipients;
  }

  private String encodeRecipients(Collection<Long> recipients) {
    return recipients.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  private String fingerprint(
      TenantLegalDraftKind kind,
      String sourceRevision,
      TenantLegalProposalAudience audience,
      Set<Long> selectedTenantIds) {
    SortedSet<Long> selected =
        selectedTenantIds == null ? new TreeSet<>() : new TreeSet<>(selectedTenantIds);
    String canonical =
        kind + "|" + sourceRevision + "|" + audience + "|" + encodeRecipients(selected);
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private void validateRequestKey(String requestKey) {
    if (requestKey == null || requestKey.isBlank() || requestKey.length() > 128) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid idempotency key");
    }
  }

  private void requireRevision(TenantLegalDraftEntity draft, String submitted) {
    String current = draftRevision(draft);
    if (!current.equals(submitted)) throw new SettingsUpdateConflictException(current, submitted);
  }

  private void requireRevision(TenantLegalProposalEntity proposal, String submitted) {
    String current = revision(proposal);
    if (!current.equals(submitted)) throw new SettingsUpdateConflictException(current, submitted);
  }

  private String draftRevision(TenantLegalDraftEntity draft) {
    return draft.getId() + ":" + draft.getVersion();
  }

  private String sourceRevision(TenantLegalProposalEntity proposal) {
    return proposal.getSourceDraftId() + ":" + proposal.getSourceDraftVersion();
  }

  private String sourceRevision(TenantLegalProposalDistributionEntity distribution) {
    return distribution.getSourceDraftId() + ":" + distribution.getSourceDraftVersion();
  }
}
