package com.vi.tenantservice.api.policy;

public final class CaseHandoverDurationPolicy {

  public static final int MINIMUM_MINUTES = 15;
  public static final int STEP_MINUTES = 15;
  public static final int DEFAULT_MINUTES = 180;

  private CaseHandoverDurationPolicy() {}

  public static void validateAdviceNeeded(Integer minutes) {
    if (minutes == null || minutes < MINIMUM_MINUTES || minutes % STEP_MINUTES != 0) {
      throw new IllegalArgumentException(
          "Advice Needed duration must be a non-null multiple of 15 minutes and at least 15");
    }
  }

  public static void validateTakeover(Integer minutes) {
    if (minutes != null) {
      throw new IllegalArgumentException("Takeover duration must be null (non-expiring)");
    }
  }
}
