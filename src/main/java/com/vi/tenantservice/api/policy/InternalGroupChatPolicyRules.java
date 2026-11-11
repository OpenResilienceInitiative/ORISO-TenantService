package com.vi.tenantservice.api.policy;

import com.vi.tenantservice.api.model.InternalGroupChatPolicies;
import com.vi.tenantservice.api.model.InternalGroupChatRecipientScope;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.RecipientScopePermissionPolicy;

/**
 * Platform defaults and normalisation for the internal group chat policies (#319). The recipient
 * scope defaults to the most restrictive value, the counsellor's own counselling centre, as a
 * recommendation: a Traeger that never touched it behaves as "counselling centre only".
 */
public final class InternalGroupChatPolicyRules {

  public static final InternalGroupChatRecipientScope DEFAULT_RECIPIENT_SCOPE =
      InternalGroupChatRecipientScope.AGENCY;

  private InternalGroupChatPolicyRules() {}

  public static InternalGroupChatPolicies defaults() {
    return new InternalGroupChatPolicies()
        .recipientScope(
            new RecipientScopePermissionPolicy(
                DEFAULT_RECIPIENT_SCOPE, PermissionPolicyMode.SUGGESTED));
  }

  /**
   * The stored platform preset with every missing part filled from {@link #defaults()}. The
   * read-only {@code inherited} marker never belongs to a stored preset and is dropped.
   */
  public static InternalGroupChatPolicies normalize(InternalGroupChatPolicies stored) {
    RecipientScopePermissionPolicy scope = stored == null ? null : stored.getRecipientScope();
    if (scope == null || scope.getValue() == null || scope.getMode() == null) {
      return defaults();
    }
    return new InternalGroupChatPolicies()
        .recipientScope(new RecipientScopePermissionPolicy(scope.getValue(), scope.getMode()));
  }
}
