package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Persistable;

/**
 * Row in the authoritative tenant ID allocation ledger (TEN-INV-U1). The primary key on the
 * reserved tenant ID is the database-level concurrency guarantee: of two parallel allocations of
 * the same ID exactly one insert succeeds.
 *
 * <p>Implements {@link Persistable} so that {@code save()} on a new row always issues an {@code
 * INSERT} (the ID is assigned by the application, never generated), making duplicate allocations
 * fail fast on the primary key instead of silently turning into updates.
 */
@Entity
@Table(name = "tenant_id_reservation")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TenantIdReservationEntity implements Persistable<Long> {

  @Id
  @Column(name = "tenant_id", updatable = false, nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private TenantIdReservationStatus status;

  @Column(name = "token", nullable = false, unique = true, length = 36)
  private String token;

  @Column(name = "reserved_by")
  private String reservedBy;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @Column(name = "update_date", nullable = false)
  private LocalDateTime updateDate;

  @Transient @Builder.Default private transient boolean isNew = true;

  @Override
  public Long getId() {
    return tenantId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}
