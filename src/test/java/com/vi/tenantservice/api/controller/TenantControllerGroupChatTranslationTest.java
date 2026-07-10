package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.facade.TenantDpaFacade;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.facade.TranslationFacade;
import com.vi.tenantservice.api.model.TranslationRequestDTO;
import com.vi.tenantservice.api.model.TranslationResponseDTO;
import com.vi.tenantservice.api.service.TenantDpaService;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TenantControllerGroupChatTranslationTest {

  @Mock private TenantServiceFacade tenantServiceFacade;
  @Mock private AuthorisationService authorisationService;
  @Mock private TenantDtoMapper tenantDtoMapper;
  @Mock private TenantDpaService tenantDpaService;
  @Mock private TenantDpaFacade tenantDpaFacade;
  @Mock private TranslationFacade translationFacade;
  @InjectMocks private TenantController controller;

  @Test
  void boundedAuthorContentUsesTheExistingTranslationFacade() {
    var request =
        new TranslationRequestDTO()
            .sourceLang("de")
            .targetLangs(List.of("en"))
            .texts(Map.of("welcome", "Willkommen"));
    var translated =
        new TranslationResponseDTO().translations(Map.of("en", Map.of("welcome", "Welcome")));
    when(translationFacade.translate(request)).thenReturn(translated);

    var response = controller.translateGroupChatAuthorContent(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(translated);
  }

  @Test
  void authorContentRejectsMessagesLongerThanTheSeriesContract() {
    var request =
        new TranslationRequestDTO()
            .sourceLang("de")
            .targetLangs(List.of("en"))
            .texts(Map.of("rule-1", "x".repeat(121)));

    assertThatThrownBy(() -> controller.translateGroupChatAuthorContent(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            error ->
                assertThat(((ResponseStatusException) error).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));
    verifyNoInteractions(translationFacade);
  }

  @Test
  void endpointRequiresTheDedicatedConsultantTranslationAuthority() throws Exception {
    var method =
        TenantController.class.getMethod(
            "translateGroupChatAuthorContent", TranslationRequestDTO.class);

    assertThat(method.getAnnotation(PreAuthorize.class).value())
        .isEqualTo("hasAuthority('AUTHORIZATION_TRANSLATE_GROUP_CHAT_CONTENT')");
  }
}
