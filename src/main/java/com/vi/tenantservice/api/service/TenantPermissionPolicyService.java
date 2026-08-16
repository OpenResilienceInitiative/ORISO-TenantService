package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverPolicies;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.MultilingualTextPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.StringListPermissionPolicy;
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
    TenantPermissionPolicyEntity entity =
        repository.findByTenantId(tenantId).orElseGet(TenantPermissionPolicyEntity::new);
    entity.setTenantId(tenantId);
    entity.setPolicies(serialize(overrides));
    if (caseHandoverOverrides != null) {
      entity.setCaseHandoverPolicies(serializeCaseHandover(caseHandoverOverrides));
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
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private String serialize(Map<String, PolicyValue<Boolean>> policies) {
    try {
      return objectMapper.writeValueAsString(policies);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private CaseHandoverPolicies deserializeCaseHandover(String policies) {
    try {
      return objectMapper.readValue(policies, CaseHandoverPolicies.class);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
    }
  }

  private String serializeCaseHandover(CaseHandoverPolicies policies) {
    try {
      return objectMapper.writeValueAsString(policies);
    } catch (JsonProcessingException exception) {
      throw new RuntimeJsonMappingException(exception.getMessage());
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
