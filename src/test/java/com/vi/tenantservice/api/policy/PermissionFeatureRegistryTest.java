package com.vi.tenantservice.api.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionTogglesSettings;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PermissionFeatureRegistryTest {

  @Test
  void registry_shouldCoverEveryLegacyPermissionToggleExactlyOnce() {
    Set<String> legacyFields =
        Arrays.stream(TenantAdminAllowedPermissionTogglesSettings.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(field -> field.getName())
            .collect(Collectors.toSet());
    Set<String> mappedLegacyFields =
        Arrays.stream(PermissionFeature.values())
            .map(PermissionFeature::legacyToggleKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    assertThat(mappedLegacyFields).containsExactlyInAnyOrderElementsOf(legacyFields);
    assertThat(
            Arrays.stream(PermissionFeature.values())
                .map(PermissionFeature::legacyToggleKey)
                .filter(Objects::nonNull))
        .doesNotHaveDuplicates();
  }

  @Test
  void registry_shouldIncludeAskerAndCaseHandoverFeaturesWithoutLegacyAliases() {
    assertThat(PermissionFeature.byApiKey("featureDisplayNameEditable")).isPresent();
    assertThat(PermissionFeature.byApiKey("featureAskerEmailEnabled")).isPresent();
    assertThat(PermissionFeature.byApiKey("caseHandoverEnabled")).isPresent();
    assertThat(PermissionFeature.byApiKey("caseHandoverTeamAccessOptOut")).isPresent();
  }
}
