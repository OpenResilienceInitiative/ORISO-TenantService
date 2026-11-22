package com.vi.tenantservice.api.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CaseHandoverDurationPolicyTest {

  @ParameterizedTest
  @MethodSource("validAdviceNeededDurations")
  void validateAdviceNeeded_shouldAcceptEveryPositiveFifteenMinuteStep(int minutes) {
    assertThatCode(() -> CaseHandoverDurationPolicy.validateAdviceNeeded(minutes))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("invalidAdviceNeededDurations")
  void validateAdviceNeeded_shouldRejectNullBelowMinimumAndPartialSteps(Integer minutes) {
    assertThatThrownBy(() -> CaseHandoverDurationPolicy.validateAdviceNeeded(minutes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateTakeover_shouldReserveNullForNonExpiringAccess() {
    assertThatCode(() -> CaseHandoverDurationPolicy.validateTakeover(null))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> CaseHandoverDurationPolicy.validateTakeover(180))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<Integer> validAdviceNeededDurations() {
    return Stream.of(15, 180, 15_000_000);
  }

  private static Stream<Integer> invalidAdviceNeededDurations() {
    return Stream.of(null, 0, 14, 16);
  }
}
