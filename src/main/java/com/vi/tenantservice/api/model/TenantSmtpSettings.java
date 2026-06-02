package com.vi.tenantservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantSmtpSettings {

  boolean enabled;
  String host;
  Integer port;
  boolean secure;
  String username;
  String password;
  String from;
  String emailThemeColor;
}
