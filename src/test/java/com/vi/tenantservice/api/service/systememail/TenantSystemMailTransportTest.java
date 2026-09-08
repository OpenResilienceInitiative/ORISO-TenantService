package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vi.tenantservice.api.model.TenantSmtpSettings;
import jakarta.mail.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantSystemMailTransportTest {
  final TenantSystemMailTransport transport = new TenantSystemMailTransport();

  TenantSmtpSettings settings(boolean secure) {
    return TenantSmtpSettings.builder()
        .host("smtp.example.org")
        .port(secure ? 465 : 587)
        .secure(secure)
        .username("test-user")
        .from("sender@example.org")
        .build();
  }

  @Test
  void starttlsIsRequiredAndServerCertificateChecked() {
    var props = transport.properties(settings(false));
    assertThat(props)
        .containsEntry("mail.smtp.starttls.required", "true")
        .containsEntry("mail.smtp.ssl.checkserveridentity", "true");
    assertThat(props).doesNotContainKey("mail.smtp.ssl.trust");
  }

  @Test
  void implicitTlsHasNoPlaintextFallback() {
    var props = transport.properties(settings(true));
    assertThat(props)
        .containsEntry("mail.smtp.ssl.enable", "true")
        .containsEntry("mail.smtp.ssl.checkserveridentity", "true");
    assertThat(props).doesNotContainKey("mail.smtp.starttls.enable");
  }

  @Test
  void sendsExactlyOneRecipientAndMultipartTextBeforeHtml() throws Exception {
    var captured = new java.util.concurrent.atomic.AtomicReference<Message>();
    try (var smtp = mockStatic(Transport.class)) {
      smtp.when(() -> Transport.send(any(Message.class)))
          .thenAnswer(
              i -> {
                captured.set(i.getArgument(0));
                return null;
              });
      transport.send(
          settings(false),
          "synthetic",
          new SystemEmailDeliveryRequest(
              SystemEmailDeliveryRequest.Purpose.EMAIL_ADDRESS_CHANGED,
              "recipient@example.org",
              "Änderung",
              "<p>Änderung</p>",
              "Änderung",
              UUID.randomUUID()));
      smtp.verify(() -> Transport.send(any(Message.class)), times(1));
    }
    assertThat(captured.get().getAllRecipients()).hasSize(1);
    var mime = (Multipart) captured.get().getContent();
    assertThat(mime.getCount()).isEqualTo(2);
    assertThat(mime.getBodyPart(0).getContent()).isEqualTo("Änderung");
    assertThat(mime.getBodyPart(1).getContent()).isEqualTo("<p>Änderung</p>");
    assertThat(captured.get().getSubject()).isEqualTo("Änderung");
  }

  @Test
  void rejectsRecipientListsBeforeNetwork() {
    try (var smtp = mockStatic(Transport.class)) {
      assertThatThrownBy(
              () ->
                  transport.send(
                      settings(false),
                      "synthetic",
                      new SystemEmailDeliveryRequest(
                          SystemEmailDeliveryRequest.Purpose.EMAIL_ADDRESS_CHANGED,
                          "one@example.org,two@example.org",
                          "Subject",
                          "<p>Body</p>",
                          "Body",
                          UUID.randomUUID())))
          .isInstanceOf(MessagingException.class);
      smtp.verifyNoInteractions();
    }
  }
}
