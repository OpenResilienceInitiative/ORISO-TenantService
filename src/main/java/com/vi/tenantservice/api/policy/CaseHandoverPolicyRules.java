package com.vi.tenantservice.api.policy;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverConsentValue;
import com.vi.tenantservice.api.model.CaseHandoverPolicies;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
import com.vi.tenantservice.api.model.ConsentPermissionPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Shared compatibility and validation rules for platform and tenant Case Handover policies. */
public final class CaseHandoverPolicyRules {

  private CaseHandoverPolicyRules() {}

  /**
   * Removes retired reason codes and ensures both consent representations are present on reads.
   * Enforced legacy {@code false} means an explicit opt-out, while suggested {@code false} means
   * that no consent choice is required.
   */
  public static CaseHandoverPolicies normalize(CaseHandoverPolicies policies) {
    if (policies == null || policies.getReasons() == null) {
      return CaseHandoverPolicyDefaults.create();
    }
    Set<String> supported = CaseHandoverPolicyDefaults.create().getReasons().keySet();
    Map<String, CaseHandoverReasonPolicy> normalized = new LinkedHashMap<>();
    policies
        .getReasons()
        .forEach(
            (code, reason) -> {
              if (!supported.contains(code) || reason == null) {
                return;
              }
              ConsentPermissionPolicy consent = effectiveConsent(reason);
              if (consent != null) {
                validateConsent(consent);
                reason.setClientConsent(consent);
                reason.setClientConsentRequired(legacyConsentMirror(consent));
              }
              normalized.put(code, reason);
            });
    return new CaseHandoverPolicies(Map.copyOf(normalized));
  }

  public static void validate(CaseHandoverPolicies policies) {
    if (policies == null) {
      return;
    }
    if (policies.getReasons() == null) {
      throw new IllegalArgumentException("Case Handover reasons must not be null");
    }
    Set<String> knownReasons = CaseHandoverPolicyDefaults.create().getReasons().keySet();
    for (var entry : policies.getReasons().entrySet()) {
      if (!knownReasons.contains(entry.getKey()) || entry.getValue() == null) {
        throw new IllegalArgumentException("Unknown Case Handover reason: " + entry.getKey());
      }
      CaseHandoverReasonPolicy reason = entry.getValue();
      if (reason.getCode() == null || !entry.getKey().equals(reason.getCode().getValue())) {
        throw new IllegalArgumentException("Case Handover reason key must match its code");
      }
      ConsentPermissionPolicy consent = effectiveConsent(reason);
      if (consent == null) {
        throw new IllegalArgumentException("Case Handover client consent must not be null");
      }
      validateConsent(consent);
      if (CaseHandoverPolicyDefaults.ADVICE_NEEDED.equals(entry.getKey())) {
        IntegerPermissionPolicy duration = reason.getMaxAccessDurationMinutes();
        CaseHandoverDurationPolicy.validateAdviceNeeded(
            duration == null ? null : duration.getValue());
      } else {
        CaseHandoverDurationPolicy.validateTakeover(
            reason.getMaxAccessDurationMinutes() == null
                ? null
                : reason.getMaxAccessDurationMinutes().getValue());
      }
    }
  }

  public static ConsentPermissionPolicy effectiveConsent(CaseHandoverReasonPolicy reason) {
    if (reason == null) {
      return null;
    }
    BooleanPermissionPolicy legacy = reason.getClientConsentRequired();
    if (legacy != null && legacy.getMode() == PermissionPolicyMode.ENFORCED) {
      return new ConsentPermissionPolicy(
              Boolean.TRUE.equals(legacy.getValue())
                  ? CaseHandoverConsentValue.OPT_IN
                  : CaseHandoverConsentValue.OPT_OUT,
              legacy.getMode())
          .inherited(legacy.getInherited());
    }
    if (reason.getClientConsent() != null) {
      return reason.getClientConsent();
    }
    if (legacy == null) {
      return null;
    }
    return new ConsentPermissionPolicy(
            Boolean.TRUE.equals(legacy.getValue())
                ? CaseHandoverConsentValue.OPT_IN
                : CaseHandoverConsentValue.NONE,
            legacy.getMode())
        .inherited(legacy.getInherited());
  }

  public static BooleanPermissionPolicy legacyConsentMirror(ConsentPermissionPolicy consent) {
    if (consent == null) {
      return null;
    }
    return new BooleanPermissionPolicy(
            consent.getValue() == CaseHandoverConsentValue.OPT_IN, consent.getMode())
        .inherited(consent.getInherited());
  }

  private static void validateConsent(ConsentPermissionPolicy consent) {
    if (consent.getValue() == CaseHandoverConsentValue.NONE
        && consent.getMode() == PermissionPolicyMode.ENFORCED) {
      throw new IllegalArgumentException(
          "Case Handover NONE consent is a recommendation, not an enforced state");
    }
  }
}
