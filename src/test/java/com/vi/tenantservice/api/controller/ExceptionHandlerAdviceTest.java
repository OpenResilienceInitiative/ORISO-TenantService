package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class ExceptionHandlerAdviceTest {

  private final ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice();

  @Test
  void handleException_Should_notPropagate_When_genericExceptionThrown() {
    assertThatCode(() -> advice.handleException(new IllegalArgumentException("boom")))
        .doesNotThrowAnyException();
  }

  @Test
  void handleException_Should_beAnnotatedWithInternalServerError() throws NoSuchMethodException {
    var method = ExceptionHandlerAdvice.class.getDeclaredMethod("handleException", Exception.class);
    var responseStatus = method.getAnnotation(ResponseStatus.class);

    assertThat(responseStatus).isNotNull();
    assertThat(responseStatus.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
