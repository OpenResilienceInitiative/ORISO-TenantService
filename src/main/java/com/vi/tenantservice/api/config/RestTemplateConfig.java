package com.vi.tenantservice.api.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Contains the rest template configuration. */
@Configuration
public class RestTemplateConfig {

  /**
   * RestTemplate Bean.
   *
   * <p>Uses {@link JdkClientHttpRequestFactory} rather than {@code SimpleClientHttpRequestFactory}:
   * the latter is backed by {@code HttpURLConnection}, which does not support HTTP PATCH and throws
   * {@code java.net.ProtocolException: Invalid HTTP method: PATCH}. This bean is injected into the
   * generated OpenAPI clients (see {@code ConsultingTypeServiceApiControllerFactory}, {@code
   * ApplicationSettingsApiControllerFactory}, {@code UserAdminServiceApiControllerFactory}), which
   * do exercise PATCH — e.g. {@code ConsultingTypeService#patchConsultingType}. The JDK's {@link
   * HttpClient} supports PATCH natively and does not require an extra HTTP-client dependency.
   *
   * @return {@link RestTemplate}
   */
  @Bean
  public RestTemplate restTemplate() {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(8));
    RestTemplate restTemplate = new RestTemplate(factory);
    restTemplate.setErrorHandler(new CustomResponseErrorHandler());
    return restTemplate;
  }
}
