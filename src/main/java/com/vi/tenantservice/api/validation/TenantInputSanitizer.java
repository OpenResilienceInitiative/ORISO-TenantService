package com.vi.tenantservice.api.validation;

import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Theming;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class TenantInputSanitizer {

  private final @NonNull InputSanitizer inputSanitizer;

  public MultilingualTenantDTO sanitize(MultilingualTenantDTO input) {
    log.info("Sanitizing input DTO");
    MultilingualTenantDTO output = copyNotSanitizedAttributes(input);
    output.setName(inputSanitizer.sanitize(input.getName()));
    output.setSubdomain(inputSanitizer.sanitize(input.getSubdomain()));
    sanitizeTheming(input, output);
    sanitizeContent(input, output);
    return output;
  }

  private MultilingualTenantDTO copyNotSanitizedAttributes(MultilingualTenantDTO input) {
    MultilingualTenantDTO output = new MultilingualTenantDTO();
    output.setId(input.getId());
    output.setCreateDate(input.getCreateDate());
    output.setUpdateDate(input.getUpdateDate());
    output.setContent(new MultilingualContent());
    output.setTheming(new Theming());
    output.setLicensing(input.getLicensing());
    output.setSettings(input.getSettings());
    return output;
  }

  private void sanitizeTheming(MultilingualTenantDTO input, MultilingualTenantDTO output) {
    Theming theming = input.getTheming();
    if (theming != null) {
      output.getTheming().setLogo(inputSanitizer.sanitize(theming.getLogo()));
      output.getTheming().setFavicon(inputSanitizer.sanitize(theming.getFavicon()));
      output.getTheming().setPrimaryColor(inputSanitizer.sanitize(theming.getPrimaryColor()));
      output.getTheming().setSecondaryColor(inputSanitizer.sanitize(theming.getSecondaryColor()));
      output.getTheming().setAssociationLogo(inputSanitizer.sanitize(theming.getAssociationLogo()));
    }
  }

  private void sanitizeContent(MultilingualTenantDTO input, MultilingualTenantDTO output) {
    var content = input.getContent();
    if (content != null) {
      output
          .getContent()
          .setImpressum(
              sanitizeAllTranslations(
                  content.getImpressum(), inputSanitizer::sanitizeAllowingFormattingAndLinks));
      output
          .getContent()
          .setClaim(
              sanitizeAllTranslations(
                  content.getClaim(), inputSanitizer::sanitizeAllowingFormatting));
      output
          .getContent()
          .setPrivacy(
              sanitizeAllTranslations(
                  content.getPrivacy(), inputSanitizer::sanitizeAllowingFormattingAndLinks));
      output
          .getContent()
          .setTermsAndConditions(
              sanitizeAllTranslations(
                  content.getTermsAndConditions(),
                  inputSanitizer::sanitizeAllowingFormattingAndLinks));
      output.getContent().setConfirmPrivacy(content.getConfirmPrivacy());
      output.getContent().setConfirmTermsAndConditions(content.getConfirmTermsAndConditions());
    }
  }

  private Map<String, String> sanitizeAllTranslations(
      Map<String, String> translations, Function<String, String> sanitizeFuntion) {
    if (translations != null) {
      return translations.entrySet().stream()
          .filter(entry -> entry.getKey() != null)
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey, stringEntry -> sanitizeFuntion.apply(stringEntry.getValue())));
    }
    return translations;
  }
}
