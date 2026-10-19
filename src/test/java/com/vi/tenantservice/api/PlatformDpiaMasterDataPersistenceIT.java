package com.vi.tenantservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.model.DpiaOperatorDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.repository.PlatformDpiaMasterDataRepository;
import com.vi.tenantservice.api.service.PlatformDpiaMasterDataService;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

/** Exercises the real service proxy and transactions; no test transaction wraps the callers. */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.datasource.url=jdbc:h2:mem:dpia-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1"
    })
class PlatformDpiaMasterDataPersistenceIT {
  @Autowired PlatformDpiaMasterDataService service;
  @Autowired PlatformDpiaMasterDataRepository repository;

  @BeforeEach
  void clearMasterData() {
    repository.deleteAll();
  }

  @Test
  void concurrentFirstWrites_shouldAllSucceedWithOneCompleteStoredPayload() throws Exception {
    int writers = 8;
    var ready = new CountDownLatch(writers);
    var start = new CountDownLatch(1);
    var results = new ArrayList<Future<PlatformDpiaMasterDataDTO>>();
    try (var executor = Executors.newFixedThreadPool(writers)) {
      for (int i = 0; i < writers; i++) {
        final String name = "Operator " + i;
        results.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent writers did not start");
                  }
                  return service.updateMasterData(payload(name));
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (int i = 0; i < writers; i++) {
        assertThat(results.get(i).get(30, TimeUnit.SECONDS).getOperator())
            .isEqualTo(payload("Operator " + i).getOperator());
      }
    }
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.getId()).isEqualTo(1L);
              assertThat(row.getOperatorLegalName()).startsWith("Operator ");
              assertThat(row.getOperatorAddress())
                  .isEqualTo(row.getOperatorLegalName() + " address");
              assertThat(row.getOperatorShortName())
                  .isEqualTo(row.getOperatorLegalName() + " short");
            });
  }

  @Test
  void failedFirstWrite_shouldRollBackInitialization() {
    assertThatThrownBy(() -> service.updateMasterData(payload("x".repeat(256))))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(repository.count()).isZero();
    assertThat(service.getMasterData().getOperator().getLegalName()).isNull();
  }

  @Test
  void failedReplacement_shouldPreservePreviousPayload() {
    service.updateMasterData(payload("Original"));
    var original = repository.findAll().getFirst();
    assertThatThrownBy(() -> service.updateMasterData(payload("x".repeat(256))))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(repository.findAll()).singleElement().usingRecursiveComparison().isEqualTo(original);
  }

  private static PlatformDpiaMasterDataDTO payload(String name) {
    return new PlatformDpiaMasterDataDTO()
        .operator(
            new DpiaOperatorDTO()
                .legalName(name)
                .shortName(name + " short")
                .address(name + " address"));
  }
}
