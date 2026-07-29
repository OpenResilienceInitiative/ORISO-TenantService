package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.exception.TenantIdAllocationExhaustedException;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Unit tests for the exception classification of the tenant ID allocation (TEN-INV hardening): a
 * duplicate-key loss is a 409 conflict, an infrastructure failure must surface as-is (500), and
 * AUTO exhaustion is a 503, not a client error.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantIdAllocationServiceTest {

  @Mock private TenantRepository tenantRepository;
  @Mock private TenantIdReservationRepository reservationRepository;
  @Mock private PlatformTransactionManager transactionManager;

  private TenantIdAllocationService allocationService;

  @BeforeEach
  void setUp() {
    allocationService =
        new TenantIdAllocationService(tenantRepository, reservationRepository, transactionManager);
    when(tenantRepository.findAllIds()).thenReturn(List.of());
    when(reservationRepository.findAllLedgerIds()).thenReturn(List.of());
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(reservationRepository.existsById(any())).thenReturn(false);
  }

  @Test
  void reserve_Should_throwConflict_When_manualInsertLosesOnDuplicateKey() {
    when(reservationRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> allocationService.reserve(5L, "admin"))
        .isInstanceOf(TenantIdAllocationConflictException.class);
  }

  @Test
  void reserve_Should_surfaceInfrastructureFailure_When_manualInsertFailsNonIntegrity() {
    when(reservationRepository.saveAndFlush(any()))
        .thenThrow(new DataAccessResourceFailureException("connection lost"));

    assertThatThrownBy(() -> allocationService.reserve(5L, "admin"))
        .isInstanceOf(DataAccessResourceFailureException.class);
  }

  @Test
  void reserve_Should_surfaceInfrastructureFailure_When_autoInsertFailsNonIntegrity() {
    when(reservationRepository.saveAndFlush(any()))
        .thenThrow(new DataAccessResourceFailureException("connection lost"));

    assertThatThrownBy(() -> allocationService.reserve(null, "admin"))
        .isInstanceOf(DataAccessResourceFailureException.class);
  }

  @Test
  void reserve_Should_throwExhausted_When_autoModeLosesEveryRace() {
    when(reservationRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> allocationService.reserve(null, "admin"))
        .isInstanceOf(TenantIdAllocationExhaustedException.class);

    verify(reservationRepository, times(TenantIdAllocationService.MAX_AUTO_ATTEMPTS))
        .saveAndFlush(any(TenantIdReservationEntity.class));
  }

  @Test
  void assignIdForNewTenant_Should_throwConflict_When_freshInsertLosesOnDuplicateKey() {
    when(reservationRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    var entity = TenantEntity.builder().id(7L).build();
    assertThatThrownBy(() -> allocationService.assignIdForNewTenant(entity, null))
        .isInstanceOf(TenantIdAllocationConflictException.class);
  }

  @Test
  void assignIdForNewTenant_Should_surfaceInfrastructureFailure_When_freshInsertFails() {
    when(reservationRepository.saveAndFlush(any()))
        .thenThrow(new DataAccessResourceFailureException("lock wait timeout"));

    var entity = TenantEntity.builder().id(7L).build();
    assertThatThrownBy(() -> allocationService.assignIdForNewTenant(entity, null))
        .isInstanceOf(DataAccessResourceFailureException.class);
  }

  @Test
  void exhaustedException_Should_mapToServiceUnavailable() {
    var responseStatus =
        TenantIdAllocationExhaustedException.class.getAnnotation(ResponseStatus.class);

    assertThat(responseStatus).isNotNull();
    assertThat(responseStatus.value()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }
}
