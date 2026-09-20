package com.vi.tenantservice.api.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The cases mirror {@code ORISO-Admin/src/utils/isValidSubdomain.test.ts} one for one, so a
 * divergence between the browser rule and the server rule shows up as a failing test rather than as
 * data that only one of the two accepts.
 */
class SubdomainValidatorTest {

  private final SubdomainValidator validator = new SubdomainValidator();

  @ParameterizedTest
  @ValueSource(strings = {"caritas", "caritas-online", "a", "tenant1", "a1b2-c3", "123", "x-y-z"})
  void validateOnCreate_Should_accept_validSubdomains(String subdomain) {
    assertDoesNotThrow(() -> validator.validateOnCreate(subdomain));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Caritas", "-caritas", "caritas-", "cari tas", "cari_tas", "cari.tas", "cäritas"})
  void validateOnCreate_Should_reject_invalidSubdomains(String subdomain) {
    var exception =
        assertThrows(TenantValidationException.class, () -> validator.validateOnCreate(subdomain));

    assertEquals(HttpStatusExceptionReason.SUBDOMAIN_INVALID, exception.getStatusExceptionReason());
    assertEquals("SUBDOMAIN_INVALID", exception.getCustomHttpHeaders().getFirst("X-Reason"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void validateOnCreate_Should_accept_blankSubdomains(String subdomain) {
    // Emptiness is a separate concern: the service itself stores an empty subdomain for non-main
    // tenants under single-domain multitenancy.
    assertDoesNotThrow(() -> validator.validateOnCreate(subdomain));
  }

  @Test
  void validateOnUpdate_Should_accept_aStoredInvalidSubdomainThatIsNotBeingChanged() {
    assertDoesNotThrow(() -> validator.validateOnUpdate("Traeger_Nord", "Traeger_Nord"));
  }

  @Test
  void validateOnUpdate_Should_reject_aChangeToAnInvalidSubdomain() {
    assertThrows(
        TenantValidationException.class,
        () -> validator.validateOnUpdate("Traeger_Nord", "traeger-nord"));
  }

  @Test
  void validateOnUpdate_Should_accept_aChangeThatRepairsAnInvalidStoredSubdomain() {
    assertDoesNotThrow(() -> validator.validateOnUpdate("traeger-nord", "Traeger_Nord"));
  }

  @Test
  void validateOnUpdate_Should_accept_anOmittedSubdomain() {
    assertDoesNotThrow(() -> validator.validateOnUpdate(null, "Traeger_Nord"));
  }
}
