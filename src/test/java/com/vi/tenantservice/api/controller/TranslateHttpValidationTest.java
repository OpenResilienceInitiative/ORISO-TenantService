package com.vi.tenantservice.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.api.facade.TranslationFacade;
import com.vi.tenantservice.api.model.TranslationRequestDTO;
import com.vi.tenantservice.api.model.TranslationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The Admin journey, conversation-type and Erstantwort editors send the informal German variant as
 * {@code de@informal} (11 characters). The request must pass bean validation and reach the provider
 * instead of failing with HTTP 400 before translation starts.
 */
class TranslateHttpValidationTest {

  private TranslationFacade translationFacade;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    translationFacade = mock(TranslationFacade.class);
    when(translationFacade.translate(any())).thenReturn(new TranslationResponseDTO());
    var controller = mock(TenantController.class, CALLS_REAL_METHODS);
    ReflectionTestUtils.setField(controller, "translationFacade", translationFacade);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  private void postTranslate(String sourceLang, String targetLang, int expectedStatus)
      throws Exception {
    var body =
        """
        {"sourceLang":"%s","targetLangs":["%s"],"texts":{"title":"<p>Hallo</p>"}}"""
            .formatted(sourceLang, targetLang);
    mockMvc
        .perform(
            post("/tenantadmin/translate").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void informalGermanAsTargetLanguageReachesTheProvider() throws Exception {
    postTranslate("de", "de@informal", 200);
    verify(translationFacade).translate(any(TranslationRequestDTO.class));
  }

  @Test
  void informalGermanAsSourceLanguageReachesTheProvider() throws Exception {
    postTranslate("de@informal", "en", 200);
    verify(translationFacade).translate(any(TranslationRequestDTO.class));
  }

  @Test
  void languageCodesOfExactlyTwentyCharactersAreAcceptedInBothPositions() throws Exception {
    postTranslate("x".repeat(20), "en", 200);
    postTranslate("de", "x".repeat(20), 200);
  }

  @Test
  void targetLanguageCodesLongerThanTheContractAreStillRejected() throws Exception {
    postTranslate("de", "x".repeat(21), 400);
    verifyNoInteractions(translationFacade);
  }

  @Test
  void sourceLanguageCodesLongerThanTheContractAreStillRejected() throws Exception {
    postTranslate("x".repeat(21), "en", 400);
    verifyNoInteractions(translationFacade);
  }
}
