package com.vi.tenantservice.api.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@ConditionalOnProperty(name = "tenant.cors.enabled", havingValue = "true")
public class CorsConfig {

  @Value("${tenant.cors.allowed.origins}")
  private String[] allowedOrigins;

  @Value("${tenant.cors.allowed.paths}")
  private List<String> allowedPaths;

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
    var configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true);
    configuration.setAllowedOrigins(List.of(allowedOrigins));
    configuration.setAllowedMethods(List.of("OPTIONS", "GET", "POST", "PUT", "PATCH", "DELETE"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("X-Reason"));

    var source = new UrlBasedCorsConfigurationSource();
    allowedPaths.forEach(path -> source.registerCorsConfiguration(path, configuration));

    var registrationBean = new FilterRegistrationBean<>(new CorsFilter(source));
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
  }
}
