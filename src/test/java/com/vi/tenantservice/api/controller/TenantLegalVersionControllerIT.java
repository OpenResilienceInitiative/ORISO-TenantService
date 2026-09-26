package com.vi.tenantservice.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.TenantLegalVersionService;
import com.vi.tenantservice.api.service.TenantLegalVersionService.PublishedLegalTexts;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class TenantLegalVersionControllerIT {

  @Autowired private WebApplicationContext context;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantLegalVersionService versionService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private ApplicationSettingsService applicationSettingsService;

  @MockitoBean
  private ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

  @MockitoBean
  private ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

  @MockitoBean private SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean private ConsultingTypeService consultingTypeService;
  @MockitoBean private UserAdminService userAdminService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM tenant_legal_text_version");
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void traegerAdmin_Should_readItsOwnHistoryNewestFirstWithTheOlderVersionClosed()
      throws Exception {
    publishPrivacy(1L, "{\"de\":\"<p>first</p>\"}");
    publishPrivacy(1L, "{\"de\":\"<p>second</p>\"}");

    mvc.perform(get("/tenantadmin/1/legal-versions?kind=DPP").with(traegerAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].kind").value("DPP"))
        .andExpect(jsonPath("$[0].ownerLevel").value("TENANT"))
        .andExpect(jsonPath("$[0].ownerId").value(1))
        .andExpect(jsonPath("$[0].content").value("{\"de\":\"<p>second</p>\"}"))
        .andExpect(jsonPath("$[0].supersededAt").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(
            jsonPath("$[0].publishedAt")
                .value(
                    org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d")))
        .andExpect(jsonPath("$[1].content").value("{\"de\":\"<p>first</p>\"}"))
        .andExpect(jsonPath("$[1].supersededAt").isNotEmpty());

    mvc.perform(get("/tenantadmin/1/legal-versions?kind=IMPRINT").with(traegerAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void platformAdmin_Should_readPlatformAndTraegerHistory() throws Exception {
    publishPrivacy(1L, "{\"de\":\"<p>traeger</p>\"}");

    mvc.perform(get("/tenantadmin/1/legal-versions?kind=DPP").with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/tenantadmin/0/legal-versions?kind=IMPRINT").with(platformAdmin()))
        .andExpect(status().isOk());
  }

  @Test
  void otherTraeger_Should_beDenied_EvenWithTheTenantAdminRolesAllTenantsAuthority()
      throws Exception {
    publishPrivacy(1L, "{\"de\":\"<p>secret draft history</p>\"}");

    mvc.perform(get("/tenantadmin/1/legal-versions?kind=DPP").with(traegerAdmin(2L)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/0/legal-versions?kind=DPP").with(traegerAdmin(1L)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/legal-versions?kind=DPP").with(technical(1L)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenantadmin/1/legal-versions?kind=DPP").with(jwt()))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidKind_Should_beABadRequest() throws Exception {
    mvc.perform(get("/tenantadmin/1/legal-versions?kind=PRIVACY").with(traegerAdmin(1L)))
        .andExpect(status().isBadRequest());
  }

  private void publishPrivacy(Long tenantId, String content) {
    TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow();
    PublishedLegalTexts before = PublishedLegalTexts.of(tenant);
    tenant.setContentPrivacy(content);
    versionService.saveRecordingPublications(before, () -> tenantRepository.save(tenant));
  }

  /** The real tenant-admin role mapping includes GET_ALL_TENANTS; the guard must not rely on it. */
  private RequestPostProcessor traegerAdmin(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("username", "traeger-admin-" + tenantId)
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_GET_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS"));
  }

  private RequestPostProcessor platformAdmin() {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", 0L)
                    .claim("username", "platform-admin")
                    .claim("realm_access", Map.of("roles", List.of("tenant-admin"))))
        .authorities(
            new SimpleGrantedAuthority("AUTHORIZATION_GET_TENANT"),
            new SimpleGrantedAuthority("AUTHORIZATION_GET_ALL_TENANTS"));
  }

  private RequestPostProcessor technical(long tenantId) {
    return jwt()
        .jwt(
            token ->
                token
                    .claim("tenantId", tenantId)
                    .claim("username", "technical")
                    .claim("realm_access", Map.of("roles", List.of("technical"))))
        .authorities(new SimpleGrantedAuthority("AUTHORIZATION_GET_TENANT"));
  }
}
