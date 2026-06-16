package com.vi.tenantservice.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.SmtpConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestrictedPublicTenantJacksonConfig {

  @Bean
  public Module restrictedPublicTenantModule() {
    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(Settings.class, NonNullMixin.class);
    module.setMixInAnnotation(SmtpConfig.class, NonNullMixin.class);
    return module;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private abstract static class NonNullMixin {}
}
