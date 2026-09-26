package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class TenantSmtpTestServiceTest {
  final AuthorisationService auth = mock(AuthorisationService.class);
  final TenantRepository tenants = mock(TenantRepository.class);
  final SystemEmailDeliveryService delivery = mock(SystemEmailDeliveryService.class);
  final TenantSmtpTestService test = new TenantSmtpTestService(auth, tenants, delivery);
  final TenantEntity tenant = TenantEntity.builder().id(40L).build();

  @BeforeEach
  void setup() {
    when(auth.hasRole("single-tenant-admin")).thenReturn(true);
    when(auth.findTenantIdInAccessToken()).thenReturn(Optional.of(40L));
    when(auth.getVerifiedEmail()).thenReturn("admin@example.org");
    when(auth.getPreferredLanguage()).thenReturn("de");
    when(tenants.findByIdForSmtpTest(40L)).thenReturn(Optional.of(tenant));
    when(delivery.deliverTest(40L, "admin@example.org", "de")).thenReturn(true);
  }

  @Test
  void sendsOnlyToVerifiedTokenEmailAndLimitsRepeatedAttempts() {
    test.send(40L);
    verify(delivery).deliverTest(40L, "admin@example.org", "de");
    assertThatThrownBy(() -> test.send(40L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("SMTP_TEST_COOLDOWN");
    verify(delivery, times(1)).deliverTest(anyLong(), anyString(), anyString());
  }

  @Test
  void otherTenantAndUnverifiedCallerNeverLoadStoredCredentials() {
    assertThatThrownBy(() -> test.send(41L)).isInstanceOf(AccessDeniedException.class);
    when(auth.getVerifiedEmail())
        .thenThrow(new AccessDeniedException("Verified email is required for SMTP test"));
    assertThatThrownBy(() -> test.send(40L)).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(delivery);
  }

  @Test
  void platformModeDoesNotFallBackOrReportSuccess() {
    when(delivery.deliverTest(40L, "admin@example.org", "de")).thenReturn(false);
    assertThatThrownBy(() -> test.send(40L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("TENANT_SMTP_NOT_SELECTED");
  }
}
