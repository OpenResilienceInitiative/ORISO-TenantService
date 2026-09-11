package com.vi.tenantservice.api.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated, bounded pool for fire-and-forget side effects — currently only the best-effort DPA
 * signed-notice hint (ORISO-TenantService#179): the public confirm response must not wait for, or
 * fail because of, the notification round-trip to the UserService.
 *
 * <p>Own small pool rather than Spring's shared application executor, whose queue is effectively
 * unbounded: a UserService outage would otherwise let one task pile up per signature until memory
 * suffers, in the same pool every other async task shares. Here the queue is short and a full queue
 * rejects immediately — a hint is best-effort by design (the signature is already committed, and
 * the UserService re-reads the facts itself through its own authenticated call), so shedding one is
 * strictly better than accumulating work nobody can drain.
 */
@Configuration
public class AsyncConfig {

  /** Bean name of the notice pool, so the hint cannot accidentally land on the shared executor. */
  public static final String DPA_NOTICE_EXECUTOR = "dpaNoticeExecutor";

  @Value("${app.dpa.notice.executor.core-pool-size:2}")
  private int corePoolSize;

  @Value("${app.dpa.notice.executor.max-pool-size:4}")
  private int maxPoolSize;

  @Value("${app.dpa.notice.executor.queue-capacity:100}")
  private int queueCapacity;

  @Bean(DPA_NOTICE_EXECUTOR)
  public Executor dpaNoticeExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("dpa-notice-");
    // Fail fast instead of blocking the signing request or growing without bound; the submitting
    // service catches the rejection and logs it.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    // A shutdown must not hold the service hostage for a best-effort notification.
    executor.setWaitForTasksToCompleteOnShutdown(false);
    executor.initialize();
    return executor;
  }
}
