package com.vi.tenantservice.api.validation;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import java.util.HashMap;
import java.util.Map;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getLogo());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getFavicon());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getAssociationLogo());
    verify(inputSanitizer).sanitizeAllowingFormatting(Mockito.anyString());
    verify(inputSanitizer, Mockito.times(3))
        .sanitizeAllowingFormattingAndLinks(Mockito.anyString());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getPrimaryColor());
    verify(inputSanitizer).sanitize(tenantDTO.getTheming().getSecondaryColor());
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
