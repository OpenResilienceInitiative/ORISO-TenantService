package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.config.apiclient.UserAdminServiceApiControllerFactory;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.web.DeletionAuditLogsControllerApi;
import com.vi.tenantservice.useradminservice.generated.web.model.DeletionAuditEntryRequestDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.DeletionAuditEntryRequestDTO.OutcomeEnum;
import com.vi.tenantservice.useradminservice.generated.web.model.DeletionAuditEntryRequestDTO.TargetTypeEnum;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class DeletionAuditReporterTest {

  @InjectMocks private DeletionAuditReporter reporter;
  @Mock private UserAdminServiceApiControllerFactory factory;
  @Mock private SecurityHeaderSupplier securityHeaderSupplier;
  @Mock private DeletionAuditLogsControllerApi api;
  @Mock private ApiClient apiClient;

  @BeforeEach
  void wire() {
    lenient().when(factory.createDeletionAuditApi()).thenReturn(api);
    lenient().when(api.getApiClient()).thenReturn(apiClient);
    lenient()
        .when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(new HttpHeaders());
  }

  @Test
  void report_Should_ForwardTheCallersOwnToken_SoUserServiceNamesTheActor() {
    var headers = new HttpHeaders();
    headers.setBearerAuth("admin-token");
    headers.add("X-CSRF-TOKEN", "csrf");
    // UserService's CSRF filter compares the header with this cookie; both must arrive.
    headers.add("Cookie", "CSRF-TOKEN=csrf");
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders()).thenReturn(headers);

    reporter.reportTenantExecuted(9L);

    verify(apiClient).addDefaultHeader("Authorization", "Bearer admin-token");
    verify(apiClient).addDefaultHeader("X-CSRF-TOKEN", "csrf");
    verify(apiClient).addDefaultHeader("Cookie", "CSRF-TOKEN=csrf");
  }

  @Test
  void reportTenantExecuted_Should_SendOnlyTypeIdTenantAndOutcome() {
    reporter.reportTenantExecuted(9L);

    var captor = ArgumentCaptor.forClass(DeletionAuditEntryRequestDTO.class);
    verify(api).recordDeletion(captor.capture());
    var sent = captor.getValue();
    assertThat(sent.getTargetType()).isEqualTo(TargetTypeEnum.TENANT);
    assertThat(sent.getTargetId()).isEqualTo("9");
    assertThat(sent.getTenantId()).isEqualTo(9L);
    assertThat(sent.getOutcome()).isEqualTo(OutcomeEnum.EXECUTED);
    assertThat(sent.getActorId()).isNull();
    assertThat(sent.getActorRole()).isNull();
  }

  @Test
  void reportTenantExecuted_Should_WaitForTheCommit_AndSkipARolledBackDeletion() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      reporter.reportTenantExecuted(9L);
      verify(api, never()).recordDeletion(any());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verify(api, never()).recordDeletion(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void reportTenantExecuted_Should_SendAfterTheCommit() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      reporter.reportTenantExecuted(9L);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    verify(api).recordDeletion(any());
  }

  @Test
  void reportTenantExecuted_Should_NeverFailTheDeletion_WhenTheLogServiceIsDown() {
    doThrow(new RestClientException("userservice down")).when(api).recordDeletion(any());

    assertThatCode(() -> reporter.reportTenantExecuted(9L)).doesNotThrowAnyException();
  }

  @Test
  void reportTenantExecuted_Should_NeverFailTheDeletion_WhenThereIsNoCallerToken() {
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenThrow(new ClassCastException("no jwt principal"));

    assertThatCode(() -> reporter.reportTenantExecuted(9L)).doesNotThrowAnyException();
  }

  @Test
  void request_Should_HoldNoPersonalDataFields() {
    var fields =
        Arrays.stream(DeletionAuditEntryRequestDTO.class.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .map(f -> f.getName())
            .toList();
    assertThat(fields)
        .containsExactlyInAnyOrder(
            "targetType", "targetId", "tenantId", "outcome", "actorId", "actorRole");
  }
}
