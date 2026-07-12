package com.vi.tenantservice.api.controller;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Regression test for OBS-P2 (SigNoz OTLP tracing): TenantService's pom.xml was missing the
 * Spring Boot 4 modular tracing autoconfiguration (spring-boot-micrometer-tracing /
 * spring-boot-micrometer-tracing-opentelemetry), so no io.micrometer.tracing.Tracer bean was ever
 * created and every HTTP request produced zero spans - confirmed live on Pre-Dev via
 * /actuator/beans, which showed only the metrics-side ObservationHandlerGroup and no
 * Tracer/OpenTelemetry beans at all, unlike AgencyService/ConsultingTypeService.
 *
 * <p>This test runs the full filter chain (Spring Security + Spring MVC's ObservationFilter)
 * against a real request and asserts that an actual OpenTelemetry span was finished and handed to
 * an exporter - not just that the relevant beans exist. It wires a real {@link
 * InMemorySpanExporter} alongside the app's normal (OTLP) exporter chain via {@link
 * org.springframework.beans.factory.ObjectProvider}-based aggregation in {@code SpanExporters}, so
 * it exercises the production auto-configuration instead of a hand-rolled tracer.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "management.tracing.sampling.probability=1.0"
    })
@AutoConfigureMockMvc
@Import(TracingSmokeIT.InMemorySpanExporterConfig.class)
class TracingSmokeIT {

  @Autowired private WebApplicationContext context;
  @Autowired private InMemorySpanExporter inMemorySpanExporter;
  @Autowired private SdkTracerProvider sdkTracerProvider;

  private MockMvc mockMvc;

  @TestConfiguration
  static class InMemorySpanExporterConfig {

    @Bean
    InMemorySpanExporter inMemorySpanExporter() {
      return InMemorySpanExporter.create();
    }

    @Bean
    SpanExporter inMemoryTestSpanExporter(InMemorySpanExporter inMemorySpanExporter) {
      return inMemorySpanExporter;
    }
  }

  @BeforeEach
  void setup() {
    inMemorySpanExporter.reset();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void tearDown() {
    inMemorySpanExporter.reset();
  }

  @Test
  void actuatorHealthRequest_Should_produceARecordedSpan() throws Exception {
    mockMvc
        .perform(get("/actuator/health").contentType(APPLICATION_JSON))
        .andExpect(status().isOk());

    sdkTracerProvider.forceFlush().join(5, TimeUnit.SECONDS);

    assertThat(inMemorySpanExporter.getFinishedSpanItems())
        .as(
            "no span was ever exported for a real /actuator/health request - this is exactly the"
                + " OBS-P2 zero-trace bug (missing Tracer bean) if it regresses")
        .isNotEmpty();
  }
}
