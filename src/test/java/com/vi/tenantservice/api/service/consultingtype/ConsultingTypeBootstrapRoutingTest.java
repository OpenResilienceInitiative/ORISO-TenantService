package com.vi.tenantservice.api.service.consultingtype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.ConfigurationFileLoader;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeDTO;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ConsultingTypeBootstrapRoutingTest {
  @Mock private ConsultingTypeServiceApiControllerFactory factory;
  @Mock private ConfigurationFileLoader files;
  @Mock private SecurityHeaderSupplier headers;
  @Mock private TenantResolverService tenants;
  @Mock private ConsultingTypeControllerApi api;
  @Mock private ApiClient client;
  @InjectMocks private ConsultingTypeService service;

  @BeforeEach
  void setup() {
    String path = "src/test/resources/settings/bootstrap-v2-consulting-type.json";
    ReflectionTestUtils.setField(service, "defaultConsultingTypesFilePath", path);
    when(files.loadFrom(path)).thenReturn(Path.of(path).toFile());
    when(headers.getKeycloakAndCsrfHttpHeaders()).thenReturn(new HttpHeaders());
  }

  private void useMockClient() {
    when(factory.createControllerApi()).thenReturn(api);
    when(api.getApiClient()).thenReturn(client);
  }

  @Test
  void proofProtectedBootstrapRoutesWithoutAClaimOnTheMachineAccount() {
    useMockClient();
    when(tenants.tryResolve()).thenReturn(Optional.empty());

    service.createDefaultConsultingTypes(9157L, "synthetic-bootstrap-proof");

    verify(client).addDefaultHeader("tenantId", "0");
    verify(client)
        .addDefaultHeader(
            com.vi.tenantservice.config.security.TenantCreationContext.HEADER,
            "synthetic-bootstrap-proof");
    var body = ArgumentCaptor.forClass(ConsultingTypeDTO.class);
    verify(api).createConsultingType(body.capture());
    assertThat(body.getValue().getTenantId()).isEqualTo(9157);
  }

  @Test
  void ordinaryCreationKeepsTheResolvedTenant() {
    useMockClient();
    when(tenants.tryResolve()).thenReturn(Optional.of(7L));

    service.createDefaultConsultingTypes(9157L);

    verify(client).addDefaultHeader("tenantId", "7");
    verify(client, never()).addDefaultHeader("tenantId", "0");
  }

  @Test
  void anUnscopedCallDoesNotInventATechnicalTenant() {
    useMockClient();
    when(tenants.tryResolve()).thenReturn(Optional.empty());

    service.createDefaultConsultingTypes(9157L);

    verify(client, never()).addDefaultHeader(eq("tenantId"), anyString());
  }

  @Test
  void theActualClientSendsOnlyTechnicalRoutingWhileTheBodyKeepsTheCreatedTenant() {
    var transport = new RestTemplate();
    var server = MockRestServiceServer.bindTo(transport).build();
    var realClient = new ApiClient(transport).setBasePath("http://bootstrap-fixture");
    when(factory.createControllerApi()).thenReturn(new ConsultingTypeControllerApi(realClient));
    when(tenants.tryResolve()).thenReturn(Optional.of(7L));
    server
        .expect(requestTo("http://bootstrap-fixture/consultingtypes"))
        .andExpect(request -> assertThat(request.getHeaders().get("tenantId")).containsExactly("0"))
        .andExpect(content().json("{\"tenantId\":9157}"))
        .andRespond(withSuccess());

    service.createDefaultConsultingTypes(9157L, "synthetic-bootstrap-proof");

    server.verify();
  }
}
