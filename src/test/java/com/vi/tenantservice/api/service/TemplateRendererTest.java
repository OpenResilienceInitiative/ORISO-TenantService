package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// The "testing" profile targets MariaDB and expects datasource/Keycloak/service settings
// to be supplied via env (empty during the build), which the ConfigurationValidator rejects
// on startup. Override them with an embedded H2 datasource and dummy config values so the
// full application context can start for this template test.
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.datasource.url=jdbc:h2:mem:templaterenderertest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=sa",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false",
      "keycloak.auth-server-url=http://localhost:8080/auth",
      "keycloak.realm=test",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/auth/realms/test",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/auth/realms/test/protocol/openid-connect/certs",
      "consulting.type.service.api.url=http://localhost:8080",
      "user.service.api.url=http://localhost:8080"
    })
@AutoConfigureMockMvc(addFilters = false)
class TemplateRendererTest {

  @Autowired TemplateRenderer templateRenderer;

  @Test
  void renderTemplate_shouldRenderTemplateWithPlaceholder() throws TemplateException, IOException {
    // given
    String templateContent = "Hello ${name}";

    // when
    String renderedTemplate =
        templateRenderer.renderTemplate(templateContent, Map.of("name", "there"));

    // then
    assertThat(renderedTemplate).isEqualTo("Hello there");
  }

  @Test
  void
      renderTemplate_shouldRenderTemplateWithEmptyPlaceholder_When_PlaceHolderKeyMissingInDataModel()
          throws TemplateException, IOException {
    // given
    String templateContent = "Hello ${name}";

    // when
    String renderedTemplate = templateRenderer.renderTemplate(templateContent, Maps.newHashMap());

    // then
    assertThat(renderedTemplate).isEqualTo("Hello ");
  }

  @Test
  void
      renderTemplate_shouldRenderTemplateWithEmptyPlaceholder_When_PlaceHolderValueIsNullInDataModel()
          throws TemplateException, IOException {
    // given
    String templateContent = "Hello ${name}";
    Map<String, Object> dataModel = Maps.newHashMap();
    dataModel.put("name", null);

    // when
    String renderedTemplate = templateRenderer.renderTemplate(templateContent, dataModel);

    // then
    assertThat(renderedTemplate).isEqualTo("Hello ");
  }
}
