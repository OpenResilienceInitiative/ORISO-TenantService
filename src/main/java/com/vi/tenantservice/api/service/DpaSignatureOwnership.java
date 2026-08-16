package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Decides whether a token-based DPA signature belongs to the tenant that currently occupies its
 * tenant ID (ORISO-TenantService#179).
 *
 * <p>Why this exists at all: dropping the {@code tenant_dpa_signature} foreign key — necessary so a
 * link can be minted for a still-reserved ID — made orphaned signature rows possible, while status
 * derivation still trusted {@code tenant_id} alone. A tenant ID is a reusable slot: released,
 * reserved again, handed to a different organisation. So "this row carries tenant id 42" stopped
 * meaning "this row belongs to the organisation that is tenant 42 today".
 *
 * <p>Guarding the write paths one ordering at a time could not close that: each guard only covers
 * the interleavings it was written for, and a release racing a confirmation always found a new one.
 * This is the read-side answer instead — an orphan is inert BY CONSTRUCTION, whatever produced it,
 * because it simply is not counted for the current occupant.
 *
 * <p>The test is the reservation the link was minted from. A ledger row keeps its token when
 * registration consumes it (RESERVED -&gt; ASSIGNED), so a legitimate link still matches after the
 * tenant exists; a released-and-re-reserved ID gets a fresh token, so signatures from the previous
 * occupant no longer match. Signatures without a binding ({@code null}) are invites an
 * authenticated admin created for an already-existing tenant, which the tenant row itself qualifies
 * — they are always the current occupant's.
 */
@Component
@RequiredArgsConstructor
public class DpaSignatureOwnership {

  private final TenantIdReservationRepository tenantIdReservationRepository;

  /** Keeps only the signatures that belong to the current occupant of their tenant ID. */
  public List<TenantDpaSignatureEntity> filterCurrentOccupant(
      Long tenantId, List<TenantDpaSignatureEntity> signatures) {
    if (signatures.isEmpty() || signatures.stream().noneMatch(DpaSignatureOwnership::isBound)) {
      // nothing carries a binding, so nothing can be an orphan — do not pay for a ledger read
      return signatures;
    }
    var currentHash = currentReservationTokenHash(tenantId);
    return signatures.stream()
        .filter(signature -> belongsToCurrentOccupant(signature, currentHash))
        .toList();
  }

  private static boolean belongsToCurrentOccupant(
      TenantDpaSignatureEntity signature, String currentReservationTokenHash) {
    if (!isBound(signature)) {
      return true;
    }
    return currentReservationTokenHash != null
        && currentReservationTokenHash.equals(signature.getReservationTokenHash());
  }

  private static boolean isBound(TenantDpaSignatureEntity signature) {
    return signature.getReservationTokenHash() != null;
  }

  private String currentReservationTokenHash(Long tenantId) {
    return tenantIdReservationRepository
        .findById(tenantId)
        .map(row -> DpaSignToken.hash(row.getToken()))
        .orElse(null);
  }
}
