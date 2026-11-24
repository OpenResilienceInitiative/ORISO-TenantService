package com.vi.tenantservice.api.policy;

import com.vi.tenantservice.api.model.CaseSharingMode;
import com.vi.tenantservice.api.model.CaseSharingModePermissionPolicy;
import com.vi.tenantservice.api.model.CaseSharingPolicies;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Case sharing (ORISO-TenantService#374): case handover, team counselling or neither, as one
 * cascading choice. Until a level stores this policy, its stored {@code caseHandoverEnabled} reads
 * as the choice (on = case handover, off = neither), so existing installations keep their
 * behaviour. For one release {@code caseHandoverEnabled} is served derived from the mode, so
 * UserService and older Admin builds keep working; then it is retired.
 */
public final class CaseSharingPolicyRules {

  public static final String CASE_HANDOVER_KEY = PermissionFeature.CASE_HANDOVER.apiKey();

  /** Frank, 2026-11-24: case handover as a suggestion, matching today's default. */
  public static final PolicyValue<CaseSharingMode> DEFAULT =
      new PolicyValue<>(CaseSharingMode.CASE_HANDOVER, PermissionPolicyMode.SUGGESTED);

  private CaseSharingPolicyRules() {}

  /** The stored mode of one level, else its legacy {@code caseHandoverEnabled}, else null. */
  public static PolicyValue<CaseSharingMode> ownChoice(
      CaseSharingPolicies stored, PolicyValue<Boolean> legacyCaseHandover) {
    PolicyValue<CaseSharingMode> typed = toDomain(stored == null ? null : stored.getMode());
    if (typed != null) {
      return typed;
    }
    return legacyCaseHandover == null ? null : fromLegacy(legacyCaseHandover);
  }

  /**
   * The platform preset: stored mode, else the platform's {@code caseHandoverEnabled}, else
   * default.
   */
  public static PolicyValue<CaseSharingMode> platformChoice(
      CaseSharingPolicies stored, PolicyValue<Boolean> legacyCaseHandover) {
    PolicyValue<CaseSharingMode> own = ownChoice(stored, legacyCaseHandover);
    return own == null ? DEFAULT : own;
  }

  public static PolicyValue<CaseSharingMode> fromLegacy(PolicyValue<Boolean> caseHandover) {
    return new PolicyValue<>(
        Boolean.TRUE.equals(caseHandover.value())
            ? CaseSharingMode.CASE_HANDOVER
            : CaseSharingMode.NONE,
        caseHandover.mode());
  }

  /** {@code caseHandoverEnabled} as derived from the mode, with the same requirement mode. */
  public static PolicyValue<Boolean> derivedCaseHandover(PolicyValue<CaseSharingMode> mode) {
    return new PolicyValue<>(mode.value() == CaseSharingMode.CASE_HANDOVER, mode.mode());
  }

  public static ResolvedPolicyValue<Boolean> derivedCaseHandover(
      ResolvedPolicyValue<CaseSharingMode> mode) {
    return new ResolvedPolicyValue<>(
        mode.value() == CaseSharingMode.CASE_HANDOVER, mode.mode(), mode.inherited());
  }

  /** Replaces {@code caseHandoverEnabled} in a resolved map by the value derived from the mode. */
  public static Map<String, ResolvedPolicyValue<Boolean>> withDerivedCaseHandover(
      Map<String, ResolvedPolicyValue<Boolean>> resolved,
      ResolvedPolicyValue<CaseSharingMode> mode) {
    var result = new LinkedHashMap<>(resolved);
    result.put(CASE_HANDOVER_KEY, derivedCaseHandover(mode));
    return Map.copyOf(result);
  }

  public static CaseSharingPolicies toApi(ResolvedPolicyValue<CaseSharingMode> mode) {
    return new CaseSharingPolicies()
        .mode(
            new CaseSharingModePermissionPolicy(
                    mode.value(),
                    com.vi.tenantservice.api.model.PermissionPolicyMode.valueOf(mode.mode().name()))
                .inherited(mode.inherited()));
  }

  public static CaseSharingPolicies toApi(PolicyValue<CaseSharingMode> mode) {
    return new CaseSharingPolicies()
        .mode(
            new CaseSharingModePermissionPolicy(
                mode.value(),
                com.vi.tenantservice.api.model.PermissionPolicyMode.valueOf(mode.mode().name())));
  }

  public static PolicyValue<CaseSharingMode> toDomain(CaseSharingModePermissionPolicy policy) {
    if (policy == null || policy.getValue() == null || policy.getMode() == null) {
      return null;
    }
    return new PolicyValue<>(
        policy.getValue(), PermissionPolicyMode.valueOf(policy.getMode().name()));
  }

  /**
   * The stored platform preset with the read-only {@code inherited} marker dropped; null stays null
   * so a platform that never chose keeps reading its legacy {@code caseHandoverEnabled}.
   */
  public static CaseSharingPolicies normalize(CaseSharingPolicies stored) {
    PolicyValue<CaseSharingMode> mode = toDomain(stored == null ? null : stored.getMode());
    return mode == null ? null : toApi(mode);
  }
}
