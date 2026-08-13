package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SmtpPasswordEncryptionServiceTest {

  private static final String TEST_SECRET = "test-smtp-encryption-secret";

  @Test
  void encrypt_Should_ReturnPlaintext_When_SecretNotConfigured() {
    var service = new SmtpPasswordEncryptionService("");

    assertThat(service.isEnabled()).isFalse();
    assertThat(service.encrypt("plain-pass")).isEqualTo("plain-pass");
  }

  @Test
  void encrypt_Should_ReturnEncryptedValue_When_SecretConfigured() {
    var service = new SmtpPasswordEncryptionService(TEST_SECRET);

    String encrypted = service.encrypt("plain-pass");

    assertThat(service.isEnabled()).isTrue();
    assertThat(encrypted).startsWith(SmtpPasswordEncryptionService.ENCRYPTED_PREFIX);
    assertThat(encrypted).isNotEqualTo("plain-pass");
  }

  @Test
  void decrypt_Should_RoundTripEncryptedValue() {
    var service = new SmtpPasswordEncryptionService(TEST_SECRET);

    String encrypted = service.encrypt("plain-pass");

    assertThat(service.decrypt(encrypted)).isEqualTo("plain-pass");
  }

  @Test
  void decrypt_Should_PassThroughLegacyPlaintext() {
    var service = new SmtpPasswordEncryptionService(TEST_SECRET);

    assertThat(service.decrypt("legacy-plaintext")).isEqualTo("legacy-plaintext");
  }

  @Test
  void encrypt_Should_ReturnEmptyString_When_PasswordIsEmpty() {
    var service = new SmtpPasswordEncryptionService(TEST_SECRET);

    assertThat(service.encrypt("")).isEmpty();
    assertThat(service.encrypt(null)).isNull();
  }

  @Test
  void encrypt_Should_NotDoubleEncrypt_When_ValueAlreadyEncrypted() {
    var service = new SmtpPasswordEncryptionService(TEST_SECRET);

    String encrypted = service.encrypt("plain-pass");

    assertThat(service.encrypt(encrypted)).isEqualTo(encrypted);
  }

  @Test
  void decrypt_Should_Throw_When_KeyDoesNotMatch() {
    var service = new SmtpPasswordEncryptionService(TEST_SECRET);
    var otherService = new SmtpPasswordEncryptionService("another-secret");

    String encrypted = service.encrypt("plain-pass");

    assertThatThrownBy(() -> otherService.decrypt(encrypted))
        .isInstanceOf(IllegalStateException.class);
  }
}
