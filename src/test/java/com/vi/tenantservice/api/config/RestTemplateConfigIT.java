package com.vi.tenantservice.api.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = RestTemplateConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportAutoConfiguration(RestTemplateAutoConfiguration.class)
class RestTemplateConfigIT {

  private static WireMockServer wireMockServer;

  @Autowired private RestTemplate restTemplate;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @Test
  void restTemplate_shouldFailWithinReadTimeoutWhenDownstreamIsSlow() {
    wireMockServer.stubFor(
        get(urlEqualTo("/slow"))
            .willReturn(aResponse().withStatus(200).withFixedDelay(15_000).withBody("ok")));

    String slowEndpoint = wireMockServer.baseUrl() + "/slow";
    long startMillis = System.currentTimeMillis();

    assertTimeout(
        Duration.ofSeconds(12),
        () ->
            assertThrows(
                ResourceAccessException.class,
                () -> restTemplate.getForObject(slowEndpoint, String.class)));

    assertThat(System.currentTimeMillis() - startMillis).isLessThan(12_000L);
  }
}
