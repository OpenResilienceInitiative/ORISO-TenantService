package com.vi.tenantservice.api.policy;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CalendarPolicies;
import com.vi.tenantservice.api.model.CalendarVisibilityScope;
import com.vi.tenantservice.api.model.CalendarVisibilityScopePolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;

/**
 * Platform defaults and normalisation for the calendar policies (ADR-032, #365). Both default to
 * the most restrictive value as a recommendation: a Traeger that never touched them shows
 * colleagues' calendars only within the counsellor's own counselling centre and hides the advice
 * seeker's username from colleagues.
 */
public final class CalendarPolicyRules {

  public static final CalendarVisibilityScope DEFAULT_VISIBILITY_SCOPE =
      CalendarVisibilityScope.AGENCY;
  public static final boolean DEFAULT_SHOW_ASKER_USERNAMES = false;

  private CalendarPolicyRules() {}

  public static CalendarPolicies defaults() {
    return new CalendarPolicies()
        .visibilityScope(defaultVisibilityScope())
        .showAskerUsernames(defaultShowAskerUsernames());
  }

  /**
   * The stored platform preset with every missing part filled from {@link #defaults()}. Each policy
   * is normalised on its own, so a preset that sets only one of them keeps it. The read-only {@code
   * inherited} marker never belongs to a stored preset and is dropped.
   */
  public static CalendarPolicies normalize(CalendarPolicies stored) {
    CalendarVisibilityScopePolicy scope = stored == null ? null : stored.getVisibilityScope();
    BooleanPermissionPolicy usernames = stored == null ? null : stored.getShowAskerUsernames();
    return new CalendarPolicies()
        .visibilityScope(
            scope == null || scope.getValue() == null || scope.getMode() == null
                ? defaultVisibilityScope()
                : new CalendarVisibilityScopePolicy(scope.getValue(), scope.getMode()))
        .showAskerUsernames(
            usernames == null || usernames.getValue() == null || usernames.getMode() == null
                ? defaultShowAskerUsernames()
                : new BooleanPermissionPolicy(usernames.getValue(), usernames.getMode()));
  }

  private static CalendarVisibilityScopePolicy defaultVisibilityScope() {
    return new CalendarVisibilityScopePolicy(
        DEFAULT_VISIBILITY_SCOPE, PermissionPolicyMode.SUGGESTED);
  }

  private static BooleanPermissionPolicy defaultShowAskerUsernames() {
    return new BooleanPermissionPolicy(
        DEFAULT_SHOW_ASKER_USERNAMES, PermissionPolicyMode.SUGGESTED);
  }
}
