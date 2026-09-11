package com.vi.tenantservice.api.model;

import java.time.LocalDateTime;

public interface TenantData extends TenantRestrictedData {

  String getAddress();

  String getDescription();

  Integer getLicensingAllowedNumberOfUsers();

  LocalDateTime getCreateDate();

  LocalDateTime getUpdateDate();

  /* ORISO-Admin#601: the Träger-authored Erstantwort Bausteine, so the Admin editor
  can read back what it wrote. Same `<lang>` → HTML JSON map as the legal texts. */
  String getContentErstantwortGreeting();

  String getContentErstantwortWhoReadsAlong();

  String getContentErstantwortEmergencyAddition();

  String getContentErstantwortFreeNotice();

  String getContentErstantwortClosing();

  Integer getErstantwortResponseDeadlineDays();
}
