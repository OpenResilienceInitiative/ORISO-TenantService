package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TenantSettings;
import com.vi.tenantservice.api.policy.PermissionPolicyMode;
import com.vi.tenantservice.api.policy.PolicyValue;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewTenantPresetServiceTest {

  @Mock TenantAdminControlsService tenantAdminControlsService;

  @InjectMocks NewTenantPresetService newTenantPresetService;

  @Test
  void applyCurrentPlatformPreset_should_writeSuggestedFalseForInternalGroupChannels() {
    // given — FE#1725: platform admin suggests both counsellor channels off
    when(tenantAdminControlsService.getExplicitPlatformPolicies())
        .thenReturn(
            Map.of(
                "featureConsultantEmailInternalGroupEnabled",
                new PolicyValue<>(false, PermissionPolicyMode.SUGGESTED),
                "featureConsultantBrowserInternalGroupEnabled",
                new PolicyValue<>(false, PermissionPolicyMode.SUGGESTED)));

    // when
    TenantSettings settings =
        newTenantPresetService.applyCurrentPlatformPreset(new TenantSettings());

    // then
    assertThat(settings.getFeatureConsultantEmailInternalGroupEnabled()).isFalse();
    assertThat(settings.getFeatureConsultantBrowserInternalGroupEnabled()).isFalse();
  }
}
