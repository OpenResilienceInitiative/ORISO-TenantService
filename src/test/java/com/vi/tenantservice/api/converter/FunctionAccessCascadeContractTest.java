package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.policy.PermissionFeature;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract (#363): a platform lock reaches every existing Träger at once; a suggestion does not
 * overwrite a Träger value. Every Funktionszugriff feature in the {@link PermissionFeature}
 * registry must cascade. A feature that is added to the registry but not wired into the applier
 * fails here.
 */
class FunctionAccessCascadeContractTest {

  private final EffectivePermissionSettingsApplier applier =
      new EffectivePermissionSettingsApplier();

  static Stream<PermissionFeature> functionAccessFeatures() {
    return Arrays.stream(PermissionFeature.values())
        .filter(f -> f.legacyToggleKey() != null && f != PermissionFeature.APPEARANCE);
  }

  @ParameterizedTest
  @MethodSource("functionAccessFeatures")
  void classifierAcceptsEveryToggleFeature(PermissionFeature feature) {
    assertThat(EffectivePermissionSettingsApplier.isFunctionAccessFeature(feature.apiKey()))
        .as(feature.apiKey())
        .isTrue();
  }

  @ParameterizedTest
  @MethodSource("functionAccessFeatures")
  void laterPlatformLockOverridesAnExistingTraegerValue(PermissionFeature feature)
      throws Exception {
    for (boolean locked : new boolean[] {true, false}) {
      var settings = new Settings();
      setSetting(settings, feature, !locked);
      applier.applyPolicies(
          settings,
          Map.of(
              feature.apiKey(),
              new BooleanPermissionPolicy(locked, PermissionPolicyMode.ENFORCED).inherited(true)));
      assertThat(getSetting(settings, feature)).as(feature.apiKey()).isEqualTo(locked);
    }
  }

  @ParameterizedTest
  @MethodSource("functionAccessFeatures")
  void inheritedSuggestionLeavesTheTraegerValue(PermissionFeature feature) throws Exception {
    var settings = new Settings();
    setSetting(settings, feature, false);
    applier.applyPolicies(
        settings,
        Map.of(
            feature.apiKey(),
            new BooleanPermissionPolicy(true, PermissionPolicyMode.SUGGESTED).inherited(true)));
    assertThat(getSetting(settings, feature)).as(feature.apiKey()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("functionAccessFeatures")
  void platformForbiddenForcesOffAndMandatoryForcesOn(PermissionFeature feature) throws Exception {
    var off = new Settings();
    setSetting(off, feature, true);
    applier.applyTo(
        off, new TenantAdminControls().allowedPermissionToggles(toggles(feature, false)));
    assertThat(getSetting(off, feature)).as("forbidden " + feature.apiKey()).isFalse();

    var on = new Settings();
    setSetting(on, feature, false);
    applier.applyTo(
        on, new TenantAdminControls().enforcedPermissionToggles(toggles(feature, true)));
    assertThat(getSetting(on, feature)).as("mandatory " + feature.apiKey()).isTrue();
  }

  private static TenantAdminAllowedPermissionToggles toggles(PermissionFeature f, boolean value)
      throws Exception {
    var toggles = new TenantAdminAllowedPermissionToggles();
    Method setter =
        TenantAdminAllowedPermissionToggles.class.getMethod(
            "set" + cap(f.legacyToggleKey()), Boolean.class);
    setter.invoke(toggles, value);
    return toggles;
  }

  private static void setSetting(Settings s, PermissionFeature f, boolean v) throws Exception {
    Settings.class.getMethod("set" + cap(f.apiKey()), Boolean.class).invoke(s, v);
  }

  private static Boolean getSetting(Settings s, PermissionFeature f) throws Exception {
    return (Boolean) Settings.class.getMethod("get" + cap(f.apiKey())).invoke(s);
  }

  private static String cap(String k) {
    return Character.toUpperCase(k.charAt(0)) + k.substring(1);
  }
}
