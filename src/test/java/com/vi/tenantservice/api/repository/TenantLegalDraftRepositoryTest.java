package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ExtendWith(SpringExtension.class)
@DataJpaTest
class TenantLegalDraftRepositoryTest {

  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantLegalDraftRepository draftRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void privacyAndImprintDrafts_Should_roundTripSeparatelyWithoutChangingPublishedContent() {
    TenantEntity tenant = tenant(7L, "Tenant", "tenant-7");
    tenant.setContentPrivacy("{\"de\":\"published tenant privacy\"}");
    tenant.setContentImpressum("{\"de\":\"published tenant imprint\"}");
    LocalDateTime publishedPrivacyActivation = LocalDateTime.of(2026, 9, 1, 9, 30);
    tenant.setContentPrivacyActivationDate(publishedPrivacyActivation);
    tenantRepository.saveAndFlush(tenant);

    TenantLegalDraftEntity platformPrivacy =
        draftRepository.saveAndFlush(draft(0L, TenantLegalDraftKind.PRIVACY, "platform draft"));
    TenantLegalDraftEntity tenantPrivacyDraft =
        draft(7L, TenantLegalDraftKind.PRIVACY, "tenant privacy");
    tenantPrivacyDraft.setPrivacyConsent("{\"de\":\"Ich habe die {{legal_links}} gelesen.\"}");
    TenantLegalDraftEntity tenantPrivacy = draftRepository.saveAndFlush(tenantPrivacyDraft);
    TenantLegalDraftEntity tenantImprint =
        draftRepository.saveAndFlush(draft(7L, TenantLegalDraftKind.IMPRINT, "tenant imprint"));
    entityManager.clear();

    assertThat(
            draftRepository
                .findByOwnerKeyAndKind(0L, TenantLegalDraftKind.PRIVACY)
                .orElseThrow()
                .getContent())
        .isEqualTo("platform draft");
    assertThat(
            draftRepository
                .findByOwnerKeyAndKind(7L, TenantLegalDraftKind.PRIVACY)
                .orElseThrow()
                .getContent())
        .isEqualTo("tenant privacy");
    assertThat(
            draftRepository
                .findByOwnerKeyAndKind(7L, TenantLegalDraftKind.PRIVACY)
                .orElseThrow()
                .getPrivacyConsent())
        .isEqualTo("{\"de\":\"Ich habe die {{legal_links}} gelesen.\"}");
    assertThat(
            draftRepository
                .findByOwnerKeyAndKind(7L, TenantLegalDraftKind.IMPRINT)
                .orElseThrow()
                .getContent())
        .isEqualTo("tenant imprint");
    assertThat(platformPrivacy.getVersion()).isZero();
    assertThat(tenantPrivacy.getVersion()).isZero();
    assertThat(tenantImprint.getVersion()).isZero();

    TenantEntity reloadedTenant = tenantRepository.findById(7L).orElseThrow();
    assertThat(reloadedTenant.getContentPrivacy())
        .isEqualTo("{\"de\":\"published tenant privacy\"}");
    assertThat(reloadedTenant.getContentImpressum())
        .isEqualTo("{\"de\":\"published tenant imprint\"}");
    assertThat(reloadedTenant.getContentPrivacyActivationDate())
        .isEqualTo(publishedPrivacyActivation);
  }

  @Test
  void deleteThenRecreate_Should_useANewRowIdEvenThoughHibernateVersionRestarts() {
    tenantRepository.saveAndFlush(tenant(7L, "Tenant", "tenant-7"));
    TenantLegalDraftEntity deleted =
        draftRepository.saveAndFlush(draft(7L, TenantLegalDraftKind.PRIVACY, "first"));
    Long deletedId = deleted.getId();

    draftRepository.delete(deleted);
    draftRepository.flush();
    TenantLegalDraftEntity recreated =
        draftRepository.saveAndFlush(draft(7L, TenantLegalDraftKind.PRIVACY, "second"));

    assertThat(recreated.getId()).isNotEqualTo(deletedId);
    assertThat(recreated.getVersion()).isZero();
  }

  private TenantEntity tenant(long id, String name, String subdomain) {
    return TenantEntity.builder()
        .id(id)
        .name(name)
        .subdomain(subdomain)
        .createDate(LocalDateTime.now())
        .build();
  }

  private TenantLegalDraftEntity draft(long tenantId, TenantLegalDraftKind kind, String content) {
    return TenantLegalDraftEntity.builder()
        .ownerKey(tenantId)
        .tenantId(tenantId == 0L ? null : tenantId)
        .kind(kind)
        .content(content)
        .updateDate(LocalDateTime.now())
        .build();
  }
}
