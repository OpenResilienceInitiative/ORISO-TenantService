package com.vi.tenantservice.api.service.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    // Absence of the plaintext is not enough: storing an arbitrary ciphertext would pass that and
    // hand the provider the wrong key at translation time.
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(Optional.of(saved.getValue()));
    assertThat(tenantAdminControlsService.getTranslationApiKeys())
        .containsEntry("mistral", RAW_KEY);
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

    // and it round-trips as the literal input, prefix and all - not as the key without it
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(Optional.of(saved.getValue()));
    assertThat(tenantAdminControlsService.getTranslationApiKeys())
        .containsEntry("mistral", "ENC:" + RAW_KEY);
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

    // Absence of the plaintext is not enough: an encryptor that wrote any non-plaintext value
    // would pass that check while destroying the key. Assert the migrated key is still usable.
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(Optional.of(saved.getValue()));
    assertThat(tenantAdminControlsService.getTranslationApiKeys())
        .containsEntry("mistral", RAW_KEY)
        .containsEntry("openrouter", "sk-already-safe");
  }

  /**
   * The controls blob is shared and read leniently: a newer build may have written fields this
   * build has never heard of. Rewriting it through the narrower settings type would delete them —
   * the same version-skew problem that took Pre-Dev down on 2026-08-18, from the other side.
   */
  @Test
  void migration_Should_keepFieldsWrittenByANewerBuild() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(
                TenantAdminControlsEntity.builder()
                    .id(1L)
                    .controls(
                        "{\"permissionsPageEnabled\":true,"
                            + "\"permissionPolicies\":{\"featureVideoCall\":{\"value\":true}},"
                            + "\"caseHandoverPolicies\":{\"requireConsent\":true},"
                            + "\"translationApiKeys\":{\"mistral\":\""
                            + RAW_KEY
                            + "\"}}")
                    .build()));

    new TranslationApiKeyEncryptionMigration(tenantAdminControlsRepository, encryptionService)
        .migrate();

    var saved = ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(saved.capture());
    assertThat(saved.getValue().getControls())
        .contains("\"permissionPolicies\"")
        .contains("\"featureVideoCall\"")
        .contains("\"caseHandoverPolicies\"")
        .contains("\"requireConsent\"")
        .contains("\"permissionsPageEnabled\":true")
        .doesNotContain(RAW_KEY);
  }

  /**
   * With no secret configured the cipher is a pass-through, so every stored value looks like
   * plaintext to {@code isEncrypted}. Without the {@code isEnabled()} guard the migration would
   * rewrite the blob with unchanged content and log that it had encrypted keys — a write that does
   * nothing and a log line that is a lie.
   */
  @Test
  void migration_Should_notTouchAnythingWhenNoSecretIsConfigured() {
    var withoutSecret =
        new TranslationApiKeyEncryptionService(new SmtpPasswordEncryptionService(""));
    assertThat(withoutSecret.isEnabled()).isFalse();

    new TranslationApiKeyEncryptionMigration(tenantAdminControlsRepository, withoutSecret)
        .migrate();

    verify(tenantAdminControlsRepository, never()).findTopByOrderByIdAsc();
    verify(tenantAdminControlsRepository, never()).save(any());
  }

  /**
   * A provider value that is not a string was not written by us. {@code asText()} would happily
   * turn a number into one, so the migration would replace a JSON scalar with an encrypted string
   * and change the shape of a document it does not understand.
   */
  @Test
  void migration_Should_leaveNonTextualProviderValuesAsTheyAre() {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(
                TenantAdminControlsEntity.builder()
                    .id(1L)
                    .controls(
                        "{\"translationApiKeys\":{\"a\":42,\"b\":true,\"c\":null,\"mistral\":\""
                            + RAW_KEY
                            + "\"}}")
                    .build()));

    new TranslationApiKeyEncryptionMigration(tenantAdminControlsRepository, encryptionService)
        .migrate();

    var saved = ArgumentCaptor.forClass(TenantAdminControlsEntity.class);
    verify(tenantAdminControlsRepository).save(saved.capture());
    assertThat(saved.getValue().getControls())
        .contains("\"a\":42")
        .contains("\"b\":true")
        .contains("\"c\":null")
        .doesNotContain(RAW_KEY);
  }

  /**
   * The migration runs on every startup, before anyone can react. A blob it cannot make sense of is
   * a separate problem: it must neither take the service down nor overwrite a document it did not
   * understand. These cases exercise the branches that decide to do nothing.
   */
  @ParameterizedTest(name = "controls = {1}")
  @MethodSource("unusableControlsBlobs")
  void migration_Should_leaveBlobsAloneThatItCannotRead(String controls, String description) {
    when(tenantAdminControlsRepository.findTopByOrderByIdAsc())
        .thenReturn(
            Optional.of(TenantAdminControlsEntity.builder().id(1L).controls(controls).build()));

    var migration =
        new TranslationApiKeyEncryptionMigration(tenantAdminControlsRepository, encryptionService);

    assertThatCode(migration::migrate).as(description).doesNotThrowAnyException();
    verify(tenantAdminControlsRepository, never()).save(any());
  }

  private static Stream<Arguments> unusableControlsBlobs() {
    return Stream.of(
        Arguments.of(null, "no blob stored yet"),
        Arguments.of("", "empty column"),
        Arguments.of("   ", "whitespace only"),
        Arguments.of("{\"translationApiKeys\":", "truncated JSON"),
        Arguments.of("not json at all", "not JSON"),
        Arguments.of("[{\"translationApiKeys\":{}}]", "valid JSON, but an array"),
        Arguments.of("\"just a string\"", "valid JSON, but a scalar"),
        Arguments.of("{\"permissionsPageEnabled\":true}", "readable, but no keys to migrate"),
        Arguments.of("{\"translationApiKeys\":\"mistral\"}", "keys present, but not an object"),
        Arguments.of("{\"translationApiKeys\":{\"mistral\":\"\"}}", "provider present, key blank"));
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
