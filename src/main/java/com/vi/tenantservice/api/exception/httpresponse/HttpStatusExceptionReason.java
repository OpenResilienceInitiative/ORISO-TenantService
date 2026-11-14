package com.vi.tenantservice.api.exception.httpresponse;

public enum HttpStatusExceptionReason {
  SUBDOMAIN_NOT_UNIQUE,

  /**
   * The submitted subdomain is not a usable routing key. The format rule used to live only in the
   * admin panel's tenant form, so every other writer stored the value unchecked
   * (ORISO-Admin/src/utils/isValidSubdomain.ts).
   */
  SUBDOMAIN_INVALID,

  NOT_ALLOWED_TO_CHANGE_SUBDOMAIN,
  NOT_ALLOWED_TO_CHANGE_LICENSING,
  NOT_ALLOWED_TO_CHANGE_SETTING,

  /**
   * The platform admin switched {@code allowedPermissionToggles.appearance} off for Träger admins,
   * so this tenant admin may not change logo, favicon or colours. Until now the restriction lived
   * only in the admin panel's UI (ORISO-Admin#688, TenantService#174).
   */
  NOT_ALLOWED_TO_CHANGE_APPEARANCE,

  NOT_ALLOWED_TO_CHANGE_LEGAL_CONTENT,

  LANGUAGE_KEY_NOT_VALID,

  ID_MUST_BE_NULL_WHEN_CREATING_TENANT,
  SUBDOMAIN_IN_REQUEST_BODY_NOT_EQUAL_TO_SUBDOMAIN_IN_URL,

  LANGUAGE_MUST_BE_NON_NULL_AND_PROPER_LENGTH,

  /**
   * An SMTP password was submitted but the server has no encryption secret, so it would have been
   * stored in plain text. Refused instead (#346). Answers 409 with this reason in {@code X-Reason}.
   */
  SMTP_PASSWORD_ENCRYPTION_NOT_CONFIGURED,

  /**
   * The Träger still has agencies, accounts or open invitations. Deleting it would leave them
   * pointing at a Träger that no longer exists, where the admin interface can no longer delete them
   * (ORISO-UserService#1466). Answers 409; the body says how many of each kind are left.
   */
  TENANT_HAS_DEPENDENTS,
}
