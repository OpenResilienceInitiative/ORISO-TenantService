package com.vi.tenantservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LiquibaseExecutedChangesetImmutabilityTest {

  private static final Map<String, String> EXECUTED_RESOURCE_SHA_256 =
      Map.of(
          "db/changelog/changeset/0030_tenant_permission_policies/0030-changeSet.xml",
          "19afe54120dcaa7b09a2ca07cc4d770221e45a53e3ae269ebb251f66ac2ed77b",
          "db/changelog/changeset/0030_tenant_permission_policies/createTenantPermissionPolicies.sql",
          "ef024c2b3d8fc734123fc975bf43e9b23e3faba2e956e9d229b1ee90a4d346af",
          "db/changelog/changeset/0031_settings_optimistic_lock/0031-changeSet.xml",
          "056a6e512e575702b7e07be61e03b47cdfbab571ace90d8040b61baef43e5b7d");

  @Test
  void changesetsAlreadyExecutedOnPreDevMustRemainImmutable() throws Exception {
    for (Map.Entry<String, String> resource : EXECUTED_RESOURCE_SHA_256.entrySet()) {
      assertThat(sha256(resource.getKey()))
          .as("immutable executed Liquibase resource %s", resource.getKey())
          .isEqualTo(resource.getValue());
    }
  }

  private String sha256(String resource) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertThat(input).as("classpath resource %s", resource).isNotNull();
      return HexFormat.of().formatHex(digest.digest(input.readAllBytes()));
    }
  }
}
