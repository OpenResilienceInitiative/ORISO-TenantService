package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.model.TenantSmtpSettings;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SystemEmailDeliveryServiceTest {
  final TenantRepository tenants = mock(TenantRepository.class);
  final ObjectMapper mapper = new ObjectMapper();
  final SmtpPasswordEncryptionService cipher = mock(SmtpPasswordEncryptionService.class);
  final TenantSystemMailTransport transport = mock(TenantSystemMailTransport.class);
  final SystemEmailDeliveryService service =
      new SystemEmailDeliveryService(tenants, mapper, cipher, transport);
  final SystemEmailDeliveryRequest request =
      new SystemEmailDeliveryRequest(
          SystemEmailDeliveryRequest.Purpose.EMAIL_ADDRESS_CHANGED,
          "recipient@example.org",
          "Subject",
          "<p>Body</p>",
          "Body",
          UUID.randomUUID());

  TenantEntity tenant(boolean enabled, boolean smtpEnabled) throws Exception {
    var smtp =
        TenantSmtpSettings.builder()
            .enabled(smtpEnabled)
            .host("tenant40.example.org")
            .port(587)
            .username("test-user")
            .password("ENC:test-fixture")
            .from("sender@example.org")
            .build();
    return TenantEntity.builder()
        .id(40L)
        .settings(
            mapper.writeValueAsString(
                TenantSettings.builder()
                    .featureSystemNotificationEmailsEnabled(enabled)
                    .smtp(smtp)
                    .build()))
        .build();
  }

  @Test
  void decryptsOnlySelectedTenantAndDeliversOnce() throws Exception {
    when(tenants.findById(40L)).thenReturn(Optional.of(tenant(true, true)));
    when(cipher.decrypt("ENC:test-fixture")).thenReturn("test-password");
    assertThat(service.deliver(40, request)).isTrue();
    verify(transport)
        .send(
            argThat(s -> s.getHost().equals("tenant40.example.org")),
            eq("test-password"),
            same(request));
    verify(tenants, never()).findById(1L);
  }

  @Test
  void disabledOrRemovedSettingsAreReadFreshWithoutDecrypting() throws Exception {
    when(tenants.findById(40L))
        .thenReturn(
            Optional.of(tenant(true, true)),
            Optional.of(tenant(false, true)),
            Optional.of(TenantEntity.builder().id(40L).build()));
    when(cipher.decrypt(anyString())).thenReturn("test-password");
    assertThat(service.deliver(40, request)).isTrue();
    assertThat(service.deliver(40, request)).isFalse();
    assertThat(service.deliver(40, request)).isFalse();
    verify(transport, times(1)).send(any(), anyString(), any());
    verify(cipher, times(1)).decrypt(anyString());
  }

  @Test
  void disabledSmtpDoesNotSend() throws Exception {
    when(tenants.findById(40L)).thenReturn(Optional.of(tenant(true, false)));
    assertThat(service.deliver(40, request)).isFalse();
    verifyNoInteractions(cipher, transport);
  }

  @Test
  void invalidOrUnknownTenantCannotSelectDefault() {
    assertThatThrownBy(() -> service.deliver(0, request))
        .isInstanceOf(ResponseStatusException.class);
    when(tenants.findById(40L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.deliver(40, request))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(cipher, transport);
  }

  @Test
  void decryptionFailureIsSanitizedAndDoesNotSend() throws Exception {
    when(tenants.findById(40L)).thenReturn(Optional.of(tenant(true, true)));
    when(cipher.decrypt(anyString())).thenThrow(new IllegalStateException("secret-fixture"));
    assertThatThrownBy(() -> service.deliver(40, request))
        .hasMessageContaining("TENANT_SMTP_INVALID")
        .hasMessageNotContaining("secret-fixture")
        .hasNoCause();
    verifyNoInteractions(transport);
  }

  @Test
  void smtpFailureIsUnconfirmedAndNeverRetried() throws Exception {
    when(tenants.findById(40L)).thenReturn(Optional.of(tenant(true, true)));
    when(cipher.decrypt(anyString())).thenReturn("test-password");
    doThrow(new jakarta.mail.MessagingException("secret-fixture"))
        .when(transport)
        .send(any(), anyString(), any());
    assertThatThrownBy(() -> service.deliver(40, request))
        .hasMessageContaining("SMTP_DELIVERY_UNCONFIRMED")
        .hasMessageNotContaining("secret-fixture")
        .hasNoCause();
    verify(transport, times(1)).send(any(), anyString(), any());
  }
}
