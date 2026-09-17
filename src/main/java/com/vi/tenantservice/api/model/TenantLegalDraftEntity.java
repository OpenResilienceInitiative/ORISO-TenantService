package com.vi.tenantservice.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "tenant_legal_draft",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "kind"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLegalDraftEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_legal_draft_id_seq")
  @SequenceGenerator(
      name = "tenant_legal_draft_id_seq",
      sequenceName = "SEQUENCE_TENANT_LEGAL_DRAFT",
      allocationSize = 1)
  private Long id;

  @Version private Long version;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TenantLegalDraftKind kind;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;
}
