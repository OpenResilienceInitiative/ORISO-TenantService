package com.vi.tenantservice.api.util;

import com.google.common.collect.Lists;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.Theming;
import java.util.List;

public class TenantTestDataBuilder {

  private static final String SUBDOMAIN = "subdomain";
  private static final String NAME = "name";
  private static final long ID = 1L;
  private static final String IMPRESSUM = "Impressum";
  private static final String CLAIM = "claim";
  private static final String SECONDARY_COLOR = "secondaryColor";
  private static final String PRIMARY_COLOR = "primary color";
  private static final String FAVICON = "favicon";
  private static final String LOGO = "logo";
  private static final int ALLOWED_NUMBER_OF_USERS = 2000;
  private static final String PRIVACY = "privacy";
  private static final String TERMS_AND_CONDITIONS = "termsandconditions";

  TenantDTO tenantDTO = new TenantDTO();

  public TenantTestDataBuilder tenantDTO() {
    tenantDTO = new TenantDTO();
    tenantDTO.setId(ID);
    tenantDTO.setName(NAME);
    tenantDTO.setSubdomain(SUBDOMAIN);
    return this;
  }

  public TenantTestDataBuilder withId(Long id) {
    tenantDTO.setId(id);
    return this;
  }

  public TenantTestDataBuilder withName(String name) {
    tenantDTO.setName(name);
    return this;
  }

  public TenantTestDataBuilder withSubdomain(String subdomain) {
    tenantDTO.setSubdomain(subdomain);
    return this;
  }

  public TenantTestDataBuilder withSettingTopicsInRegistrationEnabled(
      boolean topicsInRegistrationEnabled) {
    tenantDTO.setSettings(getSettings().topicsInRegistrationEnabled(topicsInRegistrationEnabled));
    return this;
  }

  public TenantTestDataBuilder withSettingActiveLanguages(List<String> activeLanguages) {
    tenantDTO.setSettings(
        getSettings().topicsInRegistrationEnabled(true).activeLanguages(activeLanguages));
    return this;
  }

  public TenantTestDataBuilder withContent() {
    tenantDTO.setContent(content());
    return this;
  }

  public TenantTestDataBuilder withContent(String impressum, String claim) {
    Content content = new Content();
    content.setImpressum(impressum);
    content.setClaim(claim);
    tenantDTO.setContent(content);
    return this;
  }

  public TenantTestDataBuilder withTheming() {
    tenantDTO.setTheming(theming());
    return this;
  }

  public TenantTestDataBuilder withLicensing() {
    tenantDTO.setLicensing(licensing());
    return this;
  }

  public TenantTestDataBuilder withSettings() {
    tenantDTO.setSettings(
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
        .featureAttachmentUploadDisabled(false);
  }

  private Licensing licensing() {
    Licensing licensing = new Licensing();
    licensing.setAllowedNumberOfUsers(ALLOWED_NUMBER_OF_USERS);
    return licensing;
  }

  public TenantDTO build() {
    return tenantDTO;
  }

  private Theming theming() {
    Theming theming = new Theming();
    theming.setSecondaryColor(SECONDARY_COLOR);
    theming.setPrimaryColor(PRIMARY_COLOR);
    theming.setFavicon(FAVICON);
    theming.setLogo(LOGO);
    return theming;
  }

  private Content content() {
    Content content = new Content();
    content.setImpressum(IMPRESSUM);
    content.setClaim(CLAIM);
    content.setPrivacy(PRIVACY);
    content.setTermsAndConditions(TERMS_AND_CONDITIONS);
    return content;
  }

  public String jsonify() {
    TenantDTO build = build();
    return JsonConverter.convertToJson(build);
  }
}
