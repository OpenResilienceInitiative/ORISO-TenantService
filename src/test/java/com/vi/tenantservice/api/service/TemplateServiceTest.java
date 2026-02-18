package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=testing")
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
