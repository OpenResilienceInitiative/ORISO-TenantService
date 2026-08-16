package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenant_permission_policy")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantPermissionPolicyEntity {

  @Id
  @SequenceGenerator(
      name = "tenant_permission_policy_id_seq",
      allocationSize = 1,
      sequenceName = "SEQUENCE_TENANT_PERMISSION_POLICY")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_permission_policy_id_seq")
  private Long id;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "tenant_id", nullable = false, unique = true)
  private Long tenantId;

  @Column(name = "policies", nullable = false, columnDefinition = "TEXT")
  private String policies;

  @Column(name = "case_handover_policies", columnDefinition = "TEXT")
  private String caseHandoverPolicies;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;
}
