package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantRestrictedData;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
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

  /**
   * Six constructor projections feed the Admin and the public reads. They are positional, so two
   * adjacent String columns swapped in one of them would still compile and start up; only distinct
   * values per device show it.
   */
  @ParameterizedTest
  @MethodSource("projectionsOfTheTenantTheming")
  void projections_should_carryTheClosingAnimationOfDesktopAndMobileSeparately(
      Function<TenantRepository, TenantRestrictedData> projection) {
    // given
    TenantEntity entity = tenantRepository.findById(EXISTING_ID).orElseThrow();
    entity.setThemingClosingAnimationDesktop("SLIDE_REVEAL");
    entity.setThemingClosingAnimationMobile("CURTAIN");
    tenantRepository.saveAndFlush(entity);

    // when
    TenantRestrictedData view = projection.apply(tenantRepository);

    // then
    assertThat(view.getThemingClosingAnimationDesktop()).isEqualTo("SLIDE_REVEAL");
    assertThat(view.getThemingClosingAnimationMobile()).isEqualTo("CURTAIN");
  }

  @ParameterizedTest
  @MethodSource("projectionsOfTheTenantTheming")
  void projections_should_keepAnUnconfiguredClosingAnimationNull(
      Function<TenantRepository, TenantRestrictedData> projection) {
    TenantRestrictedData view = projection.apply(tenantRepository);

    assertThat(view.getThemingClosingAnimationDesktop()).isNull();
    assertThat(view.getThemingClosingAnimationMobile()).isNull();
  }

  static Stream<Arguments> projectionsOfTheTenantTheming() {
    return Stream.of(
        projection("findTenantDataById", repository -> repository.findTenantDataById(EXISTING_ID)),
        projection("findAllTenantData", repository -> repository.findAllTenantData().get(0)),
        projection(
            "findAllTenantDataExceptTechnicalByInfix",
            repository ->
                repository
                    .findAllTenantDataExceptTechnicalByInfix("*", PageRequest.of(0, 10))
                    .getContent()
                    .get(0)),
        projection(
            "findRestrictedDataBySubdomain",
            repository -> repository.findRestrictedDataBySubdomain("happylife")),
        projection(
            "findRestrictedDataById", repository -> repository.findRestrictedDataById(EXISTING_ID)),
        projection(
            "findRestrictedDataByIdIn",
            repository -> repository.findRestrictedDataByIdIn(Set.of(EXISTING_ID)).get(0)));
  }

  private static Arguments projection(
      String name, Function<TenantRepository, TenantRestrictedData> query) {
    return Arguments.of(Named.of(name, query));
  }
}
