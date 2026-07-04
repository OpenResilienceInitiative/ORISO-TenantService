package com.vi.tenantservice.api.model;

import java.time.LocalDateTime;

public interface TenantData extends TenantRestrictedData {

  String getAddress();

  String getDescription();

  Integer getLicensingAllowedNumberOfUsers();

  LocalDateTime getCreateDate();

  LocalDateTime getUpdateDate();
}
