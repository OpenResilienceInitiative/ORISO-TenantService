package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

  private final TemplateRenderer templateRenderer = new TemplateRenderer();

  @Test
  void concurrentRequestsRenderTheirOwnTextAndContacts() throws Exception {
    var round = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> checkConcurrentRenders(round, "First organisation", "First contact"));
      var second =
          executor.submit(
              () -> checkConcurrentRenders(round, "Second organisation", "Second contact"));

      assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(0);
      assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(0);
    }
  }

  private int checkConcurrentRenders(CyclicBarrier round, String organisation, String contact)
      throws Exception {
    int mismatches = 0;
    for (int attempt = 0; attempt < 100; attempt++) {
      round.await(5, TimeUnit.SECONDS);
      String rendered =
          templateRenderer.renderTemplate(organisation + " — ${name}", Map.of("name", contact));
      if (!rendered.equals(organisation + " — " + contact)) {
        mismatches++;
      }
    }
    return mismatches;
  }

  @Test
  void renderTemplate_shouldRenderTemplateWithPlaceholder() {
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
      renderTemplate_shouldRenderTemplateWithEmptyPlaceholder_When_PlaceHolderKeyMissingInDataModel() {
    // given
    String templateContent = "Hello ${name}";

    // when
    String renderedTemplate = templateRenderer.renderTemplate(templateContent, new HashMap<>());

    // then
    assertThat(renderedTemplate).isEqualTo("Hello ");
  }

  @Test
  void
      renderTemplate_shouldRenderTemplateWithEmptyPlaceholder_When_PlaceHolderValueIsNullInDataModel() {
    // given
    String templateContent = "Hello ${name}";
    Map<String, String> dataModel = new HashMap<>();
    dataModel.put("name", null);

    // when
    String renderedTemplate = templateRenderer.renderTemplate(templateContent, dataModel);

    // then
    assertThat(renderedTemplate).isEqualTo("Hello ");
  }

  @Test
  void substitutesContactValuesLiterallyInOnePass() {
    String contact = "Contact \\ $5 ${responsible}";
    String result =
        templateRenderer.renderTemplate(
            "<p>${ dataProtectionOfficer }</p>{{legal_links}}",
            Map.of("dataProtectionOfficer", contact, "responsible", "another contact"));

    assertThat(result).isEqualTo("<p>" + contact + "</p>{{legal_links}}");
  }
}
