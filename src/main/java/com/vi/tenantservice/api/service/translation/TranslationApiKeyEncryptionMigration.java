package com.vi.tenantservice.api.service.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * One-time (idempotent) startup migration: encrypts machine-translation provider API keys that are
 * still stored in plaintext inside the {@code tenant_admin_controls.controls} JSON.
 *
 * <p>Same purpose as {@code SmtpPasswordEncryptionMigration}: without it the keys written before
 * encryption existed would stay in plaintext until a super admin happens to save them again, which
 * may be never.
 *
 * <p><b>The blob is edited as a JSON tree, not through {@code TenantAdminControlsSettings}.</b>
 * That type ignores unknown fields on purpose, so a newer build may have written keys this build
 * has never heard of — that is exactly what took Pre-Dev down on 2026-08-18. Reading leniently is
 * correct; writing the narrower type back would silently delete those fields for everyone. This
 * migration touches one key and leaves every other byte of the document alone.
 *
 * <p>Runs only when the encryption secret is configured. Values that already authenticate under the
 * key are left untouched, so re-running changes nothing. Safe with several replicas: the row is
 * versioned, a replica that loses the write race re-reads and finds the work done.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TranslationApiKeyEncryptionMigration {

  private static final int MAX_ATTEMPTS = 3;
  private static final String TRANSLATION_API_KEYS = "translationApiKeys";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final @NonNull TenantAdminControlsRepository tenantAdminControlsRepository;
  private final @NonNull TranslationApiKeyEncryptionService translationApiKeyEncryptionService;

  @EventListener(ApplicationReadyEvent.class)
  public void migrate() {
    if (!translationApiKeyEncryptionService.isEnabled()) {
      warnAboutPlaintextKeys();
      return;
    }
    // Deliberately not @Transactional: every attempt is its own read and its own write, so a lost
    // optimistic-lock race can be caught and retried here instead of poisoning one big transaction.
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        tenantAdminControlsRepository
            .findTopByOrderByIdAsc()
            .ifPresent(
                entity -> {
                  int migrated = encryptPlaintextKeys(entity);
                  if (migrated > 0) {
                    log.info("Encrypted {} stored translation provider API key(s)", migrated);
                  }
                });
        return;
      } catch (OptimisticLockingFailureException anotherWriterWonTheRace) {
        // Several replicas start together, or an admin saved meanwhile. The row is versioned, so
        // nothing was lost; read it again - the other writer may already have done our work.
        log.info(
            "Translation API key migration met a concurrent write (attempt {} of {})",
            attempt,
            MAX_ATTEMPTS);
      }
    }
    log.warn(
        "Translation API key migration gave up after {} concurrent writes; it runs again at the"
            + " next start",
        MAX_ATTEMPTS);
  }

  /** Nothing can be encrypted without a secret; make sure the operator knows what is exposed. */
  private void warnAboutPlaintextKeys() {
    int plaintext =
        tenantAdminControlsRepository
            .findTopByOrderByIdAsc()
            .map(entity -> countStoredPlaintextKeys(entity.getControls()))
            .orElse(0);
    if (plaintext > 0) {
      // Count only - never the key, never its length or prefix.
      log.warn(
          "{} translation API key(s) are stored in plain text and cannot be encrypted: no"
              + " encryption secret is configured (SETTINGS_SMTP_PASSWORD_ENCRYPTION_SECRET)",
          plaintext);
    } else {
      log.info("Skipping translation API key encryption migration: encryption is not configured");
    }
  }

  private int countStoredPlaintextKeys(String controlsJson) {
    ObjectNode controls = readControls(controlsJson);
    JsonNode apiKeys = controls == null ? null : controls.get(TRANSLATION_API_KEYS);
    if (apiKeys == null || !apiKeys.isObject()) {
      return 0;
    }
    int count = 0;
    for (JsonNode value : apiKeys) {
      if (value.isTextual()
          && StringUtils.isNotBlank(value.asText())
          && !value.asText().startsWith("ENC:")) {
        count++;
      }
    }
    return count;
  }

  private int encryptPlaintextKeys(TenantAdminControlsEntity entity) {
    ObjectNode controls = readControls(entity.getControls());
    if (controls == null) {
      return 0;
    }
    JsonNode apiKeysNode = controls.get(TRANSLATION_API_KEYS);
    if (apiKeysNode == null || !apiKeysNode.isObject()) {
      return 0;
    }
    ObjectNode apiKeys = (ObjectNode) apiKeysNode;

    List<String> plaintextProviders = new ArrayList<>();
    apiKeys
        .fieldNames()
        .forEachRemaining(
            provider -> {
              JsonNode storedNode = apiKeys.get(provider);
              // Only strings. A number or a boolean is not a key we wrote, and asText() would
              // silently turn it into one - rewriting a value we do not understand is exactly what
              // this migration must not do.
              if (!storedNode.isTextual()) {
                return;
              }
              String storedValue = storedNode.asText();
              if (StringUtils.isNotBlank(storedValue)
                  && !translationApiKeyEncryptionService.isEncrypted(storedValue)) {
                plaintextProviders.add(provider);
              }
            });
    if (plaintextProviders.isEmpty()) {
      return 0;
    }

    plaintextProviders.forEach(
        provider ->
            apiKeys.put(
                provider,
                translationApiKeyEncryptionService.encryptStoredApiKey(
                    apiKeys.get(provider).asText())));

    entity.setControls(controls.toString());
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    tenantAdminControlsRepository.save(entity);
    return plaintextProviders.size();
  }

  private ObjectNode readControls(String controlsJson) {
    if (StringUtils.isBlank(controlsJson)) {
      return null;
    }
    try {
      JsonNode tree = OBJECT_MAPPER.readTree(controlsJson);
      return tree.isObject() ? (ObjectNode) tree : null;
    } catch (JsonProcessingException unreadableBlob) {
      // A blob this service cannot read is a separate problem; never fail startup over it, and
      // never overwrite something we did not understand. Deliberately narrow: catching Exception
      // here would swallow our own bugs on a path that runs before anyone can react.
      log.warn("Skipping translation API key migration: admin controls blob is not readable");
      return null;
    }
  }
}
