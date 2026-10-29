package com.vi.tenantservice.api.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.api.facade.TenantServiceFacade;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TenantSearchHttpBindingTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "%",
        "%25",
        "50%_!",
        "owner+tag@example.test",
        "Max owner+tag@example.test",
        "plain@example.test"
      })
  void boundSearchPreservesLiteralText(String text) throws Exception {
    var facade = mock(TenantServiceFacade.class);
    var mapper = mock(TenantDtoMapper.class);
    when(mapper.mappedFieldOf(anyString())).thenReturn("name");
    var controller = mock(TenantController.class, CALLS_REAL_METHODS);
    ReflectionTestUtils.setField(controller, "tenantServiceFacade", facade);
    ReflectionTestUtils.setField(controller, "tenantDtoMapper", mapper);
    MockMvcBuilders.standaloneSetup(controller)
        .build()
        .perform(
            get(
                URI.create(
                    "/tenantadmin/search?query="
                        + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20"))))
        .andExpect(status().isOk());
    verify(facade).findTenantsExceptTechnicalByInfix(text, 0, 10, "name", true);
  }
}
