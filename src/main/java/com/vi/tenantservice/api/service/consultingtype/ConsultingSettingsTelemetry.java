package com.vi.tenantservice.api.service.consultingtype;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsultingSettingsTelemetry {

  static final String REQUEST_ID_HEADER = "X-Consulting-Settings-Request-ID";

  enum Outcome {
    CONFIRMED,
    REFUSED,
    TIMEOUT,
    TRANSPORT_ERROR,
    UPSTREAM_ERROR,
    INVALID_RESPONSE,
    OTHER;

    String tag() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  private final MeterRegistry registry;
  private final Counter saves;
  private final EnumMap<Outcome, Timer> attempts = new EnumMap<>(Outcome.class);

  public ConsultingSettingsTelemetry(MeterRegistry registry) {
    this.registry = registry;
    saves =
        Counter.builder("oriso.tenant.settings.save")
            .description("Tenant settings save requests after authorisation checks")
            .register(registry);
    for (var outcome : Outcome.values()) {
      attempts.put(
          outcome,
          Timer.builder("oriso.tenant.consulting.settings.patch")
              .description(
                  "Duration and count of TenantService requests to save consulting settings")
              .tag("outcome", outcome.tag())
              .register(registry));
    }
  }

  public void recordSave() {
    saves.increment();
  }

  PatchAttempt startPatch() {
    return new PatchAttempt(UUID.randomUUID(), Timer.start(registry));
  }

  void recordPatch(PatchAttempt attempt, Outcome outcome) {
    long elapsed = attempt.sample().stop(attempts.get(outcome));
    log.info(
        "Consulting settings PATCH requestId={} outcome={} elapsedMs={}",
        attempt.requestId(),
        outcome.tag(),
        TimeUnit.NANOSECONDS.toMillis(elapsed));
  }

  record PatchAttempt(UUID requestId, Timer.Sample sample) {}
}
