package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// The "testing" profile targets MariaDB and expects datasource/Keycloak/service settings
// to be supplied via env (empty during the build), which the ConfigurationValidator rejects
// on startup. Override them with an embedded H2 datasource and dummy config values so the
// full application context can start for this template test.
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.datasource.url=jdbc:h2:mem:templateservicetest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=sa",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false",
      "keycloak.auth-server-url=http://localhost:8080/auth",
      "keycloak.realm=test",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/auth/realms/test",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/auth/realms/test/protocol/openid-connect/certs",
      "consulting.type.service.api.url=http://localhost:8080",
      "user.service.api.url=http://localhost:8080"
    })
class TemplateServiceTest {

  @Autowired TemplateService templateService;

  @Test
  void getDefaultGermanDataProtectionTemplate_Should_ReturnTemplateDTO()
      throws TemplateDescriptionServiceException {

    // given, when
    var template = templateService.getDefaultDataProtectionTemplate();
    // then
    assertThat(template).isNotNull();
    assertThat(
            template
                .getAgencyContext()
                .getDataProtectionOfficer()
                .getDataProtectionOfficerContact())
        .contains(
            "<#if name?exists>${name}<br/></#if><#if postCode?exists>postcode: ${postCode}<br/></#if><#if city?exists> city: ${city}<br/></#if><#if phoneNumber?exists>phoneNumber: ${phoneNumber}<br/></#if><#if email?exists>email: ${email}</#if>");
    assertThat(
            template
                .getAgencyContext()
                .getDataProtectionOfficer()
                .getAlternativeRepresentativeContact())
        .contains("Die oder der Verantwortliche");
    assertThat(template.getNoAgencyContext().getResponsibleContact())
        .contains("Der Verantwortliche kann erst angezeigt werden");
  }

  @Test
  void getMultilingualDataProtectionTemplate_Should_ReturnMapOfLanguagesForEnglishAndGerman()
      throws TemplateDescriptionServiceException {

    // given, when
    var template = templateService.getMultilingualDataProtectionTemplate();
    // then
    assertThat(template.get("en")).isNotNull();
    assertThat(template.get("de")).isNotNull();
    assertThat(template).containsEntry("de", templateService.getDefaultDataProtectionTemplate());
    assertThat(
            template
                .get("en")
                .getAgencyContext()
                .getDataProtectionOfficer()
                .getDataProtectionOfficerContact())
        .isNotNull();
    assertThat(
            template
                .get("en")
                .getAgencyContext()
                .getDataProtectionOfficer()
                .getAgencyResponsibleContact())
        .isNotNull();
    assertThat(
            template
                .get("en")
                .getAgencyContext()
                .getDataProtectionOfficer()
                .getAlternativeRepresentativeContact())
        .isNotNull();
  }
}
