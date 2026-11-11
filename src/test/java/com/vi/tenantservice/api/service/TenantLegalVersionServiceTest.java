package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalTextVersionEntity;
import com.vi.tenantservice.api.repository.TenantLegalTextVersionRepository;
import com.vi.tenantservice.api.service.TenantLegalVersionService.PublishedLegalTexts;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantLegalVersionServiceTest {
  @Mock private TenantLegalTextVersionRepository repository;
  @Mock private AuthorisationService authorisationService;

  private TenantLegalVersionService service;

  @BeforeEach
  void setUp() {
    service = new TenantLegalVersionService(repository, authorisationService);
  }

  @Test
  void changedPrivacyIsRecordedAndClosesThePreviousVersion() {
    TenantLegalTextVersionEntity open = version(5L, "{\"de\":\"old\"}");
    when(repository.findLockedOpen(7L, TenantLegalDraftKind.PRIVACY)).thenReturn(List.of(open));
    when(authorisationService.getUserId()).thenReturn("publisher-id");
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TenantEntity saved =
        service.saveRecordingPublications(
            new PublishedLegalTexts("{\"de\":\"imprint\"}", "{\"de\":\"old\"}"),
            () -> tenant(7L, "{\"de\":\"imprint\"}", "{\"de\":\"new\"}"));

    assertThat(saved.getId()).isEqualTo(7L);
    ArgumentCaptor<TenantLegalTextVersionEntity> captor =
        ArgumentCaptor.forClass(TenantLegalTextVersionEntity.class);
    verify(repository).save(captor.capture());
    TenantLegalTextVersionEntity recorded = captor.getValue();
    assertThat(recorded.getTenantId()).isEqualTo(7L);
    assertThat(recorded.getKind()).isEqualTo(TenantLegalDraftKind.PRIVACY);
    assertThat(recorded.getContent()).isEqualTo("{\"de\":\"new\"}");
    assertThat(recorded.getPublishedBy()).isEqualTo("publisher-id");
    assertThat(recorded.getSupersededAt()).isNull();
    assertThat(open.getSupersededAt()).isEqualTo(recorded.getPublishedAt());
    // The imprint did not change: an unrelated tenant edit must not forge a version of it.
    verify(repository, never()).findLockedOpen(7L, TenantLegalDraftKind.IMPRINT);
  }

  @Test
  void anUpdateThatLeavesTheLegalTextsUntouchedRecordsNothing() {
    service.saveRecordingPublications(
        new PublishedLegalTexts("{\"de\":\"i\"}", "{\"de\":\"p\"}"),
        () -> tenant(7L, "{\"de\":\"i\"}", "{\"de\":\"p\"}"));

    verifyNoInteractions(repository);
  }

  @Test
  void aWordingIdenticalToTheOpenVersionIsNotRecordedAgain() {
    when(repository.findLockedOpen(0L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(List.of(version(3L, "{\"de\":\"same\"}")));

    service.saveRecordingPublications(
        new PublishedLegalTexts(null, null), () -> tenant(0L, "{\"de\":\"same\"}", null));

    verify(repository, never()).save(any());
  }

  @Test
  void removingAPublishedTextClosesItsVersionWithoutOpeningAnEmptyOne() {
    TenantLegalTextVersionEntity open = version(5L, "{\"de\":\"old\"}");
    when(repository.findLockedOpen(7L, TenantLegalDraftKind.IMPRINT)).thenReturn(List.of(open));

    service.saveRecordingPublications(
        new PublishedLegalTexts("{\"de\":\"old\"}", null), () -> tenant(7L, "{}", null));

    assertThat(open.getSupersededAt()).isNotNull();
    verify(repository).saveAll(List.of(open));
    verify(repository, never()).save(any());
  }

  @Test
  void firstPublicationOfANewTenantIsRecorded() {
    when(repository.findLockedOpen(9L, TenantLegalDraftKind.IMPRINT)).thenReturn(List.of());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(authorisationService.getUserId()).thenThrow(new ClassCastException("no jwt"));

    service.saveRecordingPublications(
        PublishedLegalTexts.NONE, () -> tenant(9L, "{\"de\":\"first\"}", null));

    ArgumentCaptor<TenantLegalTextVersionEntity> captor =
        ArgumentCaptor.forClass(TenantLegalTextVersionEntity.class);
    verify(repository).save(captor.capture());
    // An unknown publisher stays null; it is never guessed.
    assertThat(captor.getValue().getPublishedBy()).isNull();
  }

  private TenantEntity tenant(Long id, String impressum, String privacy) {
    return TenantEntity.builder()
        .id(id)
        .contentImpressum(impressum)
        .contentPrivacy(privacy)
        .build();
  }

  private TenantLegalTextVersionEntity version(Long id, String content) {
    return TenantLegalTextVersionEntity.builder()
        .id(id)
        .content(content)
        .publishedAt(LocalDateTime.parse("2026-09-01T10:00:00"))
        .build();
  }
}
