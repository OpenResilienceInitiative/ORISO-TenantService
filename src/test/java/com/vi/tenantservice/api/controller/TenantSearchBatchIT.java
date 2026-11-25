package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.config.security.JwtAuthConverter;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = TenantServiceApplication.class,
    properties = {
      "keycloak.config.app-client-id=directory-test-app",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.properties.hibernate.session.metrics.log=false"
    })
@ActiveProfiles("testing")
@Sql("/database/TenantServiceDatabase.sql")
@Transactional
class TenantSearchBatchIT {
  private static final String SEARCH = "/tenantadmin/search";
  private static final String OWNERS = "/useradmin/tenantadmins";
  private static final String BATCH = OWNERS + "/login-emails";
  private static final String APP = "directory-test-app";
  private static final String LEGACY_APP = "other-fixture-app";
  private static final String TOKEN = "directory-fixture-token";
  private static final String CROSS_FIELD_QUERY = "MÜLLER 50%_! ALPHA BETA";
  private static final String[] PLATFORM_ROLES = {"tenant-admin", "agency-admin", "user-admin"};

  private static OwnerServer owners;

  @Autowired private WebApplicationContext context;
  @Autowired private JwtAuthConverter jwtAuthConverter;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @MockitoBean private ApplicationSettingsService applicationSettingsService;

  @MockitoBean
  private ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

  @MockitoBean
  private ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean
  private com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  @MockitoBean private ConsultingTypeService consultingTypeService;

  private MockMvc mvc;

  @DynamicPropertySource
  static void configureOwnerServer(DynamicPropertyRegistry registry) throws IOException {
    owners = new OwnerServer();
    registry.add("user.service.api.url", owners::url);
  }

  @AfterAll
  static void stopOwnerServer() {
    if (owners != null) {
      owners.server.stop(0);
    }
  }

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    owners.reset();
  }

  @Test
  void twelveCandidatePlatformSearchUsesOneBatch() throws Exception {
    comparePlatformSearches(12, 1);
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 1000})
  void largePlatformSearchesUseBoundedBatches(int candidates) throws Exception {
    comparePlatformSearches(candidates, candidates / 100);
  }

  @Test
  void platformSearchIncludesThePartialBatchAfterAFullBatch() throws Exception {
    comparePlatformSearches(101, 2);

    assertThat(owners.requests).extracting(request -> request.ids().size()).containsExactly(100, 1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "*", " \u2003 "})
  void blankPlatformSearchBatchesOnlyTheRequestedPage(String query) throws Exception {
    seedTenants(12);

    search(query, 0L, APP, 2)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(12))
        .andExpect(jsonPath("$._embedded.length()").value(2))
        .andExpect(jsonPath("$._embedded[0].id").value(2))
        .andExpect(jsonPath("$._embedded[0].adminEmails[0]").value("alpha-2@example.test"))
        .andExpect(jsonPath("$._embedded[1].id").value(1))
        .andExpect(jsonPath("$._embedded[1].adminEmails[0]").value("alpha-1@example.test"));

    assertThat(owners.requests)
        .containsExactly(new OwnerRequest(BATCH, List.of(2L, 1L), "Bearer " + TOKEN));
  }

  @Test
  void fullPlatformDirectoryUsesBoundedBatchesAndKeepsTheResponse() throws Exception {
    seedTenants(101);
    owners.tenantWithoutOwners = 101L;

    var legacy =
        fullDirectory(LEGACY_APP)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andExpect(jsonPath("$[?(@.id == 101)].adminEmails[0]").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(owners.requests).hasSize(101).extracting(OwnerRequest::path).containsOnly(OWNERS);
    owners.requests.clear();

    var batched =
        fullDirectory(APP)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(batched).isEqualTo(legacy);
    assertThat(owners.requests).hasSize(2).extracting(OwnerRequest::path).containsOnly(BATCH);
    assertThat(owners.requests).extracting(request -> request.ids().size()).containsExactly(100, 1);
    assertThat(
            owners.requests.stream().flatMap(request -> request.ids().stream()).sorted().toList())
        .containsExactlyElementsOf(
            IntStream.rangeClosed(1, 101).mapToLong(id -> id).boxed().toList());
  }

  @Test
  void fullPlatformDirectoryFallsBackToLegacyEnrichmentWhenBatchFails() throws Exception {
    seedTenants(2);
    owners.unavailableOwner = 2L;
    var legacy =
        fullDirectory(LEGACY_APP)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    owners.requests.clear();
    owners.batchStatus = 503;

    var fallback =
        fullDirectory(APP)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(fallback).isEqualTo(legacy);
    assertThat(owners.requests)
        .extracting(OwnerRequest::path)
        .containsExactly(BATCH, OWNERS, OWNERS);
  }

  @Test
  void fullPlatformDirectoryWithoutVisibleTenantsSkipsEnrichment() throws Exception {
    seedTenants(0);

    fullDirectory(APP).andExpect(status().isNoContent());

    assertThat(owners.requests).isEmpty();
  }

  @Test
  void blankPageWithoutVisibleRowsDoesNotResolveOwnerEmails() throws Exception {
    seedTenants(12);

    search("*", 0L, APP, 3).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(12));

    assertThat(owners.requests).isEmpty();
  }

  @Test
  void blankScopedSearchKeepsItsSingleOwnerLookup() throws Exception {
    seedTenants(12);

    search("*", 2L, APP, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$._embedded[0].id").value(2));

    assertThat(owners.requests)
        .containsExactly(new OwnerRequest(OWNERS, List.of(2L), "Bearer " + TOKEN));
  }

  @Test
  void blankPlatformSearchFallsBackToPerRowDisplayWhenBatchFails() throws Exception {
    seedTenants(12);
    owners.batchStatus = 503;
    owners.unavailableOwner = 2L;

    search("*", 0L, APP, 2)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(12))
        .andExpect(jsonPath("$._embedded[0].id").value(2))
        .andExpect(jsonPath("$._embedded[0].adminEmails").isEmpty())
        .andExpect(jsonPath("$._embedded[1].id").value(1))
        .andExpect(jsonPath("$._embedded[1].adminEmails[0]").value("alpha-1@example.test"));

    assertThat(owners.requests)
        .extracting(OwnerRequest::path)
        .containsExactly(BATCH, OWNERS, OWNERS);
    assertThat(owners.requests)
        .extracting(OwnerRequest::ids)
        .containsExactly(List.of(2L, 1L), List.of(2L), List.of(1L));
  }

  @Test
  void organisationAdminWithAppTokenKeepsTheirSingleOwnerLookup() throws Exception {
    seedTenants(12);

    search(CROSS_FIELD_QUERY, 2L, APP, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$._embedded.length()").value(1))
        .andExpect(jsonPath("$._embedded[0].id").value(2));

    assertThat(owners.requests)
        .containsExactly(new OwnerRequest(OWNERS, List.of(2L), "Bearer " + TOKEN));
  }

  @Test
  void platformTokenWithoutAgencyAdminRoleUsesTheExistingLookup() throws Exception {
    seedTenants(1);

    search(CROSS_FIELD_QUERY, 0L, APP, 1, "tenant-admin", "user-admin")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));

    assertThat(owners.requests).extracting(OwnerRequest::path).containsExactly(OWNERS);
  }

  @Test
  void aTenantWithNoOwnersStillMatchesItsLiteralName() throws Exception {
    seedTenants(2);
    jdbc.update("UPDATE tenant SET name = 'Muller 50other!' WHERE id = 2");
    owners.batchBody = "[{\"tenantId\":1,\"emails\":[]},{\"tenantId\":2,\"emails\":[]}]";

    search("MULLER 50%_!", 0L, APP, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$._embedded[0].adminEmails").isEmpty());

    assertThat(owners.requests).extracting(OwnerRequest::path).containsExactly(BATCH);
  }

  @Test
  void emptyCandidateSetSkipsOwnerLookup() throws Exception {
    seedTenants(0);

    search(CROSS_FIELD_QUERY, 0L, APP, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));

    assertThat(owners.requests).isEmpty();
  }

  @Test
  void batchFailureRetainsTheUpstreamStatusAndSkipsSingleLookups() throws Exception {
    seedTenants(12);
    owners.batchStatus = 503;

    search(CROSS_FIELD_QUERY, 0L, APP, 1).andExpect(status().isServiceUnavailable());

    assertThat(owners.requests).extracting(OwnerRequest::path).containsExactly(BATCH);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[]",
        "[{\"tenantId\":2,\"emails\":[]}]",
        "[{\"tenantId\":1}]",
        "[{\"tenantId\":1,\"emails\":null}]",
        "[{\"tenantId\":1,\"emails\":[null]}]"
      })
  void invalidBatchPayloadFailsRatherThanReturningIncompleteResults(String body) throws Exception {
    seedTenants(1);
    owners.batchBody = body;

    search(CROSS_FIELD_QUERY, 0L, APP, 1).andExpect(status().isBadGateway());

    assertThat(owners.requests).extracting(OwnerRequest::path).containsExactly(BATCH);
  }

  private void comparePlatformSearches(int candidates, int expectedBatches) throws Exception {
    seedTenants(candidates);

    search(CROSS_FIELD_QUERY, 0L, LEGACY_APP, 2).andExpect(status().isOk());
    owners.requests.clear();
    var legacy = measure(candidates, LEGACY_APP);
    assertThat(owners.requests)
        .hasSize(candidates)
        .extracting(OwnerRequest::path)
        .containsOnly(OWNERS);
    owners.requests.clear();

    search(CROSS_FIELD_QUERY, 0L, APP, 2).andExpect(status().isOk());
    owners.requests.clear();
    var batch = measure(candidates, APP);

    assertThat(owners.requests)
        .hasSize(expectedBatches)
        .extracting(OwnerRequest::path)
        .containsOnly(BATCH);
    assertThat(owners.requests)
        .allSatisfy(
            request -> {
              assertThat(request.ids()).hasSizeBetween(1, 100);
              assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
            });
    assertThat(
            owners.requests.stream().flatMap(request -> request.ids().stream()).sorted().toList())
        .containsExactlyElementsOf(
            IntStream.rangeClosed(1, candidates).mapToLong(id -> id).boxed().toList());
    assertThat(batch.body()).isEqualTo(legacy.body());
    assertThat(batch.queries()).isEqualTo(legacy.queries());
    System.out.printf(
        "TENANT_SEARCH_HTTP candidates=%d pageSize=10 singleCalls=%d batchCalls=%d singleTenantQueries=%d batchTenantQueries=%d singleMs=%d batchMs=%d%n",
        candidates,
        candidates,
        expectedBatches,
        legacy.queries(),
        batch.queries(),
        legacy.elapsedMs(),
        batch.elapsedMs());
  }

  private Measurement measure(int candidates, String client) throws Exception {
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    long queriesBefore = statistics.getQueryExecutionCount();
    long start = System.nanoTime();
    var result =
        search(CROSS_FIELD_QUERY, 0L, client, 2)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(candidates))
            .andExpect(jsonPath("$._embedded.length()").value(Math.min(10, candidates - 10)))
            .andExpect(jsonPath("$._embedded[0].id").value(candidates - 10))
            .andExpect(
                jsonPath("$._embedded[0].adminEmails[0]")
                    .value("alpha-" + (candidates - 10) + "@example.test"))
            .andReturn();
    return new Measurement(
        result.getResponse().getContentAsString(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
        statistics.getQueryExecutionCount() - queriesBefore);
  }

  private void seedTenants(int count) {
    jdbc.update("DELETE FROM tenant");
    jdbc.batchUpdate(
        "INSERT INTO tenant (id,name,subdomain,create_date,update_date,active) VALUES (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,TRUE)",
        IntStream.rangeClosed(0, count)
            .mapToObj(id -> new Object[] {id, "Muller 50%_!", "fixture-" + id})
            .toList());
  }

  private ResultActions search(String query, long tenant, String client, int page)
      throws Exception {
    return search(query, tenant, client, page, PLATFORM_ROLES);
  }

  private ResultActions search(String query, long tenant, String client, int page, String... roles)
      throws Exception {
    return mvc.perform(
        get(SEARCH)
            .param("query", query)
            .param("page", String.valueOf(page))
            .param("perPage", "10")
            .param("field", "ID")
            .param("order", "DESC")
            .with(caller(tenant, client, roles)));
  }

  private ResultActions fullDirectory(String client) throws Exception {
    return mvc.perform(get("/tenantadmin").with(caller(0L, client, PLATFORM_ROLES)));
  }

  private RequestPostProcessor caller(long tenant, String client, String[] roles) {
    return jwt()
        .jwt(
            token ->
                token
                    .tokenValue(TOKEN)
                    .subject("directory-fixture")
                    .claim("tenantId", String.valueOf(tenant))
                    .claim("azp", client)
                    .claim("realm_access", Map.of("roles", List.of(roles))))
        .authorities(token -> jwtAuthConverter.convert(token).getAuthorities());
  }

  private record Measurement(String body, long elapsedMs, long queries) {}

  private record OwnerRequest(String path, List<Long> ids, String authorization) {}

  private static final class OwnerServer {
    private final HttpServer server;
    private final ConcurrentLinkedQueue<OwnerRequest> requests = new ConcurrentLinkedQueue<>();
    private volatile int batchStatus = 200;
    private volatile String batchBody;
    private volatile Long unavailableOwner;
    private volatile Long tenantWithoutOwners;

    private OwnerServer() throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(OWNERS, this::respond);
      server.start();
    }

    private String url() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void reset() {
      requests.clear();
      batchStatus = 200;
      batchBody = null;
      unavailableOwner = null;
      tenantWithoutOwners = null;
    }

    private void respond(HttpExchange exchange) throws IOException {
      try (exchange) {
        var uri = exchange.getRequestURI();
        boolean batch = BATCH.equals(uri.getPath());
        var ids =
            Arrays.stream(uri.getRawQuery().split("&"))
                .map(
                    parameter ->
                        URLDecoder.decode(parameter.split("=", 2)[1], StandardCharsets.UTF_8))
                .map(Long::valueOf)
                .toList();
        requests.add(
            new OwnerRequest(
                uri.getPath(), ids, exchange.getRequestHeaders().getFirst("Authorization")));
        var body = batch ? batchEmails(ids) : singleEmails(ids.getFirst());
        if (batch && batchBody != null) {
          body = batchBody;
        }
        byte[] json = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Connection", "close");
        int responseStatus = batch ? batchStatus : 200;
        if (!batch && ids.getFirst().equals(unavailableOwner)) {
          responseStatus = 503;
        }
        exchange.sendResponseHeaders(responseStatus, json.length);
        exchange.getResponseBody().write(json);
      }
    }

    private String batchEmails(List<Long> ids) {
      return "["
          + String.join(
              ",",
              ids.stream()
                  .map(
                      id ->
                          id.equals(tenantWithoutOwners)
                              ? "{\"tenantId\":%d,\"emails\":[]}".formatted(id)
                              : "{\"tenantId\":%d,\"emails\":[\"alpha-%d@example.test\",\"beta-%d@example.test\"]}"
                                  .formatted(id, id, id))
                  .toList())
          + "]";
    }

    private String singleEmails(long id) {
      if (Long.valueOf(id).equals(tenantWithoutOwners)) {
        return "[]";
      }
      return "[{\"_embedded\":{\"email\":\"alpha-%d@example.test\"}},{\"_embedded\":{\"email\":\"beta-%d@example.test\"}}]"
          .formatted(id, id);
    }
  }
}
