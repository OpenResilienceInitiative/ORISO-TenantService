package com.vi.tenantservice.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * SpringFox configuration - disabled for Spring Boot 4.0 compatibility.
 * Springfox is not compatible with Spring Boot 4.0/Spring Framework 7.
 * API documentation is generated via OpenAPI Generator from YAML specs.
 */
@Configuration
@ConditionalOnProperty(name = "springfox.enabled", havingValue = "true", matchIfMissing = false)
public class SpringFoxConfig {

  // White list for path patterns that should be white listed so that swagger UI can be accessed
  // without authorization
  public static final String[] WHITE_LIST =
      new String[] {
        "/mails/docs",
        "/mails/docs/**",
        "/v2/api-docs",
        "/configuration/ui",
        "/swagger-resources/**",
        "/configuration/security",
        "/swagger-ui",
        "/swagger-ui/**",
        "/webjars/**"
      };
}
