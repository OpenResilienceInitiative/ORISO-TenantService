package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TenantLegalProposalServiceTest {
  @Mock private TenantLegalDraftRepository draftRepository;
  @Mock private TenantLegalProposalRepository proposalRepository;
  @Mock private TenantLegalDraftArchiveRepository archiveRepository;
  @Mock private TenantRepository tenantRepository;
  @Mock private TenantLegalProposalDistributionRepository distributionRepository;
  @Mock private TenantLegalProposalDeliveryRepository deliveryRepository;

  private TenantLegalProposalService service;

  @BeforeEach
  void setUp() {
    service =
        new TenantLegalProposalService(
            draftRepository,
            proposalRepository,
            archiveRepository,
            tenantRepository,
            distributionRepository,
            deliveryRepository);
  }

  @Test
  void deliverSelectedSnapshotsTheExactPlatformRevisionForOnlyValidatedRecipients() {
    TenantLegalDraftEntity source = sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY);
    when(draftRepository.findLockedByOwnerKeyAndKind(0L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(source));
    when(tenantRepository.findAllById(Set.of(7L, 9L))).thenReturn(List.of(tenant(7L), tenant(9L)));
    when(proposalRepository.findLockedBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
            10L, 4L, Set.of(7L, 9L)))
        .thenReturn(List.of());
    when(distributionRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(proposalRepository.saveAllAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(deliveryRepository.saveAllAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<TenantLegalProposalEntity> delivered =
        service.deliver(
            "request-selected",
            TenantLegalDraftKind.PRIVACY,
            "10:4",
            TenantLegalProposalAudience.SELECTED,
            Set.of(9L, 7L),
            "platform-user");

    assertThat(delivered)
        .extracting(TenantLegalProposalEntity::getRecipientTenantId)
        .containsExactly(7L, 9L);
    assertThat(delivered)
        .allSatisfy(
            proposal -> {
              assertThat(proposal.getContent()).isEqualTo("{\"de\":\"source\"}");
              assertThat(proposal.getPrivacyConsent()).isEqualTo("{\"de\":\"consent\"}");
              assertThat(proposal.getSourceDraftId()).isEqualTo(10L);
              assertThat(proposal.getSourceDraftVersion()).isEqualTo(4L);
              assertThat(proposal.getCreatedBy()).isEqualTo("platform-user");
              assertThat(proposal.getStatus()).isEqualTo(TenantLegalProposalStatus.PENDING);
            });
    verify(proposalRepository).saveAllAndFlush(any());
    // The lookup for "who already holds this revision" must be the LOCKING one. A plain read
    // answers from the REPEATABLE READ snapshot taken before the platform-draft lock was granted,
    // misses a proposal an overlapping delivery has just committed, and turns the following insert
    // into a unique-constraint violation the caller sees as a 409. Strict stubbing alone would only
    // report an unnecessary stub if this regressed, which is not a statement about behaviour.
    verify(proposalRepository)
        .findLockedBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
            10L, 4L, Set.of(7L, 9L));
  }

  @Test
  void deliverAllExpandsCurrentNonPlatformTenantsOnce() {
    when(draftRepository.findLockedByOwnerKeyAndKind(0L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.of(sourceDraft(11L, 2L, TenantLegalDraftKind.IMPRINT)));
    when(tenantRepository.findAllIds()).thenReturn(List.of(0L, 8L, 3L));
    when(tenantRepository.findAllById(Set.of(3L, 8L))).thenReturn(List.of(tenant(3L), tenant(8L)));
    when(proposalRepository.findLockedBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
            11L, 2L, Set.of(3L, 8L)))
        .thenReturn(List.of());
    when(distributionRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(proposalRepository.saveAllAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(deliveryRepository.saveAllAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<TenantLegalProposalEntity> delivered =
        service.deliver(
            "request-all",
            TenantLegalDraftKind.IMPRINT,
            "11:2",
            TenantLegalProposalAudience.ALL,
            Set.of(),
            "platform-user");

    assertThat(delivered)
        .extracting(TenantLegalProposalEntity::getRecipientTenantId)
        .containsExactly(3L, 8L);
  }

  @Test
  void staleSourceRevisionCreatesNothing() {
    when(draftRepository.findLockedByOwnerKeyAndKind(0L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(sourceDraft(10L, 5L, TenantLegalDraftKind.PRIVACY)));

    assertThatThrownBy(
            () ->
                service.deliver(
                    "request-stale",
                    TenantLegalDraftKind.PRIVACY,
                    "10:4",
                    TenantLegalProposalAudience.SELECTED,
                    Set.of(7L),
                    "platform-user"))
        .isInstanceOf(SettingsUpdateConflictException.class);
    verifyNoInteractions(tenantRepository, proposalRepository, deliveryRepository);
  }

  @Test
  void missingAudienceFailsBeforeAnyReadInsteadOfExpandingToAllTenants() {
    assertThatThrownBy(
            () ->
                service.deliver(
                    "request-null-audience",
                    TenantLegalDraftKind.PRIVACY,
                    "10:4",
                    null,
                    Set.of(),
                    "platform-user"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("required");
    verifyNoInteractions(
        draftRepository,
        tenantRepository,
        proposalRepository,
        distributionRepository,
        deliveryRepository);
  }

  @Test
  void retryReturnsExistingProposalWithoutCreatingADuplicate() {
    TenantLegalDraftEntity source = sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY);
    TenantLegalProposalEntity existing = proposal(31L, 2L, 7L, source);
    TenantLegalProposalDistributionEntity distribution =
        TenantLegalProposalDistributionEntity.builder()
            .id("distribution")
            .requestKey("request-retry")
            .kind(TenantLegalDraftKind.PRIVACY)
            .audience(TenantLegalProposalAudience.SELECTED)
            .sourceDraftId(10L)
            .sourceDraftVersion(4L)
            .requestFingerprint("956f7ad1fad13b72b69a289032621a35592857a21d91da11ddd7d61fdaad309d")
            .recipientIds("7")
            .build();
    when(distributionRepository.findByRequestKey("request-retry"))
        .thenReturn(Optional.of(distribution));
    when(deliveryRepository.findLockedByDistributionId("distribution"))
        .thenReturn(
            List.of(
                TenantLegalProposalDeliveryEntity.builder()
                    .distributionId("distribution")
                    .proposalId(31L)
                    .build()));
    when(proposalRepository.findLockedByIdIn(List.of(31L))).thenReturn(List.of(existing));

    assertThat(
            service.deliver(
                "request-retry",
                TenantLegalDraftKind.PRIVACY,
                "10:4",
                TenantLegalProposalAudience.SELECTED,
                Set.of(7L),
                "platform-user"))
        .containsExactly(existing);
    verify(proposalRepository, never()).saveAllAndFlush(any());
    verifyNoInteractions(draftRepository, tenantRepository);
  }

  @Test
  void createIfEmptyNeverOverwritesAnExistingDraft() {
    TenantLegalProposalEntity proposal =
        proposal(31L, 2L, 7L, sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY));
    when(proposalRepository.findLockedByIdAndRecipientTenantId(31L, 7L))
        .thenReturn(Optional.of(proposal));
    when(draftRepository.findLockedByOwnerKeyAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(recipientDraft(22L, 8L)));

    assertThatThrownBy(
            () ->
                service.adopt(
                    7L,
                    31L,
                    TenantLegalProposalAdoptionMode.CREATE_IF_EMPTY,
                    "31:2",
                    null,
                    "tenant-user"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("confirmation");
    verifyNoInteractions(archiveRepository);
    verify(draftRepository, never()).saveAndFlush(any());
  }

  @Test
  void missingAdoptionModeFailsBeforeProposalLookup() {
    assertThatThrownBy(() -> service.adopt(7L, 31L, null, "31:2", null, "tenant-user"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("required");
    verifyNoInteractions(proposalRepository, draftRepository, archiveRepository);
  }

  @Test
  void confirmedReplacementArchivesCompleteOldDraftAndAdoptsProposal() {
    TenantLegalDraftEntity source = sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY);
    TenantLegalProposalEntity proposal = proposal(31L, 2L, 7L, source);
    TenantLegalDraftEntity oldDraft = recipientDraft(22L, 8L);
    oldDraft.setOriginProposalId(12L);
    oldDraft.setOriginDistributionId("older-distribution");
    oldDraft.setOriginSourceRevision("5:1");
    when(proposalRepository.findLockedByIdAndRecipientTenantId(31L, 7L))
        .thenReturn(Optional.of(proposal));
    when(draftRepository.findLockedByOwnerKeyAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(oldDraft));
    when(archiveRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(draftRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(proposalRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TenantLegalDraftEntity adopted =
        service.adopt(
            7L,
            31L,
            TenantLegalProposalAdoptionMode.ARCHIVE_AND_REPLACE,
            "31:2",
            "22:8",
            "tenant-user");

    ArgumentCaptor<TenantLegalDraftArchiveEntity> archive =
        ArgumentCaptor.forClass(TenantLegalDraftArchiveEntity.class);
    verify(archiveRepository).saveAndFlush(archive.capture());
    assertThat(archive.getValue().getContent()).isEqualTo("{\"de\":\"recipient edit\"}");
    assertThat(archive.getValue().getPrivacyConsent()).isEqualTo("{\"de\":\"old consent\"}");
    assertThat(archive.getValue().getDraftRevision()).isEqualTo("22:8");
    assertThat(archive.getValue().getOriginProposalId()).isEqualTo(12L);
    assertThat(archive.getValue().getOriginDistributionId()).isEqualTo("older-distribution");
    assertThat(adopted.getContent()).isEqualTo(source.getContent());
    assertThat(adopted.getPrivacyConsent()).isEqualTo(source.getPrivacyConsent());
    assertThat(adopted.getOriginProposalId()).isEqualTo(31L);
    assertThat(proposal.getStatus()).isEqualTo(TenantLegalProposalStatus.ADOPTED);
  }

  @Test
  void archiveConstraintRaceMapsToConflictBeforeDraftOrProposalChanges() {
    TenantLegalProposalEntity proposal =
        proposal(31L, 2L, 7L, sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY));
    TenantLegalDraftEntity oldDraft = recipientDraft(22L, 8L);
    when(proposalRepository.findLockedByIdAndRecipientTenantId(31L, 7L))
        .thenReturn(Optional.of(proposal));
    when(draftRepository.findLockedByOwnerKeyAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(oldDraft));
    when(archiveRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("tenant deleted"));

    assertThatThrownBy(
            () ->
                service.adopt(
                    7L,
                    31L,
                    TenantLegalProposalAdoptionMode.ARCHIVE_AND_REPLACE,
                    "31:2",
                    "22:8",
                    "tenant-user"))
        .isInstanceOf(SettingsUpdateConflictException.class);
    assertThat(proposal.getStatus()).isEqualTo(TenantLegalProposalStatus.PENDING);
    assertThat(oldDraft.getContent()).isEqualTo("{\"de\":\"recipient edit\"}");
    verify(draftRepository, never()).saveAndFlush(any());
    verify(proposalRepository, never()).saveAndFlush(any());
  }

  @Test
  void staleProposalOrDraftRevisionWritesNothing() {
    TenantLegalProposalEntity proposal =
        proposal(31L, 3L, 7L, sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY));
    when(proposalRepository.findLockedByIdAndRecipientTenantId(31L, 7L))
        .thenReturn(Optional.of(proposal));

    assertThatThrownBy(
            () ->
                service.adopt(
                    7L,
                    31L,
                    TenantLegalProposalAdoptionMode.ARCHIVE_AND_REPLACE,
                    "31:2",
                    "22:8",
                    "tenant-user"))
        .isInstanceOf(SettingsUpdateConflictException.class);
    verifyNoInteractions(draftRepository, archiveRepository);
  }

  @Test
  void missingDraftCreateCollisionIsAConflictInsteadOfA500() {
    TenantLegalProposalEntity proposal =
        proposal(31L, 2L, 7L, sourceDraft(10L, 4L, TenantLegalDraftKind.IMPRINT));
    when(proposalRepository.findLockedByIdAndRecipientTenantId(31L, 7L))
        .thenReturn(Optional.of(proposal));
    when(draftRepository.findLockedByOwnerKeyAndKind(7L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.empty());
    when(draftRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("concurrent create"));

    assertThatThrownBy(
            () ->
                service.adopt(
                    7L,
                    31L,
                    TenantLegalProposalAdoptionMode.CREATE_IF_EMPTY,
                    "31:2",
                    null,
                    "tenant-user"))
        .isInstanceOf(SettingsUpdateConflictException.class);
    assertThat(proposal.getStatus()).isEqualTo(TenantLegalProposalStatus.PENDING);
    verifyNoInteractions(archiveRepository);
  }

  @Test
  void dismissedProposalRemainsReadableAndCanBeAdoptedAfterReopening() {
    TenantLegalProposalEntity proposal =
        proposal(31L, 2L, 7L, sourceDraft(10L, 4L, TenantLegalDraftKind.IMPRINT));
    proposal.setStatus(TenantLegalProposalStatus.DISMISSED);
    when(proposalRepository.findLockedByIdAndRecipientTenantId(31L, 7L))
        .thenReturn(Optional.of(proposal));
    when(draftRepository.findLockedByOwnerKeyAndKind(7L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.empty());
    when(draftRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(proposalRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TenantLegalDraftEntity adopted =
        service.adopt(
            7L, 31L, TenantLegalProposalAdoptionMode.CREATE_IF_EMPTY, "31:2", null, "tenant-user");

    assertThat(adopted.getContent()).isEqualTo(proposal.getContent());
    assertThat(proposal.getStatus()).isEqualTo(TenantLegalProposalStatus.ADOPTED);
  }

  @Test
  void newerSourceVersionSupersedesOlderIncomingButPreservesDismissMetadata() {
    TenantLegalDraftEntity source = sourceDraft(10L, 5L, TenantLegalDraftKind.PRIVACY);
    TenantLegalProposalEntity older =
        proposal(30L, 3L, 7L, sourceDraft(10L, 4L, TenantLegalDraftKind.PRIVACY));
    older.setStatus(TenantLegalProposalStatus.DISMISSED);
    older.setDecidedBy("recipient-user");
    older.setDecidedAt(LocalDateTime.parse("2026-09-17T11:30:00"));
    when(draftRepository.findLockedByOwnerKeyAndKind(0L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(source));
    when(tenantRepository.findAllById(Set.of(7L))).thenReturn(List.of(tenant(7L)));
    when(proposalRepository.findLockedBySourceDraftIdAndSourceDraftVersionAndRecipientTenantIdIn(
            10L, 5L, Set.of(7L)))
        .thenReturn(List.of());
    when(distributionRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(proposalRepository.saveAllAndFlush(any()))
        .thenAnswer(
            invocation -> {
              List<TenantLegalProposalEntity> saved = invocation.getArgument(0);
              saved.stream().filter(p -> p.getId() == null).forEach(p -> p.setId(31L));
              return saved;
            });
    when(proposalRepository.findLockedByRecipientTenantIdInAndKindAndStatusIn(
            Set.of(7L),
            TenantLegalDraftKind.PRIVACY,
            List.of(TenantLegalProposalStatus.PENDING, TenantLegalProposalStatus.DISMISSED)))
        .thenReturn(List.of(older));
    when(deliveryRepository.saveAllAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<TenantLegalProposalEntity> delivered =
        service.deliver(
            "request-new-version",
            TenantLegalDraftKind.PRIVACY,
            "10:5",
            TenantLegalProposalAudience.SELECTED,
            Set.of(7L),
            "platform-user");

    assertThat(delivered)
        .singleElement()
        .extracting(TenantLegalProposalEntity::getId)
        .isEqualTo(31L);
    assertThat(older.getStatus()).isEqualTo(TenantLegalProposalStatus.SUPERSEDED);
    assertThat(older.getSupersededByProposalId()).isEqualTo(31L);
    assertThat(older.getDecidedBy()).isEqualTo("recipient-user");
    assertThat(older.getDecidedAt()).isEqualTo(LocalDateTime.parse("2026-09-17T11:30:00"));
  }

  @Test
  void retryAllUsesImmutableOriginalAudienceWithoutReadingSourceOrCurrentTenants() {
    TenantLegalProposalDistributionEntity distribution =
        TenantLegalProposalDistributionEntity.builder()
            .id("distribution")
            .requestKey("request-all-retry")
            .kind(TenantLegalDraftKind.IMPRINT)
            .audience(TenantLegalProposalAudience.ALL)
            .sourceDraftId(11L)
            .sourceDraftVersion(2L)
            .requestFingerprint("28ba0fba55f4f79cb794418d20035a9500ed323839faa7bd5f119c412130e331")
            .recipientIds("3,8")
            .build();
    TenantLegalProposalEntity remaining =
        proposal(32L, 1L, 8L, sourceDraft(11L, 2L, TenantLegalDraftKind.IMPRINT));
    when(distributionRepository.findByRequestKey("request-all-retry"))
        .thenReturn(Optional.of(distribution));
    when(deliveryRepository.findLockedByDistributionId("distribution"))
        .thenReturn(List.of(TenantLegalProposalDeliveryEntity.builder().proposalId(32L).build()));
    when(proposalRepository.findLockedByIdIn(List.of(32L))).thenReturn(List.of(remaining));

    assertThat(
            service.deliver(
                "request-all-retry",
                TenantLegalDraftKind.IMPRINT,
                "11:2",
                TenantLegalProposalAudience.ALL,
                Set.of(),
                "platform-user"))
        .containsExactly(remaining);
    assertThat(service.fixedRecipients(distribution)).containsExactly(3L, 8L);
    verifyNoInteractions(draftRepository, tenantRepository);
  }

  private TenantLegalDraftEntity sourceDraft(Long id, Long version, TenantLegalDraftKind kind) {
    return TenantLegalDraftEntity.builder()
        .id(id)
        .version(version)
        .ownerKey(0L)
        .kind(kind)
        .content("{\"de\":\"source\"}")
        .privacyConsent(kind == TenantLegalDraftKind.PRIVACY ? "{\"de\":\"consent\"}" : null)
        .updateDate(LocalDateTime.parse("2026-09-17T10:00:00"))
        .build();
  }

  private TenantLegalDraftEntity recipientDraft(Long id, Long version) {
    return TenantLegalDraftEntity.builder()
        .id(id)
        .version(version)
        .ownerKey(7L)
        .tenantId(7L)
        .kind(TenantLegalDraftKind.PRIVACY)
        .content("{\"de\":\"recipient edit\"}")
        .privacyConsent("{\"de\":\"old consent\"}")
        .updateDate(LocalDateTime.parse("2026-09-17T11:00:00"))
        .build();
  }

  private TenantLegalProposalEntity proposal(
      Long id, Long version, Long recipientId, TenantLegalDraftEntity source) {
    return TenantLegalProposalEntity.builder()
        .id(id)
        .version(version)
        .recipientTenantId(recipientId)
        .kind(source.getKind())
        .distributionId("distribution")
        .audience(TenantLegalProposalAudience.SELECTED)
        .sourceDraftId(source.getId())
        .sourceDraftVersion(source.getVersion())
        .sourceUpdatedAt(source.getUpdateDate())
        .content(source.getContent())
        .privacyConsent(source.getPrivacyConsent())
        .status(TenantLegalProposalStatus.PENDING)
        .createdBy("platform-user")
        .createdAt(LocalDateTime.parse("2026-09-17T12:00:00"))
        .build();
  }

  private TenantEntity tenant(Long id) {
    return TenantEntity.builder().id(id).build();
  }
}
