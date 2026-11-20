package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class AccountInactivitySettingsControllerIT {
  private static final String URL = "/tenantadmin/controls/account-inactivity";
  private static final String MIXED =
      "{\"askerMonths\":12,\"consultantMonths\":36,\"otherMonths\":48,\"revision\":0}";

  @Autowired WebApplicationContext context;
  @Autowired TenantAdminControlsRepository repository;
  @Autowired javax.sql.DataSource dataSource;
  @MockitoBean ApplicationSettingsService applicationSettingsService;
  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;
  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean
  com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  @MockitoBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean ConsultingTypeService consultingTypeService;
  @MockitoBean UserAdminService userAdminService;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    repository.deleteAll();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(consultingTypeServiceApiControllerFactory.createControllerApi())
        .thenReturn(consultingTypeControllerApi);
    when(consultingTypeControllerApi.getApiClient())
        .thenReturn(mock(com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
    when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    when(securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders())
        .thenReturn(mock(HttpHeaders.class));
  }

  private RequestPostProcessor admin(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS"));
  }

  @Test
  void platformWritePersistsAndFreshReadReturnsRevision() throws Exception {
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.askerMonths").value(24))
        .andExpect(jsonPath("$.consultantMonths").value(24))
        .andExpect(jsonPath("$.otherMonths").value(24))
        .andExpect(jsonPath("$.revision").value(0));
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revision").value(1));
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.askerMonths").value(12))
        .andExpect(jsonPath("$.consultantMonths").value(36))
        .andExpect(jsonPath("$.otherMonths").value(48))
        .andExpect(jsonPath("$.revision").value(1));
  }

  @Test
  void staleSaveIsRejectedAndCurrentValuesRemainReadable() throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk());
    mvc.perform(
            put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED.replace("12", "6")))
        .andExpect(status().isConflict());
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(jsonPath("$.askerMonths").value(12))
        .andExpect(jsonPath("$.revision").value(1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "1.5", "24.0", "\"12\"", "null", "2147483648"})
  void invalidMonthsAreRejectedWithoutChangingDefaults(String invalid) throws Exception {
    for (String field : List.of("askerMonths", "consultantMonths", "otherMonths")) {
      var body = new ObjectMapper().readTree(MIXED);
      ((com.fasterxml.jackson.databind.node.ObjectNode) body)
          .set(field, new ObjectMapper().readTree(invalid));
      mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(body.toString()))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(jsonPath("$.revision").value(0))
        .andExpect(jsonPath("$.askerMonths").value(24));
  }

  @Test
  void requiredFieldsAndRevisionCannotBeMissingOrInvalid() throws Exception {
    for (String field : List.of("askerMonths", "consultantMonths", "otherMonths", "revision")) {
      var body =
          (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree(MIXED);
      body.remove(field);
      mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(body.toString()))
          .andExpect(status().isBadRequest());
    }
    for (String value : List.of("-1", "0.5", "\"0\"", "null")) {
      mvc.perform(
              put(URL)
                  .with(admin(0))
                  .contentType(APPLICATION_JSON)
                  .content(MIXED.replace("\"revision\":0", "\"revision\":" + value)))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get(URL).with(admin(0))).andExpect(jsonPath("$.revision").value(0));
  }

  @Test
  void creationDefaultsSurviveGeneralControlsEditsAndArePublishedSafely() throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk());
    mvc.perform(
            put("/tenantadmin/controls")
                .with(admin(0))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionsPageEnabled\":false,\"accountInactivitySettings\":{\"askerMonths\":1,\"consultantMonths\":1,\"otherMonths\":1,\"revision\":99}}"))
        .andExpect(status().isOk());
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(jsonPath("$.askerMonths").value(12))
        .andExpect(jsonPath("$.revision").value(1));
    for (int tenantId : List.of(0, 1)) {
      var response =
          mvc.perform(get("/tenant/public/id/" + tenantId))
              .andExpect(status().isOk())
              .andExpect(
                  jsonPath("$.settings.tenantAdminControls.accountInactivitySettings.askerMonths")
                      .value(12))
              .andExpect(
                  jsonPath(
                          "$.settings.tenantAdminControls.accountInactivitySettings.consultantMonths")
                      .value(36))
              .andExpect(
                  jsonPath("$.settings.tenantAdminControls.accountInactivitySettings.otherMonths")
                      .value(48))
              .andExpect(
                  jsonPath("$.settings.tenantAdminControls.accountInactivitySettings.revision")
                      .value(1))
              .andExpect(
                  jsonPath("$.settings.tenantAdminControls.permissionPolicies").doesNotExist())
              .andExpect(
                  jsonPath("$.settings.tenantAdminControls.caseHandoverPolicies").doesNotExist())
              .andExpect(
                  jsonPath("$.settings.tenantAdminControls.translationApiKeys").doesNotExist())
              .andReturn()
              .getResponse()
              .getContentAsString();
      var controls =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              new ObjectMapper().readTree(response).at("/settings/tenantAdminControls");
      controls.properties().removeIf(entry -> entry.getValue().isNull());
      assertThat(controls.size()).isEqualTo(2);
      assertThat(controls.has("chatRecoverySettings")).isTrue();
    }
  }

  @Test
  void onlyPlatformAdministratorCanReadOrWrite() throws Exception {
    var authority = new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS");
    for (RequestPostProcessor rejected :
        List.of(
            admin(5),
            jwt(),
            jwt().authorities(authority),
            jwt().jwt(token -> token.claim("tenantId", 0)).authorities(authority))) {
      mvc.perform(get(URL).with(rejected)).andExpect(status().isForbidden());
      mvc.perform(put(URL).with(rejected).contentType(APPLICATION_JSON).content(MIXED))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    mvc.perform(put(URL).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(URL).with(admin(0))).andExpect(jsonPath("$.revision").value(0));
  }

  @Test
  void concurrentFirstSavesHaveOneWinnerAndOneConflict() throws Exception {
    // The real database pauses both inserts before either row becomes visible.
    // A start latch alone can pass through two sequential reads and a stale revision.
    ControlsInsertBarrier.barrier = new java.util.concurrent.CyclicBarrier(2);
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER inactivity_first_insert BEFORE INSERT ON tenant_admin_controls FOR EACH ROW CALL 'com.vi.tenantservice.api.controller.ControlsInsertBarrier'");
    }
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> save =
          () -> {
            return mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = executor.submit(save);
      var second = executor.submit(save);
      assertThat(
              List.of(
                  first.get(30, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(30, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    } finally {
      try (var connection = dataSource.getConnection();
          var statement = connection.createStatement()) {
        statement.execute("DROP TRIGGER IF EXISTS inactivity_first_insert");
      }
      ControlsInsertBarrier.barrier = null;
    }
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(jsonPath("$.revision").value(1))
        .andExpect(jsonPath("$.askerMonths").value(12));
  }

  @Test
  void firstGeneralControlsSaveCannotInjectInactivityDefaults() throws Exception {
    mvc.perform(
            put("/tenantadmin/controls")
                .with(admin(0))
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"permissionsPageEnabled\":false,\"accountInactivitySettings\":{\"askerMonths\":1,\"consultantMonths\":2,\"otherMonths\":3,\"revision\":99}}"))
        .andExpect(status().isOk());
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.askerMonths").value(24))
        .andExpect(jsonPath("$.consultantMonths").value(24))
        .andExpect(jsonPath("$.otherMonths").value(24))
        .andExpect(jsonPath("$.revision").value(0));
  }
}
