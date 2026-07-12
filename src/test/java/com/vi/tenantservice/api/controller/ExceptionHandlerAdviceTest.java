package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vi.tenantservice.api.exception.TenantNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

class ExceptionHandlerAdviceTest {

  private final ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice();

  @Test
  void handleException_Should_notPropagate_When_genericExceptionWithoutResponseStatusThrown() {
    assertThatCode(() -> advice.handleException(new IllegalArgumentException("boom")))
        .doesNotThrowAnyException();
  }

  @Test
  void handleException_Should_rethrow_When_accessDeniedExceptionThrown() {
    var accessDenied = new AccessDeniedException("denied");

    assertThatThrownBy(() -> advice.handleException(accessDenied)).isSameAs(accessDenied);
  }

  @Test
  void handleException_Should_rethrow_When_exceptionCarriesResponseStatusAnnotation() {
    var notFound = new TenantNotFoundException("no such tenant");

    assertThatThrownBy(() -> advice.handleException(notFound)).isSameAs(notFound);
  }

  @Test
  void handleException_Should_rethrow_When_responseStatusExceptionThrown() {
    var responseStatusException = new ResponseStatusException(HttpStatus.NOT_FOUND);

    assertThatThrownBy(() -> advice.handleException(responseStatusException))
        .isSameAs(responseStatusException);
  }

  @Test
  void handleException_Should_beAnnotatedWithInternalServerError() throws NoSuchMethodException {
    var method = ExceptionHandlerAdvice.class.getDeclaredMethod("handleException", Exception.class);
    var responseStatus = method.getAnnotation(ResponseStatus.class);

    assertThat(responseStatus).isNotNull();
    assertThat(responseStatus.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
