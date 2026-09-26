package com.vi.tenantservice.api.service.systememail;

import java.util.Map;

final class TenantSmtpTestCopy {
  record Message(String subject, String text) {}

  private static final Map<String, Message> MESSAGES =
      Map.of(
          "de",
          new Message(
              "ORISO SMTP-Test",
              "Der E-Mail-Server Ihres Trägers hat diese Testnachricht angenommen."),
          "en",
          new Message(
              "ORISO SMTP test", "Your organisation's email server accepted this test message."),
          "uk",
          new Message(
              "Тест SMTP ORISO",
              "Поштовий сервер вашої організації прийняв це тестове повідомлення."),
          "ru",
          new Message(
              "Тест SMTP ORISO",
              "Почтовый сервер вашей организации принял это тестовое сообщение."),
          "tr",
          new Message(
              "ORISO SMTP testi", "Kurumunuzun e-posta sunucusu bu test iletisini kabul etti."),
          "ar",
          new Message(
              "اختبار SMTP من ORISO", "قبل خادم البريد الإلكتروني لمؤسستك هذه الرسالة التجريبية."),
          "fa",
          new Message("آزمایش SMTP اوریزو", "سرور ایمیل سازمان شما این پیام آزمایشی را پذیرفت."));

  private TenantSmtpTestCopy() {}

  static Message forLanguage(String language) {
    if (language == null) return MESSAGES.get("de");
    return MESSAGES.getOrDefault(
        language.toLowerCase(java.util.Locale.ROOT).split("[-_]")[0], MESSAGES.get("de"));
  }
}
