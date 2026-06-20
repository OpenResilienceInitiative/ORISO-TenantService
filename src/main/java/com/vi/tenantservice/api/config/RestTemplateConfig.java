package com.vi.tenantservice.api.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Contains the rest template configuration. */
@Configuration
public class RestTemplateConfig {

  /**
   * RestTemplate Bean.
   *
   * @param builder {@link RestTemplateBuilder}
   * @return {@link RestTemplate}
   */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(2))
        .setReadTimeout(Duration.ofSeconds(8))
        .errorHandler(new CustomResponseErrorHandler())
        .build();
  }
}
