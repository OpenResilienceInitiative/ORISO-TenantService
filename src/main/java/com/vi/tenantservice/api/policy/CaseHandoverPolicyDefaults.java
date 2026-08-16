package com.vi.tenantservice.api.policy;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverPolicies;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.MultilingualTextPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.StringListPermissionPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Platform defaults for the stable Case Handover reason codes. */
public final class CaseHandoverPolicyDefaults {

  public static final String ADVICE_NEEDED = "COUNSELLOR_ASKED_FOR_ADVICE";

  private CaseHandoverPolicyDefaults() {}

  public static CaseHandoverPolicies create() {
    Map<String, CaseHandoverReasonPolicy> reasons = new LinkedHashMap<>();
    reasons.put(
        ADVICE_NEEDED,
        reason(
                CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_ASKED_FOR_ADVICE,
                Map.of("de", "Rat benötigt", "en", "Advice needed"),
                true,
                Set.of("CLIENT"),
                Map.of(
                    "de",
                    "{{newAdvisor}} kann diese Sitzung für {{duration}} zeitlich begrenzt mitlesen. Deine bisherige Berater:in bleibt für dich zuständig.",
                    "en",
                    "{{newAdvisor}} can read this session for {{duration}}. Your current counsellor remains responsible for you."))
            .maxAccessDurationMinutes(
                new IntegerPermissionPolicy(
                    CaseHandoverDurationPolicy.DEFAULT_MINUTES, PermissionPolicyMode.SUGGESTED)));
    reasons.put(
        "COUNSELLOR_ON_HOLIDAY",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_ON_HOLIDAY,
            Map.of("de", "Freistellung", "en", "Leave"),
            false,
            Set.of(),
            Map.of(
                "de", "Während der Abwesenheit übernimmt {{newAdvisor}} deine Beratung.",
                "en", "During the absence, {{newAdvisor}} takes over your counselling.")));
    reasons.put(
        "OTHER_EMERGENCY",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.OTHER_EMERGENCY,
            Map.of("de", "Anderer Notfall", "en", "Other emergency"),
            false,
            Set.of(),
            Map.of(
                "de", "Aus einem dringenden Grund hat {{newAdvisor}} deinen Fall übernommen.",
                "en", "For an urgent reason, {{newAdvisor}} has taken over your case.")));
    reasons.put(
        "COUNSELLOR_IS_ILL",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_IS_ILL,
            Map.of("de", "Ausfall", "en", "Absence"),
            false,
            Set.of(),
            Map.of(
                "de", "Wegen eines Ausfalls hat {{newAdvisor}} deinen Fall übernommen.",
                "en", "Because of an absence, {{newAdvisor}} has taken over your case.")));
    reasons.put(
        "COUNSELLOR_LEFT",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_LEFT,
            Map.of("de", "Beratungsstelle verlassen", "en", "Counsellor left"),
            false,
            Set.of(),
            Map.of(
                "de", "{{newAdvisor}} führt deine Beratung ab jetzt weiter.",
                "en", "{{newAdvisor}} will continue your counselling from now on.")));
    return new CaseHandoverPolicies(Map.copyOf(reasons));
  }

  private static CaseHandoverReasonPolicy reason(
      CaseHandoverReasonPolicy.CodeEnum code,
      Map<String, String> labels,
      boolean clientConsentRequired,
      Set<String> approvalRoles,
      Map<String, String> templates) {
    return new CaseHandoverReasonPolicy(
        code,
        multilingual(labels),
        bool(true),
        bool(true),
        bool(clientConsentRequired),
        new StringListPermissionPolicy(approvalRoles, PermissionPolicyMode.SUGGESTED),
        multilingual(templates));
  }

  private static BooleanPermissionPolicy bool(boolean value) {
    return new BooleanPermissionPolicy(value, PermissionPolicyMode.SUGGESTED);
  }

  private static MultilingualTextPermissionPolicy multilingual(Map<String, String> value) {
    return new MultilingualTextPermissionPolicy(value, PermissionPolicyMode.SUGGESTED);
  }
}
