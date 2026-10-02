package com.vi.tenantservice.api.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import org.junit.jupiter.api.Test;

/**
 * The controls blob is read on the bootstrap path, so a single unreadable policy entry must not
 * cost the whole tenant its settings. These tests pin the one-way tolerance described on {@link
 * TenantAdminControlsSettings}: drop what cannot be understood, keep everything else, and still
 * fail loudly when the document itself is broken.
 */
class LenientPolicyValueMapDeserializerTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private TenantAdminControlsSettings read(String json) throws JsonProcessingException {
    return mapper.readValue(json, TenantAdminControlsSettings.class);
  }

  @Test
  void shouldKeepWellFormedEntries() throws Exception {
    var settings =
        read(
            """
            {"permissionPolicies":{
              "featureAnonymousChatEnabled":{"value":true,"mode":"ENFORCED"},
              "featureGroupChatV2Enabled":{"value":false,"mode":"SUGGESTED"}}}
            """);

    assertThat(settings.getPermissionPolicies())
        .containsOnlyKeys("featureAnonymousChatEnabled", "featureGroupChatV2Enabled");
    assertThat(settings.getPermissionPolicies().get("featureAnonymousChatEnabled"))
        .isEqualTo(new PolicyValue<>(true, PermissionPolicyMode.ENFORCED));
    assertThat(settings.getPermissionPolicies().get("featureGroupChatV2Enabled"))
        .isEqualTo(new PolicyValue<>(false, PermissionPolicyMode.SUGGESTED));
  }

  @Test
  void shouldDropEntryWhoseKeyIsNotAKnownFeature() throws Exception {
    var settings =
        read(
            """
            {"permissionPolicies":{
              "featureInventedByANewerBuild":{"value":true,"mode":"ENFORCED"},
              "featureCallsEnabled":{"value":true,"mode":"ENFORCED"}}}
            """);

    assertThat(settings.getPermissionPolicies()).containsOnlyKeys("featureCallsEnabled");
  }

  @Test
  void shouldDropEntryWhoseModeIsUnknown() throws Exception {
    var settings =
        read(
            """
            {"permissionPolicies":{
              "featureCallsEnabled":{"value":true,"mode":"MANDATORY_IN_A_LATER_BUILD"},
              "featureThreadsEnabled":{"value":true,"mode":"SUGGESTED"}}}
            """);

    assertThat(settings.getPermissionPolicies()).containsOnlyKeys("featureThreadsEnabled");
  }

  @Test
  void shouldDropEntryWhoseValueIsNotABoolean() throws Exception {
    var settings =
        read(
            """
            {"permissionPolicies":{
              "featureCallsEnabled":{"value":"yes","mode":"ENFORCED"},
              "featureThreadsEnabled":{"value":true,"mode":"ENFORCED"}}}
            """);

    assertThat(settings.getPermissionPolicies()).containsOnlyKeys("featureThreadsEnabled");
  }

  @Test
  void shouldDropEntryMissingValueOrMode() throws Exception {
    var settings =
        read(
            """
            {"permissionPolicies":{
              "featureCallsEnabled":{"mode":"ENFORCED"},
              "featureThreadsEnabled":{"value":true},
              "featureVoiceMessagesEnabled":{"value":null,"mode":"ENFORCED"},
              "featureMediaUploadEnabled":{"value":true,"mode":"ENFORCED"}}}
            """);

    assertThat(settings.getPermissionPolicies()).containsOnlyKeys("featureMediaUploadEnabled");
  }

  @Test
  void shouldDropEntryThatIsNotAnObject() throws Exception {
    var settings =
        read(
            """
            {"permissionPolicies":{
              "featureCallsEnabled":true,
              "featureThreadsEnabled":{"value":true,"mode":"ENFORCED"}}}
            """);

    assertThat(settings.getPermissionPolicies()).containsOnlyKeys("featureThreadsEnabled");
  }

  @Test
  void shouldSurviveABlobInWhichEveryEntryIsUnreadable() throws Exception {
    var settings =
        read(
            """
            {"permissionsPageEnabled":true,
             "permissionPolicies":{"whoKnows":{"value":"maybe","mode":"???"}}}
            """);

    assertThat(settings.getPermissionPolicies()).isEmpty();
    assertThat(settings.isPermissionsPageEnabled()).isTrue();
  }

  @Test
  void shouldReadNullMapAsNull() throws Exception {
    assertThat(read("{\"permissionPolicies\":null}").getPermissionPolicies()).isNull();
  }

  @Test
  void shouldStillFailOnASyntacticallyBrokenDocument() {
    assertThatThrownBy(() -> read("{\"permissionPolicies\":{\"featureCallsEnabled\":"))
        .isInstanceOf(JsonProcessingException.class);
  }
}
