package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Self-contained persistence test for the DPA version-history entity: proves the entity↔column
 * mapping builds against a real (H2) schema and that the newest-first ordering query works. Mirrors
 * {@link TenantDpaSignatureRepositoryTest}'s H2/create-drop override.
 */
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ExtendWith(SpringExtension.class)
@DataJpaTest
class TenantDpaVersionRepositoryTest {

  @Autowired private TenantDpaVersionRepository versionRepository;

  @Test
  void save_Should_persistSnapshot_andApplyCreateDateDefault() {
    var activation = LocalDateTime.now();
    var saved =
        versionRepository.saveAndFlush(
            TenantDpaVersionEntity.builder()
                .tenantId(1L)
                .content("{\"de\":\"v1\"}")
                .activationDate(activation)
                .build()); // createDate left null -> @PrePersist fills it

    var reloaded = versionRepository.findById(saved.getId()).orElseThrow();
    assertThat(reloaded.getTenantId()).isEqualTo(1L);
    assertThat(reloaded.getContent()).isEqualTo("{\"de\":\"v1\"}");
    assertThat(reloaded.getActivationDate()).isEqualTo(activation);
    assertThat(reloaded.getCreateDate()).isNotNull();
  }

  @Test
  void findByTenantIdOrderByActivationDateDesc_Should_returnNewestFirst_andScopeByTenant() {
    var base = LocalDateTime.now();
    versionRepository.save(version(1L, base.minusDays(2), "oldest"));
    versionRepository.save(version(1L, base, "newest"));
    versionRepository.save(version(1L, base.minusDays(1), "middle"));
    versionRepository.save(version(2L, base, "other-tenant")); // must NOT leak
    versionRepository.flush();

    List<TenantDpaVersionEntity> found =
        versionRepository.findByTenantIdOrderByActivationDateDesc(1L);

    assertThat(found).hasSize(3);
    assertThat(found)
        .extracting(TenantDpaVersionEntity::getContent)
        .containsExactly("newest", "middle", "oldest");
  }

  private TenantDpaVersionEntity version(Long tenantId, LocalDateTime activation, String content) {
    return TenantDpaVersionEntity.builder()
        .tenantId(tenantId)
        .activationDate(activation)
        .content(content)
        .createDate(LocalDateTime.now())
        .build();
  }
}
