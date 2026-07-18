package com.vi.tenantservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * A tenant-owned media file (editor images for legal/help texts). Content lives in the database on
 * purpose: volumes are tiny (a few images per tenant), backups ride along, and a later swap to
 * object storage stays invisible behind the /media endpoint (PLAN media-upload-security, decision
 * 15).
 */
@Entity
@Table(name = "tenant_media")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantMediaEntity {

  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  // MEDIUMBLOB on MariaDB (changeset 0023); LONGVARBINARY keeps ddl-auto=validate happy.
  @JdbcTypeCode(Types.LONGVARBINARY)
  @Column(name = "content", nullable = false)
  private byte[] content;

  @Column(name = "content_type", nullable = false, length = 64)
  private String contentType;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "content_size", nullable = false)
  private Integer contentSize;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;
}
