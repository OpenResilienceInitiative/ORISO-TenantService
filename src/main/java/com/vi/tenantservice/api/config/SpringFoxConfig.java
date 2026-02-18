package com.vi.tenantservice.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

/** OpenAPI / Swagger UI configuration (springdoc-openapi). */
@Configuration
public class SpringFoxConfig {

  @Value("${springfox.docuTitle}")
  private String docuTitle;

  @Value("${springfox.docuDescription}")
  private String docuDescription;

  @Value("${springfox.docuVersion}")
  private String docuVersion;

  @Value("${springfox.docuTermsUrl}")
  private String docuTermsUrl;

  @Value("${springfox.docuContactName}")
  private String docuContactName;

  @Value("${springfox.docuContactUrl}")
  private String docuContactUrl;

  @Value("${springfox.docuContactEmail}")
  private String docuContactEmail;

  @Value("${springfox.docuLicense}")
  private String docuLicense;

  @Value("${springfox.docuLicenseUrl}")
  private String docuLicenseUrl;

  // White list for path patterns that should be accessible without authorization (docs + health).
  public static final String[] WHITE_LIST =
      new String[] {
        "/tenantService/docs",
        "/tenantService/docs/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/webjars/**"
      };

  @Bean
  public GroupedOpenApi tenantApi() {
    return GroupedOpenApi.builder()
        .group("tenantservice")
        .packagesToScan("com.vi.tenantservice.api")
        .build();
  }
}
