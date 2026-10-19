package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
  @Column(name = "id", updatable = false, nullable = false)
  // A fixed initial key makes concurrent singleton creation conflict at the database.
  // Existing rows retain their original identifier.
  @Builder.Default
  private Long id = 1L;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  // LONGTEXT in Liquibase; the length also keeps Hibernate's H2 test schema migration-safe.
  @Column(name = "controls", nullable = false, length = 65535)
  private String controls;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;
}
