package com.vi.tenantservice.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureMockMvc(addFilters = false)
class TenantMediaControllerIT {

  @Autowired private WebApplicationContext context;

  @MockitoBean AuthorisationService authorisationService;

  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean
  com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static byte[] pngBytes() {
    var content = new byte[32];
    byte[] magic = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, content, 0, magic.length);
    return content;
  }

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
  }

  private static org.springframework.security.core.Authentication tenantAdmin() {
    return new AuthenticationMockBuilder().withUserRole("tenant-admin").build();
  }

  @Test
  void uploadAndServe_should_roundTripWithImmutableCacheHeaders() throws Exception {
    var file = new MockMultipartFile("file", "logo.png", "application/octet-stream", pngBytes());

    var uploadResult =
        mockMvc
            .perform(multipart("/tenantadmin/media").file(file).with(authentication(tenantAdmin())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("id", notNullValue()))
            .andExpect(jsonPath("contentType", is("image/png")))
            .andExpect(jsonPath("url", containsString("/media/")))
            .andReturn();

    var id =
        objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(get("/media/" + id))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"))
        .andExpect(header().string("Cache-Control", containsString("public")))
        .andExpect(header().string("Cache-Control", containsString("immutable")));
  }

  @Test
  void getTenantMedia_should_return404_forUnknownId() throws Exception {
    mockMvc.perform(get("/media/does-not-exist")).andExpect(status().isNotFound());
  }

  @Test
  void uploadTenantMedia_should_rejectNonImageContent() throws Exception {
    var file =
        new MockMultipartFile(
            "file", "evil.png", "image/png", "<svg onload=alert(1)></svg>".getBytes());

    mockMvc
        .perform(multipart("/tenantadmin/media").file(file).with(authentication(tenantAdmin())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploadTenantMedia_should_rejectMissingAuthority() throws Exception {
    var file = new MockMultipartFile("file", "logo.png", "application/octet-stream", pngBytes());

    mockMvc
        .perform(
            multipart("/tenantadmin/media")
                .file(file)
                .with(user("nobody").authorities(() -> "no-permissions")))
        .andExpect(status().isForbidden());
  }
}
