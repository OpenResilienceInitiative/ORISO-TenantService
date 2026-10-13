package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AskerChannelPolicySettingsTest {
  @Test
  void legacySettingsKeepEmailForAsynchronousAdviceButNotLiveChat() {
    var settings = new TenantSettings().applyDefaults();
    Map<String, Object> publicSettings =
        new ObjectMapper().convertValue(settings, new TypeReference<>() {});
    assertThat(publicSettings.get("featureAskerEmailAgencyCounsellingEnabled")).isEqualTo(true);
    assertThat(publicSettings.get("featureAskerEmailLiveChatEnabled")).isEqualTo(false);
    assertThat(publicSettings.get("featureAskerEmailSelfHelpEnabled")).isEqualTo(true);
    assertThat(publicSettings.get("featureAskerBrowserLiveChatEnabled")).isEqualTo(true);
  }
}
