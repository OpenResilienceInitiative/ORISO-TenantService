package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.util.JsonConverter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time (idempotent) startup migration for #183: re-encrypts tenant SMTP passwords that are
 * still stored in plaintext inside the {@code tenant.settings} JSON. Runs only when the encryption
 * secret is configured; already-encrypted values ({@code ENC:} prefix) are left untouched, so
 * re-running the migration changes nothing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SmtpPasswordEncryptionMigration {

  private final @NonNull TenantRepository tenantRepository;
  private final @NonNull SmtpPasswordEncryptionService smtpPasswordEncryptionService;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void migrate() {
    if (!smtpPasswordEncryptionService.isEnabled()) {
      log.info("Skipping SMTP password encryption migration: encryption is not configured");
      return;
    }
    var migrated =
        tenantRepository.findAll().stream().filter(this::encryptPlaintextSmtpPassword).count();
    if (migrated > 0) {
      log.info("Encrypted the stored SMTP password of {} tenant(s)", migrated);
    }
  }

  private boolean encryptPlaintextSmtpPassword(TenantEntity tenant) {
    if (tenant.getSettings() == null) {
      return false;
    }
    TenantSettings settings = JsonConverter.convertFromJson(tenant.getSettings());
    if (settings.getSmtp() == null
        || StringUtils.isBlank(settings.getSmtp().getPassword())
        || smtpPasswordEncryptionService.isEncrypted(settings.getSmtp().getPassword())) {
      return false;
    }
    settings
        .getSmtp()
        .setPassword(smtpPasswordEncryptionService.encrypt(settings.getSmtp().getPassword()));
    tenant.setSettings(JsonConverter.convertToJson(settings));
    tenantRepository.save(tenant);
    return true;
  }
}
