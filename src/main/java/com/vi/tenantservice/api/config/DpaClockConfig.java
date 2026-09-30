package com.vi.tenantservice.api.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One authoritative time source for publication deadline validation and renewal expiry. */
@Configuration
public class DpaClockConfig {
  @Bean
  Clock dpaClock() {
    return Clock.systemDefaultZone();
  }
}
