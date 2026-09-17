package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.AccountInactivitySettings;
import com.vi.tenantservice.api.model.ChatRecoveryMode;
import com.vi.tenantservice.api.model.ChatRecoverySettings;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import com.vi.tenantservice.api.model.TenantAdminControlsSettings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyRules;
import com.vi.tenantservice.api.policy.LegacyPermissionPolicyMapper;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.repository.TenantAdminControlsRepository;
import com.vi.tenantservice.api.service.translation.TranslationApiKeyEncryptionService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantAdminControlsService {

  private final @NonNull TenantAdminControlsRepository tenantAdminControlsRepository;
  private final @NonNull TenantConverter tenantConverter;
  private final @NonNull TranslationApiKeyEncryptionService translationApiKeyEncryptionService;

  public TenantAdminControls getControls() {
    return tenantConverter.toTenantAdminControls(getControlsSettings());
  }

  @Transactional
  public TenantAdminControls updateControls(TenantAdminControls tenantAdminControls) {
    Optional<TenantAdminControlsEntity> existingEntity = findExistingControls();
    TenantAdminControlsSettings controlsSettings =
        tenantConverter.toTenantAdminControlsSettings(tenantAdminControls);
    TenantAdminControlsSettings existingSettings =
        existingEntity
            .map(entity -> parseControlsSettings(entity.getControls()))
            .orElseGet(this::createDefaultControlsSettings);
    if (existingSettings != null) {
      controlsSettings.setExplicitPermissionPolicyKeys(
          explicitPolicyKeys(existingSettings, controlsSettings.getPermissionPolicies()));
      if (controlsSettings.getPermissionPolicies() == null
          || controlsSettings.getPermissionPolicies().isEmpty()) {
        controlsSettings.setPermissionPolicies(existingSettings.getPermissionPolicies());
      }
      if (controlsSettings.getCaseHandoverPolicies() == null) {
        controlsSettings.setCaseHandoverPolicies(existingSettings.getCaseHandoverPolicies());
      }
      // the DTO never carries the translation API keys - carry the stored values over verbatim,
      // still encrypted, so this path never decrypts and re-encrypts them for nothing
      controlsSettings.setTranslationApiKeys(existingSettings.getTranslationApiKeys());
      // Only versioned subresources can change account creation defaults.
      controlsSettings.setChatRecoverySettings(existingSettings.getChatRecoverySettings());
      controlsSettings.setAccountInactivitySettings(
          existingSettings.getAccountInactivitySettings());
    }
    hydrateCanonicalPolicies(controlsSettings);
    saveControlsSettings(
        controlsSettings, existingEntity.orElseGet(TenantAdminControlsEntity::new));
    return tenantConverter.toTenantAdminControls(controlsSettings);
  }

  public AccountInactivitySettings getAccountInactivitySettings() {
    return inactivitySettings(getControlsSettings());
  }

  @Transactional
  public AccountInactivitySettings updateAccountInactivitySettings(
      AccountInactivitySettings request) {
    Optional<TenantAdminControlsEntity> existing = findExistingControls();
    TenantAdminControlsSettings settings =
        existing
            .map(entity -> parseControlsSettings(entity.getControls()))
            .orElseGet(this::createDefaultControlsSettings);
    AccountInactivitySettings current = inactivitySettings(settings);
    if (!current.getRevision().equals(request.getRevision())) {
      throw new SettingsUpdateConflictException(current.getRevision(), request.getRevision());
    }
    AccountInactivitySettings updated =
        new AccountInactivitySettings(
            request.getAskerMonths(),
            request.getConsultantMonths(),
            request.getOtherMonths(),
            Math.addExact(current.getRevision(), 1L));
    settings.setAccountInactivitySettings(updated);
    saveControlsSettings(settings, existing.orElseGet(TenantAdminControlsEntity::new));
    return updated;
  }

  private AccountInactivitySettings inactivitySettings(TenantAdminControlsSettings settings) {
    AccountInactivitySettings stored =
        settings == null ? null : settings.getAccountInactivitySettings();
    return new AccountInactivitySettings(
        stored == null || stored.getAskerMonths() == null ? 24 : stored.getAskerMonths(),
        stored == null || stored.getConsultantMonths() == null ? 24 : stored.getConsultantMonths(),
        stored == null || stored.getOtherMonths() == null ? 24 : stored.getOtherMonths(),
        stored == null || stored.getRevision() == null ? 0L : stored.getRevision());
  }

  /**
   * #251 — the platform admin's preset for new Träger: only the policies the admin set explicitly
   * (see {@link TenantAdminControlsSettings#getExplicitPermissionPolicyKeys()}). A group chat
   * format whose own policy was never set follows an explicit {@code featureGroupChatV2Enabled}
   * policy (#250 transition).
   */
  public Map<String, PolicyValue<Boolean>> getExplicitPlatformPolicies() {
    TenantAdminControlsSettings settings = getControlsSettings();
    Set<String> explicitKeys =
        settings.getExplicitPermissionPolicyKeys() == null
            ? Set.of()
            : settings.getExplicitPermissionPolicyKeys();
    Map<String, PolicyValue<Boolean>> policies =
        settings.getPermissionPolicies() == null ? Map.of() : settings.getPermissionPolicies();
    Map<String, PolicyValue<Boolean>> explicit = new LinkedHashMap<>();
    for (PermissionFeature feature : PermissionFeature.values()) {
      boolean ownExplicit = explicitKeys.contains(feature.apiKey());
      PermissionFeature fallback = feature.transitionFallback().orElse(null);
      boolean fallbackExplicit =
          !ownExplicit && fallback != null && explicitKeys.contains(fallback.apiKey());
      PolicyValue<Boolean> policy =
          ownExplicit
              ? policies.get(feature.apiKey())
              : fallbackExplicit ? policies.get(fallback.apiKey()) : null;
      if (policy != null) {
        explicit.put(feature.apiKey(), policy);
      }
    }
    return Map.copyOf(explicit);
  }

  /**
   * A requested policy counts as explicitly set when it differs from what the platform currently
   * serves for that key (value or mode). Keys marked earlier stay marked.
   */
  private static Set<String> explicitPolicyKeys(
      TenantAdminControlsSettings existing, Map<String, PolicyValue<Boolean>> requested) {
    Set<String> keys = new LinkedHashSet<>();
    if (existing.getExplicitPermissionPolicyKeys() != null) {
      keys.addAll(existing.getExplicitPermissionPolicyKeys());
    }
    if (requested == null) {
      return keys;
    }
    Map<String, PolicyValue<Boolean>> served =
        existing.getPermissionPolicies() == null ? Map.of() : existing.getPermissionPolicies();
    requested.forEach(
        (key, policy) -> {
          if (policy != null && !policy.equals(served.get(key))) {
            keys.add(key);
          }
        });
    return keys;
  }

  public ChatRecoverySettings getChatRecoverySettings() {
    return recoverySettings(getControlsSettings());
  }

  @Transactional
  public ChatRecoverySettings updateChatRecoverySettings(ChatRecoverySettings request) {
    if (request == null
        || request.getAsker() == null
        || request.getConsultant() == null
        || request.getRevision() == null
        || request.getRevision() < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Both recovery modes and a nonnegative revision are required");
    }
    Optional<TenantAdminControlsEntity> existing = findExistingControls();
    TenantAdminControlsSettings settings =
        existing
            .map(entity -> parseControlsSettings(entity.getControls()))
            .orElseGet(this::createDefaultControlsSettings);
    ChatRecoverySettings current = recoverySettings(settings);
    if (!current.getRevision().equals(request.getRevision())) {
      throw new SettingsUpdateConflictException(current.getRevision(), request.getRevision());
    }
    if (current.getAsker() == request.getAsker()
        && current.getConsultant() == request.getConsultant()) {
      return current;
    }
    ChatRecoverySettings updated =
        new ChatRecoverySettings(
            request.getAsker(), request.getConsultant(), Math.addExact(current.getRevision(), 1L));
    settings.setChatRecoverySettings(updated);
    saveControlsSettings(settings, existing.orElseGet(TenantAdminControlsEntity::new));
    return updated;
  }

  private ChatRecoverySettings recoverySettings(TenantAdminControlsSettings settings) {
    ChatRecoverySettings stored = settings == null ? null : settings.getChatRecoverySettings();
    return new ChatRecoverySettings(
        stored == null || stored.getAsker() == null
            ? ChatRecoveryMode.LOGIN_PASSWORD
            : stored.getAsker(),
        stored == null || stored.getConsultant() == null
            ? ChatRecoveryMode.LOGIN_PASSWORD
            : stored.getConsultant(),
        stored == null || stored.getRevision() == null ? 0L : stored.getRevision());
  }

  /**
   * Usable machine-translation provider API keys (provider id -> key) from the platform-global
   * admin controls, decrypted for use. Internal use only - admin endpoints must expose keys masked.
   *
   * <p>Values stored before the keys were encrypted carry no {@code ENC:} prefix and are returned
   * unchanged, so a row that the startup migration has not reached yet still works.
   */
  public Map<String, String> getTranslationApiKeys() {
    TenantAdminControlsSettings controlsSettings = getControlsSettings();
    if (controlsSettings == null || controlsSettings.getTranslationApiKeys() == null) {
      return Map.of();
    }
    Map<String, String> decrypted = new HashMap<>();
    controlsSettings
        .getTranslationApiKeys()
        .forEach(
            (provider, storedValue) ->
                decrypted.put(provider, translationApiKeyEncryptionService.decrypt(storedValue)));
    return decrypted;
  }

  /**
   * Stores the machine-translation API key for a provider in the platform-global controls,
   * encrypted at rest. The masked read path is unaffected: {@link #getTranslationApiKeys()}
   * decrypts, and the callers mask what they return.
   */
  @Transactional
  public void setTranslationApiKey(String provider, String apiKey) {
    Optional<TenantAdminControlsEntity> existingEntity = findExistingControls();
    TenantAdminControlsSettings controlsSettings =
        existingEntity
            .map(entity -> parseControlsSettings(entity.getControls()))
            .orElseGet(this::createDefaultControlsSettings);
    Map<String, String> keys = new HashMap<>();
    if (controlsSettings.getTranslationApiKeys() != null) {
      keys.putAll(controlsSettings.getTranslationApiKeys());
    }
    keys.put(provider, translationApiKeyEncryptionService.encryptNewApiKey(apiKey));
    controlsSettings.setTranslationApiKeys(keys);
    saveControlsSettings(
        controlsSettings, existingEntity.orElseGet(TenantAdminControlsEntity::new));
  }

  public void stripTenantAdminControlsFromTenantDto(MultilingualTenantDTO tenantDTO) {
    if (tenantDTO == null || tenantDTO.getSettings() == null) {
      return;
    }
    tenantDTO.getSettings().setTenantAdminControls(null);
  }

  public void enrichTenantDtoWithTenantAdminControls(MultilingualTenantDTO tenantDTO) {
    if (tenantDTO == null) {
      return;
    }
    tenantDTO.setSettings(enrichSettingsWithTenantAdminControls(tenantDTO.getSettings()));
  }

  public void enrichTenantDtoWithTenantAdminControls(TenantDTO tenantDTO) {
    if (tenantDTO == null) {
      return;
    }
    tenantDTO.setSettings(enrichSettingsWithTenantAdminControls(tenantDTO.getSettings()));
  }

  private Settings enrichSettingsWithTenantAdminControls(Settings settings) {
    Settings enrichedSettings = settings != null ? settings : new Settings();
    enrichedSettings.setTenantAdminControls(getControls());
    return enrichedSettings;
  }

  private TenantAdminControlsSettings getControlsSettings() {
    return findExistingControls()
        .map(entity -> parseControlsSettings(entity.getControls()))
        .orElseGet(this::createDefaultControlsSettings);
  }

  private void saveControlsSettings(
      TenantAdminControlsSettings controlsSettings, TenantAdminControlsEntity entity) {
    entity.setControls(serializeControlsSettings(controlsSettings));
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    try {
      tenantAdminControlsRepository.saveAndFlush(entity);
    } catch (OptimisticLockingFailureException | DataIntegrityViolationException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  private Optional<TenantAdminControlsEntity> findExistingControls() {
    return tenantAdminControlsRepository.findTopByOrderByIdAsc();
  }

  private TenantAdminControlsSettings parseControlsSettings(String controlsJson) {
    if (StringUtils.isBlank(controlsJson) || "{}".equals(controlsJson.trim())) {
      return createDefaultControlsSettings();
    }
    try {
      TenantAdminControlsSettings settings =
          new ObjectMapper().readValue(controlsJson, TenantAdminControlsSettings.class);
      if (settings == null) {
        return createDefaultControlsSettings();
      }
      hydrateCanonicalPolicies(settings);
      return settings;
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private String serializeControlsSettings(TenantAdminControlsSettings controlsSettings) {
    try {
      return new ObjectMapper().writeValueAsString(controlsSettings);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private TenantAdminControlsSettings createDefaultControlsSettings() {
    TenantAdminControlsSettings settings =
        tenantConverter.toTenantAdminControlsSettings(new TenantAdminControls());
    hydrateCanonicalPolicies(settings);
    return settings;
  }

  private void hydrateCanonicalPolicies(TenantAdminControlsSettings settings) {
    if (settings == null) {
      return;
    }
    settings.setChatRecoverySettings(recoverySettings(settings));
    settings.setAccountInactivitySettings(inactivitySettings(settings));
    // stored entries win; split-out formats read their pre-#250 rule; every other feature reads
    // the legacy toggles - on every read, so a partial list (#254) never hides a feature
    settings.setPermissionPolicies(
        LegacyPermissionPolicyMapper.complete(
            settings.getPermissionPolicies(),
            settings.getAllowedPermissionToggles(),
            settings.getEnforcedPermissionToggles()));
    if (settings.getCaseHandoverPolicies() == null) {
      settings.setCaseHandoverPolicies(CaseHandoverPolicyDefaults.create());
    } else {
      settings.setCaseHandoverPolicies(
          CaseHandoverPolicyRules.normalize(settings.getCaseHandoverPolicies()));
    }
    CaseHandoverPolicyRules.validate(settings.getCaseHandoverPolicies());
  }
}
