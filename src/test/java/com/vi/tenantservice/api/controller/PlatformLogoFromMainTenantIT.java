package com.vi.tenantservice.api.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTO;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.SettingDTO;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * ORISO-TenantService#300. On a single-domain installation the platform admin uploads the platform
 * logo on the <em>main tenant</em> (the one named by {@code mainTenantSubdomainForSingleDomain
 * Multitenancy}); tenant id 0 either has no row or carries no image. A Träger without its own logo
 * must still inherit that platform logo, exactly as Admin → Appearance shows it, and a Träger that
 * uploads its own logo must keep it.
 *
 * <p>Seed data: id 0 = technical tenant, id 1 = the Träger under test, id 2 = the main tenant.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "feature.multitenancy.with.single.domain.enabled=true",
      "branding.main-tenant-images.cache-seconds=0"
    })
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class PlatformLogoFromMainTenantIT {

  private static final String MAIN_TENANT_SUBDOMAIN = "examplesubdomain";

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean AuthorisationService authorisationService;
  @MockitoBean ApplicationSettingsService applicationSettingsService;
  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;
  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;
  @MockitoBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean TenantResolverService tenantResolverService;
  @MockitoBean ConsultingTypeService consultingTypeService;
  @MockitoBean UserAdminService userAdminService;
  @MockitoBean SubdomainExtractor subdomainExtractor;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    var settings = new ApplicationSettingsDTO();
    settings.setMainTenantSubdomainForSingleDomainMultitenancy(
        new SettingDTO().value(MAIN_TENANT_SUBDOMAIN));
    when(applicationSettingsService.getApplicationSettings()).thenReturn(settings);
    // Dev state: the technical tenant has no image, the Träger has none of its own.
    setLogo(0, null);
    setLogo(1, null);
    setLogo(2, null);
  }

  private static String png(String text) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(text.getBytes(UTF_8));
  }

  private void setLogo(int tenantId, String logo) {
    jdbc.update(
        "UPDATE TENANT SET theming_logo = ?, theming_association_logo = NULL WHERE id = ?",
        logo,
        tenantId);
  }

  @Test
  void traegerWithoutOwnLogoInheritsTheLogoOfTheMainTenant() throws Exception {
    setLogo(2, png("platform-logo"));

    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-logo".getBytes(UTF_8)));
  }

  @Test
  void theTechnicalTenantRouteServesThePlatformLogoOfTheMainTenant() throws Exception {
    setLogo(2, png("platform-logo"));

    mvc.perform(get("/tenant/public/branding/0/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-logo".getBytes(UTF_8)));
  }

  @Test
  void theTechnicalTenantRouteWorksWithoutAnyTechnicalTenantRow() throws Exception {
    setLogo(2, png("platform-logo"));
    jdbc.update("DELETE FROM TENANT WHERE id = 0");

    mvc.perform(get("/tenant/public/branding/0/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-logo".getBytes(UTF_8)));
    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-logo".getBytes(UTF_8)));
  }

  @Test
  void traegerOwnAssociationLogoWinsOverTheInheritedPlatformLogo() throws Exception {
    setLogo(2, png("platform-logo"));
    jdbc.update(
        "UPDATE TENANT SET theming_logo = NULL, theming_association_logo = ? WHERE id = 1",
        png("own-association"));

    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("own-association".getBytes(UTF_8)));
  }

  @Test
  void traegerOwnLogoWinsOverThePlatformLogo() throws Exception {
    setLogo(2, png("platform-logo"));
    setLogo(1, png("own-logo"));

    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("own-logo".getBytes(UTF_8)));
  }

  @Test
  void aLogoOnTheTechnicalTenantStillWinsOverTheMainTenant() throws Exception {
    setLogo(0, png("technical-logo"));
    setLogo(2, png("main-logo"));

    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("technical-logo".getBytes(UTF_8)));
  }

  @Test
  void noPlatformLogoAnywhereMeansNotFound() throws Exception {
    mvc.perform(get("/tenant/public/branding/1/logo")).andExpect(status().isNotFound());
    mvc.perform(get("/tenant/public/branding/0/logo")).andExpect(status().isNotFound());
  }

  @Test
  void anUnknownTenantStillNeverFallsBack() throws Exception {
    setLogo(2, png("platform-logo"));

    mvc.perform(get("/tenant/public/branding/99/logo")).andExpect(status().isNotFound());
  }

  @Test
  void traegerWithoutOwnFaviconInheritsTheFaviconOfTheMainTenant() throws Exception {
    jdbc.update("UPDATE TENANT SET theming_favicon = ? WHERE id = 2", png("platform-icon"));
    jdbc.update("UPDATE TENANT SET theming_favicon = NULL WHERE id IN (0, 1)");

    mvc.perform(get("/tenant/public/branding/1/favicon"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-icon".getBytes(UTF_8)));
  }

  @Test
  void theAssociationLogoOfTheMainTenantIsInheritedInTheTenantData() throws Exception {
    jdbc.update(
        "UPDATE TENANT SET theming_association_logo = ? WHERE id = 2", png("platform-association"));

    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.associationLogo").value(png("platform-association")));
  }

  @Test
  void onlyImagesAreInheritedFromTheMainTenantNeverItsColours() throws Exception {
    setLogo(2, png("platform-logo"));
    jdbc.update(
        "UPDATE TENANT SET theming_primary_color = '#123456', theming_accent = '#abcdef',"
            + " theming_secondary_color = '#654321' WHERE id = 2");
    jdbc.update(
        "UPDATE TENANT SET theming_primary_color = NULL, theming_accent = NULL,"
            + " theming_secondary_color = NULL WHERE id IN (0, 1)");

    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.logo").value(png("platform-logo")))
        .andExpect(jsonPath("$.theming.primaryColor").doesNotExist())
        .andExpect(jsonPath("$.theming.accent").doesNotExist())
        .andExpect(jsonPath("$.theming.secondaryColor").doesNotExist());
  }

  @Test
  void anUnreachableSettingsServiceDegradesToNoInheritanceInsteadOfFailingTheLookup()
      throws Exception {
    setLogo(2, png("platform-logo"));
    when(applicationSettingsService.getApplicationSettings())
        .thenThrow(new IllegalStateException("settings service down"));

    mvc.perform(get("/tenant/public/id/1")).andExpect(status().isOk());
    mvc.perform(get("/tenant/public/branding/1/logo")).andExpect(status().isNotFound());
  }

  @Test
  void theResolvedTenantDataCarriesTheInheritedLogoForTheMailRenderer() throws Exception {
    setLogo(2, png("platform-logo"));

    // UserService's EmailBrandingResolver only builds a logo URL when the tenant DTO it receives
    // already carries a logo, so the inheritance must also show up in the JSON, not only in bytes.
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.logo").value(png("platform-logo")));
  }

  @Test
  void publicChatReceivesTheTenantAssistantIdentityWithoutChangingOtherTenants() throws Exception {
    jdbc.update(
        "UPDATE TENANT SET theming_assistant_name = ?, theming_assistant_icon = ? WHERE id = 1",
        "Help companion",
        "robot-7341990");
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").value("Help companion"))
        .andExpect(jsonPath("$.theming.assistantIcon").value("robot-7341990"));
    mvc.perform(get("/tenant/public/id/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").doesNotExist());
  }

  @Test
  void assistantIdentitySurvivesTheRealAdminSaveAndPublicReload() throws Exception {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(java.util.Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    when(authorisationService.hasAuthority(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);
    var admin =
        jwt()
            .authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "AUTHORIZATION_UPDATE_TENANT"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "AUTHORIZATION_GET_TENANT"));
    String original =
        mvc.perform(get("/tenantadmin/1").with(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var body = mapper.readTree(original);
    var theming = (com.fasterxml.jackson.databind.node.ObjectNode) body.get("theming");
    theming.put("assistantName", "Help companion");
    theming.put("assistantIcon", "robot-5475944");
    mvc.perform(
            put("/tenantadmin/1")
                .with(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(body)))
        .andExpect(status().isOk());
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").value("Help companion"))
        .andExpect(jsonPath("$.theming.assistantIcon").value("robot-5475944"));
    theming.put("assistantName", "x".repeat(80));
    mvc.perform(
            put("/tenantadmin/1")
                .with(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(body)))
        .andExpect(status().isOk());
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").value("x".repeat(80)));
    theming.put("assistantName", "x".repeat(81));
    mvc.perform(
            put("/tenantadmin/1")
                .with(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
    jdbc.update(
        "UPDATE TENANT SET theming_assistant_name = ?, theming_assistant_icon = ? WHERE id = 0",
        "Platform helper",
        "robot-1184077");
    theming.putNull("assistantName");
    theming.putNull("assistantIcon");
    mvc.perform(
            put("/tenantadmin/1")
                .with(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(body)))
        .andExpect(status().isOk());
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").value("Platform helper"))
        .andExpect(jsonPath("$.theming.assistantIcon").value("robot-1184077"));
    mvc.perform(get("/tenantadmin/1").with(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").doesNotExist())
        .andExpect(jsonPath("$.theming.assistantIcon").doesNotExist());
  }

  @Test
  void passiveCustomSvgSurvivesAdminSaveAndPublicReload() throws Exception {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(java.util.Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    when(authorisationService.hasAuthority(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);
    var admin =
        jwt()
            .authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "AUTHORIZATION_UPDATE_TENANT"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "AUTHORIZATION_GET_TENANT"));
    var original =
        mvc.perform(get("/tenantadmin/1").with(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var body = mapper.readTree(original);
    var icon =
        "data:image/svg+xml;base64,"
            + Base64.getEncoder()
                .encodeToString(
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><circle cx=\"12\" cy=\"12\" r=\"10\" fill=\"currentColor\"/></svg>"
                        .getBytes(UTF_8));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("theming"))
        .put("assistantIcon", icon);
    mvc.perform(
            put("/tenantadmin/1")
                .with(admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(body)))
        .andExpect(status().isOk());
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantIcon").value(icon));
  }

  @Test
  void singleTenantAdministratorCannotWriteAnotherTenantsAssistantIdentity() throws Exception {
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(java.util.Optional.of(0L));
    when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
    when(authorisationService.hasAuthority(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);
    var admin =
        jwt()
            .authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "AUTHORIZATION_GET_TENANT"));
    var original =
        mvc.perform(get("/tenantadmin/1").with(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var payload = mapper.readTree(original);
    ((com.fasterxml.jackson.databind.node.ObjectNode) payload.get("theming"))
        .put("assistantName", "Wrong tenant");
    var body = mapper.writeValueAsString(payload);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(java.util.Optional.of(2L));
    when(authorisationService.hasAuthority(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(false);
    mvc.perform(
            put("/tenantadmin/1")
                .with(
                    jwt()
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "AUTHORIZATION_UPDATE_TENANT")))
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").doesNotExist());
  }

  @Test
  void mainTenantImageFallbackDoesNotLeakAssistantIdentity() throws Exception {
    jdbc.update(
        "UPDATE TENANT SET theming_assistant_name = NULL, theming_assistant_icon = NULL WHERE id IN (0,1)");
    jdbc.update(
        "UPDATE TENANT SET theming_assistant_name = ?, theming_assistant_icon = ? WHERE id = 2",
        "Main helper",
        "robot-1184077");
    mvc.perform(get("/tenant/public/id/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theming.assistantName").doesNotExist())
        .andExpect(jsonPath("$.theming.assistantIcon").doesNotExist());
  }
}
