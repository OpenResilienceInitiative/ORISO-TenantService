package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.BasicTenantLicensingDTO;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.ClosingAnimation;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.NoAgencyContextDTO;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.SmtpConfig;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.service.SmtpPasswordEncryptionService;
import com.vi.tenantservice.api.service.TemplateDescriptionServiceException;
import com.vi.tenantservice.api.service.TemplateRenderer;
import com.vi.tenantservice.api.service.TemplateService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class TenantConverterTest {

  @InjectMocks TenantConverter tenantConverter;

  @Mock TemplateService templateService;

  @Mock TemplateRenderer templateRenderer;

  @Mock com.vi.tenantservice.api.service.legal.PlatformLegalTextTokens platformLegalTextTokens;

  // disabled (no secret): converter unit tests run on plaintext passthrough
  @Spy
  SmtpPasswordEncryptionService smtpPasswordEncryptionService =
      new SmtpPasswordEncryptionService("");

  @Test
  void toEntity_should_convertToEntityAndBackToDTO() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .withSettings()
            .build();
    tenantDTO.getSettings().extendedSettings(null);
    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then
    TenantDTO converted = tenantConverter.toDTO(entity, "de");
    assertThat(converted.getId()).isEqualTo(tenantDTO.getId());
    assertThat(converted.getName()).isEqualTo(tenantDTO.getName());
    assertThat(converted.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(converted.getLicensing()).isEqualTo(tenantDTO.getLicensing());
    assertCoreSettingsAreConverted(tenantDTO.getSettings(), converted.getSettings());
    assertThat(converted.getTheming()).isEqualTo(asRead(tenantDTO.getTheming()));
    assertThat(converted.getSettings().getIsVideoCallAllowed()).isTrue();
    assertThat(converted.getSettings().getShowAskerProfile()).isTrue();
    assertThat(converted.getSettings().getEmailVisible()).isTrue();
    assertThat(converted.getSettings().getEmailRequired()).isTrue();
    // content comparision is skipped, due to i18n feature, so the structure is different
  }

  @Test
  void toEntity_should_roundTripTheLoginStageEffect() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withTheming()
            .withThemingLoginEffect(Theming.LoginEffectEnum.CRACKS)
            .build();

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then — stored as the enum NAME, not an ordinal, and read back unchanged
    assertThat(entity.getThemingLoginEffect()).isEqualTo("CRACKS");
    assertThat(tenantConverter.toDTO(entity, "de").getTheming().getLoginEffect())
        .isEqualTo(Theming.LoginEffectEnum.CRACKS);
  }

  @Test
  void toEntity_should_keepAnUnsetLoginEffectNull() {
    // given — a tenant that never configured an effect
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withTheming().build();

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then — the column stays null: a written NONE would be indistinguishable
    // from an administrator having chosen it (same reasoning as the 0027 migration)
    assertThat(entity.getThemingLoginEffect()).isNull();
    // ...but a reader is told NONE. "Never configured" is a storage fact, not an
    // answer to "which effect does this tenant run", and null in the API would
    // force every consumer to invent that mapping for itself.
    assertThat(tenantConverter.toDTO(entity, "de").getTheming().getLoginEffect())
        .isEqualTo(Theming.LoginEffectEnum.NONE);
  }

  @Test
  void toDTO_should_fallBackToNoneForAnUnknownStoredLoginEffect() {
    // given — a value written by a newer version, or a hand-edited row
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withTheming().build();
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);
    entity.setThemingLoginEffect("KALEIDOSCOPE");

    // then — the login screen must never break over decoration
    assertThat(tenantConverter.toDTO(entity, "de").getTheming().getLoginEffect())
        .isEqualTo(Theming.LoginEffectEnum.NONE);
  }

  /**
   * Every value on both sides, paired with a different value on the other side. A swapped
   * desktop/mobile mapping would still pass if both always carried the same value.
   */
  @ParameterizedTest
  @CsvSource({
    "CURTAIN,SLIDE_REVEAL",
    "SLIDE_REVEAL,CURTAIN",
  })
  void toEntity_should_roundTripTheClosingAnimationForDesktopAndMobile(
      String desktop, String mobile) {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withTheming()
            .withThemingClosingAnimations(
                ClosingAnimation.fromValue(desktop), ClosingAnimation.fromValue(mobile))
            .build();

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then — stored as the enum NAME in the matching column, and read back unchanged
    assertThat(entity.getThemingClosingAnimationDesktop()).isEqualTo(desktop);
    assertThat(entity.getThemingClosingAnimationMobile()).isEqualTo(mobile);
    Theming read = tenantConverter.toDTO(entity, "de").getTheming();
    assertThat(read.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.fromValue(desktop));
    assertThat(read.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.fromValue(mobile));
  }

  @Test
  void toEntity_should_keepAnUnsetClosingAnimationNull() {
    // given — a tenant that never chose a closing animation
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withTheming().build();

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then — NULL is the honest "never configured": no default is written into the column...
    assertThat(entity.getThemingClosingAnimationDesktop()).isNull();
    assertThat(entity.getThemingClosingAnimationMobile()).isNull();
    // ...and none is invented on the way out. Unlike the login effect, the reader gets null so
    // that inheritance from the platform tenant can tell "unset" from "chose the default".
    Theming read = tenantConverter.toDTO(entity, "de").getTheming();
    assertThat(read.getClosingAnimationDesktop()).isNull();
    assertThat(read.getClosingAnimationMobile()).isNull();
  }

  @ParameterizedTest
  @CsvSource(
      value = {"CURTAIN,null", "null,SLIDE_REVEAL"},
      nullValues = "null")
  void toEntity_should_keepEachClosingAnimationIndependentOfTheOther(
      String desktop, String mobile) {
    // given — only one side configured
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withTheming()
            .withThemingClosingAnimations(
                desktop == null ? null : ClosingAnimation.fromValue(desktop),
                mobile == null ? null : ClosingAnimation.fromValue(mobile))
            .build();

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then — the unset side stays null, in storage and on the way back out
    assertThat(entity.getThemingClosingAnimationDesktop()).isEqualTo(desktop);
    assertThat(entity.getThemingClosingAnimationMobile()).isEqualTo(mobile);
    Theming read = tenantConverter.toDTO(entity, "de").getTheming();
    assertThat(read.getClosingAnimationDesktop())
        .isEqualTo(desktop == null ? null : ClosingAnimation.fromValue(desktop));
    assertThat(read.getClosingAnimationMobile())
        .isEqualTo(mobile == null ? null : ClosingAnimation.fromValue(mobile));
  }

  /**
   * A value written by a newer version or a hand-edited row. Case matters: the stored form is the
   * exact enum name, so a lower-case spelling is as unknown as a new design.
   */
  @ParameterizedTest
  @ValueSource(strings = {"KALEIDOSCOPE", "CIRCLE_ZOOM", "curtain", ""})
  void toDTO_should_readAnUnknownStoredClosingAnimationAsNullAndKeepTheOtherSide(String unknown) {
    // given
    TenantEntity entity =
        tenantConverter.toEntity(
            new MultilingualTenantTestDataBuilder().tenantDTO().withTheming().build());

    // when / then — desktop unknown, mobile valid: only desktop falls back, to null, not a default
    entity.setThemingClosingAnimationDesktop(unknown);
    entity.setThemingClosingAnimationMobile("CURTAIN");
    Theming read = tenantConverter.toDTO(entity, "de").getTheming();
    assertThat(read.getClosingAnimationDesktop()).isNull();
    assertThat(read.getClosingAnimationMobile()).isEqualTo(ClosingAnimation.CURTAIN);

    // and the other way round
    entity.setThemingClosingAnimationDesktop("SLIDE_REVEAL");
    entity.setThemingClosingAnimationMobile(unknown);
    read = tenantConverter.toDTO(entity, "de").getTheming();
    assertThat(read.getClosingAnimationDesktop()).isEqualTo(ClosingAnimation.SLIDE_REVEAL);
    assertThat(read.getClosingAnimationMobile()).isNull();
  }

  @Test
  void toDTO_should_warnAboutAnUnknownStoredClosingAnimation() {
    // given
    TenantEntity entity =
        tenantConverter.toEntity(
            new MultilingualTenantTestDataBuilder().tenantDTO().withTheming().build());
    entity.setThemingClosingAnimationDesktop("KALEIDOSCOPE");
    Logger converterLog = (Logger) LoggerFactory.getLogger(TenantConverter.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    converterLog.addAppender(appender);

    try {
      // when
      tenantConverter.toDTO(entity, "de");

      // then — silent data loss would hide a newer client's value from the operator
      assertThat(appender.list)
          .anySatisfy(
              event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                    .contains("closingAnimationDesktop")
                    .contains("KALEIDOSCOPE");
              });
    } finally {
      converterLog.detachAppender(appender);
    }
  }

  @Test
  void toEntity_should_roundTripEmailVisibleAndEmailRequired() {
    assertEmailSettingsRoundTrip(true, false);
    assertEmailSettingsRoundTrip(false, true);
  }

  private void assertEmailSettingsRoundTrip(boolean emailVisible, boolean emailRequired) {
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO.getSettings().emailVisible(emailVisible).emailRequired(emailRequired);

    TenantEntity entity = tenantConverter.toEntity(tenantDTO);
    TenantDTO converted = tenantConverter.toDTO(entity, "de");

    assertThat(converted.getSettings().getEmailVisible()).isEqualTo(emailVisible);
    assertThat(converted.getSettings().getEmailRequired()).isEqualTo(emailRequired);
  }

  @Test
  void toEntity_should_roundTripAddressAndDescription() {
    // given
    MultilingualTenantDTO tenantDTO = new MultilingualTenantTestDataBuilder().tenantDTO().build();
    tenantDTO.setAddress("Musterstraße 1, 12345 Musterstadt");
    tenantDTO.setDescription("Short description of the tenant.");

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then
    assertThat(entity.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(entity.getDescription()).isEqualTo("Short description of the tenant.");

    // and entity -> DTO conversions preserve the values
    TenantDTO converted = tenantConverter.toDTO(entity, "de");
    assertThat(converted.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(converted.getDescription()).isEqualTo("Short description of the tenant.");

    MultilingualTenantDTO multilingualConverted = tenantConverter.toMultilingualDTO(entity);
    assertThat(multilingualConverted.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(multilingualConverted.getDescription())
        .isEqualTo("Short description of the tenant.");

    AdminTenantDTO adminConverted = tenantConverter.toAdminTenantDTO(entity);
    assertThat(adminConverted.getAddress()).isEqualTo("Musterstraße 1, 12345 Musterstadt");
    assertThat(adminConverted.getDescription()).isEqualTo("Short description of the tenant.");
  }

  @Test
  void toEntity_should_preserveServerManagedDpaFieldsOnTenantUpdate() {
    // given
    var activationDate = LocalDateTime.of(2026, 7, 19, 18, 24, 47);
    var targetEntity =
        TenantEntity.builder()
            .id(84L)
            .contentDataProcessingAgreement("{\"de\":\"Published DPA\"}")
            .contentDataProcessingAgreementActivationDate(activationDate)
            .build();
    var tenantDTO = new MultilingualTenantTestDataBuilder().tenantDTO().build();

    // when
    TenantEntity converted = tenantConverter.toEntity(targetEntity, tenantDTO);

    // then
    assertThat(converted.getContentDataProcessingAgreement())
        .isEqualTo("{\"de\":\"Published DPA\"}");
    assertThat(converted.getContentDataProcessingAgreementActivationDate())
        .isEqualTo(activationDate);
  }

  @Test
  void toRestrictedTenantDTO_should_convertAttributesProperly()
      throws TemplateException, IOException, TemplateDescriptionServiceException {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .withSettings()
            .build();
    tenantDTO.getSettings().extendedSettings(null);

    when(templateService.getMultilingualDataProtectionTemplate())
        .thenReturn(
            Map.of(
                "de",
                new DataProtectionContactTemplateDTO()
                    .noAgencyContext(
                        new NoAgencyContextDTO().dataProtectionOfficerContact("test"))));

    TenantEntity entity = tenantConverter.toEntity(tenantDTO);
    when(templateRenderer.renderTemplate(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("renderedPrivacy");
    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);
    // then
    assertThat(restrictedTenantDTO.getName()).isEqualTo(tenantDTO.getName());
    assertThat(restrictedTenantDTO.getId()).isEqualTo(tenantDTO.getId());
    assertThat(restrictedTenantDTO.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(restrictedTenantDTO.getTheming()).isEqualTo(asRead(tenantDTO.getTheming()));
    assertContentIsProperlyConverted(tenantDTO, restrictedTenantDTO);
    assertRestrictedPublicSettingsAreConverted(
        tenantDTO.getSettings(), restrictedTenantDTO.getSettings());
    Mockito.verify(templateRenderer).renderTemplate(Mockito.anyString(), Mockito.anyMap());
    assertThat(restrictedTenantDTO.getContent().getRenderedPrivacy()).isEqualTo("renderedPrivacy");
  }

  @Test
  void toRestrictedTenantDTO_should_exposeRawLanguageMapsIncludingTranslationMeta()
      throws TemplateDescriptionServiceException {
    // given
    var impressumJson =
        "{\"de\":\"<h2 id=\\\"intro\\\">Impressum</h2>\",\"en\":\"<h2>Imprint</h2>\","
            + "\"en__meta\":\"{\\\"mt\\\":true,\\\"src\\\":\\\"de\\\",\\\"at\\\":\\\"2026-07-04T10:00:00Z\\\"}\"}";
    var privacyJson =
        "{\"de\":\"<p>Datenschutz</p>\",\"en\":\"<p>Privacy</p>\","
            + "\"en__meta\":\"{\\\"mt\\\":true,\\\"src\\\":\\\"de\\\",\\\"at\\\":\\\"2026-07-04T10:00:00Z\\\"}\"}";
    var entity = new TenantEntity();
    entity.setId(5L);
    entity.setName("tenant");
    entity.setContentImpressum(impressumJson);
    entity.setContentPrivacy(privacyJson);
    when(templateService.getMultilingualDataProtectionTemplate()).thenReturn(Map.of());

    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);

    // then: resolved fields are unchanged
    assertThat(restrictedTenantDTO.getContent().getImpressum())
        .isEqualTo("<h2 id=\"intro\">Impressum</h2>");
    assertThat(restrictedTenantDTO.getContent().getPrivacy()).isEqualTo("<p>Datenschutz</p>");

    // and: the raw language maps incl. __meta keys are exposed as stored (no processing)
    assertThat(restrictedTenantDTO.getContent().getImpressumLanguages())
        .containsOnly(
            Map.entry("de", "<h2 id=\"intro\">Impressum</h2>"),
            Map.entry("en", "<h2>Imprint</h2>"),
            Map.entry("en__meta", "{\"mt\":true,\"src\":\"de\",\"at\":\"2026-07-04T10:00:00Z\"}"));
    assertThat(restrictedTenantDTO.getContent().getPrivacyLanguages())
        .containsOnly(
            Map.entry("de", "<p>Datenschutz</p>"),
            Map.entry("en", "<p>Privacy</p>"),
            Map.entry("en__meta", "{\"mt\":true,\"src\":\"de\",\"at\":\"2026-07-04T10:00:00Z\"}"));
  }

  @Test
  void toRestrictedTenantDTO_should_redactSensitivePublicSettings() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .withSettings()
            .build();
    tenantDTO
        .getSettings()
        .featureToolsOICDToken("secret-oidc-token")
        .smtp(
            new SmtpConfig()
                .enabled(true)
                .host("smtp.example.org")
                .port(587)
                .secure(false)
                .username("smtp-user")
                .password("smtp-pass")
                .from("notifications@example.org")
                .emailThemeColor("#123456"));

    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);

    // then
    assertThat(restrictedTenantDTO.getSettings().getFeatureToolsOICDToken()).isNull();
    assertThat(restrictedTenantDTO.getSettings().getSmtp()).isNotNull();
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getHost()).isEqualTo("smtp.example.org");
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getPort()).isEqualTo(587);
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getFrom())
        .isEqualTo("notifications@example.org");
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getUsername()).isNull();
    assertThat(restrictedTenantDTO.getSettings().getSmtp().getPassword()).isNull();
  }

  @Test
  void toRestrictedTenantDTO_should_convertDefaultValuesForSettingsInCaseOfNull() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .build();
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // when
    RestrictedTenantDTO restrictedTenantDTO =
        tenantConverter.toRestrictedTenantDTO(entity, TenantConverter.DE);

    // then
    assertThat(restrictedTenantDTO.getName()).isEqualTo(tenantDTO.getName());
    assertThat(restrictedTenantDTO.getId()).isEqualTo(tenantDTO.getId());
    assertThat(restrictedTenantDTO.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(restrictedTenantDTO.getTheming()).isEqualTo(asRead(tenantDTO.getTheming()));
    assertContentIsProperlyConverted(tenantDTO, restrictedTenantDTO);
    assertThat(restrictedTenantDTO.getContent().getRenderedPrivacy())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getPrivacy()));
    assertThat(restrictedTenantDTO.getSettings()).isEqualTo(new Settings());
  }

  @Test
  void toDTO_should_preserveAppearancePermissionToggle() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .allowedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().appearance(false)));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(
            converted
                .getSettings()
                .getTenantAdminControls()
                .getAllowedPermissionToggles()
                .getAppearance())
        .isFalse();
  }

  @Test
  void toDTO_should_defaultMissingAppearancePermissionToggleToTrue() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles()));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(
            converted
                .getSettings()
                .getTenantAdminControls()
                .getAllowedPermissionToggles()
                .getAppearance())
        .isTrue();
  }

  @Test
  void toDTO_should_preserveEnforcedPermissionToggle() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .enforcedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().videoCalls(true)));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(
            converted
                .getSettings()
                .getTenantAdminControls()
                .getEnforcedPermissionToggles()
                .getVideoCalls())
        .isTrue();
  }

  @Test
  void toDTO_should_defaultMissingEnforcedPermissionToggleToFalse() {
    // given - unlike allowed (defaults true), an unset enforced flag means "not enforced" = false
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .enforcedPermissionToggles(new TenantAdminAllowedPermissionToggles()));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(
            converted
                .getSettings()
                .getTenantAdminControls()
                .getEnforcedPermissionToggles()
                .getVideoCalls())
        .isFalse();
  }

  @Test
  void toDTO_should_keepEnforcedTogglesNullWhenAbsent() {
    // given - a legacy row / DTO without enforcedPermissionToggles stays null (nothing enforced)
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .tenantAdminControls(
            new TenantAdminControls()
                .allowedPermissionToggles(new TenantAdminAllowedPermissionToggles()));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(converted.getSettings().getTenantAdminControls().getEnforcedPermissionToggles())
        .isNull();
  }

  @Test
  void tenantAdminControls_shouldRoundTripTypedPermissionPolicies() {
    TenantAdminControls controls =
        new TenantAdminControls()
            .permissionPolicies(
                Map.of(
                    "featureSupervisionEnabled",
                    new BooleanPermissionPolicy(true, PermissionPolicyMode.ENFORCED),
                    "featureVideoCallsEnabled",
                    new BooleanPermissionPolicy(false, PermissionPolicyMode.SUGGESTED)));

    TenantAdminControls converted =
        tenantConverter.toTenantAdminControls(
            tenantConverter.toTenantAdminControlsSettings(controls));

    assertThat(converted.getPermissionPolicies()).isEqualTo(controls.getPermissionPolicies());
  }

  @Test
  void toDTO_should_neverExposeSmtpPassword_butReportPasswordSet() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO
        .getSettings()
        .featureToolsOICDToken("secret-oidc-token")
        .smtp(
            new SmtpConfig()
                .enabled(true)
                .host("smtp.example.org")
                .port(587)
                .secure(false)
                .username("smtp-user")
                .password("smtp-pass")
                .from("notifications@example.org")
                .emailThemeColor("#123456"));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(converted.getSettings().getFeatureToolsOICDToken()).isEqualTo("secret-oidc-token");
    assertThat(converted.getSettings().getSmtp()).isNotNull();
    assertThat(converted.getSettings().getSmtp().getUsername()).isEqualTo("smtp-user");
    assertThat(converted.getSettings().getSmtp().getPassword()).isNull();
    assertThat(converted.getSettings().getSmtp().getPasswordSet()).isTrue();
  }

  @Test
  void smtpModeRoundTripsAndIsAbsentFromPublicTenant() {
    for (Settings.SmtpModeEnum mode : Settings.SmtpModeEnum.values()) {
      MultilingualTenantDTO tenantDTO =
          new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
      tenantDTO.getSettings().smtpMode(mode);

      TenantEntity entity = tenantConverter.toEntity(tenantDTO);

      assertThat(tenantConverter.toDTO(entity, "de").getSettings().getSmtpMode()).isEqualTo(mode);
      assertThat(tenantConverter.toRestrictedTenantDTO(entity, "de").getSettings().getSmtpMode())
          .isNull();
    }
  }

  @Test
  void toDTO_should_returnNoSmtpMode_When_storedSettingsHaveNone() {
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO.getSettings().smtpMode(null);

    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    assertThat(tenantConverter.toDTO(entity, "de").getSettings().getSmtpMode()).isNull();
  }

  @Test
  void toDTO_should_reportPasswordSetFalse_When_noSmtpPasswordStored() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO.getSettings().smtp(new SmtpConfig().enabled(true).host("smtp.example.org"));

    // when
    TenantDTO converted = tenantConverter.toDTO(tenantConverter.toEntity(tenantDTO), "de");

    // then
    assertThat(converted.getSettings().getSmtp().getPassword()).isNull();
    assertThat(converted.getSettings().getSmtp().getPasswordSet()).isFalse();
  }

  @Test
  void toEntity_should_encryptSmtpPassword_When_encryptionConfigured() {
    // given
    var encryptionService = new SmtpPasswordEncryptionService("unit-test-secret");
    var encryptingConverter =
        new TenantConverter(
            templateService, templateRenderer, encryptionService, platformLegalTextTokens);
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO.getSettings().smtp(new SmtpConfig().enabled(true).password("plain-secret"));

    // when
    TenantEntity entity = encryptingConverter.toEntity(tenantDTO);

    // then
    assertThat(entity.getSettings()).doesNotContain("plain-secret").contains("ENC:");
  }

  @Test
  void toEntity_should_normalizeBlankOrMaskedSmtpPasswordToNull() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    tenantDTO.getSettings().smtp(new SmtpConfig().enabled(true).password("********"));

    // when
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // then
    assertThat(entity.getSettings()).doesNotContain("********");
  }

  private static void assertContentIsProperlyConverted(
      MultilingualTenantDTO tenantDTO, RestrictedTenantDTO restrictedTenantDTO) {
    assertThat(restrictedTenantDTO.getContent().getClaim())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getClaim()));
    assertThat(restrictedTenantDTO.getContent().getPrivacy())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getPrivacy()));
    assertThat(restrictedTenantDTO.getContent().getTermsAndConditions())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getTermsAndConditions()));
    assertThat(restrictedTenantDTO.getContent().getImpressum())
        .isEqualTo(getGermanTranslation(tenantDTO.getContent().getImpressum()));
  }

  private static void assertCoreSettingsAreConverted(Settings expected, Settings actual) {
    assertThat(actual.getFeatureStatisticsEnabled())
        .isEqualTo(expected.getFeatureStatisticsEnabled());
    assertThat(actual.getFeatureTopicsEnabled()).isEqualTo(expected.getFeatureTopicsEnabled());
    assertThat(actual.getTopicsInRegistrationEnabled())
        .isEqualTo(expected.getTopicsInRegistrationEnabled());
    assertThat(actual.getFeatureDemographicsEnabled())
        .isEqualTo(expected.getFeatureDemographicsEnabled());
    assertThat(actual.getFeatureAppointmentsEnabled())
        .isEqualTo(expected.getFeatureAppointmentsEnabled());
    assertThat(actual.getFeatureGroupChatV2Enabled())
        .isEqualTo(expected.getFeatureGroupChatV2Enabled());
    assertThat(actual.getFeatureTeamDiscussionEnabled())
        .isEqualTo(expected.getFeatureTeamDiscussionEnabled());
    assertThat(actual.getFeatureToolsEnabled()).isEqualTo(expected.getFeatureToolsEnabled());
    assertThat(actual.getFeatureMediaUploadEnabled())
        .isEqualTo(expected.getFeatureMediaUploadEnabled());
    assertThat(actual.getFeatureMediaUploadAnonymousChatsEnabled())
        .isEqualTo(expected.getFeatureMediaUploadAnonymousChatsEnabled());
    assertThat(actual.getFeatureMediaInlineDisplayEnabled())
        .isEqualTo(expected.getFeatureMediaInlineDisplayEnabled());
    assertThat(actual.getFeatureMediaAiScanEnabled())
        .isEqualTo(expected.getFeatureMediaAiScanEnabled());
    assertThat(actual.getFeatureToolsOICDToken()).isEqualTo(expected.getFeatureToolsOICDToken());
    assertThat(actual.getActiveLanguages()).isEqualTo(expected.getActiveLanguages());
    assertThat(actual.getShowAskerProfile()).isEqualTo(expected.getShowAskerProfile());
    assertThat(actual.getIsVideoCallAllowed()).isEqualTo(expected.getIsVideoCallAllowed());
    assertThat(actual.getEmailVisible()).isEqualTo(expected.getEmailVisible());
    assertThat(actual.getEmailRequired()).isEqualTo(expected.getEmailRequired());
    assertThat(actual.getFeatureCentralDataProtectionTemplateEnabled())
        .isEqualTo(expected.getFeatureCentralDataProtectionTemplateEnabled());
    assertThat(actual.getTenantAdminControls()).isEqualTo(expected.getTenantAdminControls());
  }

  private static void assertRestrictedPublicSettingsAreConverted(
      Settings expected, Settings actual) {
    assertThat(actual.getFeatureStatisticsEnabled())
        .isEqualTo(expected.getFeatureStatisticsEnabled());
    assertThat(actual.getFeatureTopicsEnabled()).isEqualTo(expected.getFeatureTopicsEnabled());
    assertThat(actual.getTopicsInRegistrationEnabled())
        .isEqualTo(expected.getTopicsInRegistrationEnabled());
    assertThat(actual.getFeatureDemographicsEnabled())
        .isEqualTo(expected.getFeatureDemographicsEnabled());
    assertThat(actual.getFeatureAppointmentsEnabled())
        .isEqualTo(expected.getFeatureAppointmentsEnabled());
    assertThat(actual.getFeatureGroupChatV2Enabled())
        .isEqualTo(expected.getFeatureGroupChatV2Enabled());
    assertThat(actual.getFeatureTeamDiscussionEnabled())
        .isEqualTo(expected.getFeatureTeamDiscussionEnabled());
    assertThat(actual.getFeatureToolsEnabled()).isEqualTo(expected.getFeatureToolsEnabled());
    assertThat(actual.getFeatureMediaUploadEnabled())
        .isEqualTo(expected.getFeatureMediaUploadEnabled());
    assertThat(actual.getFeatureMediaUploadAnonymousChatsEnabled())
        .isEqualTo(expected.getFeatureMediaUploadAnonymousChatsEnabled());
    assertThat(actual.getFeatureMediaInlineDisplayEnabled())
        .isEqualTo(expected.getFeatureMediaInlineDisplayEnabled());
    assertThat(actual.getFeatureMediaAiScanEnabled())
        .isEqualTo(expected.getFeatureMediaAiScanEnabled());
    assertThat(actual.getActiveLanguages()).isEqualTo(expected.getActiveLanguages());
    assertThat(actual.getShowAskerProfile()).isEqualTo(expected.getShowAskerProfile());
    assertThat(actual.getIsVideoCallAllowed()).isEqualTo(expected.getIsVideoCallAllowed());
    assertThat(actual.getEmailVisible()).isEqualTo(expected.getEmailVisible());
    assertThat(actual.getEmailRequired()).isEqualTo(expected.getEmailRequired());
    assertThat(actual.getFeatureCentralDataProtectionTemplateEnabled())
        .isEqualTo(expected.getFeatureCentralDataProtectionTemplateEnabled());
    assertThat(actual.getTenantAdminControls()).isEqualTo(expected.getTenantAdminControls());
    assertThat(actual.getFeatureToolsOICDToken()).isNull();
    if (expected.getSmtp() == null) {
      assertThat(actual.getSmtp()).isNull();
      return;
    }

    assertThat(actual.getSmtp()).isNotNull();
    assertThat(actual.getSmtp().getEnabled()).isEqualTo(expected.getSmtp().getEnabled());
    assertThat(actual.getSmtp().getHost()).isEqualTo(expected.getSmtp().getHost());
    assertThat(actual.getSmtp().getPort()).isEqualTo(expected.getSmtp().getPort());
    assertThat(actual.getSmtp().getSecure()).isEqualTo(expected.getSmtp().getSecure());
    assertThat(actual.getSmtp().getFrom()).isEqualTo(expected.getSmtp().getFrom());
    assertThat(actual.getSmtp().getEmailThemeColor())
        .isEqualTo(expected.getSmtp().getEmailThemeColor());
    assertThat(actual.getSmtp().getUsername()).isNull();
    assertThat(actual.getSmtp().getPassword()).isNull();
  }

  private static String getGermanTranslation(Map<String, String> translations) {
    return translations.get("de");
  }

  @Test
  void toDTO_should_applyPerFieldDefaults_When_storedSettingsJsonIsEmpty() {
    // given — all keys absent from stored JSON
    TenantEntity entity = TenantEntity.builder().settings("{}").build();

    // when
    Settings settings = tenantConverter.toDTO(entity, "de").getSettings();

    // then — false-default fields (sample of 3)
    assertThat(settings.getFeatureDemographicsEnabled()).isFalse();
    assertThat(settings.getFeatureStatisticsEnabled()).isFalse();
    assertThat(settings.getFeatureToolsEnabled()).isFalse();
    assertThat(settings.getEmailVisible()).isFalse();
    assertThat(settings.getEmailRequired()).isFalse();
    // then — true-default fields (sample of 3)
    assertThat(settings.getFeatureAudioCallsEnabled()).isTrue();
    assertThat(settings.getFeatureAnonymousChatEnabled()).isTrue();
    assertThat(settings.getFeatureCallsEnabled()).isTrue();
  }

  @Test
  void toDTO_should_respectExplicitFalse_When_trueDefaultFieldIsSetToFalseInJson() {
    // given — one true-default field explicitly false; all other keys absent
    TenantEntity entity =
        TenantEntity.builder().settings("{\"featureAudioCallsEnabled\":false}").build();

    // when
    Settings settings = tenantConverter.toDTO(entity, "de").getSettings();

    // then
    assertThat(settings.getFeatureAudioCallsEnabled()).isFalse();
    assertThat(settings.getFeatureDemographicsEnabled()).isFalse();
    assertThat(settings.getFeatureAnonymousChatEnabled()).isTrue();
    assertThat(settings.getFeatureStatisticsEnabled()).isFalse();
  }

  @Test
  void toDTO_should_notCrossContaminate_When_jsonContainsMisspelledFieldName() {
    // given — typo must not satisfy .contains() for the correctly-spelled key
    TenantEntity entity =
        TenantEntity.builder().settings("{\"featureAudioCalls_Enabled\":false}").build();

    // when
    Settings settings = tenantConverter.toDTO(entity, "de").getSettings();

    // then — correctly-spelled field keeps its true default; typo is ignored
    assertThat(settings.getFeatureAudioCallsEnabled()).isTrue();
    assertThat(settings.getFeatureDemographicsEnabled()).isFalse();
  }

  @Test
  void toBasicLicensingTenantDTO_should_convertAttributesProperly() {
    // given
    MultilingualTenantDTO tenantDTO =
        new MultilingualTenantTestDataBuilder()
            .tenantDTO()
            .withContent()
            .withTheming()
            .withLicensing()
            .build();
    TenantEntity entity = tenantConverter.toEntity(tenantDTO);

    // when
    BasicTenantLicensingDTO basicTenantLicensingDTO =
        tenantConverter.toBasicLicensingTenantDTO(entity);

    // then
    assertThat(basicTenantLicensingDTO.getId()).isEqualTo(tenantDTO.getId());
    assertThat(basicTenantLicensingDTO.getCreateDate()).isEqualTo(tenantDTO.getCreateDate());
    assertThat(basicTenantLicensingDTO.getUpdateDate()).isEqualTo(tenantDTO.getUpdateDate());
    assertThat(basicTenantLicensingDTO.getName()).isEqualTo(tenantDTO.getName());
    assertThat(basicTenantLicensingDTO.getSubdomain()).isEqualTo(tenantDTO.getSubdomain());
    assertThat(basicTenantLicensingDTO.getLicensing()).isEqualTo(tenantDTO.getLicensing());
  }

  /**
   * The theming a reader gets back for a given input theming.
   *
   * <p>Only one field is not an identity on the round trip: an unconfigured login effect is stored
   * as NULL but read as NONE, so "never configured" stays a storage fact while the API always
   * answers with an effect. Normalising here keeps these assertions about everything else.
   */
  private Theming asRead(Theming written) {
    if (written == null || written.getLoginEffect() != null) {
      return written;
    }
    return written.loginEffect(Theming.LoginEffectEnum.NONE);
  }

  // --- Träger DPO (ORISO-Admin#1067)

  @Test
  void dataProtectionOfficer_Should_roundTripThroughTheEntity_And_dropBlankFields() {
    var dto = new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();
    dto.setDataProtectionOfficer(
        new com.vi.tenantservice.api.model.TenantDataProtectionOfficerDTO()
            .nameAndLegalForm(" Dr. Maria Muster ")
            .email("datenschutz@example.org")
            .street(" "));

    var entity = tenantConverter.toEntity(dto);
    var back = tenantConverter.toMultilingualDTO(entity).getDataProtectionOfficer();

    assertThat(back.getNameAndLegalForm()).isEqualTo("Dr. Maria Muster");
    assertThat(back.getEmail()).isEqualTo("datenschutz@example.org");
    assertThat(back.getStreet()).isNull();
    assertThat(tenantConverter.toRestrictedTenantDTO(entity, "de").getDataProtectionOfficer())
        .isEqualTo(back);
  }

  @Test
  void dataProtectionOfficer_Should_keepStoredValue_When_absent_And_clear_When_allBlank() {
    var stored =
        TenantEntity.builder()
            .dataProtectionOfficer("{\"nameAndLegalForm\":\"Dr. Maria Muster\"}")
            .build();
    var withoutDpo = new MultilingualTenantTestDataBuilder().tenantDTO().withSettings().build();

    tenantConverter.toEntity(stored, withoutDpo);
    assertThat(stored.getDataProtectionOfficer()).contains("Dr. Maria Muster");

    withoutDpo.setDataProtectionOfficer(
        new com.vi.tenantservice.api.model.TenantDataProtectionOfficerDTO().nameAndLegalForm(" "));
    tenantConverter.toEntity(stored, withoutDpo);
    assertThat(stored.getDataProtectionOfficer()).isNull();
  }

  @Test
  void dataProtectionOfficer_Should_readAsNotEntered_When_storedJsonIsDamaged() {
    var stored = TenantEntity.builder().dataProtectionOfficer("{not json").build();
    assertThat(tenantConverter.toMultilingualDTO(stored).getDataProtectionOfficer()).isNull();
  }
}
