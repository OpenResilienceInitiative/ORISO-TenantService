package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

/** Real H2/JPA INSERT and native row checks, separate from the actual native bearer proof. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:bootstrap-authority;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.liquibase.enabled=false",
    "feature.multitenancy.with.single.domain.enabled=false",
    "TASK_IDENTITY_MODEL=FIVE_ACTORS",
    "TASK_IDENTITY_REQUIRED_TASKS=CONFIG_WIZARD",
    "ORISO_TASK_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@ActiveProfiles("testing")
@Sql(statements = {
    "ALTER TABLE tenant ADD COLUMN IF NOT EXISTS bootstrap_attempt_id VARCHAR(36)",
    "ALTER TABLE tenant ADD CONSTRAINT IF NOT EXISTS uq_tenant_bootstrap_attempt UNIQUE (bootstrap_attempt_id)",
    "DELETE FROM tenant_id_reservation",
    "DELETE FROM tenant"
})
class TenantBootstrapCreationIT {
  @Autowired private TenantBootstrapCreation creation;
  @Autowired private TenantIdAllocationService allocation;
  @Autowired private TenantRepository tenants;
  @Autowired private JdbcTemplate sql;
  @MockitoBean private TaskServiceIdentity identity;

  @BeforeEach
  void caller() {
    when(identity.current("CONFIG_WIZARD")).thenReturn(true);
    var jwt = Jwt.withTokenValue("synthetic-no-wire-token").header("alg", "RS256")
        .issuer("https://synthetic.identity/realms/context").subject("setup-subject")
        .claim("azp", "backend-setup").expiresAt(Instant.now().plusSeconds(120)).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @AfterEach
  void clearCaller() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void onlyTheNewCommittedInsertCarriesALiveGrantAndItClosesAfterTheCallback() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var retained = new AtomicReference<TenantBootstrapCreation.Grant>();
    long id = creation.create(input(9157L), token, (created, grant) -> {
      assertThat(created.getId()).isEqualTo(9157L);
      assertThat(grant.tenantId()).isEqualTo(9157L);
      assertThat(sql.queryForObject("SELECT COUNT(*) FROM tenant WHERE id=9157 AND bootstrap_attempt_id IS NOT NULL", Integer.class)).isEqualTo(1);
      retained.set(grant);
      return grant.tenantId();
    });
    assertThat(id).isEqualTo(9157L);
    assertThat(allocation.getStatus(id).name()).isEqualTo("ASSIGNED");
    assertThatThrownBy(() -> retained.get().tenantId()).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void anExistingTenantAndContradictoryReservationCannotBeMergedIntoANewCreation() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var existing = input(9157L);
    existing.setName("original");
    existing.setCreateDate(LocalDateTime.now());
    existing.setUpdateDate(LocalDateTime.now());
    tenants.saveAndFlush(existing);
    assertThatThrownBy(() -> creation.create(input(9157L), token, (created, grant) -> grant.tenantId()))
        .isInstanceOf(TenantIdAllocationConflictException.class);
    assertThat(tenants.findById(9157L).orElseThrow().getName()).isEqualTo("original");
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("RESERVED");
    assertThat(sql.queryForObject("SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class)).isNull();
  }

  @Test
  void localFailureBeforeSendAtomicallyRemovesOnlyTheOwnedInsertAndRestoresItsReservation() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    assertThatThrownBy(() -> creation.create(input(9157L), token, (created, grant) -> {
      throw new IllegalStateException("finite local pre-send failure");
    })).isInstanceOf(IllegalStateException.class);
    assertThat(tenants.existsById(9157L)).isFalse();
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("RESERVED");
    assertThat(sql.queryForObject("SELECT token FROM tenant_id_reservation WHERE tenant_id=9157", String.class)).isEqualTo(token);
  }

  @Test
  void nativeScalarMutationRevokesTheGrantAndCannotAuthorizeCompensation() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    assertThatThrownBy(() -> creation.create(input(9157L), token, (created, grant) -> {
      sql.update("UPDATE tenant SET name='changed' WHERE id=9157");
      return grant.tenantId();
    })).isInstanceOf(AccessDeniedException.class);
    assertThat(tenants.findById(9157L).orElseThrow().getName()).isEqualTo("changed");
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("ASSIGNED");
  }

  private static TenantEntity input(Long id) {
    return TenantEntity.builder().id(id).name("new bootstrap").subdomain("bootstrap-owned").build();
  }
}
