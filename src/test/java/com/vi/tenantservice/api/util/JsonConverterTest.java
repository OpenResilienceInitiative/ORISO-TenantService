package com.vi.tenantservice.api.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import com.vi.tenantservice.api.model.TenantSettings;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonConverterTest {

  @Test
  void convertMapFromJson_Should_returnResult_When_jsonContainsNewLineOrTabulation() {
    // given
    final String json = "{\"de\": \"test\n new line \t tabulation\"}";

    // when
    Map<String, String> result = JsonConverter.convertMapFromJson(json);

    // then
    assertThat(result, notNullValue());
  }

  @Test
  void convertFromJson_Should_returnTenantSettings_When_jsonContainsNewLineOrTabulation() {
    // given
    final String json = "{\"test\": \"test\n new line \t tabulation\"}";

    // when
    TenantSettings result = JsonConverter.convertFromJson(json);

    // then
    assertThat(result, notNullValue());
  }

  @Test
  void convertToJson_Should_returnCorrectJson() {
    // given
    final Map<String, String> translations = new HashMap<>();
    translations.put("en", "en translation");

    // when
    final String json = JsonConverter.convertToJson(translations);

    // then
    assertThat(json, is("{\"en\":\"en translation\"}"));
  }

  @Test
  void convertFromJson_Should_returnCorrectTenantSettings() {
    // given
    final String json =
        "{"
            + "\"featureStatisticsEnabled\":true,"
            + "\"featureTopicsEnabled\":true,"
            + "\"topicsInRegistrationEnabled\":true,"
            + "\"featureDemographicsEnabled\":false,"
            + "\"featureAppointmentsEnabled\":false,"
            + "\"featureGroupChatV2Enabled\":false,"
            + "\"featureToolsEnabled\":false,"
            + "\"featureAttachmentUploadDisabled\":true,"
            + "\"featureToolsOIDCToken\":null"
            + "}";

    // when
    final TenantSettings tenantSettings = JsonConverter.convertFromJson(json);

    // then
    assertThat(tenantSettings, notNullValue());
    assertThat(tenantSettings.isFeatureStatisticsEnabled(), is(true));
    assertThat(tenantSettings.isFeatureTopicsEnabled(), is(true));
    assertThat(tenantSettings.isTopicsInRegistrationEnabled(), is(true));
    assertThat(tenantSettings.isFeatureDemographicsEnabled(), is(false));
    assertThat(tenantSettings.isFeatureAppointmentsEnabled(), is(false));
    assertThat(tenantSettings.isFeatureGroupChatV2Enabled(), is(false));
    assertThat(tenantSettings.isFeatureToolsEnabled(), is(false));
    assertThat(tenantSettings.isFeatureAttachmentUploadDisabled(), is(true));
    assertThat(tenantSettings.getFeatureToolsOIDCToken(), nullValue());
  }

  @Test
  void convertMapFromJson_Should_returnCorrectMap() {
    // given
    final String json = "{\"en\":\"en translation\", \"de\":\"de translation\"}";

    // when
    final Map<String, String> translations = JsonConverter.convertMapFromJson(json);

    // then
    assertThat(translations.size(), is(2));
    assertThat(translations, hasEntry("en", "en translation"));
    assertThat(translations, hasEntry("de", "de translation"));
  }

  @Test
  void convertMapFromJson_Should_returnEmptyMap_When_jsonIsNull() {
    // given
    // when
    final Map<String, String> translations = JsonConverter.convertMapFromJson(null);

    // then
    assertThat(translations.size(), is(0));
  }
}
