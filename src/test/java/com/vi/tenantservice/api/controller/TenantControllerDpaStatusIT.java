package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.DpaSignatureStatus;
import com.vi.tenantservice.api.model.TenantDpaSignatureEntity;
import com.vi.tenantservice.api.model.TenantDpaVersionEntity;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaAdminSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaSignatureRepository;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end tests for the authenticated DPA status and audit-proof in-app signing (TEN-INV-U9,
 * ORISO-TenantService#144): one test per DPA state (MISSING, UNSIGNED, OUTDATED, VALID,
 * INCONSISTENT), the auth rules (a foreign tenant admin can neither read nor sign another tenant's
 * DPA), idempotent signing, and the append-only audit persistence.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantControllerDpaStatusIT {

  private static final String DPA_STATUS = "/tenantadmin/%d/dpa/status";
  private static final String DPA_SIGN = "/tenantadmin/%d/dpa/sign";
  private static final String DPA_VERSIONS = "/tenantadmin/%d/dpa/versions";
  private static final String DPA_GATE = "/tenantadmin/%d/dpa/gate";
  private static final String DPA_INVITE = "/tenantadmin/%d/dpa/invite";
  private static final String DPA_PUBLIC = "/tenant/public/dpa/confirm/%s";

  private static final String TENANTADMIN_RESOURCE = "/tenantadmin";

  private static final long OWN_TENANT = 1L;
  private static final long FOREIGN_TENANT = 2L;

  /** Governing operator DPA holder ({@code app.dpa.operator-tenant-id}, #569). */
  private static final long OPERATOR_TENANT = 1L;

  /** A tenant that is governed by the operator DPA instead of authoring one of its own. */
  private static final long GOVERNED_TENANT = FOREIGN_TENANT;

  private static final long ONBOARDED_TENANT = 42L;
  private static final LocalDateTime VERSION_1 = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
  private static final LocalDateTime VERSION_2 = LocalDateTime.of(2026, 7, 1, 12, 0, 0);

  private static final String SIGN_BODY =
      """
      {
        "signerName": "Toni Tenantadmin",
        "signerPosition": "Geschäftsführung",
        "signerEmail": "toni@example.org",
        "signerOrganisation": "Träger Nord e.V.",
        "language": "de",
        "accepted": true
      }
      """;

  /** Body of the public, token-based confirmation used by the forwarding path. */
  private static final String CONFIRM_BODY =
      """
      {
        "signerName": "Erika Extern",
        "signerPosition": "Vorstand",
        "signerEmail": "erika@example.org",
        "signerOrganisation": "Träger Nord e.V.",
        "signerIsMember": false,
        "forwardedByUserId": "admin-user-2",
        "language": "de",
        "accepted": true
      }
      """;

  @Autowired private WebApplicationContext context;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaVersionRepository versionRepository;
  @Autowired private TenantDpaSignatureRepository signatureRepository;
  @Autowired private TenantDpaAdminSignatureRepository adminSignatureRepository;
  @Autowired private TenantIdReservationRepository reservationRepository;

  @MockitoBean AuthorisationService authorisationService;
  @MockitoBean ApplicationSettingsService applicationSettingsService;
  @MockitoBean ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;
  @MockitoBean ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;
  @MockitoBean SecurityHeaderSupplier securityHeaderSupplier;
  @MockitoBean TenantResolverService tenantResolverService;
  @MockitoBean ConsultingTypeService consultingTypeService;
  @MockitoBean UserAdminService userAdminService;
  @MockitoBean SubdomainExtractor subdomainExtractor;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    cleanUp();
    seedTenant(OWN_TENANT, null);
    seedTenant(FOREIGN_TENANT, null);
  }

  @AfterEach
  void cleanUp() {
    // the production repository is append-only by design; tests reset the table directly
    jdbcTemplate.update("DELETE FROM tenant_dpa_admin_signature");
    signatureRepository.deleteAll();
    versionRepository.deleteAll();
    tenantRepository.deleteAll();
    // creating a tenant marks its ID ASSIGNED in the allocation ledger — reset it too, otherwise
    // the next test's creation of the same ID answers 409 instead of exercising the DPA path
    reservationRepository.deleteAll();
  }

  private void seedTenant(long id, LocalDateTime dpaActivationDate) {
    var now = LocalDateTime.now();
    tenantRepository.save(
        TenantEntity.builder()
            .id(id)
            .name("tenant-" + id)
            .subdomain("subdomain-" + id)
            .contentDataProcessingAgreementActivationDate(dpaActivationDate)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private void publishDpaVersion(long tenantId, LocalDateTime version) {
    seedTenant(tenantId, version);
    versionRepository.save(
        TenantDpaVersionEntity.builder()
            .tenantId(tenantId)
            .content("{\"de\":\"<p>AVV v" + version + "</p>\"}")
            .activationDate(version)
            .createDate(version)
            .build());
  }

  private void givenLegacySignedSignature(long tenantId, LocalDateTime version) {
    signatureRepository.save(
        TenantDpaSignatureEntity.builder()
            .tenantId(tenantId)
            .dpaVersion(version)
            .signerName("Erika Extern")
            .status(DpaSignatureStatus.SIGNED)
            .signedAt(LocalDateTime.now())
            .createDate(LocalDateTime.now())
            .build());
  }

  private void givenAuthoritiesOfRole(UserRole userRole) {
    when(authorisationService.hasAuthority(Mockito.any()))
        .thenAnswer(
            invocation ->
                Authority.getAuthoritiesByUserRole(userRole).contains(invocation.getArgument(0)));
  }

  /** A tenant admin of the tenant carried in the access token. */
  private RequestPostProcessor tenantAdminOf(long tokenTenantId) {
    givenAuthoritiesOfRole(SINGLE_TENANT_ADMIN);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(tokenTenantId));
    when(authorisationService.getUserId()).thenReturn("admin-user-" + tokenTenantId);
    when(authorisationService.getUsername()).thenReturn("tenantadmin-" + tokenTenantId);
    return authentication(
        new AuthenticationMockBuilder().withUserRole(SINGLE_TENANT_ADMIN.getValue()).build());
  }

  private RequestPostProcessor platformAdmin() {
    givenAuthoritiesOfRole(TENANT_ADMIN);
    when(authorisationService.getUserId()).thenReturn("platform-admin-user");
    when(authorisationService.getUsername()).thenReturn("platformadmin");
    return authentication(
        new AuthenticationMockBuilder().withUserRole(TENANT_ADMIN.getValue()).build());
  }

  // --- status scenarios -------------------------------------------------------------------

  @Test
  void getStatus_Should_return404_When_tenantDoesNotExist() throws Exception {
    mockMvc
        .perform(get(DPA_STATUS.formatted(999L)).with(platformAdmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getStatus_Should_returnMissing_When_noDpaWasEverPublished() throws Exception {
    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("tenantId", is((int) OWN_TENANT)))
        .andExpect(jsonPath("status", is("MISSING")))
        .andExpect(jsonPath("currentDpaVersion").doesNotExist())
        .andExpect(jsonPath("signedDpaVersion").doesNotExist());
  }

  @Test
  void getStatus_Should_returnUnsigned_When_dpaIsPublishedButNeverSigned() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UNSIGNED")))
        .andExpect(jsonPath("currentDpaVersion", is(VERSION_2.toString())));
  }

  @Test
  void getStatus_Should_returnOutdated_When_onlyAnOlderVersionWasSigned() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_1);
    givenLegacySignedSignature(OWN_TENANT, VERSION_1);
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("OUTDATED")))
        .andExpect(jsonPath("currentDpaVersion", is(VERSION_2.toString())))
        .andExpect(jsonPath("signedDpaVersion", is(VERSION_1.toString())))
        .andExpect(jsonPath("signedBy", is("Erika Extern")));
  }

  @Test
  void getStatus_Should_returnValid_When_theCurrentVersionWasSigned() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);
    givenLegacySignedSignature(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")))
        .andExpect(jsonPath("signedDpaVersion", is(VERSION_2.toString())));
  }

  @Test
  void getStatus_Should_returnInconsistent_When_aSignatureIsNewerThanTheCurrentVersion()
      throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_1);
    givenLegacySignedSignature(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("INCONSISTENT")));
  }

  @Test
  void getStatus_Should_returnInconsistent_When_aSignedRowCarriesNoVersion() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);
    givenLegacySignedSignature(OWN_TENANT, null);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("INCONSISTENT")));
  }

  // --- signing ----------------------------------------------------------------------------

  @Test
  void signDpa_Should_persistAuditProofSignatureAndTurnStatusValid() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(OWN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")))
        .andExpect(jsonPath("currentDpaVersion", is(VERSION_2.toString())))
        .andExpect(jsonPath("signedDpaVersion", is(VERSION_2.toString())))
        .andExpect(jsonPath("signedBy", is("Toni Tenantadmin")));

    var rows = adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(OWN_TENANT);
    assertThat(rows).hasSize(1);
    var row = rows.get(0);
    assertThat(row.getTenantId()).isEqualTo(OWN_TENANT);
    assertThat(row.getDpaVersion()).isEqualTo(VERSION_2);
    assertThat(row.getSignerUserId()).isEqualTo("admin-user-" + OWN_TENANT);
    assertThat(row.getSignerUsername()).isEqualTo("tenantadmin-" + OWN_TENANT);
    assertThat(row.getSignerName()).isEqualTo("Toni Tenantadmin");
    assertThat(row.getSignerPosition()).isEqualTo("Geschäftsführung");
    assertThat(row.getSignerEmail()).isEqualTo("toni@example.org");
    assertThat(row.getSignerOrganisation()).isEqualTo("Träger Nord e.V.");
    assertThat(row.getLanguage()).isEqualTo("de");
    assertThat(row.getFormData())
        .contains("\"signerName\":\"Toni Tenantadmin\"")
        .contains("\"accepted\":true");
    assertThat(row.getSignedAt()).isNotNull();
    assertThat(row.getCreateDate()).isNotNull();

    // the status endpoint reflects the signature as the authoritative state
    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(OWN_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")));
  }

  @Test
  void signDpa_Should_beIdempotent_When_theTenantIsAlreadyValid() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(OWN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")));

    // second signing attempt: sensible response, no duplicate audit row
    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(OWN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")));

    assertThat(adminSignatureRepository.countByTenantId(OWN_TENANT)).isEqualTo(1);
  }

  @Test
  void signDpa_Should_signTheCurrentVersion_When_statusIsOutdated() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_1);
    givenLegacySignedSignature(OWN_TENANT, VERSION_1);
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(OWN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")))
        .andExpect(jsonPath("signedDpaVersion", is(VERSION_2.toString())));

    var rows = adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(OWN_TENANT);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getDpaVersion()).isEqualTo(VERSION_2);
  }

  @Test
  void signDpa_Should_returnConflict_When_noDpaIsPublished() throws Exception {
    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(OWN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isConflict());

    assertThat(adminSignatureRepository.countByTenantId(OWN_TENANT)).isZero();
  }

  @Test
  void signDpa_Should_returnBadRequest_When_theAgreementWasNotAccepted() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(OWN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY.replace("\"accepted\": true", "\"accepted\": false")))
        .andExpect(status().isBadRequest());

    assertThat(adminSignatureRepository.countByTenantId(OWN_TENANT)).isZero();
  }

  // --- auth rules -------------------------------------------------------------------------

  @Test
  void getStatus_Should_returnForbidden_When_aForeignTenantAdminReadsAnotherTenantsStatus()
      throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(tenantAdminOf(FOREIGN_TENANT)))
        .andExpect(status().isForbidden());
  }

  @Test
  void signDpa_Should_returnForbidden_When_aForeignTenantAdminSignsAnotherTenantsDpa()
      throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(DPA_SIGN.formatted(OWN_TENANT))
                .with(tenantAdminOf(FOREIGN_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isForbidden());

    assertThat(adminSignatureRepository.countByTenantId(OWN_TENANT)).isZero();
  }

  @Test
  void getStatus_Should_returnOk_When_aPlatformAdminReadsAnyTenantsStatus() throws Exception {
    publishDpaVersion(OWN_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(OWN_TENANT)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UNSIGNED")))
        .andExpect(jsonPath("signedBy", is(nullValue())));
  }

  // --- public onboarding continuity (#569) -------------------------------------------------

  /**
   * The whole point of #569: a tenant created through the public onboarding carries the invitee's
   * DPA acceptance, so its very first status read is VALID and the U10 blocker lets the freshly
   * onboarded admin in — no "no DPA has been published for your organisation" dead end.
   */
  @Test
  void createTenant_Should_leaveTheOnboardedTenantWithAValidDpa() throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(ONBOARDED_TENANT)
                        .withName("Traeger Nord")
                        .withSubdomain("traeger-nord")
                        .withLicensing()
                        .withOnboardingDpaAcceptance(
                            "kc-onboarded-admin", "Toni Tenantadmin", VERSION_2.toString())
                        .jsonify()))
        .andExpect(status().isOk());

    mockMvc
        .perform(get(DPA_STATUS.formatted(ONBOARDED_TENANT)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")))
        .andExpect(jsonPath("currentDpaVersion", is(VERSION_2.toString())))
        .andExpect(jsonPath("signedDpaVersion", is(VERSION_2.toString())))
        .andExpect(jsonPath("signedBy", is("Toni Tenantadmin")));

    assertThat(adminSignatureRepository.countByTenantId(ONBOARDED_TENANT)).isEqualTo(1L);
    var signature =
        adminSignatureRepository.findByTenantIdOrderBySignedAtDescIdDesc(ONBOARDED_TENANT).get(0);
    assertThat(signature.getSignerUserId()).isEqualTo("kc-onboarded-admin");
    assertThat(signature.getDpaVersion()).isEqualTo(VERSION_2);
    assertThat(signature.getFormData()).contains("PUBLIC_TENANT_ADMIN_ONBOARDING");
  }

  /**
   * The counter-case that must keep working: a tenant a platform admin seeds directly signs
   * nothing, so the gate still blocks it — now with the actionable UNSIGNED state instead of the
   * unsignable MISSING dead end, because the governing operator DPA applies to every tenant.
   */
  @Test
  void createTenant_Should_leaveASeededTenantBlocked_When_noAcceptanceIsSubmitted()
      throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(ONBOARDED_TENANT)
                        .withName("Seeded Tenant")
                        .withSubdomain("seeded-tenant")
                        .withLicensing()
                        .jsonify()))
        .andExpect(status().isOk());

    mockMvc
        .perform(get(DPA_STATUS.formatted(ONBOARDED_TENANT)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UNSIGNED")));

    assertThat(adminSignatureRepository.countByTenantId(ONBOARDED_TENANT)).isZero();
  }

  // --- the governing operator DPA is readable and signable (#569 defect 1) -----------------

  /**
   * The dead end this fix removes: a tenant WITHOUT its own DPA is measured against the governing
   * operator document (status UNSIGNED), so the very same document must be readable through the
   * in-app signing path — otherwise the U10 blocker renders with nothing to sign.
   */
  @Test
  void getVersions_Should_serveTheGoverningOperatorDpa_When_theTenantHasNoOwnDpa()
      throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_VERSIONS.formatted(GOVERNED_TENANT)).with(tenantAdminOf(GOVERNED_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].activationDate", is(VERSION_2.toString())))
        .andExpect(jsonPath("$[0].content", containsString("AVV v" + VERSION_2)));
  }

  /** The fallback is additive: a tenant that authored its own DPA keeps pure tenant semantics. */
  @Test
  void getVersions_Should_serveOnlyTheOwnDpa_When_theTenantPublishedOne() throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_1);
    publishDpaVersion(GOVERNED_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_VERSIONS.formatted(GOVERNED_TENANT)).with(tenantAdminOf(GOVERNED_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].activationDate", is(VERSION_2.toString())));
  }

  /**
   * Read the governing document, sign it, and the gate is resolved — the complete in-app loop for a
   * tenant that has no DPA of its own.
   */
  @Test
  void signDpa_Should_resolveTheGate_When_theTenantSignsTheGoverningOperatorDpa() throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_2);

    mockMvc
        .perform(get(DPA_STATUS.formatted(GOVERNED_TENANT)).with(tenantAdminOf(GOVERNED_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UNSIGNED")));

    mockMvc
        .perform(
            post(DPA_SIGN.formatted(GOVERNED_TENANT))
                .with(tenantAdminOf(GOVERNED_TENANT))
                .contentType(APPLICATION_JSON)
                .content(SIGN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")))
        .andExpect(jsonPath("signedDpaVersion", is(VERSION_2.toString())));

    // ... and the consultation gate agrees, so agency work is not blocked afterwards
    mockMvc
        .perform(get(DPA_GATE.formatted(GOVERNED_TENANT)).with(tenantAdminOf(GOVERNED_TENANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("dpaPublished", is(true)))
        .andExpect(jsonPath("dpaSigned", is(true)));
  }

  /**
   * The forwarding path (a natural person authorised to sign for the organisation who need not be
   * an app user) must reach the same governing document — otherwise it stays a dead end for exactly
   * the tenants the in-app path was just fixed for.
   */
  @Test
  void createSignInvite_Should_forwardTheGoverningOperatorDpa_When_theTenantHasNoOwnDpa()
      throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_2);

    var inviteResponse =
        mockMvc
            .perform(
                post(DPA_INVITE.formatted(GOVERNED_TENANT)).with(tenantAdminOf(GOVERNED_TENANT)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(inviteResponse, "$.token");

    mockMvc
        .perform(get(DPA_PUBLIC.formatted(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("tenantName", is("tenant-" + GOVERNED_TENANT)))
        .andExpect(jsonPath("dpaVersion", is(VERSION_2.toString())))
        .andExpect(jsonPath("content", containsString("AVV v" + VERSION_2)));

    mockMvc
        .perform(
            post(DPA_PUBLIC.formatted(token)).contentType(APPLICATION_JSON).content(CONFIRM_BODY))
        .andExpect(status().isOk());

    // the forwarded signature counts for the gate exactly like an admin signature
    mockMvc
        .perform(get(DPA_STATUS.formatted(GOVERNED_TENANT)).with(platformAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("VALID")))
        .andExpect(jsonPath("signedBy", is("Erika Extern")));
  }

  @Test
  void createSignInvite_Should_returnConflict_When_noGoverningDpaIsPublishedAnywhere()
      throws Exception {
    mockMvc
        .perform(post(DPA_INVITE.formatted(GOVERNED_TENANT)).with(tenantAdminOf(GOVERNED_TENANT)))
        .andExpect(status().isConflict());
  }

  /** A tenant creation whose acceptance names an unpublished version must not survive. */
  @Test
  void createTenant_Should_notCreateTheTenant_When_theAcceptanceNamesAnUnknownDpaVersion()
      throws Exception {
    publishDpaVersion(OPERATOR_TENANT, VERSION_2);

    mockMvc
        .perform(
            post(TENANTADMIN_RESOURCE)
                .with(platformAdmin())
                .contentType(APPLICATION_JSON)
                .content(
                    new MultilingualTenantTestDataBuilder()
                        .withId(ONBOARDED_TENANT)
                        .withName("Traeger Nord")
                        .withSubdomain("traeger-nord")
                        .withLicensing()
                        .withOnboardingDpaAcceptance(
                            "kc-onboarded-admin", "Toni Tenantadmin", VERSION_1.toString())
                        .jsonify()))
        .andExpect(status().is4xxClientError());

    assertThat(tenantRepository.findById(ONBOARDED_TENANT)).isEmpty();
    assertThat(adminSignatureRepository.countByTenantId(ONBOARDED_TENANT)).isZero();
  }
}
