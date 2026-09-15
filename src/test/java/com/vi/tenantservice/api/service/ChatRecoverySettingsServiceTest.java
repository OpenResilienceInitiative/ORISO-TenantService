package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.translation.TranslationApiKeyEncryptionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ChatRecoverySettingsServiceTest {
  private final TenantAdminControlsRepository repository =
      mock(TenantAdminControlsRepository.class);
  private final TenantConverter converter =
      new TenantConverter(
          mock(TemplateService.class),
          mock(TemplateRenderer.class),
          mock(SmtpPasswordEncryptionService.class));
  private final TenantAdminControlsService service =
      new TenantAdminControlsService(
          repository, converter, mock(TranslationApiKeyEncryptionService.class));
  private TenantAdminControlsEntity stored;

  @BeforeEach
  void storage() {
    when(repository.findTopByOrderByIdAsc()).thenAnswer(invocation -> Optional.ofNullable(stored));
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              stored = invocation.getArgument(0);
              return stored;
            });
  }

  @Test
  void defaultsArePasswordForBothRolesWithoutCreatingARecord() {
    assertThat(service.getChatRecoverySettings())
        .isEqualTo(settings(ChatRecoveryMode.LOGIN_PASSWORD, ChatRecoveryMode.LOGIN_PASSWORD, 0));
    verify(repository, never()).saveAndFlush(any());
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "{}|LOGIN_PASSWORD|LOGIN_PASSWORD|0",
        "{\"asker\":null,\"consultant\":null,\"revision\":null}|LOGIN_PASSWORD|LOGIN_PASSWORD|0",
        "{\"asker\":\"RECOVERY_KEY\",\"revision\":7}|RECOVERY_KEY|LOGIN_PASSWORD|7",
        "{\"consultant\":\"RECOVERY_KEY\",\"revision\":7}|LOGIN_PASSWORD|RECOVERY_KEY|7",
        "{\"asker\":\"RECOVERY_KEY\",\"consultant\":\"RECOVERY_KEY\"}|RECOVERY_KEY|RECOVERY_KEY|0"
      },
      delimiter = '|')
  void partialStoredPolicyDefaultsOnlyMissingFields(
      String json, ChatRecoveryMode asker, ChatRecoveryMode consultant, long revision) {
    stored =
        TenantAdminControlsEntity.builder()
            .id(1L)
            .controls("{\"chatRecoverySettings\":" + json + "}")
            .build();
    var expected = settings(asker, consultant, revision);
    assertThat(service.getChatRecoverySettings()).isEqualTo(expected);
    assertThat(service.getControls().getChatRecoverySettings()).isEqualTo(expected);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void partialStoredPolicyCanBeUpdatedFromDefaultRevisionWithoutErasingOtherSettings() {
    stored =
        TenantAdminControlsEntity.builder()
            .id(1L)
            .controls(
                "{\"permissionsPageEnabled\":false,\"chatRecoverySettings\":{\"asker\":\"RECOVERY_KEY\"}}")
            .build();
    var updated =
        service.updateChatRecoverySettings(
            settings(ChatRecoveryMode.RECOVERY_KEY, ChatRecoveryMode.RECOVERY_KEY, 0));
    assertThat(updated)
        .isEqualTo(settings(ChatRecoveryMode.RECOVERY_KEY, ChatRecoveryMode.RECOVERY_KEY, 1));
    assertThat(service.getControls().getPermissionsPageEnabled()).isFalse();
    assertThat(service.getChatRecoverySettings()).isEqualTo(updated);
  }

  @Test
  void nullRequestIsRejectedBeforeRepositoryAccess() {
    assertThatThrownBy(() -> service.updateChatRecoverySettings(null))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "NULL,LOGIN_PASSWORD,0",
        "LOGIN_PASSWORD,NULL,0",
        "LOGIN_PASSWORD,LOGIN_PASSWORD,NULL",
        "LOGIN_PASSWORD,LOGIN_PASSWORD,-1"
      },
      nullValues = "NULL")
  void invalidSubmittedFieldsAreRejectedBeforeRepositoryAccess(
      ChatRecoveryMode asker, ChatRecoveryMode consultant, Long revision) {
    assertThatThrownBy(
            () ->
                service.updateChatRecoverySettings(
                    new ChatRecoverySettings(asker, consultant, revision)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    verifyNoInteractions(repository);
  }

  @Test
  void changedRolesPersistWithANewRevisionAndSurviveReadback() throws Exception {
    var saved =
        service.updateChatRecoverySettings(
            settings(ChatRecoveryMode.RECOVERY_KEY, ChatRecoveryMode.LOGIN_PASSWORD, 0));
    assertThat(saved.getRevision()).isEqualTo(1);
    assertThat(service.getChatRecoverySettings()).isEqualTo(saved);
    assertThat(service.getControls().getChatRecoverySettings()).isEqualTo(saved);
    var mapper = new ObjectMapper();
    assertThat(mapper.readTree(stored.getControls()).get("chatRecoverySettings"))
        .isEqualTo(
            mapper.readTree(
                "{\"asker\":\"RECOVERY_KEY\",\"consultant\":\"LOGIN_PASSWORD\",\"revision\":1}"));
  }

  @Test
  void fullControlsWriteCannotEraseOrOverrideRecoveryPolicy() {
    var saved =
        service.updateChatRecoverySettings(
            settings(ChatRecoveryMode.RECOVERY_KEY, ChatRecoveryMode.LOGIN_PASSWORD, 0));
    service.updateControls(
        new TenantAdminControls()
            .permissionsPageEnabled(false)
            .chatRecoverySettings(
                settings(ChatRecoveryMode.LOGIN_PASSWORD, ChatRecoveryMode.LOGIN_PASSWORD, 99)));
    assertThat(service.getChatRecoverySettings()).isEqualTo(saved);
    assertThat(service.getControls().getPermissionsPageEnabled()).isFalse();
  }

  @Test
  void dedicatedWritePreservesUnrelatedSettingsAndEncryptedTranslationKeys() {
    stored =
        TenantAdminControlsEntity.builder()
            .id(1L)
            .controls(
                "{\"permissionsPageEnabled\":false,\"translationApiKeys\":{\"provider\":\"ENC:fixture\"}}")
            .build();
    service.updateChatRecoverySettings(
        settings(ChatRecoveryMode.LOGIN_PASSWORD, ChatRecoveryMode.RECOVERY_KEY, 0));
    assertThat(service.getControls().getPermissionsPageEnabled()).isFalse();
    assertThat(stored.getControls()).contains("ENC:fixture");
  }

  @Test
  void staleWriteIsRejectedWithoutOverwriting() {
    var saved =
        service.updateChatRecoverySettings(
            settings(ChatRecoveryMode.RECOVERY_KEY, ChatRecoveryMode.LOGIN_PASSWORD, 0));
    assertThatThrownBy(
            () ->
                service.updateChatRecoverySettings(
                    settings(ChatRecoveryMode.LOGIN_PASSWORD, ChatRecoveryMode.RECOVERY_KEY, 0)))
        .isInstanceOf(SettingsUpdateConflictException.class)
        .hasMessageContaining("currentRevision=1", "submittedRevision=0");
    assertThat(service.getChatRecoverySettings()).isEqualTo(saved);
  }

  @Test
  void unchangedWriteDoesNotAdvanceRevision() {
    service.updateChatRecoverySettings(
        settings(ChatRecoveryMode.LOGIN_PASSWORD, ChatRecoveryMode.LOGIN_PASSWORD, 0));
    assertThat(service.getChatRecoverySettings().getRevision()).isZero();
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void concurrentJpaWriteBecomesSettingsConflict() {
    when(repository.saveAndFlush(any()))
        .thenThrow(new OptimisticLockingFailureException("concurrent write"));
    assertThatThrownBy(
            () ->
                service.updateChatRecoverySettings(
                    settings(ChatRecoveryMode.RECOVERY_KEY, ChatRecoveryMode.LOGIN_PASSWORD, 0)))
        .isInstanceOf(SettingsUpdateConflictException.class);
  }

  private ChatRecoverySettings settings(
      ChatRecoveryMode asker, ChatRecoveryMode consultant, long revision) {
    return new ChatRecoverySettings(asker, consultant, revision);
  }
}
