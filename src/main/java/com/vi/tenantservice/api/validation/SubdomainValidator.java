package com.vi.tenantservice.api.validation;

import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Server-side enforcement of the tenant subdomain format.
 *
 * <p>The subdomain is the multitenancy routing key: {@code SubdomainTenantResolver} resolves a
 * tenant from the host name, so a value that cannot appear in a host name is not a cosmetic
 * problem. Until now the format was checked only by the admin panel's tenant form ({@code
 * ORISO-Admin/src/utils/isValidSubdomain.ts}); every other writer — another admin screen, a tenant
 * admin, plain {@code curl} — stored whatever it sent. The pattern here is that form's rule,
 * character for character, so the two cannot drift apart silently.
 *
 * <p>Blank is deliberately not a format error, exactly as in the client rule: emptiness is a
 * separate concern, and the service itself writes an empty subdomain on purpose for non-main
 * tenants under single-domain multitenancy ({@code
 * TenantService#overrideSubdomainIfNeededForSingleDomainMultitenancy}).
 */
@Component
public class SubdomainValidator {

  /** Mirrors {@code SUBDOMAIN_PATTERN} in {@code isValidSubdomain.ts}. */
  static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");

  /** A new tenant has no history to protect, so its subdomain is always checked. */
  public void validateOnCreate(String subdomain) {
    validateFormat(subdomain);
  }

  /**
   * Checks the submitted subdomain only when it actually differs from the stored one.
   *
   * <p>The column never had a format constraint, so rows written before this rule may violate it.
   * The admin panel round-trips the whole tenant on every save, so checking the record instead of
   * the change would answer 400 for every edit made to such a tenant — including the edit that
   * fixes the subdomain, which travels in the same body as the rest. Scoping the check to the field
   * being changed keeps those records editable while making the bad value unreachable for new
   * writes. Same shape as the existing {@code NOT_ALLOWED_TO_CHANGE_SUBDOMAIN} guard in {@code
   * TenantFacadeAuthorisationService}.
   */
  public void validateOnUpdate(String submittedSubdomain, String storedSubdomain) {
    if (Objects.equals(submittedSubdomain, storedSubdomain)) {
      return;
    }
    validateFormat(submittedSubdomain);
  }

  private void validateFormat(String subdomain) {
    if (subdomain == null || subdomain.isBlank()) {
      return;
    }
    if (!SUBDOMAIN_PATTERN.matcher(subdomain).matches()) {
      throw new TenantValidationException(HttpStatusExceptionReason.SUBDOMAIN_INVALID);
    }
  }
}
