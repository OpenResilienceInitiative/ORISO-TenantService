package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class LegacyDpaSigningTimeTest {

  private static final String FIRST_VERIFIED_LOCAL_TIME = "2026-11-07T00:00:00";
  private static final LocalDateTime CUTOFF = LocalDateTime.parse(FIRST_VERIFIED_LOCAL_TIME);

  @Test
  void unverifiedRowsRemainUnresolvedUnlessBothSettingsArePresent() {
    assertThat(new LegacyDpaSigningTime("", "").resolve(CUTOFF)).isEmpty();
    assertThat(new LegacyDpaSigningTime("UTC", "").resolve(CUTOFF)).isEmpty();
    assertThat(new LegacyDpaSigningTime("", FIRST_VERIFIED_LOCAL_TIME).resolve(CUTOFF)).isEmpty();
  }

  @Test
  void verifiedDeploymentEnvironmentResolvesOnlyRowsAtOrAfterTheInclusiveCutoff() {
    new ApplicationContextRunner()
        .withInitializer(
            context ->
                context
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(
                        new SystemEnvironmentPropertySource(
                            "verified-writer-zone",
                            Map.of(
                                "ORISO_DPA_LEGACY_SIGNING_ZONE",
                                "UTC",
                                "ORISO_DPA_LEGACY_SIGNING_FROM",
                                FIRST_VERIFIED_LOCAL_TIME))))
        .withBean(LegacyDpaSigningTime.class)
        .run(
            context -> {
              var signerTime = context.getBean(LegacyDpaSigningTime.class);
              assertThat(signerTime.resolve(CUTOFF.minusSeconds(1))).isEmpty();
              assertThat(signerTime.resolve(CUTOFF))
                  .contains(Instant.parse("2026-11-07T00:00:00Z"));
              assertThat(signerTime.resolve(CUTOFF.plusHours(1)))
                  .contains(Instant.parse("2026-11-07T01:00:00Z"));
            });
  }
}
