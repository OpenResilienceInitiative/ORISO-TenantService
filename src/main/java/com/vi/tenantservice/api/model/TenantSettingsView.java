package com.vi.tenantservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Only the columns the account-preference read needs; no theming, legal or DPO content. */
@AllArgsConstructor
@Getter
public class TenantSettingsView {

  private final Long id;
  private final String settings;
}
