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
    var consentValue = (Map<String, Object>) schemas.get("CaseHandoverConsentValue");
    var consentPolicy = (Map<String, Object>) schemas.get("ConsentPermissionPolicy");
    var controls = (Map<String, Object>) schemas.get("TenantAdminControls");
    var controlProperties = (Map<String, Object>) controls.get("properties");

    assertThat((List<String>) mode.get("enum")).containsExactly("ENFORCED", "SUGGESTED");
    assertThat((List<String>) featureKey.get("enum"))
        .containsExactlyInAnyOrder(
            Arrays.stream(PermissionFeature.values())
                .map(PermissionFeature::apiKey)
                .toArray(String[]::new));
    assertThat((List<String>) booleanPolicy.get("required")).containsExactly("value", "mode");
    assertThat((List<String>) consentValue.get("enum"))
        .containsExactly("OPT_IN", "OPT_OUT", "NONE");
    assertThat((List<String>) consentPolicy.get("required")).containsExactly("value", "mode");
    for (String schemaName :
        List.of(
            "BooleanPermissionPolicy",
            "ConsentPermissionPolicy",
            "IntegerPermissionPolicy",
            "StringListPermissionPolicy",
            "MultilingualTextPermissionPolicy")) {
      var policySchema = (Map<String, Object>) schemas.get(schemaName);
      var properties = (Map<String, Object>) policySchema.get("properties");
      var inherited = (Map<String, Object>) properties.get("inherited");
      assertThat(inherited).as(schemaName + ".inherited").containsEntry("readOnly", true);
    }
    assertThat(controlProperties).containsKey("permissionPolicies");
    assertThat(controlProperties).containsKey("caseHandoverPolicies");
    assertThat(schemas)
        .containsKeys(
            "IntegerPermissionPolicy",
            "StringListPermissionPolicy",
            "MultilingualTextPermissionPolicy",
            "CaseHandoverReasonPolicy",
            "CaseHandoverPolicies");
    var reasonPolicy = (Map<String, Object>) schemas.get("CaseHandoverReasonPolicy");
    var reasonRequired = (List<String>) reasonPolicy.get("required");
    var reasonProperties = (Map<String, Object>) reasonPolicy.get("properties");
    assertThat(reasonRequired).contains("clientConsent");
    assertThat(reasonRequired).doesNotContain("clientConsentRequired");
    assertThat((Map<String, Object>) reasonProperties.get("clientConsentRequired"))
        .containsEntry("deprecated", true);
    var duration = (Map<String, Object>) schemas.get("IntegerPermissionPolicy");
    var durationValue =
        (Map<String, Object>) ((Map<String, Object>) duration.get("properties")).get("value");
    assertThat(durationValue.get("minimum")).isEqualTo(15);
    assertThat(durationValue.get("multipleOf")).isEqualTo(15);
    assertThat(durationValue).doesNotContainKey("maximum");
    assertThat(paths).containsKey("/tenantadmin/{id}/permission-policies");
  }
}
