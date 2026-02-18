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

@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=testing")
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
