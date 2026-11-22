package com.vi.tenantservice.api.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = RestTemplateConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
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

  /**
   * The reason this bean exists. SimpleClientHttpRequestFactory is backed by HttpURLConnection,
   * which rejects PATCH outright with {@code ProtocolException: Invalid HTTP method: PATCH} - so
   * the generated clients that do PATCH (e.g. ConsultingTypeService#patchConsultingType) failed
   * before the request ever left the JVM. Asserting the method, body and headers actually arrive at
   * the server is what distinguishes a working factory from one that never sent anything.
   */
  @Test
  void restTemplate_shouldSendPatchWithItsBodyAndHeaders() {
    wireMockServer.stubFor(
        patch(urlEqualTo("/consulting-types/1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":1}")));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Correlation-Id", "patch-regression");

    ResponseEntity<String> response =
        restTemplate.exchange(
            wireMockServer.baseUrl() + "/consulting-types/1",
            HttpMethod.PATCH,
            new HttpEntity<>("{\"languageFormal\":true}", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("{\"id\":1}");
    wireMockServer.verify(
        patchRequestedFor(urlEqualTo("/consulting-types/1"))
            .withHeader("Content-Type", containing(MediaType.APPLICATION_JSON_VALUE))
            .withHeader("X-Correlation-Id", equalTo("patch-regression"))
            .withRequestBody(equalToJson("{\"languageFormal\":true}")));
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
