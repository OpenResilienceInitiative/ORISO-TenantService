package com.vi.tenantservice.api.service.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The platform-global translation provider API keys must not sit in the admin-controls JSON in
 * plaintext. These tests use the real cipher rather than a mock, because the point of the change is
 * what actually reaches the database column — a mocked encryptor would pass while storing the key
 * verbatim.
 */
@ExtendWith(MockitoExtension.class)
class TranslationApiKeyStorageTest {

  private static final String RAW_KEY = "sk-live-do-not-store-me-in-plaintext";

  @Mock private TenantAdminControlsRepository tenantAdminControlsRepository;
  @Mock private TenantConverter tenantConverter;

  private TranslationApiKeyEncryptionService encryptionService;
  private TenantAdminControlsService tenantAdminControlsService;

  @BeforeEach
  void setUp() {
    encryptionService =
        new TranslationApiKeyEncryptionService(
            new SmtpPasswordEncryptionService("dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMzJieXRlcyE="));
    tenantAdminControlsService =
        new TenantAdminControlsService(
            tenantAdminControlsRepository, tenantConverter, encryptionService);
  }

  @Test
  void setTranslationApiKey_Should_neverWriteTheRawKeyIntoTheControlsBlob() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(Optional.of(TenantAdminControlsEntity.builder().id(1L).controls("{}").build()));
    when(tenantConverter.toTenantAdminControlsSettings(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new TenantAdminControlsSettings());

    tenantAdminControlsService.setTranslationApiKey("mistral", RAW_KEY);

    var saved = ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(saved.capture());
    assertThat(saved.getValue().getControls()).doesNotContain(RAW_KEY);
    assertThat(saved.getValue().getControls()).contains("ENC:");
  }

  @Test
  void getTranslationApiKeys_Should_returnTheUsableKey() {
    String stored = encryptionService.encryptNewApiKey(RAW_KEY);
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(
                TenantAdminControlsEntity.builder()
                    .id(1L)
                    .controls("{\"translationApiKeys\":{\"mistral\":\"" + stored + "\"}}")
                    .build()));

    assertThat(tenantAdminControlsService.getTranslationApiKeys())
        .containsEntry("mistral", RAW_KEY);
  }

  /** A key written before this change has no prefix and must keep working until the migration. */
  @Test
  void getTranslationApiKeys_Should_passLegacyPlaintextThrough() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(
                TenantAdminControlsEntity.builder()
                    .id(1L)
                    .controls("{\"translationApiKeys\":{\"openrouter\":\"" + RAW_KEY + "\"}}")
                    .build()));

    assertThat(tenantAdminControlsService.getTranslationApiKeys())
        .containsEntry("openrouter", RAW_KEY);
  }

  /** A value that merely carries the prefix must not slip past encryption on the write path. */
  @Test
  void setTranslationApiKey_Should_encryptEvenWhenTheInputFakesTheCiphertextPrefix() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(Optional.of(TenantAdminControlsEntity.builder().id(1L).controls("{}").build()));
    when(tenantConverter.toTenantAdminControlsSettings(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new TenantAdminControlsSettings());

    tenantAdminControlsService.setTranslationApiKey("mistral", "ENC:" + RAW_KEY);

    var saved = ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(saved.capture());
    assertThat(saved.getValue().getControls()).doesNotContain(RAW_KEY);
  }

  @Test
  void migration_Should_encryptPlaintextKeysAndLeaveEncryptedOnesAlone() {
    String alreadyEncrypted = encryptionService.encryptNewApiKey("sk-already-safe");
    var entity =
        TenantAdminControlsEntity.builder()
            .id(1L)
            .controls(
                "{\"translationApiKeys\":{\"mistral\":\""
                    + RAW_KEY
                    + "\",\"openrouter\":\""
                    + alreadyEncrypted
                    + "\"}}")
            .build();
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(entity));

    new TranslationApiKeyEncryptionMigration(tenantAdminControlsRepository, encryptionService)
        .migrate();

    var saved = ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(saved.capture());
    assertThat(saved.getValue().getControls()).doesNotContain(RAW_KEY);
    assertThat(saved.getValue().getControls()).contains(alreadyEncrypted);
  }

  @Test
  void migration_Should_doNothingWhenEverythingIsAlreadyEncrypted() {
    String encrypted = encryptionService.encryptNewApiKey(RAW_KEY);
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(
                TenantAdminControlsEntity.builder()
                    .id(1L)
                    .controls("{\"translationApiKeys\":{\"mistral\":\"" + encrypted + "\"}}")
                    .build()));

    new TranslationApiKeyEncryptionMigration(tenantAdminControlsRepository, encryptionService)
        .migrate();

    verify(tenantAdminControlsRepository, org.mockito.Mockito.never())
        .save(org.mockito.ArgumentMatchers.any());
  }
}
