package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Self-contained persistence test for the AVV / Data Processing Agreement model. The module's other
 * {@code @DataJpaTest} repository tests assume an externally provisioned schema (the configured
 * MariaDB dialect produces DDL that an empty embedded H2 cannot build), so this test overrides the
 * dialect to H2 and lets Hibernate create the schema from the entities — making it runnable and
 * meaningful in a bare local build.
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
class TenantDpaSignatureRepositoryTest {

  @Autowired private TenantDpaSignatureRepository signatureRepository;
  @Autowired private TenantRepository tenantRepository;

  @Test
  void tenantEntity_Should_persistDataProcessingAgreementFields() {
    // given
    TenantEntity entity = new TenantEntity();
    entity.setName("avv tenant");
    entity.setSubdomain("avv-subdomain");
    entity.setCreateDate(LocalDateTime.now());
    entity.setContentDataProcessingAgreement("<p>Auftragsverarbeitungsvertrag</p>");
    entity.setContentDataProcessingAgreementActivationDate(LocalDateTime.now());

    // when
    TenantEntity saved = tenantRepository.save(entity);
    tenantRepository.flush();

    // then
    Optional<TenantEntity> reloaded = tenantRepository.findById(saved.getId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getContentDataProcessingAgreement())
        .isEqualTo("<p>Auftragsverarbeitungsvertrag</p>");
    assertThat(reloaded.get().getContentDataProcessingAgreementActivationDate()).isNotNull();
  }

  @Test
  void save_Should_persistSignatureAndFindByTenantId() {
    // given
    var now = LocalDateTime.now();
    TenantDpaSignatureEntity signature =
        TenantDpaSignatureEntity.builder()
            .tenantId(1L)
            .dpaVersion(now.minusDays(1))
            .signerName("Erika Mustermann")
            .signerPosition("Geschäftsführerin")
            .signerIsMember(false)
            .language("de")
            .status(DpaSignatureStatus.SIGNED)
            .signedAt(now)
            .createDate(now)
            .build();

    // when
    signatureRepository.save(signature);
    signatureRepository.flush();

    // then
    List<TenantDpaSignatureEntity> found = signatureRepository.findByTenantId(1L);
    assertThat(found).hasSize(1);
    var saved = found.get(0);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getSignerName()).isEqualTo("Erika Mustermann");
    assertThat(saved.getSignerPosition()).isEqualTo("Geschäftsführerin");
    assertThat(saved.getSignerIsMember()).isFalse();
    assertThat(saved.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(saved.getLanguage()).isEqualTo("de");
  }

  @Test
  void findByTenantIdAndStatus_Should_filterByStatus() {
    // given
    var now = LocalDateTime.now();
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(2L)
            .status(DpaSignatureStatus.SIGNED)
            .signerName("Signed Person")
            .dpaVersion(now)
            .createDate(now)
            .build());
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(2L)
            .status(DpaSignatureStatus.DENIED)
            .signerName("Denied Person")
            .dpaVersion(now)
            .createDate(now)
            .build());
    signatureRepository.flush();

    // when
    var signed = signatureRepository.findByTenantIdAndStatus(2L, DpaSignatureStatus.SIGNED);

    // then
    assertThat(signed).hasSize(1);
    assertThat(signed.get(0).getSignerName()).isEqualTo("Signed Person");
  }

  @Test
  void consumeSignToken_Should_signExactlyOnce_andRejectReuse() {
    // given a PENDING row carrying a token
    var now = LocalDateTime.now();
    var pending =
        signatureRepository.saveAndFlush(
            TenantDpaSignatureEntity.builder()
                .tenantId(3L)
                .status(DpaSignatureStatus.PENDING)
                .tokenHash("HASH")
                .tokenExpiresAt(now.plusDays(1))
                .createDate(now)
                .build());

    // when the first consume wins
    int first = signatureRepository.consumeSignToken("HASH", "Erika", "GF", false, "de", now);
    // and a second consume of the same token affects nothing (single-use)
    int second = signatureRepository.consumeSignToken("HASH", "Mallory", "X", true, "en", now);
    signatureRepository.flush();

    // then
    assertThat(first).isEqualTo(1);
    assertThat(second).isZero();
    var reloaded = signatureRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(DpaSignatureStatus.SIGNED);
    assertThat(reloaded.getSignerName()).isEqualTo("Erika"); // not overwritten by the 2nd attempt
    assertThat(reloaded.getTokenHash()).isNull();
    assertThat(signatureRepository.findByTokenHashAndStatus("HASH", DpaSignatureStatus.PENDING))
        .isEmpty();
  }
}
