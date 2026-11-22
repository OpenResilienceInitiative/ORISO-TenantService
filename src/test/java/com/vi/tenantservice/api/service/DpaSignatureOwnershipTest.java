package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantIdReservationEntity;
import com.vi.tenantservice.api.model.TenantIdReservationStatus;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A tenant ID is a reusable slot, so "carries tenant id 42" is not the same as "belongs to the
 * organisation that is tenant 42 today" (ORISO-TenantService#179).
 */
@ExtendWith(MockitoExtension.class)
class DpaSignatureOwnershipTest {

  private static final Long TENANT_ID = 42L;

  @Mock private TenantIdReservationRepository tenantIdReservationRepository;
  @InjectMocks private DpaSignatureOwnership dpaSignatureOwnership;

  private static TenantDpaSignatureEntity signature(String reservationTokenHash) {
    return TenantDpaSignatureEntity.builder()
        .tenantId(TENANT_ID)
        .status(DpaSignatureStatus.SIGNED)
        .signerName("Erika Mustermann")
        .reservationTokenHash(reservationTokenHash)
        .build();
  }

  private void givenLedgerToken(String token) {
    when(tenantIdReservationRepository.findById(TENANT_ID))
        .thenReturn(
            Optional.of(
                TenantIdReservationEntity.builder()
                    .tenantId(TENANT_ID)
                    .token(token)
                    .status(TenantIdReservationStatus.ASSIGNED)
                    .build()));
  }

  @Test
  void filterCurrentOccupant_Should_keepASignatureBoundToTheCurrentReservation() {
    givenLedgerToken("reservation-token");
    var signed = signature(DpaSignToken.hash("reservation-token"));

    assertThat(dpaSignatureOwnership.filterCurrentOccupant(TENANT_ID, List.of(signed)))
        .containsExactly(signed);
  }

  @Test
  void filterCurrentOccupant_Should_dropAnOrphanFromAPreviousOccupantOfTheSameId() {
    // reservation A was released and the id reserved again as B; A's signature is inert
    givenLedgerToken("reservation-token-B");
    var orphan = signature(DpaSignToken.hash("reservation-token-A"));

    assertThat(dpaSignatureOwnership.filterCurrentOccupant(TENANT_ID, List.of(orphan))).isEmpty();
  }

  @Test
  void filterCurrentOccupant_Should_dropABoundSignature_When_theLedgerRowIsGone() {
    // released and never reserved again: nothing can claim the signature
    when(tenantIdReservationRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

    assertThat(
            dpaSignatureOwnership.filterCurrentOccupant(
                TENANT_ID, List.of(signature(DpaSignToken.hash("reservation-token-A")))))
        .isEmpty();
  }

  @Test
  void filterCurrentOccupant_Should_keepUnboundSignatures() {
    // an authenticated admin's invite is bound to the tenant row itself, not to a reservation
    var adminInviteSignature = signature(null);

    assertThat(
            dpaSignatureOwnership.filterCurrentOccupant(TENANT_ID, List.of(adminInviteSignature)))
        .containsExactly(adminInviteSignature);
    // and it costs no ledger read
    verify(tenantIdReservationRepository, never()).findById(TENANT_ID);
  }

  @Test
  void filterCurrentOccupant_Should_separateOccupants_When_bothKindsAreMixed() {
    givenLedgerToken("reservation-token-B");
    var mine = signature(DpaSignToken.hash("reservation-token-B"));
    var orphan = signature(DpaSignToken.hash("reservation-token-A"));
    var adminInvite = signature(null);

    assertThat(
            dpaSignatureOwnership.filterCurrentOccupant(
                TENANT_ID, List.of(mine, orphan, adminInvite)))
        .containsExactly(mine, adminInvite);
  }

  @Test
  void filterCurrentOccupant_Should_shortCircuit_When_nothingIsBound() {
    assertThat(dpaSignatureOwnership.filterCurrentOccupant(TENANT_ID, List.of())).isEmpty();
    verify(tenantIdReservationRepository, never()).findById(TENANT_ID);
  }
}
