package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.exception.TenantBadRequestException;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverConsentValue;
import com.vi.tenantservice.api.model.CaseHandoverPolicies;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
import com.vi.tenantservice.api.model.ConsentPermissionPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.InternalGroupChatPolicies;
import com.vi.tenantservice.api.model.InternalGroupChatRecipientScope;
import com.vi.tenantservice.api.model.MultilingualTextPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.RecipientScopePermissionPolicy;
import com.vi.tenantservice.api.model.StringListPermissionPolicy;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyRules;
import com.vi.tenantservice.api.policy.InternalGroupChatPolicyRules;
import com.vi.tenantservice.api.policy.LegacyPermissionPolicyMapper;
import com.vi.tenantservice.api.policy.PermissionFeature;
import com.vi.tenantservice.api.policy.PermissionPolicyResolver;
import com.vi.tenantservice.api.policy.PolicyValue;
import com.vi.tenantservice.api.policy.ResolvedPolicyValue;
import com.vi.tenantservice.api.repository.TenantPermissionPolicyRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantPermissionPolicyService {

  private final @NonNull TenantPermissionPolicyRepository repository;
  private final @NonNull TenantAdminControlsService platformControls;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Map<String, ResolvedPolicyValue<Boolean>> getResolvedPolicies(Long tenantId) {
    Map<String, PolicyValue<Boolean>> inherited = platformPolicies(platformControls.getControls());
    Map<String, PolicyValue<Boolean>> overrides =
        repository.findByTenantId(tenantId).map(this::deserialize).orElse(Map.of());
    return resolvePolicies(inherited, overrides);
  }

  public Map<Long, Map<String, ResolvedPolicyValue<Boolean>>> getResolvedPolicies(
      Set<Long> tenantIds) {
    if (tenantIds.isEmpty()) {
      return Map.of();
    }
    Map<String, PolicyValue<Boolean>> inherited = platformPolicies(platformControls.getControls());
    Map<Long, Map<String, PolicyValue<Boolean>>> overridesByTenant = new LinkedHashMap<>();
    repository
        .findByTenantIdIn(tenantIds)
        .forEach(entity -> overridesByTenant.put(entity.getTenantId(), deserialize(entity)));
    Map<Long, Map<String, ResolvedPolicyValue<Boolean>>> resolvedByTenant = new LinkedHashMap<>();
    tenantIds.forEach(
        tenantId ->
            resolvedByTenant.put(
                tenantId,
                resolvePolicies(inherited, overridesByTenant.getOrDefault(tenantId, Map.of()))));
    return Map.copyOf(resolvedByTenant);
  }

  /**
   * The platform rule for every registry feature. The stored platform list may be partial (dev
   * carried only {@code caseHandoverEnabled}, #254); the missing features take the same default the
   * platform read uses: the legacy toggle mapping, else SUGGESTED/on.
   */
  private Map<String, PolicyValue<Boolean>> platformPolicies(TenantAdminControls platform) {
    return LegacyPermissionPolicyMapper.complete(
        toDomain(platform.getPermissionPolicies()),
        platform.getAllowedPermissionToggles(),
        platform.getEnforcedPermissionToggles());
  }

  private Map<String, ResolvedPolicyValue<Boolean>> resolvePolicies(
      Map<String, PolicyValue<Boolean>> inherited, Map<String, PolicyValue<Boolean>> overrides) {
    Map<String, ResolvedPolicyValue<Boolean>> resolved = new LinkedHashMap<>();
    // tenant overrides stored before the group chat formats were split out (#250) still apply
    Map<String, PolicyValue<Boolean>> effectiveOverrides =
        PermissionFeature.withTransitionFallbacks(overrides);
    // resolve over the registry, never over the platform keys: a tenant override for a feature the
    // platform list does not mention must survive the read (#254)
    for (PermissionFeature feature : PermissionFeature.values()) {
      String key = feature.apiKey();
      resolved.put(
          key,
          PermissionPolicyResolver.resolveWithOrigin(
              inherited.get(key), effectiveOverrides.get(key)));
    }
    return Map.copyOf(resolved);
  }

  @Transactional
  public void saveOverrides(Long tenantId, Map<String, PolicyValue<Boolean>> overrides) {
    saveOverrides(tenantId, overrides, null);
  }

  @Transactional
  public void saveOverrides(
      Long tenantId,
      Map<String, PolicyValue<Boolean>> overrides,
      CaseHandoverPolicies caseHandoverOverrides) {
    saveOverrides(tenantId, overrides, caseHandoverOverrides, null);
  }

  /**
   * Saves the Traeger's overrides. A {@code null} {@code caseHandoverOverrides} or {@code
   * internalGroupChatOverrides} leaves what is stored untouched (#319); a platform requirement
   * (ENFORCED) rejects any change and discards an unchanged echo.
   */
  @Transactional
  public void saveOverrides(
      Long tenantId,
      Map<String, PolicyValue<Boolean>> overrides,
      CaseHandoverPolicies caseHandoverOverrides,
      InternalGroupChatPolicies internalGroupChatOverrides) {
    overrides.keySet().forEach(this::assertKnownFeature);
    CaseHandoverPolicyRules.validate(caseHandoverOverrides);
    TenantAdminControls platform = platformControls.getControls();
    Map<String, PolicyValue<Boolean>> writableOverrides =
        sanitizeBooleanOverrides(platformPolicies(platform), overrides);
    CaseHandoverPolicies parentCaseHandover = platform.getCaseHandoverPolicies();
    if (parentCaseHandover == null) {
      parentCaseHandover = CaseHandoverPolicyDefaults.create();
    }
    CaseHandoverPolicies writableCaseHandoverOverrides =
        sanitizeCaseHandoverOverrides(parentCaseHandover, caseHandoverOverrides);
    InternalGroupChatPolicies writableInternalGroupChat =
        sanitizeInternalGroupChatOverrides(
            platformInternalGroupChat(platform), internalGroupChatOverrides);
    TenantPermissionPolicyEntity entity =
        repository.findByTenantId(tenantId).orElseGet(TenantPermissionPolicyEntity::new);
    entity.setTenantId(tenantId);
    entity.setPolicies(serialize(writableOverrides));
    if (caseHandoverOverrides != null) {
      entity.setCaseHandoverPolicies(serializeCaseHandover(writableCaseHandoverOverrides));
    }
    if (internalGroupChatOverrides != null) {
      entity.setInternalGroupChatPolicies(serializeInternalGroupChat(writableInternalGroupChat));
    }
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    try {
      repository.saveAndFlush(entity);
    } catch (OptimisticLockingFailureException | DataIntegrityViolationException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  public CaseHandoverPolicies getResolvedCaseHandoverPolicies(Long tenantId) {
    CaseHandoverPolicies parent = platformControls.getControls().getCaseHandoverPolicies();
    if (parent == null) {
      parent = CaseHandoverPolicyDefaults.create();
    }
    CaseHandoverPolicies local =
        repository
            .findByTenantId(tenantId)
            .map(TenantPermissionPolicyEntity::getCaseHandoverPolicies)
            .filter(value -> value != null && !value.isBlank())
            .map(this::deserializeCaseHandover)
            .orElse(null);
    Map<String, CaseHandoverReasonPolicy> resolved = new LinkedHashMap<>();
    for (var entry : parent.getReasons().entrySet()) {
      CaseHandoverReasonPolicy localReason =
          local == null ? null : local.getReasons().get(entry.getKey());
      resolved.put(entry.getKey(), resolveReason(entry.getValue(), localReason));
    }
    return new CaseHandoverPolicies(Map.copyOf(resolved));
  }

  /** The Traeger's internal group chat policies: platform preset, Traeger override on top. */
  public InternalGroupChatPolicies getResolvedInternalGroupChatPolicies(Long tenantId) {
    InternalGroupChatPolicies parent = platformInternalGroupChat(platformControls.getControls());
    InternalGroupChatPolicies local =
        repository.findByTenantId(tenantId).map(this::deserializeInternalGroupChat).orElse(null);
    return resolveInternalGroupChat(parent, local);
  }

  public Map<Long, InternalGroupChatPolicies> getResolvedInternalGroupChatPolicies(
      Set<Long> tenantIds) {
    if (tenantIds.isEmpty()) {
      return Map.of();
    }
    InternalGroupChatPolicies parent = platformInternalGroupChat(platformControls.getControls());
    Map<Long, InternalGroupChatPolicies> localByTenant = new LinkedHashMap<>();
    repository
        .findByTenantIdIn(tenantIds)
        .forEach(
            entity ->
                localByTenant.put(entity.getTenantId(), deserializeInternalGroupChat(entity)));
    Map<Long, InternalGroupChatPolicies> resolvedByTenant = new LinkedHashMap<>();
    tenantIds.forEach(
        tenantId ->
            resolvedByTenant.put(
                tenantId, resolveInternalGroupChat(parent, localByTenant.get(tenantId))));
    return Map.copyOf(resolvedByTenant);
  }

  private InternalGroupChatPolicies platformInternalGroupChat(TenantAdminControls platform) {
    return InternalGroupChatPolicyRules.normalize(platform.getInternalGroupChat());
  }

  private InternalGroupChatPolicies resolveInternalGroupChat(
      InternalGroupChatPolicies parent, InternalGroupChatPolicies local) {
    ResolvedPolicyValue<InternalGroupChatRecipientScope> scope =
        PermissionPolicyResolver.resolveWithOrigin(
            toDomain(parent.getRecipientScope()),
            local == null ? null : toDomain(local.getRecipientScope()));
    return new InternalGroupChatPolicies()
        .recipientScope(
            new RecipientScopePermissionPolicy(
                    scope.value(), PermissionPolicyMode.valueOf(scope.mode().name()))
                .inherited(scope.inherited()));
  }

  private InternalGroupChatPolicies sanitizeInternalGroupChatOverrides(
      InternalGroupChatPolicies parent, InternalGroupChatPolicies local) {
    if (local == null) {
      return null;
    }
    PolicyValue<InternalGroupChatRecipientScope> parentScope = toDomain(parent.getRecipientScope());
    PolicyValue<InternalGroupChatRecipientScope> localScope = toDomain(local.getRecipientScope());
    if (localScope != null
        && parentScope.mode() == com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED) {
      if (!Objects.equals(parentScope, localScope)) {
        throw new TenantBadRequestException(
            "Cannot override enforced policy: internalGroupChat.recipientScope");
      }
      return new InternalGroupChatPolicies();
    }
    return new InternalGroupChatPolicies()
        .recipientScope(
            local.getRecipientScope() == null
                ? null
                : new RecipientScopePermissionPolicy(
                    local.getRecipientScope().getValue(), local.getRecipientScope().getMode()));
  }

  private InternalGroupChatPolicies deserializeInternalGroupChat(
      TenantPermissionPolicyEntity entity) {
    String json = entity.getInternalGroupChatPolicies();
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, InternalGroupChatPolicies.class);
    } catch (JsonProcessingException exception) {
      throw mappingFailure("deserialize internal group chat policies", exception);
    }
  }

  private String serializeInternalGroupChat(InternalGroupChatPolicies policies) {
    try {
      return objectMapper.writeValueAsString(policies);
    } catch (JsonProcessingException exception) {
      throw mappingFailure("serialize internal group chat policies", exception);
    }
  }

  private PolicyValue<InternalGroupChatRecipientScope> toDomain(
      RecipientScopePermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private Map<String, PolicyValue<Boolean>> toDomain(
      Map<String, BooleanPermissionPolicy> policies) {
    if (policies == null) {
      return Map.of();
    }
    Map<String, PolicyValue<Boolean>> result = new LinkedHashMap<>();
    policies.forEach(
        (feature, policy) -> {
          assertKnownFeature(feature);
          result.put(
              feature,
              new PolicyValue<>(
                  policy.getValue(),
                  com.vi.tenantservice.api.policy.PermissionPolicyMode.valueOf(
                      policy.getMode().name())));
        });
    return Map.copyOf(result);
  }

  private Map<String, PolicyValue<Boolean>> deserialize(TenantPermissionPolicyEntity entity) {
    try {
      return objectMapper.readValue(
          entity.getPolicies(), new TypeReference<Map<String, PolicyValue<Boolean>>>() {});
    } catch (JsonProcessingException exception) {
      throw mappingFailure("deserialize permission policies", exception);
    }
  }

  private String serialize(Map<String, PolicyValue<Boolean>> policies) {
    try {
      return objectMapper.writeValueAsString(policies);
    } catch (JsonProcessingException exception) {
      throw mappingFailure("serialize permission policies", exception);
    }
  }

  private CaseHandoverPolicies deserializeCaseHandover(String policies) {
    try {
      return objectMapper.readValue(policies, CaseHandoverPolicies.class);
    } catch (JsonProcessingException exception) {
      throw mappingFailure("deserialize Case Handover policies", exception);
    }
  }

  private String serializeCaseHandover(CaseHandoverPolicies policies) {
    try {
      return objectMapper.writeValueAsString(policies);
    } catch (JsonProcessingException exception) {
      throw mappingFailure("serialize Case Handover policies", exception);
    }
  }

  private IllegalStateException mappingFailure(
      String operation, JsonProcessingException exception) {
    return new IllegalStateException("Could not " + operation, exception);
  }

  private Map<String, PolicyValue<Boolean>> sanitizeBooleanOverrides(
      Map<String, PolicyValue<Boolean>> inherited, Map<String, PolicyValue<Boolean>> overrides) {
    Map<String, PolicyValue<Boolean>> writable = new LinkedHashMap<>();
    overrides.forEach(
        (feature, local) -> {
          PolicyValue<Boolean> parent = inherited.get(feature);
          if (parent == null
              || parent.mode() != com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED) {
            writable.put(feature, local);
          } else {
            assertEnforcedEcho(parent, local, feature);
          }
        });
    return Map.copyOf(writable);
  }

  private CaseHandoverPolicies sanitizeCaseHandoverOverrides(
      CaseHandoverPolicies parent, CaseHandoverPolicies local) {
    if (local == null) {
      return null;
    }
    Map<String, CaseHandoverReasonPolicy> writableReasons = new LinkedHashMap<>();
    local
        .getReasons()
        .forEach(
            (code, localReason) -> {
              CaseHandoverReasonPolicy parentReason = parent.getReasons().get(code);
              ConsentPermissionPolicy consent =
                  sanitizePolicy(
                      CaseHandoverPolicyRules.effectiveConsent(parentReason),
                      CaseHandoverPolicyRules.effectiveConsent(localReason),
                      code + ".clientConsent");
              writableReasons.put(
                  code,
                  new CaseHandoverReasonPolicy(
                          localReason.getCode(),
                          sanitizePolicy(
                              parentReason.getLabels(), localReason.getLabels(), code + ".labels"),
                          sanitizePolicy(
                              parentReason.getEnabled(),
                              localReason.getEnabled(),
                              code + ".enabled"),
                          sanitizePolicy(
                              parentReason.getAccessAllowed(),
                              localReason.getAccessAllowed(),
                              code + ".accessAllowed"),
                          CaseHandoverPolicyRules.legacyConsentMirror(consent),
                          sanitizePolicy(
                              parentReason.getApprovalRoles(),
                              localReason.getApprovalRoles(),
                              code + ".approvalRoles"),
                          sanitizePolicy(
                              parentReason.getClientNotificationTemplates(),
                              localReason.getClientNotificationTemplates(),
                              code + ".clientNotificationTemplates"))
                      .clientConsent(consent)
                      .maxAccessDurationMinutes(
                          sanitizePolicy(
                              parentReason.getMaxAccessDurationMinutes(),
                              localReason.getMaxAccessDurationMinutes(),
                              code + ".maxAccessDurationMinutes")));
            });
    return new CaseHandoverPolicies(Map.copyOf(writableReasons));
  }

  private BooleanPermissionPolicy sanitizePolicy(
      BooleanPermissionPolicy parent, BooleanPermissionPolicy local, String field) {
    return sanitizePolicy(parent, local, field, this::toDomain);
  }

  private IntegerPermissionPolicy sanitizePolicy(
      IntegerPermissionPolicy parent, IntegerPermissionPolicy local, String field) {
    return sanitizePolicy(parent, local, field, this::toDomain);
  }

  private ConsentPermissionPolicy sanitizePolicy(
      ConsentPermissionPolicy parent, ConsentPermissionPolicy local, String field) {
    return sanitizePolicy(parent, local, field, this::toDomain);
  }

  private StringListPermissionPolicy sanitizePolicy(
      StringListPermissionPolicy parent, StringListPermissionPolicy local, String field) {
    return sanitizePolicy(parent, local, field, this::toDomain);
  }

  private MultilingualTextPermissionPolicy sanitizePolicy(
      MultilingualTextPermissionPolicy parent,
      MultilingualTextPermissionPolicy local,
      String field) {
    return sanitizePolicy(parent, local, field, this::toDomain);
  }

  private <T, P> P sanitizePolicy(
      P parent, P local, String field, java.util.function.Function<P, PolicyValue<T>> converter) {
    if (parent == null || local == null) {
      return local;
    }
    PolicyValue<T> parentPolicy = converter.apply(parent);
    if (parentPolicy.mode() != com.vi.tenantservice.api.policy.PermissionPolicyMode.ENFORCED) {
      return local;
    }
    assertEnforcedEcho(parentPolicy, converter.apply(local), field);
    return null;
  }

  private <T> void assertEnforcedEcho(PolicyValue<T> parent, PolicyValue<T> local, String field) {
    if (!Objects.equals(parent, local)) {
      throw new IllegalArgumentException("Cannot override enforced policy: " + field);
    }
  }

  private CaseHandoverReasonPolicy resolveReason(
      CaseHandoverReasonPolicy parent, CaseHandoverReasonPolicy local) {
    ConsentPermissionPolicy consent =
        resolveConsent(
            CaseHandoverPolicyRules.effectiveConsent(parent),
            local == null ? null : CaseHandoverPolicyRules.effectiveConsent(local));
    return new CaseHandoverReasonPolicy(
            parent.getCode(),
            resolveMultilingual(parent.getLabels(), local == null ? null : local.getLabels()),
            resolveBoolean(parent.getEnabled(), local == null ? null : local.getEnabled()),
            resolveBoolean(
                parent.getAccessAllowed(), local == null ? null : local.getAccessAllowed()),
            CaseHandoverPolicyRules.legacyConsentMirror(consent),
            resolveStringList(
                parent.getApprovalRoles(), local == null ? null : local.getApprovalRoles()),
            resolveMultilingual(
                parent.getClientNotificationTemplates(),
                local == null ? null : local.getClientNotificationTemplates()))
        .clientConsent(consent)
        .maxAccessDurationMinutes(
            parent.getMaxAccessDurationMinutes() == null
                ? null
                : resolveInteger(
                    parent.getMaxAccessDurationMinutes(),
                    local == null ? null : local.getMaxAccessDurationMinutes()));
  }

  private BooleanPermissionPolicy resolveBoolean(
      BooleanPermissionPolicy parent, BooleanPermissionPolicy local) {
    ResolvedPolicyValue<Boolean> resolved =
        PermissionPolicyResolver.resolveWithOrigin(toDomain(parent), toDomain(local));
    return new BooleanPermissionPolicy(
            resolved.value(), PermissionPolicyMode.valueOf(resolved.mode().name()))
        .inherited(resolved.inherited());
  }

  private ConsentPermissionPolicy resolveConsent(
      ConsentPermissionPolicy parent, ConsentPermissionPolicy local) {
    ResolvedPolicyValue<CaseHandoverConsentValue> resolved =
        PermissionPolicyResolver.resolveWithOrigin(toDomain(parent), toDomain(local));
    return new ConsentPermissionPolicy(
            resolved.value(), PermissionPolicyMode.valueOf(resolved.mode().name()))
        .inherited(resolved.inherited());
  }

  private IntegerPermissionPolicy resolveInteger(
      IntegerPermissionPolicy parent, IntegerPermissionPolicy local) {
    ResolvedPolicyValue<Integer> resolved =
        PermissionPolicyResolver.resolveWithOrigin(toDomain(parent), toDomain(local));
    return new IntegerPermissionPolicy(
            resolved.value(), PermissionPolicyMode.valueOf(resolved.mode().name()))
        .inherited(resolved.inherited());
  }

  private StringListPermissionPolicy resolveStringList(
      StringListPermissionPolicy parent, StringListPermissionPolicy local) {
    ResolvedPolicyValue<Set<String>> resolved =
        PermissionPolicyResolver.resolveWithOrigin(toDomain(parent), toDomain(local));
    return new StringListPermissionPolicy(
            resolved.value(), PermissionPolicyMode.valueOf(resolved.mode().name()))
        .inherited(resolved.inherited());
  }

  private MultilingualTextPermissionPolicy resolveMultilingual(
      MultilingualTextPermissionPolicy parent, MultilingualTextPermissionPolicy local) {
    ResolvedPolicyValue<Map<String, String>> resolved =
        PermissionPolicyResolver.resolveWithOrigin(toDomain(parent), toDomain(local));
    return new MultilingualTextPermissionPolicy(
            resolved.value(), PermissionPolicyMode.valueOf(resolved.mode().name()))
        .inherited(resolved.inherited());
  }

  private <T> PolicyValue<T> toDomain(
      com.vi.tenantservice.api.model.PermissionPolicyMode mode, T value) {
    return value == null || mode == null
        ? null
        : new PolicyValue<>(
            value, com.vi.tenantservice.api.policy.PermissionPolicyMode.valueOf(mode.name()));
  }

  private PolicyValue<Boolean> toDomain(BooleanPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private PolicyValue<CaseHandoverConsentValue> toDomain(ConsentPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private PolicyValue<Integer> toDomain(IntegerPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private PolicyValue<Set<String>> toDomain(StringListPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private PolicyValue<Map<String, String>> toDomain(MultilingualTextPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private void assertKnownFeature(String feature) {
    if (PermissionFeature.byApiKey(feature).isEmpty()) {
      throw new IllegalArgumentException("Unknown permission feature: " + feature);
    }
  }
}
