package com.vi.tenantservice.api.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PermissionPolicyOpenApiContractTest {

  @Test
  @SuppressWarnings("unchecked")
  void openApi_shouldExposeTheTypedRegistryAndFourStatePolicyContract() throws IOException {
    Map<String, Object> specification =
        new Yaml().load(Files.readString(Path.of("api/tenantservice.yaml")));
    var components = (Map<String, Object>) specification.get("components");
    var paths = (Map<String, Object>) specification.get("paths");
    var schemas = (Map<String, Object>) components.get("schemas");
    var mode = (Map<String, Object>) schemas.get("PermissionPolicyMode");
    var featureKey = (Map<String, Object>) schemas.get("PermissionFeatureKey");
    var booleanPolicy = (Map<String, Object>) schemas.get("BooleanPermissionPolicy");
    var controls = (Map<String, Object>) schemas.get("TenantAdminControls");
    var controlProperties = (Map<String, Object>) controls.get("properties");

    assertThat((List<String>) mode.get("enum")).containsExactly("ENFORCED", "SUGGESTED");
    assertThat((List<String>) featureKey.get("enum"))
        .containsExactlyInAnyOrder(
            Arrays.stream(PermissionFeature.values())
                .map(PermissionFeature::apiKey)
                .toArray(String[]::new));
    assertThat((List<String>) booleanPolicy.get("required")).containsExactly("value", "mode");
    assertThat((Map<String, Object>) booleanPolicy.get("properties")).containsKey("inherited");
    assertThat(controlProperties).containsKey("permissionPolicies");
    assertThat(controlProperties).containsKey("caseHandoverPolicies");
    assertThat(schemas)
        .containsKeys(
            "IntegerPermissionPolicy",
            "StringListPermissionPolicy",
            "MultilingualTextPermissionPolicy",
            "CaseHandoverReasonPolicy",
            "CaseHandoverPolicies");
    var duration = (Map<String, Object>) schemas.get("IntegerPermissionPolicy");
    var durationValue =
        (Map<String, Object>) ((Map<String, Object>) duration.get("properties")).get("value");
    assertThat(durationValue.get("minimum")).isEqualTo(15);
    assertThat(durationValue.get("multipleOf")).isEqualTo(15);
    assertThat(durationValue).doesNotContainKey("maximum");
    assertThat(paths).containsKey("/tenantadmin/{id}/permission-policies");
  }
}
