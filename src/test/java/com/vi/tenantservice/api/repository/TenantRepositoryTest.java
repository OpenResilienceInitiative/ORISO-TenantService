package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vi.tenantservice.api.model.TenantEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
}
