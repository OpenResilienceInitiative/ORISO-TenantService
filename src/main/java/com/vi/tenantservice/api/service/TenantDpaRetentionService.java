package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention for DENIED DPA confirmations: when a tenant refuses/denies its DPA the record is kept
 * for a window and then auto-purged (the flow's "denied → deleted after 365 days"). Runs daily; the
 * window and cron are configurable.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantDpaRetentionService {

  private final TenantDpaSignatureRepository signatureRepository;

  @Value("${dpa.denied-retention-days:365}")
  private long deniedRetentionDays;

  /** Deletes DENIED signatures older than the retention window. Returns the number removed. */
  @Transactional
  public long purgeExpiredDeniedSignatures() {
    // floor at 1 day: a misconfigured 0/negative window must never purge all (or future) DENIED
    // rows
    var effectiveDays = Math.max(1L, deniedRetentionDays);
    var cutoff = LocalDateTime.now().minusDays(effectiveDays);
    long removed =
        signatureRepository.deleteByStatusAndCreateDateBefore(DpaSignatureStatus.DENIED, cutoff);
    if (removed > 0) {
      log.info("DPA retention: purged {} DENIED signatures older than {}", removed, cutoff);
    }
    return removed;
  }

  /**
   * Scheduled entry point. {@code @Transactional} lives here (not only on {@link
   * #purgeExpiredDeniedSignatures()}) because the scheduler self-invokes and self-invocation does
   * not pass through the Spring proxy — so the transaction would otherwise be inert on this path.
   */
  @Scheduled(cron = "${dpa.retention-purge-cron:0 0 3 * * *}")
  @Transactional
  void scheduledPurge() {
    purgeExpiredDeniedSignatures();
  }
}
