package com.vi.tenantservice.api.service.systememail;

// What goes on the wire. Kept apart from the relay purpose so the admin SMTP test needs no
// public relay purpose that a technical client could also submit.
public interface TenantSystemMail {
  String recipient();

  String subject();

  String html();

  String text();
}
