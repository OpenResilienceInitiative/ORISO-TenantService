package com.vi.tenantservice.api.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One authoritative time source for publication deadline validation and renewal expiry. */
@Configuration
public class DpaClockConfig {
  @Bean
  Clock dpaClock() {
    // Version keys and signature dates predate this clock and use local wall time. Keep their
    // existing zone; deadline comparisons use instant() and persist an explicit UTC projection.
    return Clock.systemDefaultZone();
  }
}
