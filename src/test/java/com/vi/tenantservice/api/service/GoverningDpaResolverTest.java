package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Which DPA document is in force for a tenant (#569). Every DPA path — status, in-app signing,
 * reading the document and forwarding it for signature — resolves through this component, so its
 * answers are what keeps a tenant from being blocked on a document it cannot obtain.
 */
@ExtendWith(MockitoExtension.class)
class GoverningDpaResolverTest {

  private static final Long TENANT_ID = 7L;
  private static final Long OPERATOR_TENANT_ID = 1L;
  private static final LocalDateTime OWN_VERSION = LocalDateTime.of(2026, 5, 1, 10, 0);
  private static final LocalDateTime OPERATOR_VERSION = LocalDateTime.of(2026, 7, 1, 12, 0);

  @Mock private TenantRepository tenantRepository;
  @Mock private TenantDpaVersionRepository versionRepository;

  private GoverningDpaResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new GoverningDpaResolver(tenantRepository, versionRepository);
    ReflectionTestUtils.setField(resolver, "operatorTenantId", OPERATOR_TENANT_ID.longValue());
  }

  private void givenTenant(Long tenantId, LocalDateTime embeddedVersion) {
    when(tenantRepository.findById(tenantId))
        .thenReturn(
            Optional.of(
                TenantEntity.builder()
                    .id(tenantId)
                    .contentDataProcessingAgreementActivationDate(embeddedVersion)
                    .build()));
  }

  @Test
  void resolve_Should_returnTheOwnDocument_When_theTenantPublishedOne() {
    givenTenant(TENANT_ID, OWN_VERSION);

    var governing = resolver.resolve(TENANT_ID);

    assertThat(governing).isNotNull();
    assertThat(governing.documentTenantId()).isEqualTo(TENANT_ID);
    assertThat(governing.version()).isEqualTo(OWN_VERSION);
  }

  @Test
  void resolve_Should_recoverTheOwnDocumentFromHistory_When_theActivationDateWasCleared() {
    givenTenant(TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of(TenantDpaVersionEntity.builder().activationDate(OWN_VERSION).build()));

    var governing = resolver.resolve(TENANT_ID);

    assertThat(governing.documentTenantId()).isEqualTo(TENANT_ID);
    assertThat(governing.version()).isEqualTo(OWN_VERSION);
  }

  @Test
  void resolve_Should_returnTheOperatorDocument_When_theTenantHasNoneOfItsOwn() {
    givenTenant(TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
    givenTenant(OPERATOR_TENANT_ID, OPERATOR_VERSION);

    var governing = resolver.resolve(TENANT_ID);

    assertThat(governing.documentTenantId()).isEqualTo(OPERATOR_TENANT_ID);
    assertThat(governing.version()).isEqualTo(OPERATOR_VERSION);
  }

  @Test
  void resolve_Should_returnNull_When_nothingIsPublishedAnywhere() {
    givenTenant(TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
    givenTenant(OPERATOR_TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(OPERATOR_TENANT_ID))
        .thenReturn(List.of());

    assertThat(resolver.resolve(TENANT_ID)).isNull();
  }

  /** An absent tenant must never read as "has a contract to sign". */
  @Test
  void resolve_Should_returnNull_When_theTenantDoesNotExist() {
    when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

    assertThat(resolver.resolve(TENANT_ID)).isNull();
  }

  @Test
  void resolve_Should_notFallBackToItself_When_theOperatorTenantIsAsked() {
    givenTenant(OPERATOR_TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(OPERATOR_TENANT_ID))
        .thenReturn(List.of());

    assertThat(resolver.resolve(OPERATOR_TENANT_ID)).isNull();
  }

  @Test
  void resolve_Should_beDisabled_When_theOperatorTenantIdIsNotConfigured() {
    ReflectionTestUtils.setField(resolver, "operatorTenantId", 0L);
    givenTenant(TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());

    assertThat(resolver.resolve(TENANT_ID)).isNull();
  }

  @Test
  void documentTenantIdFor_Should_returnTheOperator_When_theTenantHasNoOwnDpa() {
    givenTenant(TENANT_ID, null);
    when(versionRepository.findByTenantIdOrderByActivationDateDesc(TENANT_ID))
        .thenReturn(List.of());
    givenTenant(OPERATOR_TENANT_ID, OPERATOR_VERSION);

    assertThat(resolver.documentTenantIdFor(TENANT_ID)).isEqualTo(OPERATOR_TENANT_ID);
  }

  /**
   * Nothing published anywhere: the caller gets the tenant's own (empty) history, not a foreign.
   */
  @Test
  void documentTenantIdFor_Should_returnTheTenantItself_When_nothingIsPublished() {
    when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

    assertThat(resolver.documentTenantIdFor(TENANT_ID)).isEqualTo(TENANT_ID);
  }

  @Test
  void findPublishedVersion_Should_fallBackToTheOperatorSnapshot_When_theTenantHasNone() {
    var operatorSnapshot =
        TenantDpaVersionEntity.builder()
            .tenantId(OPERATOR_TENANT_ID)
            .activationDate(OPERATOR_VERSION)
            .content("{\"de\":\"operator\"}")
            .build();
    when(versionRepository.findFirstByTenantIdAndActivationDate(TENANT_ID, OPERATOR_VERSION))
        .thenReturn(Optional.empty());
    when(versionRepository.findFirstByTenantIdAndActivationDate(
            OPERATOR_TENANT_ID, OPERATOR_VERSION))
        .thenReturn(Optional.of(operatorSnapshot));

    assertThat(resolver.findPublishedVersion(TENANT_ID, OPERATOR_VERSION))
        .contains(operatorSnapshot);
    assertThat(resolver.isPublishedVersion(TENANT_ID, OPERATOR_VERSION)).isTrue();
  }

  @Test
  void findPublishedVersion_Should_preferTheTenantsOwnSnapshot() {
    var ownSnapshot =
        TenantDpaVersionEntity.builder()
            .tenantId(TENANT_ID)
            .activationDate(OWN_VERSION)
            .content("{\"de\":\"own\"}")
            .build();
    when(versionRepository.findFirstByTenantIdAndActivationDate(TENANT_ID, OWN_VERSION))
        .thenReturn(Optional.of(ownSnapshot));

    assertThat(resolver.findPublishedVersion(TENANT_ID, OWN_VERSION)).contains(ownSnapshot);
  }

  @Test
  void findPublishedVersion_Should_returnEmpty_When_noVersionIsGiven() {
    assertThat(resolver.findPublishedVersion(TENANT_ID, null)).isEmpty();
    assertThat(resolver.isPublishedVersion(TENANT_ID, null)).isFalse();
  }
}
