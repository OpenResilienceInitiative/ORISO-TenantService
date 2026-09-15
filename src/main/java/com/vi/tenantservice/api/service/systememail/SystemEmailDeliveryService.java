package com.vi.tenantservice.api.service.systememail;

import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
import com.vi.tenantservice.api.util.JsonConverter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SystemEmailDeliveryService {
  private final TenantRepository tenants;
  private final SmtpPasswordEncryptionService cipher;
  private final TenantSystemMailTransport transport;

  public boolean deliver(long tenantId, SystemEmailDeliveryRequest request) {
    if (tenantId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TENANT");
    var tenant =
        tenants
            .findById(tenantId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
    TenantSettings settings;
    try {
      settings =
          tenant.getSettings() == null ? null : JsonConverter.convertFromJson(tenant.getSettings());
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TENANT_SMTP_INVALID");
    }
    if (settings == null
        || !Boolean.TRUE.equals(settings.getFeatureSystemNotificationEmailsEnabled())
        || settings.getSmtp() == null
        || !settings.getSmtp().isEnabled()) return false;
    var smtp = settings.getSmtp();
    if (StringUtils.isBlank(smtp.getHost())
        || StringUtils.isBlank(smtp.getFrom())
        || StringUtils.isBlank(smtp.getUsername())
        || StringUtils.isBlank(smtp.getPassword())
        || smtp.getPort() == null
        || smtp.getPort() < 1
        || smtp.getPort() > 65535)
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TENANT_SMTP_INVALID");
    String password;
    try {
      password = cipher.decrypt(smtp.getPassword());
      if (StringUtils.isBlank(password) || password.startsWith("ENC:"))
        throw new IllegalStateException("Unusable credential");
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TENANT_SMTP_INVALID");
    }
    try {
      transport.send(smtp, password, request);
    } catch (Exception exception) {
      // Never attach SMTP exceptions: they can contain server replies, recipient or credentials.
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SMTP_DELIVERY_UNCONFIRMED");
    }
    return true;
  }
}
