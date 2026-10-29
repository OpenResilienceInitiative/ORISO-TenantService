package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class TenantHeldBootstrapFailureTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void recoveryReadRetainsBothFailuresWithoutSuppressingItself(boolean sameFailure) {
    var primary = new IllegalStateException("Synthetic primary failure");
    var recovery = sameFailure ? primary : new IllegalStateException("Synthetic recovery failure");
    var journal = mock(TenantHeldBootstrapJournal.class);
    var operation = mock(TenantHeldBootstrapJournal.Operation.class);
    operation.open = true;
    var validator = mock(Validator.class);
    var command = JsonNodeFactory.instance.objectNode();
    when(journal.prepare(command)).thenReturn(operation);
    when(validator.validate(any())).thenThrow(primary);
    when(journal.read(operation, false)).thenThrow(recovery);
    var bootstrap =
        new TenantHeldBootstrap(
            journal,
            mock(TenantBootstrapCreation.class),
            mock(TenantServiceFacade.class),
            mock(ConsultingTypeService.class),
            validator);

    var failure = org.assertj.core.api.Assertions.catchThrowable(() -> bootstrap.create(command));

    assertThat(failure).isInstanceOf(ResponseStatusException.class).hasCause(recovery);
    assertThat(((ResponseStatusException) failure).getStatusCode().value()).isEqualTo(503);
    if (sameFailure) assertThat(recovery.getSuppressed()).isEmpty();
    else assertThat(recovery.getSuppressed()).containsExactly(primary);
    assertThat(operation.open).isFalse();
  }
}
