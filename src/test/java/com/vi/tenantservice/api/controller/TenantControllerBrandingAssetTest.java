package com.vi.tenantservice.api.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.api.facade.PlatformDpiaMasterDataFacade;
import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.facade.TranslationFacade;
import com.vi.tenantservice.api.service.BrandingAssetDecoder.DecodedAsset;
import com.vi.tenantservice.api.service.DpaSignedNoticeHintService;
import com.vi.tenantservice.api.service.PublicBrandingAssetService;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.TenantMediaService;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Serves the public branding image through the real MVC dispatch, because the revalidation
 * behaviour under test (ETag / If-None-Match) is produced by Spring's conditional-GET processing of
 * the returned {@code ResponseEntity}, not by controller code alone.
 */
@ExtendWith(MockitoExtension.class)
class TenantControllerBrandingAssetTest {

  @Mock private TenantServiceFacade tenantServiceFacade;
  @Mock private AuthorisationService authorisationService;
  @Mock private TenantDtoMapper tenantDtoMapper;
  @Mock private TenantDpaService tenantDpaService;
  @Mock private TenantDpaFacade tenantDpaFacade;
  @Mock private TranslationFacade translationFacade;
  @Mock private TenantMediaService tenantMediaService;
  @Mock private TenantIdAllocationService tenantIdAllocationService;
  @Mock private PublicBrandingAssetService publicBrandingAssetService;
  @Mock private PlatformDpiaMasterDataFacade platformDpiaMasterDataFacade;
  @Mock private DpaSignedNoticeHintService dpaSignedNoticeHintService;

  @InjectMocks private TenantController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void getPublicBrandingAsset_shouldAnswerNotModifiedWhenTheClientAlreadyHoldsTheImage()
      throws Exception {
    when(publicBrandingAssetService.find("logo"))
        .thenReturn(Optional.of(new DecodedAsset("image/png", "logo-bytes".getBytes(UTF_8))));

    var etag =
        mockMvc
            .perform(get("/tenant/public/branding/logo"))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.ETAG))
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.ETAG);

    mockMvc
        .perform(get("/tenant/public/branding/logo").header(HttpHeaders.IF_NONE_MATCH, etag))
        .andExpect(status().isNotModified());
  }

  @Test
  void getPublicBrandingAsset_shouldKeepAnsweringNotFoundWhenNoImageIsConfigured()
      throws Exception {
    when(publicBrandingAssetService.find("logo")).thenReturn(Optional.empty());

    mockMvc.perform(get("/tenant/public/branding/logo")).andExpect(status().isNotFound());
  }
}
