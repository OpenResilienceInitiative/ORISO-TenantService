package com.vi.tenantservice.api.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Resolves old wall times only for the deployment interval whose writer zone is known. */
@Component
public class LegacyDpaSigningTime {

  private final ZoneId writerZone;
  private final LocalDateTime verifiedFrom;

  public LegacyDpaSigningTime(
      @Value("${oriso.dpa.legacy.signing.zone:}") String writerZone,
      @Value("${oriso.dpa.legacy.signing.from:}") String verifiedFrom) {
    boolean verifiedWindow = !writerZone.isBlank() && !verifiedFrom.isBlank();
    this.writerZone = verifiedWindow ? ZoneId.of(writerZone) : null;
    this.verifiedFrom = verifiedWindow ? LocalDateTime.parse(verifiedFrom) : null;
  }

  public Optional<Instant> resolve(LocalDateTime signedAt) {
    if (verifiedFrom == null || signedAt == null || signedAt.isBefore(verifiedFrom)) {
      return Optional.empty();
    }
    var offsets = writerZone.getRules().getValidOffsets(signedAt);
    return offsets.size() == 1
        ? Optional.of(signedAt.toInstant(offsets.getFirst()))
        : Optional.empty();
  }
}
