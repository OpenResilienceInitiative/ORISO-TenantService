package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vi.tenantservice.api.authorisation.Authority.AuthorityValue;
import com.vi.tenantservice.api.converter.ConsultingTypePatchDTOConverter;
import com.vi.tenantservice.api.converter.TenantConverter;
import com.vi.tenantservice.api.exception.TenantNotFoundException;
import com.vi.tenantservice.api.exception.TenantValidationException;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.model.RestrictedTenantDTO;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.TenantDTO;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.service.SingleDomainTenantOverrideService;
import com.vi.tenantservice.api.service.TemplateRenderer;
import com.vi.tenantservice.api.service.TemplateService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

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

  @Mock
  private TenantFacadeDependentSettingsOverrideService tenantFacadeDependentSettingsOverrideService;

  @Mock private SingleDomainTenantOverrideService singleDomainTenantOverrideService;

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
    when(tenantService.create(tenantEntity)).thenReturn(tenantEntity);

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    verify(converter).toEntity(sanitizedTenantDTO);
    verify(tenantService).create(tenantEntity);
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
    when(tenantService.create(entity)).thenReturn(entity);
    ReflectionTestUtils.setField(tenantServiceFacade, "multitenancyWithSingleDomain", true);
    when(tenantService.getAllTenants()).thenReturn(List.of(technicalTenant));
    when(subdomainExtractor.getCurrentSubdomain()).thenReturn(Optional.of("app1"));

    // when
    tenantServiceFacade.createTenant(tenantMultilingualDTO);

    // then
    verify(converter).toEntity(sanitizedTenantDTO);
    verify(tenantService).create(entity);
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
    when(tenantService.create(entity)).thenReturn(entity);
    ReflectionTestUtils.setField(tenantServiceFacade, "multitenancyWithSingleDomain", true);
    when(tenantService.getAllTenants()).thenReturn(List.of(technicalTenant));
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
  void createTenant_Should_throwBadRequest_When_tenantIdIsProvided() {
    // given
    MultilingualTenantDTO tenantDTOWithId = mock(MultilingualTenantDTO.class);
    when(tenantDTOWithId.getId()).thenReturn(1L);
    when(tenantInputSanitizer.sanitize(tenantDTOWithId)).thenReturn(sanitizedTenantDTO);

    // then
    assertThrows(
        TenantValidationException.class,
        () -> {
          tenantServiceFacade.createTenant(tenantDTOWithId);
        });
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
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
    when(translationService.getCurrentLanguageContext()).thenReturn("de");
    when(converter.toDTO(tenantEntity, "de")).thenReturn(tenantDTO);
    // when
    Optional<TenantDTO> tenantById = tenantServiceFacade.findTenantById(ID);
    assertThat(tenantById).contains(tenantDTO);
  }

  @Test
  void findMultilingualTenantById_Should_findTenant_When_ExistingIdIsPassedForSingleTenantAdmin() {
    // given
    when(tenantService.findTenantById(ID)).thenReturn(Optional.of(tenantEntity));
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
    verify(tenantService).getAllTenants();
  }

  @Test
  void getSingleTenant_Should_findTenant_When_onlyOneTenantIsPresent() {
    // given
    when(tenantService.getAllTenants()).thenReturn(List.of(tenantEntity));
    when(translationService.getCurrentLanguageContext()).thenReturn(DE);
    when(converter.toRestrictedTenantDTO(tenantEntity, DE)).thenReturn(restrictedTenantDTO);

    // when
    tenantServiceFacade.getSingleTenant();

    // then
    verify(tenantService).getAllTenants();
    verify(converter).toRestrictedTenantDTO(tenantEntity, DE);
  }

  @Test
  void getSingleTenant_Should_shouldThrowIllegalStateException_When_moreTenantsArePresent() {
    // given
    TenantEntity secondTenantEntity = new TenantEntity();
    secondTenantEntity.setId(2L);
    when(tenantService.getAllTenants()).thenReturn(List.of(tenantEntity, secondTenantEntity));

    // then
    assertThrows(
        IllegalStateException.class,
        () -> {
          // when
          tenantServiceFacade.getSingleTenant();
        });

    verify(tenantService).getAllTenants();
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
        new TenantConverter(new TemplateService(), templateRenderer));

    Optional<TenantEntity> defaultTenant = getTenantWithPrivacy("{\"de\":\"content1\"}");
    Optional<TenantEntity> accessTokenTenantData = getTenantWithPrivacy("{\"de\":\"content2\"}");

    when(tenantService.findTenantBySubdomain(SINGLE_DOMAIN_SUBDOMAIN_NAME))
        .thenReturn(defaultTenant);
    when(tenantResolverService.tryResolveForNonAuthUsers()).thenReturn(Optional.of(2L));
    when(tenantService.findTenantById(2L)).thenReturn(accessTokenTenantData);

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

  private static Optional<TenantEntity> getTenantWithPrivacy(String contentPrivacy) {
    TenantEntity defaultTenantEntity = new TenantEntity();
    defaultTenantEntity.setContentPrivacy(contentPrivacy);
    Optional<TenantEntity> defaultTenant = Optional.of(defaultTenantEntity);
    return defaultTenant;
  }
}
