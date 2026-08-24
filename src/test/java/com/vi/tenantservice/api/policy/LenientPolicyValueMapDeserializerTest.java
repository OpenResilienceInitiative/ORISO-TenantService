package com.vi.tenantservice.api.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

/**
 * Edge cases of the shared helper that the service-level tests cannot reach: the helper is also
 * called directly by TenantPermissionPolicyService with whatever readTree produced.
 */
class LenientPolicyValueMapDeserializerTest {

  @Test
  void readIntelligibleEntries_Should_returnEmptyMap_When_theNodeIsNull() {
    assertThat(LenientPolicyValueMapDeserializer.readIntelligibleEntries(null)).isEmpty();
  }

  @Test
  void readIntelligibleEntries_Should_returnEmptyMap_When_theNodeIsTheJsonNullLiteral() {
    assertThat(LenientPolicyValueMapDeserializer.readIntelligibleEntries(NullNode.getInstance()))
        .isEmpty();
  }
}
