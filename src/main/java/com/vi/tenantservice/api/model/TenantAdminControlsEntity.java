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
@Table(name = "tenant_admin_controls")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAdminControlsEntity {

  @Id
  @SequenceGenerator(
      name = "tenant_admin_controls_id_seq",
      allocationSize = 1,
      sequenceName = "SEQUENCE_TENANT_ADMIN_CONTROLS")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_admin_controls_id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  // LONGTEXT in Liquibase; the length also keeps Hibernate's H2 test schema migration-safe.
  @Column(name = "controls", nullable = false, length = 65535)
  private String controls;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;
}
