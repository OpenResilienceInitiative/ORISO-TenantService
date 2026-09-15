package com.vi.tenantservice.api.validation;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Theming;
import java.util.HashMap;
import java.util.Map;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantInputSanitizerTest {

  private static final String LINK_CONTENT =
      "<a href=\"http://onlineberatung.net\">content</a>further content";
  private static final String IMAGE_CONTENT =
      "<img src=\"http://onlineberatung.net/images/test.png\" width=\"272\" height=\"92\" />";

  @InjectMocks TenantInputSanitizer tenantInputSanitizer;

  @Mock InputSanitizer inputSanitizer;

  @Test
  void sanitize_Should_sanitizeTenantDTO() {
    // given
    EasyRandom generator = new EasyRandom();
    MultilingualTenantDTO tenantDTO = generator.nextObject(MultilingualTenantDTO.class);
    tenantDTO.getContent().setImpressum(getDefaultTranslationsAsMap("impressum"));
    tenantDTO.getContent().setClaim(getDefaultTranslationsAsMap("claim"));
    tenantDTO.getContent().setPrivacy(getDefaultTranslationsAsMap("privacy"));
    tenantDTO
        .getContent()
        .setTermsAndConditions(getDefaultTranslationsAsMap("terms and conditions"));
    when(inputSanitizer.sanitizeAllowingFormattingAndLinks(Mockito.anyString())).thenReturn("");
    when(inputSanitizer.sanitizeAllowingFormatting(Mockito.anyString())).thenReturn("");

    // when
    MultilingualTenantDTO sanitized = tenantInputSanitizer.sanitize(tenantDTO);

    // then
    verifyNeededSanitizationsAreCalled(tenantDTO);
    assertNonSanitizableFieldsHaveSameValues(tenantDTO, sanitized);
  }

  @Test
  void sanitize_Should_sanitizeAndAllowLinksForContentInTenantDTO() {
    // given
    EasyRandom generator = new EasyRandom();
    MultilingualTenantDTO tenantDTO = generator.nextObject(MultilingualTenantDTO.class);
    tenantDTO.getContent().setTermsAndConditions(getDefaultTranslationsAsMap(LINK_CONTENT));
    tenantDTO.getContent().setPrivacy(getDefaultTranslationsAsMap(LINK_CONTENT));
    tenantDTO.getContent().setImpressum(getDefaultTranslationsAsMap(LINK_CONTENT));
    TenantInputSanitizer nonMockedTenantInputSanitizer =
        new TenantInputSanitizer(new InputSanitizer());
    // when
    MultilingualTenantDTO sanitized = nonMockedTenantInputSanitizer.sanitize(tenantDTO);

    // then
    assertThat(sanitized.getContent().getTermsAndConditions())
        .isEqualTo(getDefaultTranslationsAsMap(LINK_CONTENT));
    assertThat(sanitized.getContent().getPrivacy())
        .isEqualTo(getDefaultTranslationsAsMap(LINK_CONTENT));
    assertThat(sanitized.getContent().getImpressum())
        .isEqualTo(getDefaultTranslationsAsMap(LINK_CONTENT));
  }

  private Map<String, String> getDefaultTranslationsAsMap(String content) {
    var map = new HashMap<String, String>();
    map.put("de", content);
    return map;
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  void sanitize_Should_turnBlankThemingSeedsIntoNull(String blankSeed) {
    MultilingualTenantDTO tenantDTO = tenantWithSeeds(blankSeed, blankSeed, blankSeed, blankSeed);

    MultilingualTenantDTO sanitized =
        new TenantInputSanitizer(new InputSanitizer()).sanitize(tenantDTO);

    assertThat(sanitized.getTheming().getPrimaryColor()).isNull();
    assertThat(sanitized.getTheming().getSecondaryColor()).isNull();
    assertThat(sanitized.getTheming().getAccent()).isNull();
    assertThat(sanitized.getTheming().getSignal()).isNull();
  }

  @Test
  void sanitize_Should_keepValidThemingSeeds() {
    MultilingualTenantDTO tenantDTO = tenantWithSeeds("#8B1A2B", "#FFFFFF", "#FFB3C7", "#E53935");

    MultilingualTenantDTO sanitized =
        new TenantInputSanitizer(new InputSanitizer()).sanitize(tenantDTO);

    assertThat(sanitized.getTheming().getPrimaryColor()).isEqualTo("#8B1A2B");
    assertThat(sanitized.getTheming().getSecondaryColor()).isEqualTo("#FFFFFF");
    assertThat(sanitized.getTheming().getAccent()).isEqualTo("#FFB3C7");
    assertThat(sanitized.getTheming().getSignal()).isEqualTo("#E53935");
  }

  @Test
  void sanitize_Should_nullOnlyTheBlankSeedAndKeepTheValidOnes() {
    MultilingualTenantDTO tenantDTO = tenantWithSeeds("#8B1A2B", "", "  ", "#E53935");

    MultilingualTenantDTO sanitized =
        new TenantInputSanitizer(new InputSanitizer()).sanitize(tenantDTO);

    assertThat(sanitized.getTheming().getPrimaryColor()).isEqualTo("#8B1A2B");
    assertThat(sanitized.getTheming().getSecondaryColor()).isNull();
    assertThat(sanitized.getTheming().getAccent()).isNull();
    assertThat(sanitized.getTheming().getSignal()).isEqualTo("#E53935");
  }

  private static MultilingualTenantDTO tenantWithSeeds(
      String primary, String secondary, String accent, String signal) {
    EasyRandom generator = new EasyRandom();
    MultilingualTenantDTO tenantDTO = generator.nextObject(MultilingualTenantDTO.class);
    Theming theming = tenantDTO.getTheming();
    theming.setPrimaryColor(primary);
    theming.setSecondaryColor(secondary);
    theming.setAccent(accent);
    theming.setSignal(signal);
    return tenantDTO;
  }

  @Test
  void sanitize_Should_sanitizeAndAllowImageSrcForContentInTenantDTO() {
    // given
    EasyRandom generator = new EasyRandom();
    MultilingualTenantDTO tenantDTO = generator.nextObject(MultilingualTenantDTO.class);
    tenantDTO.getContent().setTermsAndConditions(getDefaultTranslationsAsMap(IMAGE_CONTENT));
    tenantDTO.getContent().setPrivacy(getDefaultTranslationsAsMap(IMAGE_CONTENT));
    tenantDTO.getContent().setImpressum(getDefaultTranslationsAsMap(IMAGE_CONTENT));
    TenantInputSanitizer nonMockedTenantInputSanitizer =
        new TenantInputSanitizer(new InputSanitizer());
    // when
    MultilingualTenantDTO sanitized = nonMockedTenantInputSanitizer.sanitize(tenantDTO);

    // then
    assertThat(sanitized.getContent().getTermsAndConditions())
        .isEqualTo(getDefaultTranslationsAsMap(IMAGE_CONTENT));
    assertThat(sanitized.getContent().getPrivacy())
        .isEqualTo(getDefaultTranslationsAsMap(IMAGE_CONTENT));
    assertThat(sanitized.getContent().getImpressum())
        .isEqualTo(getDefaultTranslationsAsMap(IMAGE_CONTENT));
  }

  private void verifyNeededSanitizationsAreCalled(MultilingualTenantDTO tenantDTO) {
    verify(inputSanitizer).sanitize(tenantDTO.getName());
    verify(inputSanitizer).sanitize(tenantDTO.getSubdomain());
    verify(inputSanitizer).sanitize(tenantDTO.getAddress());
    verify(inputSanitizer).sanitize(tenantDTO.getDescription());
    // Assets go through the URL whitelist, NOT the HTML sanitizer — the latter
    // encoded their base64 payload and broke every stored logo/favicon.
    verify(inputSanitizer).sanitizeAssetUrl(tenantDTO.getTheming().getLogo());
    verify(inputSanitizer).sanitizeAssetUrl(tenantDTO.getTheming().getFavicon());
    verify(inputSanitizer).sanitizeAssetUrl(tenantDTO.getTheming().getAssociationLogo());
    verify(inputSanitizer).sanitizeAllowingFormatting(Mockito.anyString());
    verify(inputSanitizer, Mockito.times(3))
        .sanitizeAllowingFormattingAndLinks(Mockito.anyString());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getPrimaryColor());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getSecondaryColor());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getAccent());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getSignal());
    verifyNoMoreInteractions(inputSanitizer);
  }

  private void assertNonSanitizableFieldsHaveSameValues(
      MultilingualTenantDTO tenantDTO, MultilingualTenantDTO sanitized) {
    assertThat(tenantDTO.getId()).isEqualTo(sanitized.getId());
    assertThat(tenantDTO.getCreateDate()).isEqualTo(sanitized.getCreateDate());
    assertThat(tenantDTO.getUpdateDate()).isEqualTo(sanitized.getUpdateDate());
    assertThat(tenantDTO.getLicensing().getAllowedNumberOfUsers())
        .isEqualTo(sanitized.getLicensing().getAllowedNumberOfUsers());
  }
}
