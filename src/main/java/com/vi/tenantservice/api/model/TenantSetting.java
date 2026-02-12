package com.vi.tenantservice.api.model;

import static java.util.Collections.singletonList;

import com.vi.tenantservice.api.authorisation.UserRole;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TenantSetting {
  FEATURE_TOPICS_ENABLED(singletonList(UserRole.TENANT_ADMIN)),
  FEATURE_DEMOGRAPHICS_ENABLED(singletonList(UserRole.TENANT_ADMIN)),
  ENABLE_TOPICS_IN_REGISTRATION(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_STATISTICS_ENABLED(singletonList(UserRole.TENANT_ADMIN)),
  FEATURE_APPOINTMENTS_ENABLED(singletonList(UserRole.TENANT_ADMIN)),
  FEATURE_GROUP_CHAT_V2_ENABLED(singletonList(UserRole.TENANT_ADMIN)),
  FEATURE_TOOLS_ENABLED(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_ANONYMOUS_CHAT_ENABLED(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_CALLS_ENABLED(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_THREADS_ENABLED(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_THREADS_GROUP_CHATS_ENABLED(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_THREADS_ONE_ON_ONE_ENABLED(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN)),
  FEATURE_ATTACHMENT_UPLOAD_DISABLED(List.of(UserRole.TENANT_ADMIN)),
  FEATURE_ACTIVE_LANGUAGES(List.of(UserRole.TENANT_ADMIN, UserRole.SINGLE_TENANT_ADMIN));

  private List<UserRole> rolesAuthorisedToChange;
}
