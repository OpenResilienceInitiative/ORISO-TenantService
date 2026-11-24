package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vi.tenantservice.api.model.TenantEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// Runs fully self-contained against an embedded H2 database: the MariaDB dialect
// and driver from the "testing" profile are overridden here, Hibernate builds the
// schema from the entities (ddl-auto=create-drop) and the single seed tenant is
// inserted before each test method.
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tenantrepositorytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Sql(
    value = "/database/TenantRepositoryTestData.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
class TenantRepositoryTest {

  @Test
  void searchRequiresEveryWordAcrossNameSubdomainAndOwnerEmailsBeforePaging() {
    var result =
        tenantRepository.searchTenantData(
            java.util.List.of("happy", "owner"),
            java.util.Map.of("owner", java.util.Set.of(1L)),
            org.springframework.data.domain.PageRequest.of(0, 1));
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
    assertThat(
            tenantRepository
                .searchTenantData(
                    java.util.List.of("happy", "absent"),
                    java.util.Map.of(),
                    org.springframework.data.domain.PageRequest.of(0, 1))
                .getTotalElements())
        .isZero();
  }

  @Test
  void existingSearchEntryPointMatchesSubdomain() {
    var tenant = tenantRepository.findById(1L).orElseThrow();
    tenant.setSubdomain("unique-subdomain");
    tenantRepository.saveAndFlush(tenant);
    assertThat(
            tenantRepository
                .findAllTenantDataExceptTechnicalByInfix(
                    "unique-subdomain", org.springframework.data.domain.PageRequest.of(0, 1))
                .getTotalElements())
        .isEqualTo(1);
  }

  /** The tenant list reads the deletion date from the search projection (ORISO-Admin#1525). */
  @org.junit.jupiter.api.Test
  void searchCarriesTheDeleteDateOfATraegerInDeletion() {
    var tenant = tenantRepository.findById(1L).orElseThrow();
    tenant.setSubdomain("in-deletion");
    var marked = java.time.LocalDateTime.of(2026, 11, 24, 9, 30);
    tenant.setDeleteDate(marked);
    tenantRepository.saveAndFlush(tenant);

    var found =
        tenantRepository
            .findAllTenantDataExceptTechnicalByInfix(
                "in-deletion", org.springframework.data.domain.PageRequest.of(0, 1))
            .getContent();

    org.assertj.core.api.Assertions.assertThat(found).hasSize(1);
    org.assertj.core.api.Assertions.assertThat(found.get(0).getDeleteDate()).isEqualTo(marked);
    org.assertj.core.api.Assertions.assertThat(
            tenantRepository.findTenantDataById(1L).getDeleteDate())
        .isEqualTo(marked);
    org.assertj.core.api.Assertions.assertThat(tenantRepository.findIdsMarkedForDeletion())
        .containsExactly(1L);
  }

  private static final long EXISTING_ID = 1L;

  @Autowired private TenantRepository tenantRepository;

  @Test
  void findById_Should_findTenantById() {
    // given, when
    Optional<TenantEntity> tenantEntity = tenantRepository.findById(1L);
    // then
    assertThat(tenantEntity).isPresent();
  }

  @Test
  void delete_Should_removeTenantById() {
    // when
    tenantRepository.deleteById(EXISTING_ID);
    // then
    Optional<TenantEntity> tenantEntity = tenantRepository.findById(1L);
    assertThat(tenantEntity).isNotPresent();
  }

  @Test
  void save_Should_saveTenant() {
    // given
    TenantEntity entity = new TenantEntity();
    entity.setName("new tenant");
    entity.setSubdomain("a subdomain");
    entity.setCreateDate(LocalDateTime.now());

    // when
    TenantEntity saved = tenantRepository.save(entity);
    tenantRepository.flush();

    Optional<TenantEntity> tenantEntity = tenantRepository.findById(saved.getId());
    // then
    assertThat(tenantEntity).isPresent();
    assertThat(tenantEntity).contains(entity);
  }

  @Test
  void save_Should_updateTenant() {
    // given
    TenantEntity tenant = tenantRepository.findById(EXISTING_ID).get();

    // when
    tenant.setName("updated name");
    tenant.setSubdomain("updated subdomain");
    tenantRepository.save(tenant);
    tenantRepository.flush();

    Optional<TenantEntity> tenantEntity = tenantRepository.findById(EXISTING_ID);
    // then
    assertThat(tenantEntity).isPresent();
    assertThat(tenantEntity.get().getName()).isEqualTo("updated name");
    assertThat(tenantEntity.get().getSubdomain()).isEqualTo("updated subdomain");
  }

  @Test
  void findBySubdomain_Should_FindTenantBySubdomain() {
    // when
    var tenant = tenantRepository.findBySubdomain("happylife");

    // then
    assertNotNull(tenant);
  }

  @Test
  void findTenantDataById_Should_NotRequireDpaColumnsInProjection() {
    // when
    var tenant = tenantRepository.findTenantDataById(EXISTING_ID);

    // then
    assertNotNull(tenant);
    assertThat(tenant.getId()).isEqualTo(EXISTING_ID);
    assertThat(tenant.getName()).isEqualTo("Happylife Gmbh");
    assertThat(tenant.getSubdomain()).isEqualTo("happylife");
  }

  @Test
  void findAllTenantData_Should_NotRequireDpaColumnsInProjection() {
    // when
    var tenants = tenantRepository.findAllTenantData();

    // then
    assertThat(tenants.size()).isEqualTo(1);
    assertThat(tenants.get(0).getId()).isEqualTo(EXISTING_ID);
  }

  @Test
  void findTenantDataById_Should_carryLegalNameAndContact() {
    TenantEntity entity = tenantRepository.findById(EXISTING_ID).orElseThrow();
    entity.setLegalName("Caritasverband für die Erzdiözese Musterstadt e.V.");
    entity.setContactEmail("beratung@caritas-musterstadt.de");
    entity.setContactPhone("+49 761 200-0");
    tenantRepository.saveAndFlush(entity);

    var view = tenantRepository.findTenantDataById(EXISTING_ID);

    assertThat(view.getLegalName()).isEqualTo("Caritasverband für die Erzdiözese Musterstadt e.V.");
    assertThat(view.getContactEmail()).isEqualTo("beratung@caritas-musterstadt.de");
    assertThat(view.getContactPhone()).isEqualTo("+49 761 200-0");
    assertThat(
            tenantRepository.findAllTenantData().stream()
                .filter(tenant -> tenant.getId() == EXISTING_ID)
                .findFirst()
                .orElseThrow()
                .getLegalName())
        .isEqualTo("Caritasverband für die Erzdiözese Musterstadt e.V.");
  }

  /** Two overlapping deletions: only the first sets the date (ORISO-Admin#1525). */
  @Test
  void markForDeletion_Should_setTheDateOnlyOnce_And_switchTheTraegerOff() {
    var first = LocalDateTime.of(2026, 11, 24, 9, 0);

    org.assertj.core.api.Assertions.assertThat(tenantRepository.markForDeletion(EXISTING_ID, first))
        .isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            tenantRepository.markForDeletion(EXISTING_ID, first.plusMinutes(5)))
        .isZero();

    var tenant = tenantRepository.findById(EXISTING_ID).orElseThrow();
    assertThat(tenant.getDeleteDate()).isEqualTo(first);
    assertThat(tenant.getActive()).isEqualTo(false);
  }

  /** A Träger in deletion cannot be switched on again (ORISO-Admin#1525). */
  @Test
  void updateOperationalStatus_Should_leaveATraegerInDeletionOff() {
    var first = LocalDateTime.of(2026, 11, 24, 9, 0);
    tenantRepository.markForDeletion(EXISTING_ID, first);

    org.assertj.core.api.Assertions.assertThat(
            tenantRepository.updateOperationalStatus(EXISTING_ID, true, first.plusHours(1)))
        .isZero();

    assertThat(tenantRepository.findById(EXISTING_ID).orElseThrow().getActive()).isEqualTo(false);
  }

  /** A repeated deletion switches the Träger off again and keeps its first date. */
  @Test
  void deactivateMarkedForDeletion_Should_switchOffWithoutTouchingTheFirstDate() {
    var first = LocalDateTime.of(2026, 11, 24, 9, 0);
    var tenant = tenantRepository.findById(EXISTING_ID).orElseThrow();
    tenant.setDeleteDate(first);
    tenant.setActive(true);
    tenantRepository.saveAndFlush(tenant);

    org.assertj.core.api.Assertions.assertThat(
            tenantRepository.deactivateMarkedForDeletion(EXISTING_ID, first.plusDays(1)))
        .isEqualTo(1);

    var reloaded = tenantRepository.findById(EXISTING_ID).orElseThrow();
    assertThat(reloaded.getActive()).isEqualTo(false);
    assertThat(reloaded.getDeleteDate()).isEqualTo(first);
  }

  /** Never touches a Träger that is not in deletion. */
  @Test
  void deactivateMarkedForDeletion_Should_ignoreATraegerNotInDeletion() {
    org.assertj.core.api.Assertions.assertThat(
            tenantRepository.deactivateMarkedForDeletion(EXISTING_ID, LocalDateTime.now()))
        .isZero();
  }
}
