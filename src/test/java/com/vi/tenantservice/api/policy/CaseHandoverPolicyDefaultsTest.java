package com.vi.tenantservice.api.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class CaseHandoverPolicyDefaultsTest {

  private static final Set<String> SUPPORTED_LANGUAGES =
      Set.of("de", "en", "fr", "ru", "tr", "uk", "ti");

  @Test
  void everyReasonProvidesAnEditableNotificationTemplateInEverySupportedLanguage() {
    var reasons = CaseHandoverPolicyDefaults.create().getReasons();

    reasons.forEach(
        (code, reason) -> {
          var templates = reason.getClientNotificationTemplates().getValue();
          assertEquals(SUPPORTED_LANGUAGES, templates.keySet(), code);
          templates.forEach(
              (language, template) ->
                  assertTrue(template.contains("{{newAdvisor}}"), code + ":" + language));
        });

    var adviceTemplates =
        reasons
            .get(CaseHandoverPolicyDefaults.ADVICE_NEEDED)
            .getClientNotificationTemplates()
            .getValue();
    adviceTemplates.forEach(
        (language, template) ->
            assertTrue(template.contains("{{duration}}"), "advice:" + language));
  }
}
