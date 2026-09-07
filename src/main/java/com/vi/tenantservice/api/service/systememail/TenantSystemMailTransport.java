package com.vi.tenantservice.api.service.systememail;

import com.vi.tenantservice.api.model.TenantSmtpSettings;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class TenantSystemMailTransport {
  public void send(TenantSmtpSettings settings, String password, SystemEmailDeliveryRequest request)
      throws MessagingException {
    InternetAddress[] recipients = InternetAddress.parse(request.recipient(), true);
    if (recipients.length != 1 || recipients[0].isGroup())
      throw new AddressException("Exactly one recipient required");
    recipients[0].validate();
    var session =
        Session.getInstance(
            properties(settings),
            new Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(settings.getUsername(), password);
              }
            });
    var message = new MimeMessage(session);
    message.setFrom(new InternetAddress(settings.getFrom(), true));
    message.setRecipients(Message.RecipientType.TO, recipients);
    message.setSubject(request.subject(), "UTF-8");
    var text = new MimeBodyPart();
    text.setText(request.text(), "UTF-8");
    var html = new MimeBodyPart();
    html.setContent(request.html(), "text/html; charset=UTF-8");
    var alternatives = new MimeMultipart("alternative");
    alternatives.addBodyPart(text);
    alternatives.addBodyPart(html);
    message.setContent(alternatives);
    Transport.send(message);
  }

  Properties properties(TenantSmtpSettings settings) {
    var props = new Properties();
    props.setProperty("mail.smtp.host", settings.getHost());
    props.setProperty("mail.smtp.port", String.valueOf(settings.getPort()));
    props.setProperty("mail.smtp.auth", "true");
    props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
    props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
    props.setProperty("mail.smtp.connectiontimeout", "10000");
    props.setProperty("mail.smtp.timeout", "30000");
    props.setProperty("mail.smtp.writetimeout", "30000");
    if (settings.isSecure()) props.setProperty("mail.smtp.ssl.enable", "true");
    else {
      props.setProperty("mail.smtp.starttls.enable", "true");
      props.setProperty("mail.smtp.starttls.required", "true");
    }
    return props;
  }
}
