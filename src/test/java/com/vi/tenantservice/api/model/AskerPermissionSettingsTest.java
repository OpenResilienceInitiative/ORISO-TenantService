package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * ORISO-Admin#602 owns two settings that did not exist anywhere: whether an advice seeker may
 * <b>type</b> their own display name (versus only re-rolling the generated one), and whether they
 * may <b>leave an e-mail address</b> at all. ORISO-Frontend#259 and ORISO-Frontend#825 both consume
 * these keys, so they are defined once, here.
 *
 * <p>Both default to {@code true}, which is deliberate: they are <b>opt-out</b>. Every tenant that
 * exists today behaves exactly as it does now until somebody switches one off, so shipping the keys
 * changes nothing on its own.
 *
 * <p>The naming matters as much as the behaviour. Switch 1 governs the <b>Anzeigename</b> ({@code
 * displayName}), never the <b>Anmeldename</b> ({@code userName}) — the login name is immutable at
 * every layer and no setting can change that. A Träger admin reading "Nutzername" would decide
 * about the wrong thing.
 *
 * <p>The trap this test exists for is named in the issue itself: {@code TenantSettings} coalesces
 * nulls from {@code BOOLEAN_FIELD_DEFAULTS}, so <b>a new boolean added without an entry in that map
 * stays {@code null}</b> rather than taking its default — and a null Boolean unboxes into a
 * NullPointerException at the first consumer.
 */
class AskerPermissionSettingsTest {

  @Test
  void displayNameEditableDefaultsToEnabledSoNoTenantChangesBehaviourOnUpgrade() {
    var settings = new TenantSettings();
    settings.applyDefaults();

    assertThat(settings.getFeatureDisplayNameEditable()).isTrue();
  }

  @Test
  void askerEmailDefaultsToEnabledSoNoTenantChangesBehaviourOnUpgrade() {
    var settings = new TenantSettings();
    settings.applyDefaults();

    assertThat(settings.getFeatureAskerEmailEnabled()).isTrue();
  }

  @Test
  void neitherFieldStaysNullAfterDefaultsAreApplied() {
    /* The documented trap: a boolean with no BOOLEAN_FIELD_DEFAULTS entry survives
    applyDefaults() as null and unboxes into an NPE at the first consumer. */
    var settings = new TenantSettings();
    settings.applyDefaults();

    assertThat(settings.getFeatureDisplayNameEditable()).isNotNull();
    assertThat(settings.getFeatureAskerEmailEnabled()).isNotNull();
  }

  @Test
  void anExplicitlyDisabledSettingSurvivesApplyDefaults() {
    /* Opt-out only works if switching a setting off actually sticks — a defaults
    pass that overwrote an explicit false would silently re-enable the e-mail
    invitation for exactly the Träger who turned it off. */
    var settings = new TenantSettings();
    settings.setFeatureDisplayNameEditable(false);
    settings.setFeatureAskerEmailEnabled(false);

    settings.applyDefaults();

    assertThat(settings.getFeatureDisplayNameEditable()).isFalse();
    assertThat(settings.getFeatureAskerEmailEnabled()).isFalse();
  }
}
