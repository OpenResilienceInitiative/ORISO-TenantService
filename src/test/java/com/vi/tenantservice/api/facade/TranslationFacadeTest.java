package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TranslationApiKeyUpdateDTO;
import com.vi.tenantservice.api.model.TranslationRequestDTO;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.api.service.translation.TranslationErrorCode;
import com.vi.tenantservice.api.service.translation.TranslationException;
import com.vi.tenantservice.api.service.translation.TranslationProviderClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TranslationFacadeTest {

  private static final String OPENROUTER_KEY = "sk-or-v1-0123456789abcd";
  private static final String MISTRAL_KEY = "mistral-0123456789wxyz";

  @Mock private TenantAdminControlsService tenantAdminControlsService;
  @Mock private TenantFacadeAuthorisationService tenantFacadeAuthorisationService;

  @Mock(name = "openRouterClient")
  private TranslationProviderClient openRouterClient;

  @Mock(name = "mistralClient")
  private TranslationProviderClient mistralClient;

  private TranslationFacade translationFacade;

  @BeforeEach
  void setUp() {
    translationFacade =
        new TranslationFacade(
            tenantAdminControlsService,
            tenantFacadeAuthorisationService,
            List.of(openRouterClient, mistralClient));
  }

  private void givenProviderIds() {
    when(openRouterClient.getProviderId()).thenReturn("openrouter");
    when(mistralClient.getProviderId()).thenReturn("mistral");
  }

  private void givenSuperAdmin(boolean isSuperAdmin) {
    when(tenantFacadeAuthorisationService.isSuperAdmin()).thenReturn(isSuperAdmin);
  }

  @Test
  void getMaskedApiKeys_Should_returnMaskedKeys_forSuperAdmin() {
    givenSuperAdmin(true);
    when(tenantAdminControlsService.getTranslationApiKeys())
        .thenReturn(Map.of("openrouter", OPENROUTER_KEY, "mistral", MISTRAL_KEY));

    var keys = translationFacade.getMaskedApiKeys();

    assertThat(keys.getOpenrouter()).isEqualTo("sk-…abcd").doesNotContain("0123456789");
    assertThat(keys.getMistral()).isEqualTo("mis…wxyz").doesNotContain("0123456789");
  }

  @Test
  void getMaskedApiKeys_Should_returnNulls_When_noKeysConfigured() {
    givenSuperAdmin(true);
    when(tenantAdminControlsService.getTranslationApiKeys()).thenReturn(Map.of());

    var keys = translationFacade.getMaskedApiKeys();

    assertThat(keys.getOpenrouter()).isNull();
    assertThat(keys.getMistral()).isNull();
  }

  @Test
  void getMaskedApiKeys_Should_denyAccess_When_notSuperAdmin() {
    givenSuperAdmin(false);

    assertThatThrownBy(() -> translationFacade.getMaskedApiKeys())
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void setApiKey_Should_storeTrimmedKey_andReturnMasked() {
    givenProviderIds();
    givenSuperAdmin(true);
    when(tenantAdminControlsService.getTranslationApiKeys())
        .thenReturn(Map.of("openrouter", OPENROUTER_KEY));

    var keys =
        translationFacade.setApiKey(
            "openrouter", new TranslationApiKeyUpdateDTO().apiKey("  " + OPENROUTER_KEY + "  "));

    verify(tenantAdminControlsService).setTranslationApiKey("openrouter", OPENROUTER_KEY);
    assertThat(keys.getOpenrouter()).isEqualTo("sk-…abcd");
  }

  @Test
  void setApiKey_Should_denyAccess_When_notSuperAdmin() {
    givenSuperAdmin(false);

    assertThatThrownBy(
            () ->
                translationFacade.setApiKey(
                    "openrouter", new TranslationApiKeyUpdateDTO().apiKey("x")))
        .isInstanceOf(AccessDeniedException.class);
    verify(tenantAdminControlsService, never()).setTranslationApiKey(any(), any());
  }

  @Test
  void setApiKey_Should_rejectUnknownProvider() {
    givenProviderIds();
    givenSuperAdmin(true);

    assertThatThrownBy(
            () ->
                translationFacade.setApiKey("deepl", new TranslationApiKeyUpdateDTO().apiKey("x")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void setApiKey_Should_rejectBlankKey() {
    givenProviderIds();
    givenSuperAdmin(true);

    assertThatThrownBy(
            () ->
                translationFacade.setApiKey(
                    "openrouter", new TranslationApiKeyUpdateDTO().apiKey("   ")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void translate_Should_useExplicitProvider_andAssembleResponse() {
    givenProviderIds();
    when(tenantAdminControlsService.getTranslationApiKeys())
        .thenReturn(Map.of("mistral", MISTRAL_KEY));
    when(mistralClient.getModel()).thenReturn("mistral-small-latest");
    when(mistralClient.translateHtml(MISTRAL_KEY, "de", "en", "<p>Hallo</p>"))
        .thenReturn("<p>Hello</p>");
    when(mistralClient.translateHtml(MISTRAL_KEY, "de", "fr", "<p>Hallo</p>"))
        .thenReturn("<p>Bonjour</p>");

    var response =
        translationFacade.translate(
            new TranslationRequestDTO()
                .sourceLang("de")
                .targetLangs(List.of("en", "fr"))
                .provider("mistral")
                .texts(Map.of("privacy", "<p>Hallo</p>")));

    assertThat(response.getProvider()).isEqualTo("mistral");
    assertThat(response.getModel()).isEqualTo("mistral-small-latest");
    assertThat(response.getTranslations().get("en")).containsEntry("privacy", "<p>Hello</p>");
    assertThat(response.getTranslations().get("fr")).containsEntry("privacy", "<p>Bonjour</p>");
  }

  @Test
  void translate_Should_preferOpenrouter_When_noExplicitProvider_andBothKeysSet() {
    givenProviderIds();
    when(tenantAdminControlsService.getTranslationApiKeys())
        .thenReturn(Map.of("openrouter", OPENROUTER_KEY, "mistral", MISTRAL_KEY));
    when(openRouterClient.getModel()).thenReturn("openai/gpt-4o-mini");
    when(openRouterClient.translateHtml(OPENROUTER_KEY, "de", "en", "<p>Hallo</p>"))
        .thenReturn("<p>Hello</p>");

    var response =
        translationFacade.translate(
            new TranslationRequestDTO()
                .sourceLang("de")
                .targetLangs(List.of("en"))
                .texts(Map.of("privacy", "<p>Hallo</p>")));

    assertThat(response.getProvider()).isEqualTo("openrouter");
    verify(mistralClient, never()).translateHtml(any(), any(), any(), any());
  }

  @Test
  void translate_Should_fallBackToMistral_When_onlyMistralKeySet() {
    givenProviderIds();
    when(tenantAdminControlsService.getTranslationApiKeys())
        .thenReturn(Map.of("mistral", MISTRAL_KEY));
    when(mistralClient.getModel()).thenReturn("mistral-small-latest");
    when(mistralClient.translateHtml(MISTRAL_KEY, "de", "en", "<p>Hallo</p>"))
        .thenReturn("<p>Hello</p>");

    var response =
        translationFacade.translate(
            new TranslationRequestDTO()
                .sourceLang("de")
                .targetLangs(List.of("en"))
                .texts(Map.of("privacy", "<p>Hallo</p>")));

    assertThat(response.getProvider()).isEqualTo("mistral");
  }

  @Test
  void translate_Should_throwNotConfigured_When_noKeySetAtAll() {
    when(tenantAdminControlsService.getTranslationApiKeys()).thenReturn(Map.of());

    assertThatThrownBy(
            () ->
                translationFacade.translate(
                    new TranslationRequestDTO()
                        .sourceLang("de")
                        .targetLangs(List.of("en"))
                        .texts(Map.of("privacy", "<p>x</p>"))))
        .isInstanceOfSatisfying(
            TranslationException.class,
            e ->
                assertThat(e.getErrorCode())
                    .isEqualTo(TranslationErrorCode.TRANSLATION_NOT_CONFIGURED));
  }

  @Test
  void translate_Should_throwNotConfigured_When_explicitProviderHasNoKey() {
    givenProviderIds();
    when(tenantAdminControlsService.getTranslationApiKeys())
        .thenReturn(Map.of("mistral", MISTRAL_KEY));

    assertThatThrownBy(
            () ->
                translationFacade.translate(
                    new TranslationRequestDTO()
                        .sourceLang("de")
                        .targetLangs(List.of("en"))
                        .provider("openrouter")
                        .texts(Map.of("privacy", "<p>x</p>"))))
        .isInstanceOfSatisfying(
            TranslationException.class,
            e -> {
              assertThat(e.getErrorCode())
                  .isEqualTo(TranslationErrorCode.TRANSLATION_NOT_CONFIGURED);
              assertThat(e.getProvider()).isEqualTo("openrouter");
            });
  }

  @Test
  void translate_Should_rejectUnknownExplicitProvider() {
    givenProviderIds();
    when(tenantAdminControlsService.getTranslationApiKeys()).thenReturn(Map.of());

    assertThatThrownBy(
            () ->
                translationFacade.translate(
                    new TranslationRequestDTO()
                        .sourceLang("de")
                        .targetLangs(List.of("en"))
                        .provider("deepl")
                        .texts(Map.of("privacy", "<p>x</p>"))))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }
}
