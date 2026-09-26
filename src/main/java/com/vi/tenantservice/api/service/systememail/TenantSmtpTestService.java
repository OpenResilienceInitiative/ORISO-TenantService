package com.vi.tenantservice.api.service.systememail;

import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantSmtpTestService {
  private final AuthorisationService authorisation;
  private final TenantRepository tenants;
  private final SystemEmailDeliveryService delivery;

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public void send(long tenantId) {
    if (!authorisation.hasRole("single-tenant-admin")
        || authorisation.hasRole("technical")
        || !authorisation.findTenantIdInAccessToken().filter(id -> id == tenantId).isPresent()) {
      throw new AccessDeniedException("Tenant SMTP test is restricted to its admin");
    }
    String recipient = authorisation.getVerifiedEmail();
    var tenant =
        tenants
            .findByIdForSmtpTest(tenantId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    if (tenant.getSmtpTestRequestedAt() != null
        && tenant.getSmtpTestRequestedAt().isAfter(now.minusMinutes(1))) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "SMTP_TEST_COOLDOWN");
    }
    tenant.setSmtpTestRequestedAt(now);
    if (!delivery.deliverTest(tenantId, recipient, authorisation.getPreferredLanguage())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "TENANT_SMTP_NOT_SELECTED");
    }
  }
}
