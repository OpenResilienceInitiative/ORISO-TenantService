package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.util.JsonConverter;
import org.junit.jupiter.api.Test;

class TenantSettingsTest {

  @Test
  void applyDefaults_should_defaultMediaUploadAndInlineDisplayOn_andAiScanOff() {
    var settings = new TenantSettings().applyDefaults();

    assertThat(settings.getFeatureMediaUploadEnabled()).isTrue();
    assertThat(settings.getFeatureMediaUploadAnonymousChatsEnabled()).isTrue();
    assertThat(settings.getFeatureMediaUploadOneOnOneChatsEnabled()).isTrue();
    assertThat(settings.getFeatureMediaUploadGroupChatsEnabled()).isTrue();
    assertThat(settings.getFeatureMediaUploadSupervisionChatsEnabled()).isTrue();

    assertThat(settings.getFeatureMediaInlineDisplayEnabled()).isTrue();
    assertThat(settings.getFeatureMediaInlineDisplayAnonymousChatsEnabled()).isTrue();
    assertThat(settings.getFeatureMediaInlineDisplayOneOnOneChatsEnabled()).isTrue();
    assertThat(settings.getFeatureMediaInlineDisplayGroupChatsEnabled()).isTrue();
    assertThat(settings.getFeatureMediaInlineDisplaySupervisionChatsEnabled()).isTrue();

    assertThat(settings.getFeatureMediaAiScanEnabled()).isFalse();
    assertThat(settings.getFeatureMediaAiScanAnonymousChatsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaAiScanOneOnOneChatsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaAiScanGroupChatsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaAiScanSupervisionChatsEnabled()).isFalse();
  }

  @Test
  void applyDefaults_should_translateLegacyAttachmentUploadDisabled_toMediaUploadOff() {
    var settings =
        JsonConverter.convertFromJson("{\"featureAttachmentUploadDisabled\": true}")
            .applyDefaults();

    assertThat(settings.getFeatureMediaUploadEnabled()).isFalse();
    assertThat(settings.getFeatureMediaUploadAnonymousChatsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaUploadOneOnOneChatsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaUploadGroupChatsEnabled()).isFalse();
    assertThat(settings.getFeatureMediaUploadSupervisionChatsEnabled()).isFalse();
    // the other families are not the legacy flag's concern
    assertThat(settings.getFeatureMediaInlineDisplayEnabled()).isTrue();
    assertThat(settings.getFeatureMediaAiScanEnabled()).isFalse();
  }

  @Test
  void applyDefaults_should_keepMediaUploadOn_whenLegacyFlagFalseOrAbsent() {
    var legacyFalse =
        JsonConverter.convertFromJson("{\"featureAttachmentUploadDisabled\": false}")
            .applyDefaults();
    assertThat(legacyFalse.getFeatureMediaUploadEnabled()).isTrue();

    var legacyAbsent = JsonConverter.convertFromJson("{}").applyDefaults();
    assertThat(legacyAbsent.getFeatureMediaUploadEnabled()).isTrue();
  }

  @Test
  void applyDefaults_should_preferExplicitMediaUploadValues_overLegacyTranslation() {
    var settings =
        JsonConverter.convertFromJson(
                "{\"featureAttachmentUploadDisabled\": true,"
                    + " \"featureMediaUploadEnabled\": true,"
                    + " \"featureMediaUploadGroupChatsEnabled\": false}")
            .applyDefaults();

    assertThat(settings.getFeatureMediaUploadEnabled()).isTrue();
    assertThat(settings.getFeatureMediaUploadGroupChatsEnabled()).isFalse();
    // untouched variants still translate from the legacy flag
    assertThat(settings.getFeatureMediaUploadOneOnOneChatsEnabled()).isFalse();
  }

  @Test
  void convertToJson_should_neverSerializeTheRetiredLegacyKey() {
    var settings =
        JsonConverter.convertFromJson("{\"featureAttachmentUploadDisabled\": true}")
            .applyDefaults();

    var json = JsonConverter.convertToJson(settings);

    assertThat(json).doesNotContain("featureAttachmentUploadDisabled");
    assertThat(json).doesNotContain("legacyFeature");
    assertThat(json).contains("\"featureMediaUploadEnabled\":false");
  }

  @Test
  void applyDefaults_should_defaultEmailVisibleAndRequired_toFalse() {
    var settings = new TenantSettings().applyDefaults();

    assertThat(settings.getEmailVisible()).isFalse();
    assertThat(settings.getEmailRequired()).isFalse();
  }

  @Test
  void applyDefaults_should_preferExplicitEmailFlagValues() {
    var settings =
        JsonConverter.convertFromJson("{\"emailVisible\": true, \"emailRequired\": false}")
            .applyDefaults();

    assertThat(settings.getEmailVisible()).isTrue();
    assertThat(settings.getEmailRequired()).isFalse();

    var inverseSettings =
        JsonConverter.convertFromJson("{\"emailVisible\": false, \"emailRequired\": true}")
            .applyDefaults();

    assertThat(inverseSettings.getEmailVisible()).isFalse();
    assertThat(inverseSettings.getEmailRequired()).isTrue();
  }

  @Test
  void fullyPopulatedSettings_should_fitIntoTheSettingsColumn() {
    // The tenant.settings column is TEXT (64k bytes) since changeset 0022 — the old
    // VARCHAR(4000) overflowed once the media flag families landed. Guard with a generous
    // safety margin so the build fails long before real saves would start truncating.
    var settings = new TenantSettings().applyDefaults();
    settings.setTenantAdminControls(
        TenantAdminControlsSettings.builder()
            .permissionsPageEnabled(true)
            .allowedPermissionToggles(TenantAdminAllowedPermissionTogglesSettings.builder().build())
            .enforcedPermissionToggles(
                TenantAdminAllowedPermissionTogglesSettings.builder().build())
            .build());

    assertThat(JsonConverter.convertToJson(settings).length()).isLessThan(32000);
  }
}
