package com.vi.tenantservice.api.facade;

import com.vi.tenantservice.api.model.TranslationApiKeyUpdateDTO;
import com.vi.tenantservice.api.model.TranslationApiKeysDTO;
import com.vi.tenantservice.api.model.TranslationRequestDTO;
import com.vi.tenantservice.api.model.TranslationResponseDTO;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.api.service.translation.ApiKeyMasker;
import com.vi.tenantservice.api.service.translation.MistralClient;
import com.vi.tenantservice.api.service.translation.OpenRouterClient;
import com.vi.tenantservice.api.service.translation.TranslationErrorCode;
import com.vi.tenantservice.api.service.translation.TranslationException;
import com.vi.tenantservice.api.service.translation.TranslationProviderClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Machine translation for admin-managed legal texts. Provider API keys live in the platform-global
 * admin controls (see {@link TenantAdminControlsService}); managing them mirrors the other
 * super-admin settings guard ({@code isSuperAdmin}), and keys are only ever returned masked.
 * Translation itself is available to tenant admins with the update authority.
 */
@Service
public class TranslationFacade {

  private final @NonNull TenantAdminControlsService tenantAdminControlsService;
  private final @NonNull TenantFacadeAuthorisationService tenantFacadeAuthorisationService;
  private final Map<String, TranslationProviderClient> clientsById;

  public TranslationFacade(
      @NonNull TenantAdminControlsService tenantAdminControlsService,
      @NonNull TenantFacadeAuthorisationService tenantFacadeAuthorisationService,
      @NonNull List<TranslationProviderClient> translationProviderClients) {
    this.tenantAdminControlsService = tenantAdminControlsService;
    this.tenantFacadeAuthorisationService = tenantFacadeAuthorisationService;
    this.clientsById =
        translationProviderClients.stream()
            .collect(
                Collectors.toMap(TranslationProviderClient::getProviderId, Function.identity()));
  }

  /** A provider client together with the API key it was validated against (single read). */
  private record ResolvedProvider(TranslationProviderClient client, String apiKey) {}

  /** Masked provider API keys (super admin only). */
  public TranslationApiKeysDTO getMaskedApiKeys() {
    assertSuperAdmin();
    return maskedKeys();
  }

  /** Sets a provider API key (super admin only) and returns all keys masked. */
  public TranslationApiKeysDTO setApiKey(String provider, TranslationApiKeyUpdateDTO update) {
    assertSuperAdmin();
    if (clientFor(provider).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unknown translation provider: " + provider);
    }
    if (update == null || StringUtils.isBlank(update.getApiKey())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key must not be empty");
    }
    tenantAdminControlsService.setTranslationApiKey(provider, update.getApiKey().trim());
    return maskedKeys();
  }

  /**
   * Translates the given HTML texts into every requested target language. Provider is picked
   * explicitly or by default order: openrouter if its key is configured, else mistral.
   *
   * @throws TranslationException typed provider errors, see {@link TranslationErrorCode}
   */
  public TranslationResponseDTO translate(TranslationRequestDTO request) {
    var resolved = resolveProvider(request.getProvider());
    var client = resolved.client();
    var apiKey = resolved.apiKey();

    var translations = new LinkedHashMap<String, Map<String, String>>();
    for (String targetLang : request.getTargetLangs()) {
      var translatedFields = new LinkedHashMap<String, String>();
      request
          .getTexts()
          .forEach(
              (fieldKey, html) ->
                  translatedFields.put(
                      fieldKey,
                      client.translateHtml(apiKey, request.getSourceLang(), targetLang, html)));
      translations.put(targetLang, translatedFields);
    }
    return new TranslationResponseDTO()
        .translations(translations)
        .provider(client.getProviderId())
        .model(client.getModel());
  }

  private ResolvedProvider resolveProvider(String requestedProvider) {
    var apiKeys = tenantAdminControlsService.getTranslationApiKeys();
    if (StringUtils.isNotBlank(requestedProvider)) {
      var client =
          clientFor(requestedProvider)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST,
                          "Unknown translation provider: " + requestedProvider));
      var apiKey = apiKeys.get(client.getProviderId());
      if (StringUtils.isBlank(apiKey)) {
        throw new TranslationException(
            TranslationErrorCode.TRANSLATION_NOT_CONFIGURED,
            client.getProviderId(),
            "No API key configured for provider " + client.getProviderId());
      }
      return new ResolvedProvider(client, apiKey);
    }
    // default order: openrouter if its key is set, else mistral
    for (String providerId : List.of(OpenRouterClient.PROVIDER_ID, MistralClient.PROVIDER_ID)) {
      var client = clientsById.get(providerId);
      var apiKey = apiKeys.get(providerId);
      if (client != null && StringUtils.isNotBlank(apiKey)) {
        return new ResolvedProvider(client, apiKey);
      }
    }
    throw new TranslationException(
        TranslationErrorCode.TRANSLATION_NOT_CONFIGURED,
        null,
        "No translation provider API key configured");
  }

  private Optional<TranslationProviderClient> clientFor(String providerId) {
    return Optional.ofNullable(clientsById.get(providerId));
  }

  private TranslationApiKeysDTO maskedKeys() {
    var apiKeys = tenantAdminControlsService.getTranslationApiKeys();
    return new TranslationApiKeysDTO()
        .openrouter(ApiKeyMasker.mask(apiKeys.get(OpenRouterClient.PROVIDER_ID)))
        .mistral(ApiKeyMasker.mask(apiKeys.get(MistralClient.PROVIDER_ID)));
  }

  private void assertSuperAdmin() {
    if (!tenantFacadeAuthorisationService.isSuperAdmin()) {
      throw new AccessDeniedException("Only super admin can manage translation provider API keys");
    }
  }
}
