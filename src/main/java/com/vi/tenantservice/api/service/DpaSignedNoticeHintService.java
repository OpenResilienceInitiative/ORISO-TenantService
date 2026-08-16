package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.config.AsyncConfig;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Best-effort hint to the UserService that a forwarded DPA signature just landed for a tenant
 * (ORISO-UserService#1005: DPA_SIGNED_NOTICE to the forwarding administrator).
 *
 * <p>Deliberately a HINT, not a command: the public confirm endpoint runs without any
 * authentication context, and TenantService holds no technical-user credentials, so the call
 * carries no data and no authority. The UserService treats it as untrusted, reads the signature
 * facts back from this service's authenticated signatures endpoint, and decides idempotently
 * whether a notice is due — a spoofed or repeated hint can therefore never fabricate a notice.
 *
 * <p>Failure isolation is total, because the signature is legally recorded the moment the confirm
 * transaction commits and a notification hiccup must never turn that into an error for the signer:
 * the dispatch runs on a small bounded pool ({@link AsyncConfig#DPA_NOTICE_EXECUTOR}), a full queue
 * sheds the hint instead of blocking or growing, and every transport failure is swallowed. The
 * submission is explicit rather than {@code @Async} precisely so the rejection is handled here
 * instead of propagating into the caller's request.
 */
@Service
@Slf4j
public class DpaSignedNoticeHintService {

  private final RestTemplate restTemplate;
  private final Executor noticeExecutor;
  private final String userServiceApiUrl;

  public DpaSignedNoticeHintService(
      RestTemplate restTemplate,
      @Qualifier(AsyncConfig.DPA_NOTICE_EXECUTOR) Executor noticeExecutor,
      @Value("${user.service.api.url:}") String userServiceApiUrl) {
    this.restTemplate = restTemplate;
    this.noticeExecutor = noticeExecutor;
    this.userServiceApiUrl = userServiceApiUrl;
  }

  /** Fires the hint for the tenant; no-op when no UserService URL is configured. */
  public void notifySignatureRecorded(Long tenantId) {
    if (userServiceApiUrl == null || userServiceApiUrl.isBlank() || tenantId == null) {
      return;
    }
    try {
      noticeExecutor.execute(() -> postHint(tenantId));
    } catch (RejectedExecutionException exception) {
      log.warn(
          "DPA signed-notice hint for tenant {} was shed (notice queue full): the signature is"
              + " recorded; the notice will not be sent automatically",
          tenantId);
    }
  }

  private void postHint(Long tenantId) {
    var url =
        normalizeBaseUrl(userServiceApiUrl) + "/users/tenants/" + tenantId + "/dpa-signed-notices";
    try {
      restTemplate.postForLocation(url, null);
    } catch (RuntimeException exception) {
      log.warn(
          "DPA signed-notice hint for tenant {} could not be delivered ({}): the signature is"
              + " recorded; the notice will not be sent automatically",
          tenantId,
          exception.getClass().getSimpleName());
    }
  }

  private static String normalizeBaseUrl(String value) {
    var trimmed = value.trim();
    return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
  }
}
