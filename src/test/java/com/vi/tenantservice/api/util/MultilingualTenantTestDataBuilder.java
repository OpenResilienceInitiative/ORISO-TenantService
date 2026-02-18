package com.vi.tenantservice.api.util;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.NoAgencyContextDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.Theming;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultilingualTenantTestDataBuilder {

  private static final String SUBDOMAIN = "subdomain";
  private static final String NAME = "name";
  private static final long ID = 1L;
  private static final String IMPRESSUM = "Impressum";
  private static final String CLAIM = "claim";
  private static final String SECONDARY_COLOR = "secondaryColor";
  private static final String PRIMARY_COLOR = "primary color";
  private static final String FAVICON = "favicon";
  private static final String LOGO = "logo";
  private static final int ALLOWED_NUMBER_OF_CONSULTANTS = 2000;
  private static final String PRIVACY = "privacy";
  private static final String TERMS_AND_CONDITIONS = "termsandconditions";

  MultilingualTenantDTO tenantMultilingualDTO = new MultilingualTenantDTO();

  public MultilingualTenantTestDataBuilder tenantDTO() {
    tenantMultilingualDTO = new MultilingualTenantDTO();
    tenantMultilingualDTO.setId(ID);
    tenantMultilingualDTO.setName(NAME);
    tenantMultilingualDTO.setSubdomain(SUBDOMAIN);
    return this;
  }

  public MultilingualTenantTestDataBuilder withId(Long id) {
    tenantMultilingualDTO.setId(id);
    return this;
  }

  public MultilingualTenantTestDataBuilder withName(String name) {
    tenantMultilingualDTO.setName(name);
    return this;
  }

  public MultilingualTenantTestDataBuilder withSubdomain(String subdomain) {
    tenantMultilingualDTO.setSubdomain(subdomain);
    return this;
  }

  public MultilingualTenantTestDataBuilder withSettingTopicsInRegistrationEnabled(
      boolean topicsInRegistrationEnabled) {
    tenantMultilingualDTO.setSettings(
        getSettings().topicsInRegistrationEnabled(topicsInRegistrationEnabled));
    return this;
  }

  public MultilingualTenantTestDataBuilder withSettingActiveLanguages(
      List<String> activeLanguages) {
    tenantMultilingualDTO.setSettings(
        getSettings().topicsInRegistrationEnabled(true).activeLanguages(activeLanguages));
    return this;
  }

  public MultilingualTenantTestDataBuilder withContent() {
    tenantMultilingualDTO.setContent(content());
    return this;
  }

  public MultilingualTenantTestDataBuilder withTheming() {
    tenantMultilingualDTO.setTheming(theming());
    return this;
  }

  public MultilingualTenantTestDataBuilder withLicensing() {
    tenantMultilingualDTO.setLicensing(licensing(ALLOWED_NUMBER_OF_CONSULTANTS));
    return this;
  }

  public MultilingualTenantTestDataBuilder withLicensing(int numberOfConsultants) {
    tenantMultilingualDTO.setLicensing(licensing(numberOfConsultants));
    return this;
  }

  public MultilingualTenantTestDataBuilder withSettings() {
    tenantMultilingualDTO.setSettings(
        getSettings()
            .topicsInRegistrationEnabled(true)
            .activeLanguages(Lists.newArrayList("de", "en")));
    return this;
  }

  private static Settings getSettings() {
    return new Settings()
        .featureTopicsEnabled(true)
        .featureDemographicsEnabled(true)
        .featureAppointmentsEnabled(true)
        .featureStatisticsEnabled(true)
        .featureTopicsEnabled(true)
        .featureGroupChatV2Enabled(true)
        .featureToolsEnabled(true)
        .featureToolsOICDToken("1234")
        .featureAttachmentUploadDisabled(false)
        .showAskerProfile(true)
        .isVideoCallAllowed(true)
        .featureCentralDataProtectionTemplateEnabled(true)
        .extendedSettings(
            new ConsultingTypePatchDTO().isVideoCallAllowed(true).languageFormal(true));
  }

  private Licensing licensing(int numberOfUsers) {
    Licensing licensing = new Licensing();
    licensing.setAllowedNumberOfUsers(numberOfUsers);
    return licensing;
  }

  public MultilingualTenantDTO build() {
    return tenantMultilingualDTO;
  }

  private Theming theming() {
    Theming theming = new Theming();
    theming.setSecondaryColor(SECONDARY_COLOR);
    theming.setPrimaryColor(PRIMARY_COLOR);
    theming.setFavicon(FAVICON);
    theming.setLogo(LOGO);
    return theming;
  }

  private MultilingualContent content() {
    MultilingualContent content = new MultilingualContent();
    content.setImpressum(defaultTranslations(IMPRESSUM));
    content.setClaim(defaultTranslations(CLAIM));
    content.setPrivacy(defaultTranslations(PRIVACY));
    content.setTermsAndConditions(defaultTranslations(TERMS_AND_CONDITIONS));
    content.setDataProtectionContactTemplate(
        Map.of(
            "de",
            new DataProtectionContactTemplateDTO()
                .noAgencyContext(
                    new NoAgencyContextDTO()
                        .responsibleContact("responsibleContact placeholder text")
                        .dataProtectionOfficerContact(
                            "dataProtectionOfficerContact placeholder text"))));
    return content;
  }

  private Map<String, String> defaultTranslations(String content) {
    var map = new HashMap<String, String>();
    map.put("de", content);
    return map;
  }

  public String jsonify() {
    MultilingualTenantDTO build = build();
    return JsonConverter.convertToJson(build);
  }

  public MultilingualTenantTestDataBuilder withContent(String impressum, String claim) {
    MultilingualContent content = new MultilingualContent();
    content.setImpressum(defaultTranslations(impressum));
    content.setClaim(defaultTranslations(claim));
    tenantMultilingualDTO.setContent(content);
    return this;
  }

  public MultilingualTenantTestDataBuilder withTranslatedImpressum(
      String language, String impressum) {
    MultilingualContent content = new MultilingualContent();
    var translatedMap = new HashMap<String, String>();
    translatedMap.put(language, impressum);
    content.setImpressum(translatedMap);
    content.setClaim(defaultTranslations(""));
    tenantMultilingualDTO.setContent(content);
    return this;
  }
}
