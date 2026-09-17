package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.TenantLegalDraftEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.repository.TenantLegalDraftRepository;
import com.vi.tenantservice.api.validation.InputSanitizer;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TenantLegalDraftServiceTest {

  @Mock private TenantLegalDraftRepository repository;

  private TenantLegalDraftService service;

  @BeforeEach
  void setUp() {
    service = new TenantLegalDraftService(repository, new InputSanitizer());
  }

  @Test
  void save_Should_sanitizeEveryLanguageAndReturnPersistedOpaqueRevision() {
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.empty());
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              TenantLegalDraftEntity saved = invocation.getArgument(0);
              saved.setId(41L);
              saved.setVersion(0L);
              return saved;
            });

    TenantLegalDraftEntity saved =
        service.save(
            7L,
            TenantLegalDraftKind.PRIVACY,
            Map.of(
                "de", "<h2 id=\"privacy\">Datenschutz</h2><script>alert(1)</script>",
                "en", "<p>Privacy</p>"),
            TenantLegalDraftService.NEW_REVISION);

    assertThat(service.revision(saved)).isEqualTo("41:0");
    assertThat(service.content(saved).get("de"))
        .contains("<h2 id=\"privacy\">Datenschutz</h2>")
        .doesNotContain("script", "alert");
    assertThat(service.content(saved).get("en")).isEqualTo("<p>Privacy</p>");
  }

  @Test
  void save_Should_rejectNewTokenWhenDraftAlreadyExists() {
    TenantLegalDraftEntity existing = persisted(9L, 2L);
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                service.save(
                    7L,
                    TenantLegalDraftKind.IMPRINT,
                    Map.of("de", "neu"),
                    TenantLegalDraftService.NEW_REVISION))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("currentRevision=9:2")
        .hasMessageContaining("submittedRevision=new");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void save_Should_allowOnlyOneOfTwoInitialWritersUsingNewToken() {
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.empty(), Optional.empty());
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              TenantLegalDraftEntity saved = invocation.getArgument(0);
              saved.setId(11L);
              saved.setVersion(0L);
              return saved;
            })
        .thenThrow(new DataIntegrityViolationException("tenant and kind already inserted"));

    TenantLegalDraftEntity winner =
        service.save(
            7L,
            TenantLegalDraftKind.PRIVACY,
            Map.of("de", "winner"),
            TenantLegalDraftService.NEW_REVISION);

    assertThat(service.revision(winner)).isEqualTo("11:0");

    assertThatThrownBy(
            () ->
                service.save(
                    7L,
                    TenantLegalDraftKind.PRIVACY,
                    Map.of("de", "stale initial writer"),
                    TenantLegalDraftService.NEW_REVISION))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("changed while saving");
  }

  @Test
  void save_Should_mapOptimisticUpdateRaceToConflict() {
    TenantLegalDraftEntity existing = persisted(12L, 4L);
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(existing))
        .thenThrow(new OptimisticLockingFailureException("stale writer"));

    assertThatThrownBy(
            () -> service.save(7L, TenantLegalDraftKind.PRIVACY, Map.of("de", "stale"), "12:4"))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("changed while saving");
  }

  @Test
  void delete_Should_rejectStaleTokenBeforeDeleting() {
    TenantLegalDraftEntity existing = persisted(18L, 3L);
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.delete(7L, TenantLegalDraftKind.IMPRINT, "18:2"))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("currentRevision=18:3");

    verify(repository, never()).delete(any());
  }

  @Test
  void delete_Should_flushAndMapAConcurrentDeleteToConflict() {
    TenantLegalDraftEntity existing = persisted(18L, 3L);
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.of(existing));
    doThrow(new OptimisticLockingFailureException("already updated")).when(repository).flush();

    assertThatThrownBy(() -> service.delete(7L, TenantLegalDraftKind.IMPRINT, "18:3"))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("changed while saving");

    verify(repository).delete(existing);
    verify(repository).flush();
  }

  @Test
  void delete_Should_returnNotFoundWhenNoDraftExists() {
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.IMPRINT))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(7L, TenantLegalDraftKind.IMPRINT, "1:0"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void recreatedDraft_Should_rejectTokenFromDeletedIncarnation() {
    TenantLegalDraftEntity recreated = persisted(22L, 0L);
    when(repository.findByTenantIdAndKind(7L, TenantLegalDraftKind.PRIVACY))
        .thenReturn(Optional.of(recreated));

    assertThatThrownBy(
            () ->
                service.save(
                    7L,
                    TenantLegalDraftKind.PRIVACY,
                    Map.of("de", "stale save from deleted draft"),
                    "21:0"))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("currentRevision=22:0")
        .hasMessageContaining("submittedRevision=21:0");

    verify(repository, never()).saveAndFlush(any());
  }

  private TenantLegalDraftEntity persisted(long id, long version) {
    return TenantLegalDraftEntity.builder()
        .id(id)
        .version(version)
        .tenantId(7L)
        .kind(TenantLegalDraftKind.PRIVACY)
        .content("{}")
        .build();
  }
}
