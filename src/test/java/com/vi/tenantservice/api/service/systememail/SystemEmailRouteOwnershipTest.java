package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.vi.tenantservice.api.controller.TenantController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SystemEmailRouteOwnershipTest {
  @Test
  void separateProviderContractMatchesTheManualRequest() throws Exception {
    java.util.Map<String, Object> api;
    try (var input =
        java.nio.file.Files.newInputStream(
            java.nio.file.Path.of("api/internal-system-email.yaml"))) {
      api = new org.yaml.snakeyaml.Yaml().load(input);
    }
    assertThat(api.get("openapi")).isEqualTo("3.0.1");
    var paths = (java.util.Map<?, ?>) api.get("paths");
    assertThat(paths).hasSize(1);
    assertThat(paths.containsKey("/tenant/{tenantId}/internal/system-email-deliveries")).isTrue();
    var components = (java.util.Map<?, ?>) api.get("components");
    var schemas = (java.util.Map<?, ?>) components.get("schemas");
    var schema = (java.util.Map<?, ?>) schemas.get("InternalSystemEmailDeliveryDTO");
    assertThat(schema.get("additionalProperties")).isEqualTo(false);
    assertThat((java.util.List<String>) schema.get("required"))
        .containsExactlyInAnyOrder(
            java.util.Arrays.stream(SystemEmailDeliveryRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toArray(String[]::new));
    assertThat(
            java.util.Arrays.stream(
                    com.vi.tenantservice.generated.api.controller.TenantApi.class.getMethods())
                .map(java.lang.reflect.Method::getName))
        .doesNotContain("deliverTenantSystemEmail");
  }

  @Test
  void existingTenantControllerAndDeliveryControllerCanRegisterTogether() {
    var mvc =
        MockMvcBuilders.standaloneSetup(
                mock(TenantController.class),
                new SystemEmailDeliveryController(mock(SystemEmailDeliveryService.class)))
            .build();
    var mappings =
        mvc.getDispatcherServlet()
            .getWebApplicationContext()
            .getBean(
                org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
                    .class);
    var handlers =
        mappings.getHandlerMethods().entrySet().stream()
            .filter(
                entry ->
                    entry
                        .getKey()
                        .getPatternValues()
                        .contains("/tenant/{tenantId}/internal/system-email-deliveries"))
            .toList();
    assertThat(handlers).hasSize(1);
    assertThat(handlers.getFirst().getValue().getBeanType())
        .isEqualTo(SystemEmailDeliveryController.class);
  }
}
