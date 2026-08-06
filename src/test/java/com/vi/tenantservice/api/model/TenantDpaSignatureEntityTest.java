package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Pure unit test for the @PrePersist NOT-NULL defaulting (no DB). */
class TenantDpaSignatureEntityTest {

  @Test
  void applyDefaults_Should_setPendingAndCreateDate_When_unset() {
    var entity = new TenantDpaSignatureEntity();

    entity.applyDefaults();

    assertThat(entity.getStatus()).isEqualTo(DpaSignatureStatus.PENDING);
    assertThat(entity.getCreateDate()).isNotNull();
  }

  @Test
  void applyDefaults_Should_notOverwrite_When_alreadySet() {
    var fixed = LocalDateTime.of(2020, 1, 1, 0, 0);
    var entity = new TenantDpaSignatureEntity();
    entity.setStatus(DpaSignatureStatus.SIGNED);
    entity.setCreateDate(fixed);

    entity.applyDefaults();

    assertThat(entity.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(entity.getCreateDate()).isEqualTo(fixed);
  }
}
