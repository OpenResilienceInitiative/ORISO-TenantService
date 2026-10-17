package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.ClosingAnimation;
import com.vi.tenantservice.api.model.Theming;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

/**
 * The closing animation a person sees when leaving a waiting room is chosen per tenant, once for
 * desktop and once for mobile (ORISO-Frontend#1737). Admin and the frontend generate clients from
 * api/tenantservice.yaml, so the names and values are pinned here rather than only in the
 * implementation: renaming a value would silently break every stored choice.
 */
class ClosingAnimationOpenApiContractTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  @SuppressWarnings("unchecked")
  void closingAnimation_should_offerExactlyTheThreeDesigns() throws IOException {
    var schema = (Map<String, Object>) schemas().get("ClosingAnimation");

    assertThat(schema).isNotNull();
    assertThat(schema.get("type")).isEqualTo("string");
    assertThat((List<String>) schema.get("enum"))
        .containsExactly("CURTAIN", "CIRCLE_ZOOM", "SLIDE_REVEAL");
  }

  @ParameterizedTest
  @ValueSource(strings = {"closingAnimationDesktop", "closingAnimationMobile"})
  @SuppressWarnings("unchecked")
  void theming_should_offerEachDeviceAsAnOptionalReferenceToTheSharedEnum(String property)
      throws IOException {
    var theming = (Map<String, Object>) schemas().get("Theming");
    var properties = (Map<String, Object>) theming.get("properties");

    var field = (Map<String, Object>) properties.get(property);
    assertThat(field).as(property).isNotNull();
    assertThat(field.get("$ref")).isEqualTo("#/components/schemas/ClosingAnimation");
    // optional: "never configured" is a valid state and means the frontend's own default
    assertThat((List<String>) theming.getOrDefault("required", List.of())).doesNotContain(property);
  }

  @Test
  void theming_should_speakTheDesignNamesOnTheWire() throws IOException {
    Theming theming =
        new Theming()
            .closingAnimationDesktop(ClosingAnimation.CIRCLE_ZOOM)
            .closingAnimationMobile(ClosingAnimation.SLIDE_REVEAL);

    JsonNode written = JSON.readTree(JSON.writeValueAsString(theming));

    assertThat(written.get("closingAnimationDesktop").asText()).isEqualTo("CIRCLE_ZOOM");
    assertThat(written.get("closingAnimationMobile").asText()).isEqualTo("SLIDE_REVEAL");
  }

  @Test
  void theming_should_readTheDesignNamesAndTreatAMissingOneAsNotConfigured() throws IOException {
    Theming read = JSON.readValue("{\"closingAnimationDesktop\":\"CURTAIN\"}", Theming.class);

    assertThat(read.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.CURTAIN);
    assertThat(read.getClosingAnimationMobile()).isNull();
  }

  @Test
  void theming_should_refuseADesignNameTheContractDoesNotKnow() {
    assertThatThrownBy(
            () -> JSON.readValue("{\"closingAnimationMobile\":\"SPIRAL\"}", Theming.class))
        .isInstanceOf(IOException.class);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> schemas() throws IOException {
    Map<String, Object> specification =
        new Yaml().load(Files.readString(Path.of("api/tenantservice.yaml")));
    return (Map<String, Object>)
        ((Map<String, Object>) specification.get("components")).get("schemas");
  }
}
