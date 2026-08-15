package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vi.tenantservice.api.authorisation.Authority.AuthorityValue;
import com.vi.tenantservice.api.converter.ConsultingTypePatchDTOConverter;
import com.vi.tenantservice.api.converter.EffectivePermissionSettingsApplier;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.exception.TenantIdAllocationExhaustedException;
import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.OnboardingDpaAcceptanceDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import com.vi.tenantservice.api.model.TenantAdminControls;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantRestrictedData;
import com.vi.tenantservice.api.service.SingleDomainTenantOverrideService;
import com.vi.tenantservice.api.service.TemplateRenderer;
import com.vi.tenantservice.api.service.TemplateService;
import com.vi.tenantservice.api.service.TenantAdminControlsService;
import com.vi.tenantservice.api.service.TenantDpaStatusService;
import com.vi.tenantservice.api.service.TenantDpaStatusService.AdminSignatureForm;
import com.vi.tenantservice.api.service.TenantIdAllocationService;
import com.vi.tenantservice.api.service.TenantService;
import com.vi.tenantservice.api.service.TranslationService;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.validation.TenantInputSanitizer;
import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class TenantServiceFacadeTest {

  private static final Long ID = 1L;
  public static final String DE = "de";
  public static final String SINGLE_DOMAIN_SUBDOMAIN_NAME = "app";
  public static final int CONSULTING_TYPE_ID = 2;
  private final MultilingualTenantDTO tenantMultilingualDTO = getMultilingualTenantDTO();

  @Mock TemplateRenderer templateRenderer;

  private MultilingualTenantDTO getMultilingualTenantDTO() {
    var tenantDTO = new MultilingualTenantDTO();
    Settings settings = new Settings();
    settings.setExtendedSettings(new ConsultingTypePatchDTO());
    tenantDTO.settings(settings);
    return tenantDTO;
  }

  private final TenantDTO tenantDTO = new TenantDTO();
  private final MultilingualTenantDTO sanitizedTenantDTO = getMultilingualTenantDTO();
  private final RestrictedTenantDTO restrictedTenantDTO = new RestrictedTenantDTO();
  private final TenantEntity tenantEntity = new TenantEntity();

  @Mock private TenantConverter converter;

  @Mock private ConsultingTypePatchDTOConverter consultingTypePatchDTOConverter;

  @Mock private TenantService tenantService;

  @Mock private TenantInputSanitizer tenantInputSanitizer;

  @Mock private TenantFacadeAuthorisationService tenantFacadeAuthorisationService;

  @Mock private AuthorisationService authorisationService;

  @Mock private TranslationService translationService;

  @Mock private ConsultingTypeService consultingTypeService;

  @Mock private ApplicationSettingsService applicationSettingsService;

  @Mock private SubdomainExtractor subdomainExtractor;

  @Mock private UserAdminService userAdminService;

  @Mock private TenantResolverService tenantResolverService;

  @Mock private TenantDpaStatusService tenantDpaStatusService;

  @Mock private com.vi.tenantservice.api.service.TenantDpaService tenantDpaService;

  @Mock
  private TenantFacadeDependentSettingsOverrideService tenantFacadeDependentSettingsOverrideService;

  @Mock private TenantAdminControlsService tenantAdminControlsService;

  @Spy
  private EffectivePermissionSettingsApplier effectivePermissionSettingsApplier =
      new EffectivePermissionSettingsApplier();

  @Mock private SingleDomainTenantOverrideService singleDomainTenantOverrideService;
  @Mock private TenantIdAllocationService tenantIdAllocationService;

  @InjectMocks private TenantServiceFacade tenantServiceFacade;

  @BeforeEach
  public void initialize() {
    tenantEntity.setId(ID);
  }

  @Test
  void createTenant_Should_createTenant() {
    // given
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(tenantMultilingualDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    verify(converter).toEntity(sanitizedTenantDTO);
    verify(tenantService).create(tenantEntity, null);
    verify(consultingTypeService).createDefaultConsultingTypes(tenantEntity.getId());
    verify(applicationSettingsService, never()).saveMainTenantSubDomain(any());
  }

  @Test
  void
      createTenant_Should_createTenantWithMainTenantSubDomain_When_multitenancyWithSingleDomainAndIsFirstNonTechnicalTenant() {
    // given
    TenantEntity entity = mock(TenantEntity.class);
    when(entity.getSubdomain()).thenReturn("app1");
    when(entity.getId()).thenReturn(1L);

    TenantEntity technicalTenant = mock(TenantEntity.class);
    when(technicalTenant.getId()).thenReturn(0L);

    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(tenantMultilingualDTO)).thenReturn(entity);
    when(tenantService.create(entity, null)).thenReturn(entity);
    ReflectionTestUtils.setField(tenantServiceFacade, "multitenancyWithSingleDomain", true);
    when(tenantService.getAllTenantData()).thenReturn(List.of(technicalTenant));
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("app1"));

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    verify(converter).toEntity(sanitizedTenantDTO);
    verify(tenantService).create(entity, null);
    verify(consultingTypeService).createDefaultConsultingTypes(entity.getId());
    verify(applicationSettingsService).saveMainTenantSubDomain("app1");
  }

  @Test
  void
      createTenant_Should_notSaveMainTenantSubDomain_When_subDomainInRequestDifferentFromSubdomainInUrl() {
    // given
    TenantEntity entity = mock(TenantEntity.class);
    when(entity.getSubdomain()).thenReturn("app1");
    when(entity.getId()).thenReturn(1L);

    TenantEntity technicalTenant = mock(TenantEntity.class);
    when(technicalTenant.getId()).thenReturn(0L);

    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(tenantMultilingualDTO)).thenReturn(entity);
    when(tenantService.create(entity, null)).thenReturn(entity);
    ReflectionTestUtils.setField(tenantServiceFacade, "multitenancyWithSingleDomain", true);
    when(tenantService.getAllTenantData()).thenReturn(List.of(technicalTenant));
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("app2"));

    // when
    assertThrows(
        TenantValidationException.class,
        () -> {
          tenantServiceFacade.createTenant(tenantMultilingualDTO);
        });

    // then
    verify(consultingTypeService).createDefaultConsultingTypes(entity.getId());
    verify(applicationSettingsService, never()).saveMainTenantSubDomain(any());
  }

  @Test
  void createTenant_Should_notSaveMainTenantSubDomain_When_activeLanguageHasIncorrectContent() {
    // given
    tenantMultilingualDTO.settings(new Settings().activeLanguages(Lists.newArrayList(null, "de")));

    // when
    assertThrows(
        TenantValidationException.class,
        () -> {
          tenantServiceFacade.createTenant(tenantMultilingualDTO);
        });
  }

  @Test
  void createTenant_Should_passReservationTokenToAllocationAwareCreate_When_tokenIsProvided() {
    // given (TEN-INV-U1: manual IDs are no longer rejected upfront; they are re-validated
    // against the allocation ledger inside the creating transaction)
    tenantMultilingualDTO.setTenantIdReservationToken("reservation-token");
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, "reservation-token")).thenReturn(tenantEntity);

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    verify(tenantService).create(tenantEntity, "reservation-token");
  }

  // --- onboarding DPA acceptance (#569, TEN-INV-U9) ---------------------------------------

  private OnboardingDpaAcceptanceDTO onboardingAcceptance() {
    return new OnboardingDpaAcceptanceDTO()
        .accepted(true)
        .signerUserId("kc-admin-id")
        .signerUsername("toni@traeger-nord.example")
        .signerName("Toni Tenantadmin")
        .signerPosition("Geschäftsführung")
        .signerEmail("toni@traeger-nord.example")
        .signerOrganisation("Träger Nord e.V.")
        .dpaVersion("2026-07-01T12:00:00");
  }

  /**
   * The acceptance the invitee gave in the public onboarding must land in the append-only audit
   * trail — otherwise the tenant's DPA status can never resolve to VALID and the freshly onboarded
   * admin logs straight into the non-bypassable blocker (#569 defect 1 + 2).
   */
  @Test
  void createTenant_Should_persistTheOnboardingDpaAcceptance_When_itIsSubmitted() {
    // given
    tenantMultilingualDTO.setOnboardingDpaAcceptance(onboardingAcceptance());
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    var formCaptor = ArgumentCaptor.forClass(AdminSignatureForm.class);
    verify(tenantDpaStatusService)
        .signOnboarding(
            org.mockito.ArgumentMatchers.eq(ID),
            org.mockito.ArgumentMatchers.eq("kc-admin-id"),
            org.mockito.ArgumentMatchers.eq("toni@traeger-nord.example"),
            org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 7, 1, 12, 0)),
            formCaptor.capture());
    var form = formCaptor.getValue();
    assertThat(form.signerName()).isEqualTo("Toni Tenantadmin");
    assertThat(form.signerPosition()).isEqualTo("Geschäftsführung");
    assertThat(form.signerEmail()).isEqualTo("toni@traeger-nord.example");
    assertThat(form.signerOrganisation()).isEqualTo("Träger Nord e.V.");
    assertThat(form.formDataJson()).contains("PUBLIC_TENANT_ADMIN_ONBOARDING");
    assertThat(form.formDataJson()).contains("\"accepted\":true");
  }

  /**
   * The signer fields are PLAIN TEXT of a legal record. Running them through the HTML sanitizer
   * entity-encoded the audit trail (#569 defect 2), so the stored signature no longer matched what
   * the signer submitted. They are persisted verbatim; consumers encode at render time.
   */
  @Test
  void createTenant_Should_storeTheOnboardingSignerFieldsVerbatim() {
    // given a signer whose plain-text fields carry characters an HTML sanitizer would mangle
    tenantMultilingualDTO.setOnboardingDpaAcceptance(
        onboardingAcceptance()
            .signerUsername("toni&nord")
            .signerName("Toni & Söhne <Tenantadmin>")
            .signerPosition("Leitung Recht & Ordnung")
            .signerEmail("toni+dpa@traeger-nord.example")
            .signerOrganisation("Träger & Nord e.V."));
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    var formCaptor = ArgumentCaptor.forClass(AdminSignatureForm.class);
    verify(tenantDpaStatusService)
        .signOnboarding(
            org.mockito.ArgumentMatchers.eq(ID),
            org.mockito.ArgumentMatchers.eq("kc-admin-id"),
            org.mockito.ArgumentMatchers.eq("toni&nord"),
            any(),
            formCaptor.capture());
    var form = formCaptor.getValue();
    assertThat(form.signerName()).isEqualTo("Toni & Söhne <Tenantadmin>");
    assertThat(form.signerPosition()).isEqualTo("Leitung Recht & Ordnung");
    assertThat(form.signerEmail()).isEqualTo("toni+dpa@traeger-nord.example");
    assertThat(form.signerOrganisation()).isEqualTo("Träger & Nord e.V.");
    assertThat(form.formDataJson()).contains("Toni & Söhne <Tenantadmin>");
    assertThat(form.formDataJson()).contains("Träger & Nord e.V.");
  }

  /** Verbatim is not unbounded: an over-long field is rejected instead of silently truncated. */
  @Test
  void createTenant_Should_rollback_When_aSignerFieldExceedsItsColumnLength() {
    // given
    tenantMultilingualDTO.setOnboardingDpaAcceptance(
        onboardingAcceptance().signerName("x".repeat(256)));
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);

    // when
    assertThrows(
        RuntimeException.class, () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then
    verify(tenantService).delete(tenantEntity);
    verify(tenantDpaStatusService, never()).signOnboarding(anyLong(), any(), any(), any(), any());
  }

  /**
   * Never report a successful onboarding for an unrecorded acceptance: a failing signature write
   * rolls the tenant creation back (reservation restored) so the invite link stays retryable.
   */
  @Test
  void
      createTenant_Should_rollbackWithReservationToken_When_theOnboardingAcceptanceCannotBeStored() {
    // given
    tenantMultilingualDTO.setTenantIdReservationToken("reservation-token");
    tenantMultilingualDTO.setOnboardingDpaAcceptance(onboardingAcceptance());
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, "reservation-token")).thenReturn(tenantEntity);
    doThrow(new IllegalStateException("signature write failed"))
        .when(tenantDpaStatusService)
        .signOnboarding(anyLong(), any(), any(), any(), any());

    // when
    assertThrows(
        RuntimeException.class, () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then
    verify(tenantService).delete(tenantEntity);
    verify(tenantIdAllocationService).rollbackAssignment(ID, "reservation-token");
  }

  @Test
  void createTenant_Should_notTouchTheAuditTrail_When_noOnboardingAcceptanceIsSubmitted() {
    // given (platform admin seeding a tenant directly — tenant B of the chain test)
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(tenantMultilingualDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    verifyNoInteractions(tenantDpaStatusService);
  }

  @Test
  void createTenant_Should_rollback_When_theAcceptanceCheckboxWasNotTicked() {
    // given
    tenantMultilingualDTO.setOnboardingDpaAcceptance(onboardingAcceptance().accepted(false));
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);

    // when
    assertThrows(
        RuntimeException.class, () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then
    verify(tenantService).delete(tenantEntity);
    verify(tenantDpaStatusService, never()).signOnboarding(anyLong(), any(), any(), any(), any());
  }

  @Test
  void createTenant_Should_rollbackWithReservationToken_When_consultingTypeCreationFails() {
    // given (TEN-INV hardening: a consumed invite reservation must be restored, not deleted)
    tenantMultilingualDTO.setTenantIdReservationToken("reservation-token");
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, "reservation-token")).thenReturn(tenantEntity);
    doThrow(new RestClientException("consulting type service down"))
        .when(consultingTypeService)
        .createDefaultConsultingTypes(tenantEntity.getId());

    // when (the facade converts the failure to a BadRequestException; in this plain unit-test
    // classpath no JAX-RS RuntimeDelegate is present, so only RuntimeException can be asserted)
    assertThrows(
        RuntimeException.class, () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then
    verify(tenantService).delete(tenantEntity);
    verify(tenantIdAllocationService).rollbackAssignment(ID, "reservation-token");
  }

  @Test
  void createTenant_Should_keepLedgerRow_When_rollbackTenantDeleteFails() {
    // given (TEN-INV hardening: if the tenant row survives, the ASSIGNED ledger row must too)
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null)).thenReturn(tenantEntity);
    doThrow(new RestClientException("consulting type service down"))
        .when(consultingTypeService)
        .createDefaultConsultingTypes(tenantEntity.getId());
    doThrow(new RuntimeException("delete failed")).when(tenantService).delete(tenantEntity);

    // when
    assertThrows(
        RuntimeException.class, () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then
    verify(tenantIdAllocationService, never()).rollbackAssignment(anyLong(), any());
  }

  @Test
  void createTenant_Should_throwExhausted_When_autoIdAllocationRetriesAreUsedUp() {
    // given (TEN-INV hardening: AUTO exhaustion is a 503 contention condition, not a 409)
    TenantEntity autoEntity = new TenantEntity();
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(autoEntity);
    when(tenantService.create(autoEntity, null))
        .thenThrow(new TenantIdAllocationConflictException("lost the race"));

    // when
    assertThrows(
        TenantIdAllocationExhaustedException.class,
        () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then (the facade's retry budget is aligned with the allocation service's)
    verify(tenantService, times(TenantIdAllocationService.MAX_AUTO_ATTEMPTS))
        .create(autoEntity, null);
  }

  @Test
  void createTenant_Should_throwConflict_When_manualIdIsTaken() {
    // given (manual mode keeps its 409 and is never retried)
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(converter.toEntity(sanitizedTenantDTO)).thenReturn(tenantEntity);
    when(tenantService.create(tenantEntity, null))
        .thenThrow(new TenantIdAllocationConflictException("taken"));

    // when
    assertThrows(
        TenantIdAllocationConflictException.class,
        () -> tenantServiceFacade.createTenant(tenantMultilingualDTO));

    // then
    verify(tenantService, times(1)).create(tenantEntity, null);
  }

  @Test
  void updateTenant_Should_updateTenant_When_tenantIsFoundAndUserIsMultipleTenantAdmin() {
    // given
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(converter.toEntity(tenantEntity, sanitizedTenantDTO)).thenReturn(tenantEntity);
    givenConsultingTypeReturnsConsultingTypeByTenantId();
    when(tenantService.update(tenantEntity)).thenReturn(tenantEntity);
    when(converter.toMultilingualDTO(tenantEntity)).thenReturn(sanitizedTenantDTO);

    // when
    tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);

    // then
    verify(tenantService).findTenantById(ID);
    verify(converter).toEntity(tenantEntity, sanitizedTenantDTO);
    verify(tenantService).update(tenantEntity);
  }

  private void givenConsultingTypeReturnsConsultingTypeByTenantId() {

    when(consultingTypeService.getConsultingTypesByTenantId(ID.intValue()))
        .thenReturn(new FullConsultingTypeResponseDTO().id(CONSULTING_TYPE_ID));
  }

  @Test
  void updateTenant_Should_throwBadRequest_When_languageKeyIsNotValid() {
    // given
    HashMap<String, String> claim = Maps.newHashMap();
    claim.put("en", "english claim");
    claim.put("not existent", "not existing claim");
    tenantMultilingualDTO.setContent(new MultilingualContent().claim(claim));

    // when
    assertThrows(
        TenantValidationException.class,
        () -> {
          tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);
        });
  }

  @Test
  void updateTenant_Should_passValidation_When_languageKeyIsValid() {
    // given
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(converter.toEntity(tenantEntity, sanitizedTenantDTO)).thenReturn(tenantEntity);
    HashMap<String, String> claim = Maps.newHashMap();
    claim.put("en", "english claim");
    claim.put("de", "german claim");
    tenantMultilingualDTO.setContent(new MultilingualContent().claim(claim));
    givenConsultingTypeReturnsConsultingTypeByTenantId();
    when(tenantService.update(tenantEntity)).thenReturn(tenantEntity);
    when(converter.toMultilingualDTO(tenantEntity)).thenReturn(sanitizedTenantDTO);
    // when
    tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);

    // then
    verify(tenantService).findTenantById(ID);
    verify(converter).toEntity(tenantEntity, sanitizedTenantDTO);
    verify(tenantService).update(tenantEntity);
  }

  @Test
  void updateTenant_Should_passValidation_When_translationMetadataKeyIsValid() {
    // given
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(converter.toEntity(tenantEntity, sanitizedTenantDTO)).thenReturn(tenantEntity);
    HashMap<String, String> privacy = Maps.newHashMap();
    privacy.put("en", "english privacy text");
    privacy.put("en__meta", "{\"mt\":true,\"src\":\"de\"}");
    tenantMultilingualDTO.setContent(new MultilingualContent().privacy(privacy));
    givenConsultingTypeReturnsConsultingTypeByTenantId();
    when(tenantService.update(tenantEntity)).thenReturn(tenantEntity);
    when(converter.toMultilingualDTO(tenantEntity)).thenReturn(sanitizedTenantDTO);

    // when
    tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);

    // then
    verify(tenantService).update(tenantEntity);
  }

  @Test
  void updateTenant_Should_ThrowTenantNotFoundException_When_IdNotFound() {
    // then
    assertThrows(
        TenantNotFoundException.class,
        () -> {

          // when
          tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);
        });
    verify(tenantService).findTenantById(ID);
  }

  @Test
  void
      updateTenant_Should_updateTenantAndExtendedTenantSettings_When_tenantIsFoundAndUserIsSingleTenantAdminForThatTenant() {
    // given
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(converter.toEntity(
            Mockito.any(TenantEntity.class), Mockito.any(MultilingualTenantDTO.class)))
        .thenReturn(tenantEntity);
    when(tenantService.update(tenantEntity)).thenReturn(tenantEntity);
    when(converter.toMultilingualDTO(tenantEntity)).thenReturn(sanitizedTenantDTO);
    when(consultingTypePatchDTOConverter.convertToConsultingTypeServiceModel(
            Mockito.any(ConsultingTypePatchDTO.class)))
        .thenReturn(
            new com.vi.tenantservice.consultingtypeservice.generated.web.model
                .ConsultingTypePatchDTO());

    tenantEntity.setId(ID);
    givenConsultingTypeReturnsConsultingTypeByTenantId();
    // when
    tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);

    // then
    verify(tenantService).findTenantById(ID);
    verify(converter).toEntity(tenantEntity, sanitizedTenantDTO);
    verify(tenantService).update(tenantEntity);
    verify(consultingTypeService)
        .patchConsultingType(
            Mockito.eq(2),
            Mockito.any(
                com.vi.tenantservice.consultingtypeservice.generated.web.model
                    .ConsultingTypePatchDTO.class));
  }

  @Test
  void
      updateTenant_Should_updateTenantButNotExtendedTenantSettings_When_tenantIsFoundAndExtendedTenantSettingsDidNotChange() {
    // given
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(converter.toEntity(
            Mockito.any(TenantEntity.class), Mockito.any(MultilingualTenantDTO.class)))
        .thenReturn(tenantEntity);
    when(tenantService.update(tenantEntity)).thenReturn(tenantEntity);
    when(converter.toMultilingualDTO(tenantEntity)).thenReturn(sanitizedTenantDTO);

    tenantEntity.setId(ID);
    givenConsultingTypeReturnsConsultingTypeByTenantId();
    when(consultingTypePatchDTOConverter.convertConsultingTypePatchDTO(
            Mockito.any(FullConsultingTypeResponseDTO.class)))
        .thenReturn(sanitizedTenantDTO.getSettings().getExtendedSettings());
    // when
    tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);

    // then
    verify(tenantService).findTenantById(ID);
    verify(converter).toEntity(tenantEntity, sanitizedTenantDTO);
    verify(tenantService).update(tenantEntity);
    verify(consultingTypeService, never())
        .patchConsultingType(
            Mockito.anyInt(),
            Mockito.any(
                com.vi.tenantservice.consultingtypeservice.generated.web.model
                    .ConsultingTypePatchDTO.class));
  }

  @Test
  void updateTenant_Should_ThrowAccessDeniedException_When_UserNotAuthorizedToPerformOperation() {
    // given
    doThrow(AccessDeniedException.class)
        .when(tenantFacadeAuthorisationService)
        .assertUserIsAuthorizedToAccessTenant(ID);
    // then
    assertThrows(
        AccessDeniedException.class,
        () -> {
          // when
          tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);
        });
    verify(tenantService, Mockito.never()).findTenantById(ID);
  }

  @Test
  void
      updateTenant_Should_ThrowAccessDeniedException_When_UserIsSingleTenantAdminAndDoesAndTokenIdAttributeDoesNotMatch() {
    // given
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(tenantInputSanitizer.sanitize(tenantMultilingualDTO)).thenReturn(sanitizedTenantDTO);

    Mockito.doThrow(AccessDeniedException.class)
        .when(tenantFacadeAuthorisationService)
        .assertUserHasSufficientPermissionsToChangeAttributes(
            Mockito.any(MultilingualTenantDTO.class), Mockito.any(TenantEntity.class));

    // then
    assertThrows(
        AccessDeniedException.class,
        () -> {
          // when
          tenantServiceFacade.updateTenant(ID, tenantMultilingualDTO);
        });
  }

  @Test
  void findTenantById_Should_notFindTenant_When_NotExistingIdIsPassedForSingleTenantAdmin() {
    // when
    Optional<TenantDTO> tenantById = tenantServiceFacade.findTenantById(2L);

    // then
    assertThat(tenantById).isNotPresent();
  }

  @Test
  void findTenantById_Should_findTenant_When_ExistingIdIsPassedForSingleTenantAdmin() {
    // given
    when(tenantService.findTenantDataById(ID)).thenReturn(Optional.of(tenantEntity));
    when(translationService.getCurrentLanguageContext()).thenReturn("de");
    when(converter.toDTO(tenantEntity, "de")).thenReturn(tenantDTO);
    // when
    Optional<TenantDTO> tenantById = tenantServiceFacade.findTenantById(ID);
    assertThat(tenantById).contains(tenantDTO);
  }

  @Test
  void findMultilingualTenantById_Should_findTenant_When_ExistingIdIsPassedForSingleTenantAdmin() {
    // given
    when(tenantService.findTenantDataById(ID)).thenReturn(Optional.of(tenantEntity));
    tenantEntity.setId(1L);
    tenantMultilingualDTO.setId(1L);
    when(consultingTypeService.getConsultingTypesByTenantId(Mockito.anyInt()))
        .thenReturn(new FullConsultingTypeResponseDTO());
    when(converter.toMultilingualDTO(tenantEntity)).thenReturn(tenantMultilingualDTO);
    when(userAdminService.getTenantAdmins(1))
        .thenReturn(
            Lists.newArrayList(
                new AdminResponseDTO().embedded(new AdminDTO().email("admin@admin.com"))));
    when(authorisationService.hasAuthority(AuthorityValue.GET_TENANT_ADMIN_DATA)).thenReturn(true);
    // when
    Optional<MultilingualTenantDTO> tenantById = tenantServiceFacade.findMultilingualTenantById(ID);
    assertThat(tenantById).contains(tenantMultilingualDTO);
    assertThat(tenantById.get().getAdminEmails()).containsOnly("admin@admin.com");
  }

  @Test
  void getAllTenant_Should_CallServiceToGetAllTenants() {
    // when
    tenantServiceFacade.getAllTenants();
    // then
    verify(tenantService).getAllTenantData();
  }

  @Test
  void getSingleTenant_Should_findTenant_When_onlyOneTenantIsPresent() {
    // given
    when(tenantService.getAllTenantData()).thenReturn(List.of(tenantEntity));
    when(translationService.getCurrentLanguageContext()).thenReturn(DE);
    when(converter.toRestrictedTenantDTO(tenantEntity, DE)).thenReturn(restrictedTenantDTO);

    // when
    tenantServiceFacade.getSingleTenant();

    // then
    verify(tenantService).getAllTenantData();
    verify(converter).toRestrictedTenantDTO(tenantEntity, DE);
  }

  @Test
  void getSingleTenant_Should_applyEffectivePlatformControls_toPublicSettings() {
    // given: platform disallows video calls; the tenant itself has them on
    var publicDto =
        new RestrictedTenantDTO().settings(new Settings().featureVideoCallsEnabled(true));
    when(tenantService.getAllTenantData()).thenReturn(List.of(tenantEntity));
    when(translationService.getCurrentLanguageContext()).thenReturn(DE);
    when(converter.toRestrictedTenantDTO(tenantEntity, DE)).thenReturn(publicDto);
    when(tenantAdminControlsService.getControls())
        .thenReturn(
            new TenantAdminControls()
                .allowedPermissionToggles(
                    new TenantAdminAllowedPermissionToggles().videoCalls(false)));

    // when
    var result = tenantServiceFacade.getSingleTenant();

    // then: the public settings served to the counselling app reflect the platform constraint
    assertThat(result).isPresent();
    assertThat(result.get().getSettings().getFeatureVideoCallsEnabled()).isFalse();
  }

  @Test
  void findRestrictedTenantsByIds_Should_loadPlatformControlsOnceForTheBatch() {
    var firstTenant = mock(TenantRestrictedData.class);
    var secondTenant = mock(TenantRestrictedData.class);
    var firstDto = new RestrictedTenantDTO().id(1L).settings(new Settings());
    var secondDto = new RestrictedTenantDTO().id(2L).settings(new Settings());
    when(tenantService.findRestrictedTenantDataByIds(Set.of(1L, 2L)))
        .thenReturn(List.of(firstTenant, secondTenant));
    when(translationService.getCurrentLanguageContext()).thenReturn(DE);
    when(converter.toRestrictedTenantDTO(firstTenant, DE)).thenReturn(firstDto);
    when(converter.toRestrictedTenantDTO(secondTenant, DE)).thenReturn(secondDto);
    when(tenantAdminControlsService.getControls()).thenReturn(new TenantAdminControls());

    var result = tenantServiceFacade.findRestrictedTenantsByIds(Set.of(1L, 2L));

    assertThat(result).containsExactly(firstDto, secondDto);
    verify(tenantAdminControlsService, times(1)).getControls();
  }

  @Test
  void getSingleTenant_Should_shouldThrowIllegalStateException_When_moreTenantsArePresent() {
    // given
    TenantEntity secondTenantEntity = new TenantEntity();
    secondTenantEntity.setId(2L);
    when(tenantService.getAllTenantData()).thenReturn(List.of(tenantEntity, secondTenantEntity));

    // then
    assertThrows(
        IllegalStateException.class,
        () -> {
          // when
          tenantServiceFacade.getSingleTenant();
        });

    verify(tenantService).getAllTenantData();
    verifyNoInteractions(converter);
  }

  @Test
  void
      findTenantBySubdomain_Should_overridePrivacyDataFromDifferentTenant_When_TenantIdProvidedInRequest() {
    // given

    ReflectionTestUtils.setField(tenantServiceFacade, "multitenancyWithSingleDomain", true);
    ReflectionTestUtils.setField(
        tenantServiceFacade,
        "tenantConverter",
        new TenantConverter(
            new TemplateService(),
            templateRenderer,
            new com.vi.tenantservice.api.service.SmtpPasswordEncryptionService("")));

    Optional<TenantRestrictedData> defaultTenant = getTenantWithPrivacy("{\"de\":\"content1\"}");
    Optional<TenantRestrictedData> accessTokenTenantData =
        getTenantWithPrivacy("{\"de\":\"content2\"}");

    when(tenantService.findRestrictedTenantDataBySubdomain(SINGLE_DOMAIN_SUBDOMAIN_NAME))
        .thenReturn(defaultTenant);
    when(tenantResolverService.tryResolveForNonAuthUsers()).thenReturn(Optional.of(2L));
    when(tenantService.findRestrictedTenantDataById(2L)).thenReturn(accessTokenTenantData);

    RestrictedTenantDTO overriddenDTO =
        new RestrictedTenantDTO().content(new Content().privacy("content2"));
    when(singleDomainTenantOverrideService.overridePrivacyAndCertainSettings(
            defaultTenant.get(), accessTokenTenantData.get()))
        .thenReturn(overriddenDTO);
    // when
    Optional<RestrictedTenantDTO> tenantDTO =
        tenantServiceFacade.findTenantBySubdomain(SINGLE_DOMAIN_SUBDOMAIN_NAME, null);

    // then
    assertThat(tenantDTO.get().getContent().getPrivacy()).contains("content2");
  }

  @Test
  void findTenantBySubdomain_Should_ReturnEmpty_When_MainTenantIsNotFoundInSingleDomainMode() {
    // given
    ReflectionTestUtils.setField(tenantServiceFacade, "multitenancyWithSingleDomain", true);

    when(tenantService.findRestrictedTenantDataBySubdomain(SINGLE_DOMAIN_SUBDOMAIN_NAME))
        .thenReturn(Optional.empty());
    when(tenantResolverService.tryResolveForNonAuthUsers()).thenReturn(Optional.of(2L));

    // when
    Optional<RestrictedTenantDTO> tenantDTO =
        tenantServiceFacade.findTenantBySubdomain(SINGLE_DOMAIN_SUBDOMAIN_NAME, null);

    // then
    assertThat(tenantDTO).isEmpty();
    verify(tenantService, never()).findRestrictedTenantDataById(2L);
    verifyNoInteractions(singleDomainTenantOverrideService);
  }

  @Test
  void canAccessTenant_Should_useTenantIdLookupInsteadOfLoadingTenantEntity() {
    // given
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("localhost"));
    when(tenantService.findTenantIdBySubdomain("localhost")).thenReturn(Optional.of(3L));
    when(tenantFacadeAuthorisationService.canAccessTenantById(Optional.of(3L))).thenReturn(true);

    // when
    boolean canAccessTenant = tenantServiceFacade.canAccessTenant();

    // then
    assertThat(canAccessTenant).isTrue();
    verify(tenantService).findTenantIdBySubdomain("localhost");
    verify(tenantService, never()).findTenantBySubdomain("localhost");
  }

  private static Optional<TenantRestrictedData> getTenantWithPrivacy(String contentPrivacy) {
    TenantEntity defaultTenantEntity = new TenantEntity();
    defaultTenantEntity.setContentPrivacy(contentPrivacy);
    Optional<TenantRestrictedData> defaultTenant = Optional.of(defaultTenantEntity);
    return defaultTenant;
  }
}
