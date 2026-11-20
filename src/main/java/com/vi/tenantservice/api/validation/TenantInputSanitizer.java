package com.vi.tenantservice.api.validation;

import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.TenantDataProtectionOfficerDTO;
import com.vi.tenantservice.api.model.Theming;
import com.vi.tenantservice.api.util.TranslationMetaUtil;
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
    // null must survive as null: on update it means "keep the stored value", not "clear it".
    output.setLegalName(inputSanitizer.sanitizePlainText(input.getLegalName()));
    output.setContactEmail(inputSanitizer.sanitizePlainText(input.getContactEmail()));
    output.setContactPhone(inputSanitizer.sanitizePlainText(input.getContactPhone()));
    output.setDataProtectionOfficer(
        sanitizeDataProtectionOfficer(input.getDataProtectionOfficer()));
    sanitizeTheming(input, output);
    sanitizeContent(input, output);
    return output;
  }

  /** Plain text like the sender block; null stays null ("keep" on update). */
  private TenantDataProtectionOfficerDTO sanitizeDataProtectionOfficer(
      TenantDataProtectionOfficerDTO input) {
    if (input == null) {
      return null;
    }
    return new TenantDataProtectionOfficerDTO()
        .nameAndLegalForm(inputSanitizer.sanitizePlainText(input.getNameAndLegalForm()))
        .street(inputSanitizer.sanitizePlainText(input.getStreet()))
        .postcode(inputSanitizer.sanitizePlainText(input.getPostcode()))
        .city(inputSanitizer.sanitizePlainText(input.getCity()))
        .phoneNumber(inputSanitizer.sanitizePlainText(input.getPhoneNumber()))
        .email(inputSanitizer.sanitizePlainText(input.getEmail()));
  }

  private MultilingualTenantDTO copyNotSanitizedAttributes(MultilingualTenantDTO input) {
    MultilingualTenantDTO output = new MultilingualTenantDTO();
    output.setId(input.getId());
    output.setActive(input.getActive());
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
      output.getTheming().setAssistantName(theming.getAssistantName());
      output.getTheming().setAssistantIcon(theming.getAssistantIcon());
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
      var out = output.getContent();
      // Erstantwort Bausteine: same HTML policy as the legal texts. `<lang>__meta` keys are strict
      // JSON (machine-translation provenance) and must survive verbatim.
      out.setErstantwortGreeting(sanitizeBaustein(content.getErstantwortGreeting()));
      out.setErstantwortWhoReadsAlong(sanitizeBaustein(content.getErstantwortWhoReadsAlong()));
      out.setErstantwortEmergencyAddition(
          sanitizeBaustein(content.getErstantwortEmergencyAddition()));
      out.setErstantwortFreeNotice(sanitizeBaustein(content.getErstantwortFreeNotice()));
      out.setErstantwortClosing(sanitizeBaustein(content.getErstantwortClosing()));
      out.setErstantwortResponseDeadlineDays(content.getErstantwortResponseDeadlineDays());
      output.getContent().setConfirmPrivacy(content.getConfirmPrivacy());
      output.getContent().setConfirmTermsAndConditions(content.getConfirmTermsAndConditions());
    }
  }

  /** Null values become "" (the clear signal) instead of failing the whole save. */
  private Map<String, String> sanitizeBaustein(Map<String, String> translations) {
    if (translations == null) {
      return null;
    }
    var blankSafe = new java.util.LinkedHashMap<String, String>();
    translations.forEach((key, value) -> blankSafe.put(key, value == null ? "" : value));
    return sanitizeAllTranslations(
        blankSafe,
        inputSanitizer::sanitizeAllowingFormattingAndLinks,
        TranslationMetaUtil::isMetaKey);
  }

  private Map<String, String> sanitizeAllTranslations(
      Map<String, String> translations,
      Function<String, String> sanitizeFuntion,
      java.util.function.Predicate<String> keepVerbatim) {
    if (translations != null) {
      return translations.entrySet().stream()
          .filter(entry -> entry.getKey() != null)
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  e ->
                      keepVerbatim.test(e.getKey())
                          ? e.getValue()
                          : sanitizeFuntion.apply(e.getValue())));
    }
    return translations;
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
