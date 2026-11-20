package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class LegacyDpaSigningTimeTest {

  private static final String FIRST_VERIFIED_LOCAL_TIME = "2026-11-07T00:00:00";
  private static final String LAST_VERIFIED_LOCAL_TIME = "2026-11-07T23:59:59";
  private static final LocalDateTime CUTOFF = LocalDateTime.parse(FIRST_VERIFIED_LOCAL_TIME);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ORISO_DPA_LEGACY_SIGNING_ZONE",
        "ORISO_DPA_LEGACY_SIGNING_FROM",
        "ORISO_DPA_LEGACY_SIGNING_TO"
      })
  void aMissingSettingDoesNotEnableConversion(String missing) {
    var settings =
        new HashMap<>(window("UTC", FIRST_VERIFIED_LOCAL_TIME, LAST_VERIFIED_LOCAL_TIME));
    settings.remove(missing);
    runnerWithSettings(settings)
        .run(
            context ->
                assertThat(context.getBean(LegacyDpaSigningTime.class).resolve(CUTOFF)).isEmpty());
  }

  @Test
  void verifiedDeploymentEnvironmentResolvesOnlyRowsWithinBothInclusiveBounds() {
    var end = LocalDateTime.parse(LAST_VERIFIED_LOCAL_TIME);
    runnerWithSettings(window("UTC", FIRST_VERIFIED_LOCAL_TIME, LAST_VERIFIED_LOCAL_TIME))
        .run(
            context -> {
              var signerTime = context.getBean(LegacyDpaSigningTime.class);
              assertThat(signerTime.resolve(CUTOFF.minusNanos(1))).isEmpty();
              assertThat(signerTime.resolve(CUTOFF))
                  .contains(Instant.parse("2026-11-07T00:00:00Z"));
              assertThat(signerTime.resolve(CUTOFF.plusHours(1)))
                  .contains(Instant.parse("2026-11-07T01:00:00Z"));
              assertThat(signerTime.resolve(end)).contains(Instant.parse("2026-11-07T23:59:59Z"));
              assertThat(signerTime.resolve(end.plusNanos(1))).isEmpty();
            });
  }

  @Test
  void rejectsAnEndBeforeTheStart() {
    runnerWithSettings(window("UTC", LAST_VERIFIED_LOCAL_TIME, FIRST_VERIFIED_LOCAL_TIME))
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class));
  }

  @Test
  void acceptsASingleVerifiedLocalTimestamp() {
    runnerWithSettings(window("UTC", FIRST_VERIFIED_LOCAL_TIME, FIRST_VERIFIED_LOCAL_TIME))
        .run(
            context -> {
              var signerTime = context.getBean(LegacyDpaSigningTime.class);
              assertThat(signerTime.resolve(CUTOFF))
                  .contains(Instant.parse("2026-11-07T00:00:00Z"));
              assertThat(signerTime.resolve(CUTOFF.plusSeconds(1))).isEmpty();
            });
  }

  @Test
  void configuredBerlinZoneRejectsDstGapAndOverlapWithinTheVerifiedWindow() {
    runnerWithSettings(window("Europe/Berlin", "2026-03-01T00:00", "2026-10-31T23:59"))
        .run(
            context -> {
              var signerTime = context.getBean(LegacyDpaSigningTime.class);
              assertThat(signerTime.resolve(LocalDateTime.parse("2026-07-02T15:00")))
                  .contains(Instant.parse("2026-07-02T13:00:00Z"));
              assertThat(signerTime.resolve(LocalDateTime.parse("2026-03-29T02:30"))).isEmpty();
              assertThat(signerTime.resolve(LocalDateTime.parse("2026-10-25T02:30"))).isEmpty();
            });
  }

  private static Map<String, Object> window(String zone, String from, String to) {
    return Map.of(
        "ORISO_DPA_LEGACY_SIGNING_ZONE", zone,
        "ORISO_DPA_LEGACY_SIGNING_FROM", from,
        "ORISO_DPA_LEGACY_SIGNING_TO", to);
  }

  private static ApplicationContextRunner runnerWithSettings(Map<String, Object> settings) {
    return new ApplicationContextRunner()
        .withInitializer(
            context ->
                context
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(
                        new SystemEnvironmentPropertySource("verified-writer-zone", settings)))
        .withBean(LegacyDpaSigningTime.class);
  }
}
