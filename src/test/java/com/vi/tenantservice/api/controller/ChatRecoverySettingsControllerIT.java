package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class ChatRecoverySettingsControllerIT {
  private static final String URL = "/tenantadmin/controls/chat-recovery";
  private static final String MIXED =
      "{\"asker\":\"RECOVERY_KEY\",\"consultant\":\"LOGIN_PASSWORD\",\"revision\":0}";

  @Autowired WebApplicationContext context;
  @MockitoSpyBean TenantAdminControlsRepository repository;
  @Autowired EntityManagerFactory entityManagerFactory;
  @MockitoBean ApplicationSettingsService applicationSettingsService;
  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;
  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean
  com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi
      consultingTypeControllerApi;

  @MockitoBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean TenantResolverService tenantResolverService;
  @MockitoBean ConsultingTypeService consultingTypeService;
  @MockitoBean UserAdminService userAdminService;
  @MockitoBean SubdomainExtractor subdomainExtractor;
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
        .andExpect(jsonPath("$.asker").value("LOGIN_PASSWORD"))
        .andExpect(jsonPath("$.revision").value(0));
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revision").value(1));
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asker").value("RECOVERY_KEY"))
        .andExpect(jsonPath("$.consultant").value("LOGIN_PASSWORD"))
        .andExpect(jsonPath("$.revision").value(1));
    assertThat(repository.findAll()).hasSize(1);
  }

  @Test
  void publicTenantLookupExposesCreationDefaultsWithoutAdminKeys() throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk());
    mvc.perform(get("/tenantadmin/controls").with(admin(0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionsPageEnabled").exists())
        .andExpect(jsonPath("$.permissionPolicies").isMap())
        .andExpect(jsonPath("$.caseHandoverPolicies").exists());
    var stored = repository.findTopByOrderByIdAsc().orElseThrow();
    var mapper = new ObjectMapper();
    var storedControls =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(stored.getControls());
    assertThat(storedControls.get("permissionPolicies").size()).isPositive();
    assertThat(storedControls.get("caseHandoverPolicies").get("reasons").size()).isPositive();
    String encryptedKey = "ENC:synthetic-translation-key-public-leak-canary";
    storedControls.putObject("translationApiKeys").put("provider", encryptedKey);
    stored.setControls(mapper.writeValueAsString(storedControls));
    repository.saveAndFlush(stored);
    var response =
        mvc.perform(get("/tenant/public/id/1"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.settings.tenantAdminControls.chatRecoverySettings.asker")
                    .value("RECOVERY_KEY"))
            .andExpect(
                jsonPath("$.settings.tenantAdminControls.chatRecoverySettings.revision").value(1))
            .andExpect(jsonPath("$.settings.tenantAdminControls.translationApiKeys").doesNotExist())
            .andExpect(jsonPath("$.settings.tenantAdminControls.permissionPolicies").doesNotExist())
            .andExpect(
                jsonPath("$.settings.tenantAdminControls.caseHandoverPolicies").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).doesNotContain(encryptedKey);
    var publicControls =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(response).at("/settings/tenantAdminControls");
    // Generated DTOs may serialize absent values as null; only recovery data may be present.
    publicControls.properties().removeIf(entry -> entry.getValue().isNull());
    assertThat(publicControls)
        .isEqualTo(
            mapper.readTree(
                "{\"chatRecoverySettings\":{\"asker\":\"RECOVERY_KEY\",\"consultant\":\"LOGIN_PASSWORD\",\"revision\":1}}"));
  }

  @Test
  void tenantAdministratorCannotReadOrWriteEvenWithForgedPlatformAuthority() throws Exception {
    mvc.perform(get(URL).with(admin(5))).andExpect(status().isForbidden());
    mvc.perform(put(URL).with(admin(5)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isForbidden());
    assertThat(repository.count()).isZero();
  }

  @Test
  void missingAuthenticationCannotReadOrWrite() throws Exception {
    mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    mvc.perform(put(URL).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingPlatformAuthorityIsRejected() throws Exception {
    mvc.perform(put(URL).with(jwt()).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"asker\":null,\"consultant\":\"LOGIN_PASSWORD\",\"revision\":0}",
        "{\"asker\":\"DISABLED\",\"consultant\":\"LOGIN_PASSWORD\",\"revision\":0}",
        "{\"asker\":\"RECOVERY_KEY\",\"consultant\":\"LOGIN_PASSWORD\",\"revision\":-1}"
      })
  void invalidPayloadIsRejectedWithoutMutation(String body) throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    assertThat(repository.count()).isZero();
  }

  @Test
  void staleRevisionReturnsConflictAndRetainsStoredChoices() throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk());
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isConflict());
    mvc.perform(get(URL).with(admin(0))).andExpect(jsonPath("$.revision").value(1));
  }

  @Test
  void fullControlsUpdatePreservesRecoveryDefaultsInDatabase() throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk());
    mvc.perform(
            put("/tenantadmin/controls")
                .with(admin(0))
                .contentType(APPLICATION_JSON)
                .content("{\"permissionsPageEnabled\":false}"))
        .andExpect(status().isOk());
    mvc.perform(get(URL).with(admin(0)))
        .andExpect(jsonPath("$.asker").value("RECOVERY_KEY"))
        .andExpect(jsonPath("$.revision").value(1));
  }

  @Test
  void concurrentFirstWritesCannotCreateTwoPlatformSettingsRows() throws Exception {
    var barrier = new java.util.concurrent.CyclicBarrier(2);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              Object result =
                  repository.findAll().stream()
                      .min(java.util.Comparator.comparing(TenantAdminControlsEntity::getId));
              barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
              return result;
            })
        .when(repository)
        .findTopByOrderByIdAsc();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> write =
          () ->
              mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var first = executor.submit(write);
      var second = executor.submit(write);
      assertThat(
              List.of(
                  first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(20, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
      assertThat(repository.count()).isEqualTo(1);
    }
  }

  @Test
  void databaseVersionRejectsConcurrentControlsWrites() throws Exception {
    mvc.perform(put(URL).with(admin(0)).contentType(APPLICATION_JSON).content(MIXED))
        .andExpect(status().isOk());
    Long id = repository.findTopByOrderByIdAsc().orElseThrow().getId();
    var first = entityManagerFactory.createEntityManager();
    var second = entityManagerFactory.createEntityManager();
    try {
      first.getTransaction().begin();
      second.getTransaction().begin();
      var a = first.find(TenantAdminControlsEntity.class, id);
      var b = second.find(TenantAdminControlsEntity.class, id);
      a.setControls("{\"permissionsPageEnabled\":true}");
      first.getTransaction().commit();
      b.setControls("{\"permissionsPageEnabled\":false}");
      assertThatThrownBy(() -> second.getTransaction().commit())
          .hasRootCauseInstanceOf(org.hibernate.StaleStateException.class);
    } finally {
      if (first.getTransaction().isActive()) first.getTransaction().rollback();
      if (second.getTransaction().isActive()) second.getTransaction().rollback();
      first.close();
      second.close();
    }
  }
}
