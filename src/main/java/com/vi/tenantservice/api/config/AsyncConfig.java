package com.vi.tenantservice.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} execution for fire-and-forget side effects — currently only the
 * best-effort DPA signed-notice hint (ORISO-TenantService#179): the public confirm response must
 * not wait for, or fail because of, the notification round-trip to the UserService.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
