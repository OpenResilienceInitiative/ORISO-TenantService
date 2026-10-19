package com.vi.tenantservice.api.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * The Keycloak mail header loads a Träger's logo from {@code /tenant/public/branding/{id}/logo}.
 * Through the real dispatch and database, a Träger without its own image must get the image Admin
 * shows it: the inherited platform one.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
class PublicTenantBrandingInheritanceIT {

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
  }

  private static String png(String text) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(text.getBytes(UTF_8));
  }

  @Test
  void traegerWithoutOwnLogoGetsThePlatformLogo() throws Exception {
    jdbc.update(
        "UPDATE TENANT SET theming_logo = ?, theming_association_logo = NULL WHERE id = 0",
        png("platform-logo"));
    jdbc.update(
        "UPDATE TENANT SET theming_logo = NULL, theming_association_logo = NULL WHERE id = 1");
    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-logo".getBytes(UTF_8)));
  }

  @Test
  void traegerWithBlankLogoGetsThePlatformLogo() throws Exception {
    jdbc.update(
        "UPDATE TENANT SET theming_logo = ?, theming_association_logo = NULL WHERE id = 0",
        png("platform-logo"));
    jdbc.update(
        "UPDATE TENANT SET theming_logo = '  ', theming_association_logo = NULL WHERE id = 1");
    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-logo".getBytes(UTF_8)));
  }

  @Test
  void noPlatformLogoMeansNotFound() throws Exception {
    jdbc.update(
        "UPDATE TENANT SET theming_logo = NULL, theming_association_logo = NULL WHERE id = 0");
    jdbc.update(
        "UPDATE TENANT SET theming_logo = NULL, theming_association_logo = NULL WHERE id = 1");
    mvc.perform(get("/tenant/public/branding/1/logo")).andExpect(status().isNotFound());
  }

  @Test
  void ownLogoWins() throws Exception {
    jdbc.update("UPDATE TENANT SET theming_logo = ? WHERE id = 0", png("platform-logo"));
    jdbc.update("UPDATE TENANT SET theming_logo = ? WHERE id = 1", png("own-logo"));
    mvc.perform(get("/tenant/public/branding/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("own-logo".getBytes(UTF_8)));
  }

  @Test
  void traegerWithoutFaviconGetsThePlatformFavicon() throws Exception {
    jdbc.update("UPDATE TENANT SET theming_favicon = ? WHERE id = 0", png("platform-icon"));
    jdbc.update("UPDATE TENANT SET theming_favicon = NULL WHERE id = 1");
    mvc.perform(get("/tenant/public/branding/1/favicon"))
        .andExpect(status().isOk())
        .andExpect(content().bytes("platform-icon".getBytes(UTF_8)));
  }
}
