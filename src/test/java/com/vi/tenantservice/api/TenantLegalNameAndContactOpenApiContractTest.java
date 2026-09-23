package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

/**
 * The Träger's sender block mirrors the platform operator's ("Betreiber", DpiaOperatorDTO): same
 * names, same limits. UserService and the Admin panel generate clients from this file, so the
 * contract is pinned here rather than only in the implementation.
 */
class TenantLegalNameAndContactOpenApiContractTest {

  @ParameterizedTest
  @ValueSource(strings = {"TenantDTO", "MultilingualTenantDTO"})
  @SuppressWarnings("unchecked")
  void tenantSchemas_should_offerOptionalLegalNameAndContactWithTheOperatorsLimits(String schema)
      throws IOException {
    Map<String, Object> schemas = schemas();
    var tenant =
        (Map<String, Object>) ((Map<String, Object>) schemas.get(schema)).get("properties");
    var operator =
        (Map<String, Object>)
            ((Map<String, Object>) schemas.get("DpiaOperatorDTO")).get("properties");

    for (String field : new String[] {"legalName", "contactEmail", "contactPhone"}) {
      var tenantField = (Map<String, Object>) tenant.get(field);
      var operatorField = (Map<String, Object>) operator.get(field);
      assertThat(tenantField).as("%s.%s", schema, field).isNotNull();
      assertThat(tenantField.get("type")).isEqualTo("string");
      assertThat(tenantField.get("maxLength"))
          .as("%s.%s maxLength", schema, field)
          .isEqualTo(operatorField.get("maxLength"));
    }
    assertThat(((Map<String, Object>) tenant.get("contactEmail")).get("format")).isEqualTo("email");
    var required =
        (java.util.List<String>) ((Map<String, Object>) schemas.get(schema)).get("required");
    assertThat(required).doesNotContain("legalName", "contactEmail", "contactPhone");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> schemas() throws IOException {
    Map<String, Object> specification =
        new Yaml().load(Files.readString(Path.of("api/tenantservice.yaml")));
    return (Map<String, Object>)
        ((Map<String, Object>) specification.get("components")).get("schemas");
  }
}
