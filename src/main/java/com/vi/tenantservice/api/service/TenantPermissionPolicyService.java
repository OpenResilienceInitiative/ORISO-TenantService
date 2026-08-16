package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverPolicies;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.MultilingualTextPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.StringListPermissionPolicy;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantPermissionPolicyEntity;
import com.vi.tenantservice.api.policy.CaseHandoverDurationPolicy;
import com.vi.tenantservice.api.policy.CaseHandoverPolicyDefaults;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantPermissionPolicyService {

  private final @NonNull TenantPermissionPolicyRepository repository;
  private final @NonNull TenantAdminControlsService platformControls;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Map<String, ResolvedPolicyValue<Boolean>> getResolvedPolicies(Long tenantId) {
    Map<String, PolicyValue<Boolean>> inherited =
        toDomain(platformControls.getControls().getPermissionPolicies());
    Map<String, PolicyValue<Boolean>> overrides =
        repository.findByTenantId(tenantId).map(this::deserialize).orElse(Map.of());
    return resolvePolicies(inherited, overrides);
  }

  public Map<Long, Map<String, ResolvedPolicyValue<Boolean>>> getResolvedPolicies(
      Set<Long> tenantIds) {
    if (tenantIds.isEmpty()) {
      return Map.of();
    }
    Map<String, PolicyValue<Boolean>> inherited =
        toDomain(platformControls.getControls().getPermissionPolicies());
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

  private Map<String, ResolvedPolicyValue<Boolean>> resolvePolicies(
      Map<String, PolicyValue<Boolean>> inherited, Map<String, PolicyValue<Boolean>> overrides) {
    Map<String, ResolvedPolicyValue<Boolean>> resolved = new LinkedHashMap<>();
    inherited.forEach(
        (feature, parent) ->
            resolved.put(
                feature,
                PermissionPolicyResolver.resolveWithOrigin(parent, overrides.get(feature))));
    return Map.copyOf(resolved);
  }

  public void saveOverrides(Long tenantId, Map<String, PolicyValue<Boolean>> overrides) {
    saveOverrides(tenantId, overrides, null);
  }

  public void saveOverrides(
      Long tenantId,
      Map<String, PolicyValue<Boolean>> overrides,
      CaseHandoverPolicies caseHandoverOverrides) {
    overrides.keySet().forEach(this::assertKnownFeature);
    validateCaseHandoverOverrides(caseHandoverOverrides);
    TenantAdminControls platform = platformControls.getControls();
    Map<String, PolicyValue<Boolean>> writableOverrides =
        sanitizeBooleanOverrides(toDomain(platform.getPermissionPolicies()), overrides);
    CaseHandoverPolicies parentCaseHandover = platform.getCaseHandoverPolicies();
    if (parentCaseHandover == null) {
      parentCaseHandover = CaseHandoverPolicyDefaults.create();
    }
    CaseHandoverPolicies writableCaseHandoverOverrides =
        sanitizeCaseHandoverOverrides(parentCaseHandover, caseHandoverOverrides);
    TenantPermissionPolicyEntity entity =
        repository.findByTenantId(tenantId).orElseGet(TenantPermissionPolicyEntity::new);
    entity.setTenantId(tenantId);
    entity.setPolicies(serialize(writableOverrides));
    if (caseHandoverOverrides != null) {
      entity.setCaseHandoverPolicies(serializeCaseHandover(writableCaseHandoverOverrides));
    }
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    repository.save(entity);
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
                          sanitizePolicy(
                              parentReason.getClientConsentRequired(),
                              localReason.getClientConsentRequired(),
                              code + ".clientConsentRequired"),
                          sanitizePolicy(
                              parentReason.getApprovalRoles(),
                              localReason.getApprovalRoles(),
                              code + ".approvalRoles"),
                          sanitizePolicy(
                              parentReason.getClientNotificationTemplates(),
                              localReason.getClientNotificationTemplates(),
                              code + ".clientNotificationTemplates"))
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
    return new CaseHandoverReasonPolicy(
            parent.getCode(),
            resolveMultilingual(parent.getLabels(), local == null ? null : local.getLabels()),
            resolveBoolean(parent.getEnabled(), local == null ? null : local.getEnabled()),
            resolveBoolean(
                parent.getAccessAllowed(), local == null ? null : local.getAccessAllowed()),
            resolveBoolean(
                parent.getClientConsentRequired(),
                local == null ? null : local.getClientConsentRequired()),
            resolveStringList(
                parent.getApprovalRoles(), local == null ? null : local.getApprovalRoles()),
            resolveMultilingual(
                parent.getClientNotificationTemplates(),
                local == null ? null : local.getClientNotificationTemplates()))
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

  private PolicyValue<Integer> toDomain(IntegerPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private PolicyValue<Set<String>> toDomain(StringListPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private PolicyValue<Map<String, String>> toDomain(MultilingualTextPermissionPolicy policy) {
    return policy == null ? null : toDomain(policy.getMode(), policy.getValue());
  }

  private void validateCaseHandoverOverrides(CaseHandoverPolicies policies) {
    if (policies == null) {
      return;
    }
    if (policies.getReasons() == null) {
      throw new IllegalArgumentException("Case Handover reasons must not be null");
    }
    Set<String> knownReasons = CaseHandoverPolicyDefaults.create().getReasons().keySet();
    for (var entry : policies.getReasons().entrySet()) {
      if (!knownReasons.contains(entry.getKey()) || entry.getValue() == null) {
        throw new IllegalArgumentException("Unknown Case Handover reason: " + entry.getKey());
      }
      CaseHandoverReasonPolicy reason = entry.getValue();
      if (!entry.getKey().equals(reason.getCode().getValue())) {
        throw new IllegalArgumentException("Case Handover reason key must match its code");
      }
      if (CaseHandoverPolicyDefaults.ADVICE_NEEDED.equals(entry.getKey())) {
        IntegerPermissionPolicy duration = reason.getMaxAccessDurationMinutes();
        CaseHandoverDurationPolicy.validateAdviceNeeded(
            duration == null ? null : duration.getValue());
      } else {
        CaseHandoverDurationPolicy.validateTakeover(
            reason.getMaxAccessDurationMinutes() == null
                ? null
                : reason.getMaxAccessDurationMinutes().getValue());
      }
    }
  }

  private void assertKnownFeature(String feature) {
    if (PermissionFeature.byApiKey(feature).isEmpty()) {
      throw new IllegalArgumentException("Unknown permission feature: " + feature);
    }
  }
}
