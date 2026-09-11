package com.vi.tenantservice.api.repository;

import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformDpiaMasterDataRepository
    extends JpaRepository<PlatformDpiaMasterDataEntity, Long> {

  Optional<PlatformDpiaMasterDataEntity> findTopByOrderByIdAsc();
}
