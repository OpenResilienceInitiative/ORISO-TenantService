package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

/**
 * The signed-notice hint is fire-and-forget by contract: the signature is already committed when it
 * runs, so NOTHING it does may surface to the signer. These tests pin that against the two ways the
 * submission itself can fail — a saturated pool and an executor that is no longer usable.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DpaSignedNoticeHintServiceTest {

  private static final String USER_SERVICE_URL = "http://userservice.example";

  @Mock private RestTemplate restTemplate;

  private ThreadPoolTaskExecutor executor;
  private CountDownLatch release;

  @AfterEach
  void tearDown() {
    if (release != null) {
      release.countDown();
    }
    if (executor != null) {
      executor.shutdown();
    }
  }

  /** A pool with exactly one worker and one queue slot, so the third submission is rejected. */
  private ThreadPoolTaskExecutor saturableExecutor() {
    var pool = new ThreadPoolTaskExecutor();
    pool.setCorePoolSize(1);
    pool.setMaxPoolSize(1);
    pool.setQueueCapacity(1);
    pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    pool.initialize();
    return pool;
  }

  @Test
  void notifySignatureRecorded_Should_notFailTheConfirmation_When_theNoticePoolIsSaturated() {
    // given a pool whose only worker is blocked and whose single queue slot is taken
    executor = saturableExecutor();
    release = new CountDownLatch(1);
    var started = new CountDownLatch(1);
    when(restTemplate.postForLocation(anyString(), any()))
        .thenAnswer(
            invocation -> {
              started.countDown();
              release.await(5, TimeUnit.SECONDS);
              return null;
            });
    var service = new DpaSignedNoticeHintService(restTemplate, executor, USER_SERVICE_URL);

    service.notifySignatureRecorded(1L); // occupies the worker
    assertThatCode(() -> started.await(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
    service.notifySignatureRecorded(2L); // fills the queue

    // when the pool has nothing left to give, the hint must be shed silently — the signer's
    // confirmation has already committed and must not turn into an HTTP 500 (a retry would then
    // hit 410, the token being consumed)
    assertThatCode(() -> service.notifySignatureRecorded(3L)).doesNotThrowAnyException();
    assertThatCode(() -> service.notifySignatureRecorded(4L)).doesNotThrowAnyException();
  }

  @Test
  void notifySignatureRecorded_Should_notFailTheConfirmation_When_theExecutorIsNoLongerUsable() {
    // an executor rejecting with something OTHER than a rejection type — e.g. a pool shut down
    // during deployment — must be shed just as silently
    Executor broken =
        task -> {
          throw new IllegalStateException("ThreadPoolTaskExecutor not initialized");
        };
    var service = new DpaSignedNoticeHintService(restTemplate, broken, USER_SERVICE_URL);

    assertThatCode(() -> service.notifySignatureRecorded(1L)).doesNotThrowAnyException();
    verify(restTemplate, never()).postForLocation(anyString(), any());
  }

  @Test
  void notifySignatureRecorded_Should_doNothing_When_noUserServiceUrlIsConfigured() {
    Executor rejecting =
        task -> {
          throw new IllegalStateException("must not be reached");
        };

    assertThatCode(
            () ->
                new DpaSignedNoticeHintService(restTemplate, rejecting, "")
                    .notifySignatureRecorded(1L))
        .doesNotThrowAnyException();
    verify(restTemplate, never()).postForLocation(anyString(), any());
  }

  @Test
  void notifySignatureRecorded_Should_swallowTransportFailures() {
    executor = saturableExecutor();
    var delivered = new CountDownLatch(1);
    when(restTemplate.postForLocation(anyString(), any()))
        .thenAnswer(
            invocation -> {
              delivered.countDown();
              throw new org.springframework.web.client.ResourceAccessException(
                  "connection refused");
            });
    var service = new DpaSignedNoticeHintService(restTemplate, executor, USER_SERVICE_URL);

    service.notifySignatureRecorded(7L);

    assertThatCode(() -> delivered.await(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
    assertThat(delivered.getCount()).isZero();
  }
}
