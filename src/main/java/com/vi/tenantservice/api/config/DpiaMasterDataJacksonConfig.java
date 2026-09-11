package com.vi.tenantservice.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vi.tenantservice.api.model.DpiaBrandingDTO;
import com.vi.tenantservice.api.model.DpiaDocumentMetadataDTO;
import com.vi.tenantservice.api.model.DpiaKeyFigureDTO;
import com.vi.tenantservice.api.model.DpiaKeyFiguresDTO;
import com.vi.tenantservice.api.model.DpiaOperatorDTO;
import com.vi.tenantservice.api.model.DpiaSupervisoryAuthorityDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.PublicDpiaMasterDataDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serializes the DPIA master data DTOs without null members, mirroring {@link
 * RestrictedPublicTenantJacksonConfig}: unmaintained fields disappear from the public payload
 * instead of being served as explicit nulls.
 */
@Configuration
public class DpiaMasterDataJacksonConfig {

  @Bean
  public Module dpiaMasterDataModule() {
    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(PlatformDpiaMasterDataDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(PublicDpiaMasterDataDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(DpiaOperatorDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(DpiaSupervisoryAuthorityDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(DpiaDocumentMetadataDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(DpiaKeyFiguresDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(DpiaKeyFigureDTO.class, NonNullMixin.class);
    module.setMixInAnnotation(DpiaBrandingDTO.class, NonNullMixin.class);
    return module;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private abstract static class NonNullMixin {}
}
