package com.vi.tenantservice.api.service.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time (idempotent) startup migration: encrypts machine-translation provider API keys that are
 * still stored in plaintext inside the {@code tenant_admin_controls.controls} JSON.
 *
 * <p>Same shape as {@code SmtpPasswordEncryptionMigration} and for the same reason: without it the
 * keys written before this change would stay in plaintext until a super admin happens to save them
 * again, which may be never.
 *
 * <p>Runs only when the encryption secret is configured. Values that already authenticate under the
 * key are left untouched, so re-running changes nothing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TranslationApiKeyEncryptionMigration {

  private final @NonNull TenantAdminControlsRepository tenantAdminControlsRepository;
  private final @NonNull TranslationApiKeyEncryptionService translationApiKeyEncryptionService;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void migrate() {
    if (!translationApiKeyEncryptionService.isEnabled()) {
      log.info("Skipping translation API key encryption migration: encryption is not configured");
      return;
    }
    tenantAdminControlsRepository
        .findTopByOrderByIdAsc()
        .ifPresent(
            entity -> {
              int migrated = encryptPlaintextKeys(entity);
              if (migrated > 0) {
                log.info("Encrypted {} stored translation provider API key(s)", migrated);
              }
            });
  }

  private int encryptPlaintextKeys(TenantAdminControlsEntity entity) {
    TenantAdminControlsSettings settings = parse(entity.getControls());
    if (settings == null || settings.getTranslationApiKeys() == null) {
      return 0;
    }
    Map<String, String> rewritten = new HashMap<>();
    int migrated = 0;
    for (Map.Entry<String, String> entry : settings.getTranslationApiKeys().entrySet()) {
      String storedValue = entry.getValue();
      if (StringUtils.isBlank(storedValue)
          || translationApiKeyEncryptionService.isEncrypted(storedValue)) {
        rewritten.put(entry.getKey(), storedValue);
        continue;
      }
      rewritten.put(
          entry.getKey(), translationApiKeyEncryptionService.encryptStoredApiKey(storedValue));
      migrated++;
    }
    if (migrated == 0) {
      return 0;
    }
    settings.setTranslationApiKeys(rewritten);
    entity.setControls(serialize(settings));
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    tenantAdminControlsRepository.save(entity);
    return migrated;
  }

  private TenantAdminControlsSettings parse(String controlsJson) {
    if (StringUtils.isBlank(controlsJson)) {
      return null;
    }
    try {
      return new ObjectMapper().readValue(controlsJson, TenantAdminControlsSettings.class);
    } catch (JsonProcessingException exception) {
      // A blob this service cannot read is a separate problem; never fail startup over it.
      log.warn("Skipping translation API key migration: admin controls blob is not readable");
      return null;
    }
  }

  private String serialize(TenantAdminControlsSettings settings) {
    try {
      return new ObjectMapper().writeValueAsString(settings);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize admin controls", exception);
    }
  }
}
