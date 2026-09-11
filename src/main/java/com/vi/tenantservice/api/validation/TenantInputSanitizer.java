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
    output.setAddress(inputSanitizer.sanitize(input.getAddress()));
    output.setDescription(inputSanitizer.sanitize(input.getDescription()));
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
      // Assets are URLs, not markup — HTML-sanitizing them encoded the base64
      // payload ("+" -> "&#43;") and left an undecodable data URL behind, which
      // is what made the tenant logo and favicon disappear from every public
      // surface. See InputSanitizer#sanitizeAssetUrl.
      output.getTheming().setLogo(inputSanitizer.sanitizeAssetUrl(theming.getLogo()));
      output.getTheming().setFavicon(inputSanitizer.sanitizeAssetUrl(theming.getFavicon()));
      output
          .getTheming()
          .setAssociationLogo(inputSanitizer.sanitizeAssetUrl(theming.getAssociationLogo()));
      // Blank/whitespace seeds must stay absent (null), not persist as "". An empty
      // string is what made the public app discard the entire tenant palette.
      output.getTheming().setPrimaryColor(sanitizeSeed(theming.getPrimaryColor()));
      output.getTheming().setSecondaryColor(sanitizeSeed(theming.getSecondaryColor()));
      output.getTheming().setAccent(sanitizeSeed(theming.getAccent()));
      output.getTheming().setSignal(sanitizeSeed(theming.getSignal()));
      // loginEffect is a generated enum, not free text — there is nothing to strip,
      // and the enum itself is the whitelist.
      output.getTheming().setLoginEffect(theming.getLoginEffect());
    }
  }

  private String sanitizeSeed(String value) {
    String sanitized = inputSanitizer.sanitize(value);
    return sanitized == null || sanitized.isBlank() ? null : sanitized;
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
