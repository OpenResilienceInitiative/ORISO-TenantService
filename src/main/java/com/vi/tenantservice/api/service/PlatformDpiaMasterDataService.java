package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.converter.PlatformDpiaMasterDataConverter;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import com.vi.tenantservice.api.repository.PlatformDpiaMasterDataRepository;
import com.vi.tenantservice.api.validation.InputSanitizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Reads and replaces the platform-level DPIA operator master data singleton (ORISO-Admin#735). Free
 * text is HTML-sanitized on write, mirroring how tenant name/address are handled.
 */
@Service
@RequiredArgsConstructor
public class PlatformDpiaMasterDataService {

  private final @NonNull PlatformDpiaMasterDataRepository platformDpiaMasterDataRepository;
  private final @NonNull PlatformDpiaMasterDataConverter platformDpiaMasterDataConverter;
  private final @NonNull InputSanitizer inputSanitizer;

  public PlatformDpiaMasterDataDTO getMasterData() {
    return platformDpiaMasterDataRepository
        .findTopByOrderByIdAsc()
        .map(platformDpiaMasterDataConverter::toDto)
        .orElseGet(() -> platformDpiaMasterDataConverter.toDto(new PlatformDpiaMasterDataEntity()));
  }

  public PlatformDpiaMasterDataDTO updateMasterData(PlatformDpiaMasterDataDTO masterData) {
    PlatformDpiaMasterDataEntity entity =
        platformDpiaMasterDataRepository
            .findTopByOrderByIdAsc()
            .orElseGet(PlatformDpiaMasterDataEntity::new);
    platformDpiaMasterDataConverter.applyDtoToEntity(masterData, entity);
    sanitizeFreeTextFields(entity);
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    return platformDpiaMasterDataConverter.toDto(platformDpiaMasterDataRepository.save(entity));
  }

  private void sanitizeFreeTextFields(PlatformDpiaMasterDataEntity entity) {
    sanitizeField(entity::getOperatorLegalName, entity::setOperatorLegalName);
    sanitizeField(entity::getOperatorShortName, entity::setOperatorShortName);
    sanitizeField(entity::getOperatorAddress, entity::setOperatorAddress);
    sanitizeField(entity::getOperatorContactEmail, entity::setOperatorContactEmail);
    sanitizeField(entity::getOperatorContactPhone, entity::setOperatorContactPhone);
    sanitizeField(entity::getOperatorDpoName, entity::setOperatorDpoName);
    sanitizeField(entity::getOperatorDepartment, entity::setOperatorDepartment);
    sanitizeField(entity::getOperatorResponsiblePerson, entity::setOperatorResponsiblePerson);
    sanitizeField(entity::getSupervisoryAuthorityName, entity::setSupervisoryAuthorityName);
    sanitizeField(entity::getSupervisoryAuthorityAddress, entity::setSupervisoryAuthorityAddress);
    sanitizeField(entity::getSupervisoryAuthorityEmail, entity::setSupervisoryAuthorityEmail);
  }

  private void sanitizeField(Supplier<String> getter, Consumer<String> setter) {
    setter.accept(sanitizeOrNull(getter.get()));
  }

  private String sanitizeOrNull(String value) {
    if (value == null) {
      return null;
    }
    String sanitized = inputSanitizer.sanitize(value).trim();
    return sanitized.isEmpty() ? null : sanitized;
  }
}
