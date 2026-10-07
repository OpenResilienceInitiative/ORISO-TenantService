package com.vi.tenantservice.api.model;

/** The mail dispatcher receives routing and footer fields, never transport credentials. */
public record SystemEmailContext(
    Long id,
    String subdomain,
    String name,
    String legalName,
    String address,
    String contactEmail,
    String contactPhone,
    MailSettings settings) {
  public record MailSettings(
      Boolean featureSystemNotificationEmailsEnabled, TenantSmtpMode smtpMode, MailSmtp smtp) {}

  public record MailSmtp(boolean enabled, String emailThemeColor, boolean configured) {}

  public static SystemEmailContext from(TenantDTO tenant, TenantSettings settings) {
    TenantSmtpSettings smtp = settings == null ? null : settings.getSmtp();
    boolean configured =
        smtp != null
            && smtp.isEnabled()
            && notBlank(smtp.getHost())
            && notBlank(smtp.getFrom())
            && notBlank(smtp.getUsername())
            && notBlank(smtp.getPassword())
            && smtp.getPort() != null
            && smtp.getPort() > 0
            && smtp.getPort() <= 65535;
    return new SystemEmailContext(
        tenant.getId(),
        tenant.getSubdomain(),
        tenant.getName(),
        tenant.getLegalName(),
        tenant.getAddress(),
        tenant.getContactEmail(),
        tenant.getContactPhone(),
        new MailSettings(
            settings == null || settings.getFeatureSystemNotificationEmailsEnabled() == null
                ? Boolean.TRUE
                : settings.getFeatureSystemNotificationEmailsEnabled(),
            settings == null || settings.getSmtpMode() == null
                ? TenantSmtpMode.PLATFORM
                : settings.getSmtpMode(),
            new MailSmtp(
                smtp != null && smtp.isEnabled(),
                smtp == null ? null : smtp.getEmailThemeColor(),
                configured)));
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
