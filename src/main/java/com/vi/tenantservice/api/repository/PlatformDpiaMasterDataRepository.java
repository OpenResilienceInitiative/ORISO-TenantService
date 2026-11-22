package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlatformDpiaMasterDataRepository
    extends JpaRepository<PlatformDpiaMasterDataEntity, Long> {

  /**
   * Creates the fixed row or locks the existing row until the caller's transaction commits. Both
   * MariaDB and H2 in MySQL mode support this atomic upsert, including concurrent first writes.
   */
  @Modifying
  @Query(
      value =
          """
      INSERT INTO platform_dpia_master_data (id, update_date)
      VALUES (1, CURRENT_TIMESTAMP)
      ON DUPLICATE KEY UPDATE id = 1
      """,
      nativeQuery = true)
  void initializeSingletonForUpdate();
}
