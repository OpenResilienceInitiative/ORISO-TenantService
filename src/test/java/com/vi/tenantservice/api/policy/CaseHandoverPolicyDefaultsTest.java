package com.vi.tenantservice.api.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class CaseHandoverPolicyDefaultsTest {

  private static final Set<String> SUPPORTED_LANGUAGES =
      Set.of("de", "en", "fr", "ru", "tr", "uk", "ti");

  @Test
  void exposesOnlyTheCurrentReasonCatalogueWithExplicitConsentDefaults() {
    var reasons = CaseHandoverPolicyDefaults.create().getReasons();

    assertFalse(reasons.containsKey("OTHER_EMERGENCY"));
    assertEquals(
        "Geplant verhindert",
        reasons.get("COUNSELLOR_ON_HOLIDAY").getLabels().getValue().get("de"));
    assertEquals(
        "Ungeplant verhindert", reasons.get("COUNSELLOR_IS_ILL").getLabels().getValue().get("de"));
    assertEquals(
        "OPT_IN",
        reasons
            .get(CaseHandoverPolicyDefaults.ADVICE_NEEDED)
            .getClientConsent()
            .getValue()
            .getValue());
    reasons.forEach(
        (code, reason) -> {
          if (!CaseHandoverPolicyDefaults.ADVICE_NEEDED.equals(code)) {
            assertEquals("NONE", reason.getClientConsent().getValue().getValue(), code);
          }
        });
  }

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
