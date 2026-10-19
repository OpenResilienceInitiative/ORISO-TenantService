package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "tenant_legal_proposal_delivery",
    uniqueConstraints = @UniqueConstraint(columnNames = {"distribution_id", "proposal_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalProposalDeliveryEntity {
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "tenant_legal_proposal_delivery_id_seq")
  @SequenceGenerator(
      name = "tenant_legal_proposal_delivery_id_seq",
      sequenceName = "SEQUENCE_TENANT_LEGAL_PROPOSAL_DELIVERY",
      allocationSize = 1)
  private Long id;

  @Column(name = "distribution_id", nullable = false, updatable = false, length = 36)
  private String distributionId;

  @Column(name = "proposal_id", nullable = false, updatable = false)
  private Long proposalId;
}
