package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.TenantSmtpSettings;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.util.JsonConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class SmtpPasswordEncryptionMigrationIT {

  @Autowired private TenantRepository tenantRepository;
  @Autowired private SmtpPasswordEncryptionService smtpPasswordEncryptionService;
  @Autowired private SmtpPasswordEncryptionMigration migration;

  @Test
  void migrate_Should_encryptPlaintextPasswords_and_beIdempotent() {
    // given: a tenant row with a legacy plaintext SMTP password
    var tenant = tenantRepository.findById(1L).orElseThrow();
    TenantSettings settings = JsonConverter.convertFromJson(tenant.getSettings());
    settings.setSmtp(
        TenantSmtpSettings.builder()
            .enabled(true)
            .host("smtp.example.org")
            .username("smtp-user")
            .password("legacy-plaintext-secret")
            .build());
    tenant.setSettings(JsonConverter.convertToJson(settings));
    tenantRepository.save(tenant);

    // when
    migration.migrate();

    // then
    String storedPassword = storedSmtpPassword();
    assertThat(smtpPasswordEncryptionService.isEncrypted(storedPassword)).isTrue();
    assertThat(smtpPasswordEncryptionService.decrypt(storedPassword))
        .isEqualTo("legacy-plaintext-secret");

    // and: re-running changes nothing (no double encryption)
    migration.migrate();
    assertThat(storedSmtpPassword()).isEqualTo(storedPassword);
  }

  @Test
  void migrate_Should_reencryptReservedPrefixJunk_ThatIsNotRealCiphertext() {
    // given: a row whose password abuses the ENC: prefix without being our ciphertext
    var tenant = tenantRepository.findById(1L).orElseThrow();
    TenantSettings settings = JsonConverter.convertFromJson(tenant.getSettings());
    settings.setSmtp(
        TenantSmtpSettings.builder().enabled(true).password("ENC:not-a-ciphertext").build());
    tenant.setSettings(JsonConverter.convertToJson(settings));
    tenantRepository.save(tenant);

    // when
    migration.migrate();

    // then: it is treated as plaintext and properly encrypted
    String storedPassword = storedSmtpPassword();
    assertThat(smtpPasswordEncryptionService.isEncrypted(storedPassword)).isTrue();
    assertThat(smtpPasswordEncryptionService.decrypt(storedPassword))
        .isEqualTo("ENC:not-a-ciphertext");
  }

  @Test
  void migrate_Should_reencryptCounterfeitValidBase64Payload() {
    // given: a legacy plaintext password that even carries valid Base64 behind the prefix,
    // but was never encrypted by us (AES-GCM authentication fails)
    String counterfeit = "ENC:" + java.util.Base64.getEncoder().encodeToString(new byte[28]);
    var tenant = tenantRepository.findById(1L).orElseThrow();
    TenantSettings settings = JsonConverter.convertFromJson(tenant.getSettings());
    settings.setSmtp(TenantSmtpSettings.builder().enabled(true).password(counterfeit).build());
    tenant.setSettings(JsonConverter.convertToJson(settings));
    tenantRepository.save(tenant);

    // when
    migration.migrate();

    // then: the migration does not skip it — it is encrypted and round-trips
    String storedPassword = storedSmtpPassword();
    assertThat(storedPassword).isNotEqualTo(counterfeit);
    assertThat(smtpPasswordEncryptionService.isEncrypted(storedPassword)).isTrue();
    assertThat(smtpPasswordEncryptionService.decrypt(storedPassword)).isEqualTo(counterfeit);
  }

  private String storedSmtpPassword() {
    TenantSettings stored =
        JsonConverter.convertFromJson(tenantRepository.findById(1L).orElseThrow().getSettings());
    return stored.getSmtp().getPassword();
  }
}
