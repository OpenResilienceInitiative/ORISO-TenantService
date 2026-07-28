package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantIdAllocationStatus;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for the authoritative tenant ID allocation contract (TEN-INV-U1,
 * ORISO-TenantService#143). Runs against the H2 database of the testing profile; concurrency
 * guarantees are enforced by the primary key of the tenant_id_reservation ledger, which behaves
 * identically on H2 and MariaDB for these cases.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantIdAllocationServiceIT {

  @Autowired private TenantIdAllocationService allocationService;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;

  @BeforeEach
  void seedWorkedExample() {
    cleanUp();
    // Worked example from the issue: tenant IDs 1-20 and 30-35 are taken.
    LongStream.concat(LongStream.rangeClosed(1, 20), LongStream.rangeClosed(30, 35))
        .forEach(this::insertTenant);
  }

  @AfterEach
  void cleanUp() {
    reservationRepository.deleteAll();
    tenantRepository.deleteAll();
  }

  private void insertTenant(long id) {
    var now = LocalDateTime.now();
    tenantRepository.save(
        TenantEntity.builder()
            .id(id)
            .name("tenant-" + id)
            .subdomain("subdomain-" + id)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  @Test
  void reserve_Should_reserveSmallestFreeId_When_autoMode() {
    var reservation = allocationService.reserve(null, "platform-admin");

    assertThat(reservation.getTenantId()).isEqualTo(21L);
    assertThat(reservation.getStatus()).isEqualTo(TenantIdReservationStatus.RESERVED);
    assertThat(reservation.getToken()).isNotBlank();
  }

  @Test
  void nextFreeId_Should_skipTakenAndReservedIds_InBothDirections() {
    assertThat(allocationService.nextFreeId(29L, true)).contains(36L);
    assertThat(allocationService.nextFreeId(36L, false)).contains(29L);
  }

  @Test
  void nextFreeId_Should_returnEmpty_When_noFreeIdExistsBelow() {
    assertThat(allocationService.nextFreeId(21L, false)).isEmpty();
  }

  @Test
  void nextFreeId_Should_skipReservedIds() {
    allocationService.reserve(21L, "platform-admin");

    assertThat(allocationService.nextFreeId(20L, true)).contains(22L);
    assertThat(allocationService.reserve(null, "platform-admin").getTenantId()).isEqualTo(22L);
  }

  @Test
  void reserve_Should_throwConflict_When_idIsTaken() {
    assertThatThrownBy(() -> allocationService.reserve(30L, "platform-admin"))
        .isInstanceOf(TenantIdAllocationConflictException.class);
  }

  @Test
  void reserve_Should_throwConflict_When_idIsAlreadyReserved() {
    allocationService.reserve(21L, "platform-admin");

    assertThatThrownBy(() -> allocationService.reserve(21L, "platform-admin"))
        .isInstanceOf(TenantIdAllocationConflictException.class);
  }

  @Test
  void getStatus_Should_reportFreeReservedAndAssigned() {
    allocationService.reserve(25L, "platform-admin");

    assertThat(allocationService.getStatus(21L)).isEqualTo(TenantIdAllocationStatus.FREE);
    assertThat(allocationService.getStatus(25L)).isEqualTo(TenantIdAllocationStatus.RESERVED);
    assertThat(allocationService.getStatus(30L)).isEqualTo(TenantIdAllocationStatus.ASSIGNED);
  }

  @Test
  void release_Should_makeReservedIdAssignableAgain() {
    allocationService.reserve(21L, "platform-admin");

    assertThat(allocationService.release(21L)).isTrue();
    assertThat(allocationService.getStatus(21L)).isEqualTo(TenantIdAllocationStatus.FREE);
    assertThat(allocationService.reserve(21L, "platform-admin").getTenantId()).isEqualTo(21L);
  }

  @Test
  void release_Should_returnFalse_When_noOpenReservationExists() {
    assertThat(allocationService.release(21L)).isFalse();
    assertThat(allocationService.release(30L)).isFalse();
  }

  @Test
  void reserve_Should_returnDifferentIds_When_twoParallelAutoReservations() throws Exception {
    List<Long> reservedIds = runConcurrently(() -> allocationService.reserve(null, "admin"));

    assertThat(reservedIds).hasSize(2).doesNotHaveDuplicates().contains(21L);
  }

  @Test
  void reserve_Should_letExactlyOneWin_When_twoParallelReservationsOfSameFreeId() throws Exception {
    var successes = new ArrayList<Long>();
    var conflicts = new ArrayList<Throwable>();

    collectConcurrentResults(() -> allocationService.reserve(21L, "admin"), successes, conflicts);

    assertThat(successes).containsExactly(21L);
    assertThat(conflicts).hasSize(1);
    assertThat(conflicts.get(0)).isInstanceOf(TenantIdAllocationConflictException.class);
  }

  private List<Long> runConcurrently(
      Callable<com.vi.tenantservice.api.model.TenantIdReservationEntity> action) throws Exception {
    var successes = new ArrayList<Long>();
    var failures = new ArrayList<Throwable>();
    collectConcurrentResults(action, successes, failures);
    assertThat(failures).isEmpty();
    return successes;
  }

  private void collectConcurrentResults(
      Callable<com.vi.tenantservice.api.model.TenantIdReservationEntity> action,
      List<Long> successes,
      List<Throwable> failures)
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      var startLatch = new CountDownLatch(1);
      Callable<Long> gated =
          () -> {
            startLatch.await();
            return action.call().getTenantId();
          };
      Future<Long> first = executor.submit(gated);
      Future<Long> second = executor.submit(gated);
      startLatch.countDown();
      for (Future<Long> future : List.of(first, second)) {
        try {
          successes.add(future.get());
        } catch (java.util.concurrent.ExecutionException e) {
          failures.add(e.getCause());
        }
      }
    } finally {
      executor.shutdownNow();
    }
  }
}
