package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vi.tenantservice.api.model.TenantEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
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
