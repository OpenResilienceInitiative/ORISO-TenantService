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
  private final LocalDateTime verifiedTo;

  public LegacyDpaSigningTime(
      @Value("${oriso.dpa.legacy.signing.zone:}") String writerZone,
      @Value("${oriso.dpa.legacy.signing.from:}") String verifiedFrom,
      @Value("${oriso.dpa.legacy.signing.to:}") String verifiedTo) {
    boolean verifiedWindow =
        !writerZone.isBlank() && !verifiedFrom.isBlank() && !verifiedTo.isBlank();
    this.writerZone = verifiedWindow ? ZoneId.of(writerZone) : null;
    this.verifiedFrom = verifiedWindow ? LocalDateTime.parse(verifiedFrom) : null;
    this.verifiedTo = verifiedWindow ? LocalDateTime.parse(verifiedTo) : null;
    if (verifiedWindow && this.verifiedTo.isBefore(this.verifiedFrom)) {
      throw new IllegalArgumentException("Legacy DPA signing interval ends before it starts");
    }
  }

  public Optional<Instant> resolve(LocalDateTime signedAt) {
    if (verifiedFrom == null
        || signedAt == null
        || signedAt.isBefore(verifiedFrom)
        || signedAt.isAfter(verifiedTo)) {
      return Optional.empty();
    }
    var offsets = writerZone.getRules().getValidOffsets(signedAt);
    return offsets.size() == 1
        ? Optional.of(signedAt.toInstant(offsets.getFirst()))
        : Optional.empty();
  }
}
