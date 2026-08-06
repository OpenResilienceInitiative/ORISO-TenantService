package com.vi.tenantservice.api.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Contains the rest template configuration. */
@Configuration
public class RestTemplateConfig {

  /**
   * RestTemplate Bean.
   *
   * @return {@link RestTemplate}
   */
  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(8));
    RestTemplate restTemplate = new RestTemplate(factory);
    restTemplate.setErrorHandler(new CustomResponseErrorHandler());
    return restTemplate;
  }
}
