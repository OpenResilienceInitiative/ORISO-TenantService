package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.controller.AccountPolicyReceiverFixture;
import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

/** Real H2/JPA INSERT and native row checks, separate from the actual native bearer proof. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:bootstrap-authority;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
      "spring.liquibase.enabled=false",
      "feature.multitenancy.with.single.domain.enabled=false",
      "TASK_IDENTITY_MODEL=FIVE_ACTORS",
      "TASK_IDENTITY_REQUIRED_TASKS=CONFIG_WIZARD",
      "ORISO_TASK_CONTEXT_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    })
@ActiveProfiles("testing")
@Sql(
    statements = {
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
  @Autowired private ApplicationContext application;
  @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
  @MockitoBean private TaskServiceIdentity identity;

  @BeforeEach
  void caller() {
    when(identity.current("CONFIG_WIZARD")).thenReturn(true);
    var jwt =
        Jwt.withTokenValue("synthetic-no-wire-token")
            .header("alg", "RS256")
            .issuer("https://synthetic.identity/realms/context")
            .subject("setup-subject")
            .claim("azp", "backend-setup")
            .expiresAt(Instant.now().plusSeconds(120))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @AfterEach
  void clearCaller() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void anUnrelatedBootstrapContextCannotInstallTheNativePolicyObserver() {
    assertThat(application.containsBean("policySqlObserver")).isFalse();
  }

  @Test
  void theIntendedPolicyFixtureCanExplicitlyRegisterItsNativeSqlObserver() {
    var factory = mock(EntityManagerFactory.class);
    var session = mock(SessionFactory.class);
    when(factory.unwrap(SessionFactory.class)).thenReturn(session);
    when(session.getStatistics()).thenReturn(mock(Statistics.class));
    try (var explicit = new AnnotationConfigApplicationContext()) {
      explicit.registerBean(EntityManagerFactory.class, () -> factory);
      explicit.register(AccountPolicyReceiverFixture.SqlObservation.class);
      explicit.refresh();
      assertThat(explicit.containsBean("policySqlObserver")).isTrue();
    }
  }

  @Test
  void onlyTheNewCommittedInsertCarriesALiveGrantAndItClosesAfterTheCallback() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var retained = new AtomicReference<TenantBootstrapCreation.Grant>();
    long id =
        creation.create(
            input(9157L),
            token,
            (created, grant) -> {
              assertThat(created.getId()).isEqualTo(9157L);
              assertThat(grant.tenantId()).isEqualTo(9157L);
              assertThat(
                      sql.queryForObject(
                          "SELECT COUNT(*) FROM tenant WHERE id=9157 AND bootstrap_attempt_id IS NOT NULL",
                          Integer.class))
                  .isEqualTo(1);
              retained.set(grant);
              return grant.tenantId();
            });
    assertThat(id).isEqualTo(9157L);
    assertThat(allocation.getStatus(id).name()).isEqualTo("ASSIGNED");
    assertThatThrownBy(() -> retained.get().tenantId()).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void everyProofForTheLiveCreationKeepsTheActualPrivateInsertAttemptNonce() throws Exception {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var dto =
        json.readValue(
            TenantBootstrapCreationIT.class.getResourceAsStream(
                "/settings/bootstrap-v2-complete-consulting-type.json"),
            com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeDTO.class);
    dto.setTenantId(9157);
    var signer =
        application.getBean(com.vi.tenantservice.config.security.TenantCreationContext.class);
    creation.create(
        input(9157L),
        token,
        (created, grant) -> {
          String first = signer.issue(grant, dto);
          String second = signer.issue(grant, dto);
          try {
            var firstClaims =
                json.readTree(java.util.Base64.getUrlDecoder().decode(first.split("\\.", -1)[0]));
            var secondClaims =
                json.readTree(java.util.Base64.getUrlDecoder().decode(second.split("\\.", -1)[0]));
            String marker =
                sql.queryForObject(
                    "SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class);
            assertThat(firstClaims.path("nonce").asText()).isEqualTo(marker.replace("-", ""));
            assertThat(secondClaims.path("nonce").asText())
                .isEqualTo(firstClaims.path("nonce").asText());
            assertThat(firstClaims.size()).isEqualTo(16);
            assertThat(secondClaims.size()).isEqualTo(16);
          } catch (java.io.IOException failure) {
            throw new AssertionError("Synthetic proof could not be inspected", failure);
          }
          return null;
        });
  }

  @Test
  void anExistingTenantAndContradictoryReservationCannotBeMergedIntoANewCreation() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var existing = input(9157L);
    existing.setName("original");
    existing.setCreateDate(LocalDateTime.now());
    existing.setUpdateDate(LocalDateTime.now());
    tenants.saveAndFlush(existing);
    assertThatThrownBy(
            () -> creation.create(input(9157L), token, (created, grant) -> grant.tenantId()))
        .isInstanceOf(TenantIdAllocationConflictException.class);
    assertThat(tenants.findById(9157L).orElseThrow().getName()).isEqualTo("original");
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("RESERVED");
    assertThat(
            sql.queryForObject(
                "SELECT bootstrap_attempt_id FROM tenant WHERE id=9157", String.class))
        .isNull();
  }

  @Test
  void localFailureBeforeSendAtomicallyRemovesOnlyTheOwnedInsertAndRestoresItsReservation() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    assertThatThrownBy(
            () ->
                creation.create(
                    input(9157L),
                    token,
                    (created, grant) -> {
                      throw new IllegalStateException("finite local pre-send failure");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(tenants.existsById(9157L)).isFalse();
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("RESERVED");
    assertThat(
            sql.queryForObject(
                "SELECT token FROM tenant_id_reservation WHERE tenant_id=9157", String.class))
        .isEqualTo(token);
  }

  @Test
  void invalidConfiguredDefaultCannotLeaveAnOwnedTenantOrReachTheOutboundClient() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var factory =
        org.mockito.Mockito.mock(
            com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory
                .class);
    var service =
        new com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService(
            factory,
            org.mockito.Mockito.mock(
                com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier.class),
            org.mockito.Mockito.mock(com.vi.tenantservice.api.tenant.TenantResolverService.class),
            new ConfigurationFileLoader());
    org.springframework.test.util.ReflectionTestUtils.setField(
        service,
        "defaultConsultingTypesFilePath",
        "src/test/resources/settings/bootstrap-v2-consulting-type.json");
    var signer =
        org.mockito.Mockito.mock(com.vi.tenantservice.config.security.TenantCreationContext.class);
    org.springframework.test.util.ReflectionTestUtils.setField(service, "creationContext", signer);
    assertThatThrownBy(
            () ->
                creation.create(
                    input(9157L),
                    token,
                    (created, grant) -> {
                      service.createDefaultConsultingTypes(grant);
                      return null;
                    }))
        .isInstanceOf(IllegalStateException.class);
    org.mockito.Mockito.verifyNoInteractions(signer, factory);
    assertThat(tenants.existsById(9157L)).isFalse();
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("RESERVED");
    assertThat(
            sql.queryForObject(
                "SELECT token FROM tenant_id_reservation WHERE tenant_id=9157", String.class))
        .isEqualTo(token);
  }

  @Test
  void nativeScalarMutationRevokesTheGrantAndCannotAuthorizeCompensation() {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    assertThatThrownBy(
            () ->
                creation.create(
                    input(9157L),
                    token,
                    (created, grant) -> {
                      sql.update("UPDATE tenant SET name='changed' WHERE id=9157");
                      return grant.tenantId();
                    }))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(tenants.findById(9157L).orElseThrow().getName()).isEqualTo("changed");
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("ASSIGNED");
  }

  @Test
  void aMixedAutocommitPairCannotMintAGrantOrEnterTheCallback() throws Exception {
    String token = allocation.reserve(9157L, "synthetic-bootstrap").getToken();
    var original =
        (javax.sql.DataSource)
            org.springframework.test.util.ReflectionTestUtils.getField(creation, "dataSource");
    var armed = new java.util.concurrent.atomic.AtomicBoolean(true);
    var flipped = new java.util.concurrent.atomic.AtomicBoolean(false);
    var callback = new java.util.concurrent.atomic.AtomicBoolean(false);
    var transaction =
        new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    javax.sql.DataSource interleaved =
        new org.springframework.jdbc.datasource.DelegatingDataSource(original) {
          @Override
          public java.sql.Connection getConnection() throws java.sql.SQLException {
            java.sql.Connection connection = super.getConnection();
            if (!armed.compareAndSet(true, false)) return connection;
            String owner =
                sql.queryForObject(
                    "SELECT reserved_by FROM tenant_id_reservation WHERE tenant_id=9157",
                    String.class);
            sql.update(
                "UPDATE tenant_id_reservation SET reserved_by='different-state' WHERE tenant_id=9157");
            return (java.sql.Connection)
                java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] {java.sql.Connection.class},
                    (proxy, method, args) -> {
                      Object answer = invokeNative(method, connection, args);
                      if (!method.getName().equals("prepareStatement")
                          || !(args[0] instanceof String)
                          || !((String) args[0]).contains("FROM tenant ")) return answer;
                      java.sql.PreparedStatement statement = (java.sql.PreparedStatement) answer;
                      return java.lang.reflect.Proxy.newProxyInstance(
                          getClass().getClassLoader(),
                          new Class<?>[] {java.sql.PreparedStatement.class},
                          (prepared, operation, parameters) -> {
                            Object selected = invokeNative(operation, statement, parameters);
                            if (!operation.getName().equals("executeQuery")) return selected;
                            java.sql.ResultSet rows = (java.sql.ResultSet) selected;
                            return java.lang.reflect.Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class<?>[] {java.sql.ResultSet.class},
                                (result, read, values) -> {
                                  Object cell = invokeNative(read, rows, values);
                                  if (read.getName().equals("close")
                                      && flipped.compareAndSet(false, true)) {
                                    transaction.executeWithoutResult(
                                        status -> {
                                          assertThat(
                                                  sql.update(
                                                      "UPDATE tenant SET name='interleaved' WHERE id=9157"))
                                              .isEqualTo(1);
                                          assertThat(
                                                  sql.update(
                                                      "UPDATE tenant_id_reservation SET reserved_by=? WHERE tenant_id=9157",
                                                      owner))
                                              .isEqualTo(1);
                                        });
                                  }
                                  return cell;
                                });
                          });
                    });
          }
        };
    Throwable failure;
    org.springframework.test.util.ReflectionTestUtils.setField(creation, "dataSource", interleaved);
    try {
      failure =
          org.assertj.core.api.Assertions.catchThrowable(
              () ->
                  creation.create(
                      input(9157L),
                      token,
                      (created, grant) -> {
                        callback.set(true);
                        return null;
                      }));
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(creation, "dataSource", original);
    }
    assertThat(flipped.get()).as("controlled atomic state transition occurred").isTrue();
    assertThat(callback.get())
        .as("mixed row observations cannot enter the grant callback")
        .isFalse();
    assertThat(failure).isInstanceOf(AccessDeniedException.class);
    assertThat(tenants.findById(9157L).orElseThrow().getName()).isEqualTo("interleaved");
    assertThat(allocation.getStatus(9157L).name()).isEqualTo("ASSIGNED");
  }

  private static Object invokeNative(java.lang.reflect.Method method, Object target, Object[] args)
      throws Throwable {
    try {
      return method.invoke(target, args);
    } catch (java.lang.reflect.InvocationTargetException failure) {
      throw failure.getCause();
    }
  }

  private static TenantEntity input(Long id) {
    return TenantEntity.builder().id(id).name("new bootstrap").subdomain("bootstrap-owned").build();
  }
}
