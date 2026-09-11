package com.vi.tenantservice.api.exception.httpresponse;

public enum HttpStatusExceptionReason {
  SUBDOMAIN_NOT_UNIQUE,
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
}
