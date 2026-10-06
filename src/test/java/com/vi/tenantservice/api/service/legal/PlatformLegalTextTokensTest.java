package com.vi.tenantservice.api.service.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import com.vi.tenantservice.api.repository.PlatformDpiaMasterDataRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformLegalTextTokensTest {

  @Mock PlatformDpiaMasterDataRepository repository;

  @InjectMocks PlatformLegalTextTokens tokens;

  private void platformDpo(String name) {
    var entity = new PlatformDpiaMasterDataEntity();
    entity.setOperatorDpoName(name);
    when(repository.findById(PlatformDpiaMasterDataEntity.SINGLETON_ID))
        .thenReturn(Optional.of(entity));
  }

  @Test
  void fill_Should_substitutePlatformDpo_InEveryPublicLegalField_IncludingSanitizerSplitForm() {
    platformDpo("Dr. Paula Plattform");
    var content =
        new Content("<p>{{Plattform_Datenschutzbeauftragte}}</p>")
            .privacy("<p>DSB: {<!-- -->{Plattform_Datenschutzbeauftragte}}</p>")
            .renderedPrivacy("<p>DSB: {<!-- -->{Plattform_Datenschutzbeauftragte}}</p>")
            .privacyLanguages(
                Map.of(
                    "de", "<p>{<!-- -->{Plattform_Datenschutzbeauftragte}}</p>",
                    "de__meta", "{\"x\":\"{{Plattform_Datenschutzbeauftragte}}\"}"));

    tokens.fill(content);

    assertThat(content.getImpressum()).isEqualTo("<p>Dr. Paula Plattform</p>");
    assertThat(content.getPrivacy()).isEqualTo("<p>DSB: Dr. Paula Plattform</p>");
    assertThat(content.getRenderedPrivacy()).isEqualTo("<p>DSB: Dr. Paula Plattform</p>");
    assertThat(content.getPrivacyLanguages())
        .containsEntry("de", "<p>Dr. Paula Plattform</p>")
        .containsEntry("de__meta", "{\"x\":\"{{Plattform_Datenschutzbeauftragte}}\"}");
  }

  @Test
  void fill_Should_renderEmpty_When_noPlatformDpoIsEntered() {
    when(repository.findById(PlatformDpiaMasterDataEntity.SINGLETON_ID))
        .thenReturn(Optional.empty());
    var content = new Content("x").privacy("<p>DSB: {{Plattform_Datenschutzbeauftragte}}</p>");

    tokens.fill(content);

    assertThat(content.getPrivacy()).isEqualTo("<p>DSB: </p>");
  }

  @Test
  void fill_Should_leaveTheTraegerDpoTokenForAgencyService() {
    var content = new Content("x").privacy("<p>{{Datenschutzbeauftragte}}</p>");

    tokens.fill(content);

    assertThat(content.getPrivacy()).isEqualTo("<p>{{Datenschutzbeauftragte}}</p>");
  }
}
