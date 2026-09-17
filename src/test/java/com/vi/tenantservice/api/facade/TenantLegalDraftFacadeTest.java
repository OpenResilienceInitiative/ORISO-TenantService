package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftDTO;
import com.vi.tenantservice.api.model.TenantLegalDraftEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalDraftUpdateRequest;
import com.vi.tenantservice.api.service.TenantLegalDraftService;
import com.vi.tenantservice.api.service.TenantService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TenantLegalDraftFacadeTest {

  @Mock private TenantFacadeAuthorisationService authorisation;
  @Mock private TenantService tenantService;
  @Mock private TenantLegalDraftService drafts;

  @ParameterizedTest
  @EnumSource(TenantLegalDraftKind.class)
  void save_Should_authorizeAndPersistBothLegalKindsForTheRequestedTenant(
      TenantLegalDraftKind kind) {
    TenantLegalDraftFacade facade =
        new TenantLegalDraftFacade(authorisation, tenantService, drafts);
    Map<String, String> privacyConsent =
        kind == TenantLegalDraftKind.PRIVACY
            ? Map.of("de", "Ich habe die {{legal_links}} gelesen.")
            : null;
    TenantLegalDraftUpdateRequest request =
        new TenantLegalDraftUpdateRequest().content(Map.of("de", "<p>draft</p>")).revision("new");
    if (privacyConsent != null) request.privacyConsent(privacyConsent);
    TenantLegalDraftEntity saved = entity(51L, 0L, 7L, kind);
    when(tenantService.findTenantById(7L))
        .thenReturn(Optional.of(TenantEntity.builder().id(7L).build()));
    when(drafts.save(7L, kind, request.getContent(), privacyConsent, "new")).thenReturn(saved);
    when(drafts.content(saved)).thenReturn(request.getContent());
    when(drafts.privacyConsent(saved)).thenReturn(privacyConsent);
    when(drafts.revision(saved)).thenReturn("51:0");

    TenantLegalDraftDTO result = facade.save(7L, kind.name(), request);

    verify(authorisation).assertUserIsAuthorizedToAccessTenant(7L);
    assertThat(result.getKind().getValue()).isEqualTo(kind.name());
    assertThat(result.getContent()).containsEntry("de", "<p>draft</p>");
    if (privacyConsent == null) {
      assertThat(result.getPrivacyConsent()).isEmpty();
    } else {
      assertThat(result.getPrivacyConsent()).isEqualTo(privacyConsent);
    }
    assertThat(result.getRevision()).isEqualTo("51:0");
  }

  @Test
  void get_Should_allowPlatformTenantZeroToKeepItsOwnDraft() {
    TenantLegalDraftFacade facade =
        new TenantLegalDraftFacade(authorisation, tenantService, drafts);
    TenantLegalDraftEntity platformDraft = entity(61L, 2L, 0L, TenantLegalDraftKind.PRIVACY);
    when(tenantService.findTenantById(0L))
        .thenReturn(Optional.of(TenantEntity.builder().id(0L).build()));
    when(drafts.get(0L, TenantLegalDraftKind.PRIVACY)).thenReturn(Optional.of(platformDraft));
    when(drafts.content(platformDraft)).thenReturn(Map.of("de", "platform draft"));
    when(drafts.revision(platformDraft)).thenReturn("61:2");

    TenantLegalDraftDTO result = facade.get(0L, "PRIVACY").orElseThrow();

    verify(authorisation).assertUserIsAuthorizedToAccessTenant(0L);
    assertThat(result.getContent()).containsEntry("de", "platform draft");
    assertThat(result.getRevision()).isEqualTo("61:2");
  }

  @Test
  void get_Should_notRevealWhetherACrossTenantDraftExistsBeforeAuthorization() {
    TenantLegalDraftFacade facade =
        new TenantLegalDraftFacade(authorisation, tenantService, drafts);
    org.mockito.Mockito.doThrow(new AccessDeniedException("wrong tenant"))
        .when(authorisation)
        .assertUserIsAuthorizedToAccessTenant(8L);

    assertThatThrownBy(() -> facade.get(8L, "PRIVACY")).isInstanceOf(AccessDeniedException.class);

    verify(tenantService, never()).findTenantById(8L);
    verify(drafts, never()).get(8L, TenantLegalDraftKind.PRIVACY);
  }

  @Test
  void delete_Should_authorizeBeforeCheckingTheDraft() {
    TenantLegalDraftFacade facade =
        new TenantLegalDraftFacade(authorisation, tenantService, drafts);
    when(tenantService.findTenantById(7L))
        .thenReturn(Optional.of(TenantEntity.builder().id(7L).build()));

    facade.delete(7L, "IMPRINT", "72:4");

    verify(authorisation).assertUserIsAuthorizedToAccessTenant(7L);
    verify(drafts).delete(7L, TenantLegalDraftKind.IMPRINT, "72:4");
  }

  private TenantLegalDraftEntity entity(
      long id, long version, long tenantId, TenantLegalDraftKind kind) {
    return TenantLegalDraftEntity.builder()
        .id(id)
        .version(version)
        .tenantId(tenantId)
        .kind(kind)
        .content("{}")
        .updateDate(LocalDateTime.of(2026, 9, 17, 9, 0))
        .build();
  }
}
