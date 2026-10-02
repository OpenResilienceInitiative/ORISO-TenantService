package com.vi.tenantservice.api.service.systememail;

import java.util.Map;

final class TenantSmtpTestCopy {
  record Message(String subject, String text) {}

  private static final Map<String, Message> MESSAGES =
      Map.of(
          "de-sie",
          new Message(
              "ORISO SMTP-Test",
              "Der E-Mail-Server Ihres Trägers hat diese Testnachricht angenommen."),
          "de-du",
          new Message(
              "ORISO SMTP-Test",
              "Der E-Mail-Server deiner Organisation hat diese Testnachricht angenommen."),
          "en",
          new Message(
              "ORISO SMTP test", "Your organisation's email server accepted this test message."),
          "fr",
          new Message(
              "Test SMTP ORISO",
              "Le serveur de messagerie de votre organisme a accepté ce message de test."),
          "ru",
          new Message(
              "Тест SMTP ORISO",
              "Почтовый сервер вашей организации принял это тестовое сообщение."),
          "tr",
          new Message(
              "ORISO SMTP testi", "Kurumunuzun e-posta sunucusu bu test iletisini kabul etti."),
          "ti",
          new Message("ፈተነ SMTP ORISO", "ናይ ትካልኩም ኢመይል ሰርቨር ነዚ ናይ ፈተነ መልእኽቲ ተቐቢሉዎ።"));

  private TenantSmtpTestCopy() {}

  static Message forLanguage(String language) {
    if (language == null) return MESSAGES.get("de-sie");
    String normalized = language.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    if (MESSAGES.containsKey(normalized)) return MESSAGES.get(normalized);
    String base = normalized.split("-")[0];
    return MESSAGES.getOrDefault(base, MESSAGES.get("de-sie"));
  }
}
