package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantDpaVersionRepository;
import com.vi.tenantservice.api.repository.TenantIdReservationRepository;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import com.vi.tenantservice.api.service.consultingtype.UserAdminService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.SubdomainExtractor;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.useradminservice.generated.web.model.TenantDeletionBlockersDTO;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.WebApplicationContext;

/**
 * ORISO-UserService#1466: a Träger that still has agencies, accounts or open invitations must not
 * be deleted. On staging, {@code DELETE /tenant/2} and {@code /tenant/121} answered 204 while both
 * still had agencies, counsellors and advice seekers, which were then left pointing at a Träger
 * that no longer existed. End-to-end through the real facade and exception handler, so the 409, its
 * {@code X-Reason} and the body the admin interface reads are what is asserted.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantDeleteGuardIT {

  private static final String TENANT_RESOURCE_SLASH = "/tenant/";
  private static final long TENANT = 121L;

  @Autowired private WebApplicationContext context;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private TenantDpaVersionRepository versionRepository;
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
    seedTenant(TENANT);
  }

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM tenant_dpa_admin_signature");
    versionRepository.deleteAll();
    tenantRepository.deleteAll();
    reservationRepository.deleteAll();
  }

  @Test
  void deleteTenant_Should_returnConflictWithWhatIsLeft_When_theTraegerStillHasAgenciesAndAccounts()
      throws Exception {
    when(userAdminService.getTenantDeletionBlockers(TENANT))
        .thenReturn(blockers().deletable(false).agencies(3L).consultants(5L).adviceSeekers(7L));

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isConflict())
        .andExpect(header().string("X-Reason", "TENANT_HAS_DEPENDENTS"))
        .andExpect(jsonPath("$.deletable").value(false))
        .andExpect(jsonPath("$.agencies").value(3))
        .andExpect(jsonPath("$.consultants").value(5))
        .andExpect(jsonPath("$.adviceSeekers").value(7))
        .andExpect(jsonPath("$.openInvites").value(0));

    assertThat(tenantRepository.existsById(TENANT)).isTrue();
  }

  @Test
  void deleteTenant_Should_returnConflict_When_onlyAnOpenInvitationIsLeft() throws Exception {
    when(userAdminService.getTenantDeletionBlockers(TENANT))
        .thenReturn(blockers().deletable(false).openInvites(1L));

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.openInvites").value(1));

    assertThat(tenantRepository.existsById(TENANT)).isTrue();
  }

  @Test
  void deleteTenant_Should_deleteTheTraeger_When_nothingIsLeft() throws Exception {
    when(userAdminService.getTenantDeletionBlockers(TENANT)).thenReturn(blockers().deletable(true));

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isNoContent());

    assertThat(tenantRepository.existsById(TENANT)).isFalse();
  }

  @Test
  void deleteTenant_Should_keepTheTraegerAndAnswerServiceUnavailable_When_UserServiceCannotBeAsked()
      throws Exception {
    when(userAdminService.getTenantDeletionBlockers(TENANT))
        .thenThrow(new ResourceAccessException("UserService unreachable"));

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isServiceUnavailable());

    assertThat(tenantRepository.existsById(TENANT)).isTrue();
  }

  private static TenantDeletionBlockersDTO blockers() {
    return new TenantDeletionBlockersDTO()
        .agencies(0L)
        .consultants(0L)
        .adviceSeekers(0L)
        .agencyAdmins(0L)
        .tenantAdmins(0L)
        .openInvites(0L);
  }

  private void seedTenant(long id) {
    var now = LocalDateTime.now();
    tenantRepository.save(
        TenantEntity.builder()
            .id(id)
            .name("tenant-" + id)
            .subdomain("subdomain-" + id)
            .createDate(now)
            .updateDate(now)
            .build());
  }

  private void givenAuthoritiesOfRole(UserRole userRole) {
    when(authorisationService.hasAuthority(Mockito.any()))
        .thenAnswer(
            invocation ->
                Authority.getAuthoritiesByUserRole(userRole).contains(invocation.getArgument(0)));
  }

  private RequestPostProcessor platformAdmin() {
    givenAuthoritiesOfRole(TENANT_ADMIN);
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(0L));
    when(authorisationService.getUserId()).thenReturn("platform-admin-user");
    when(authorisationService.getUsername()).thenReturn("platformadmin");
    return authentication(
        new AuthenticationMockBuilder().withUserRole(TENANT_ADMIN.getValue()).build());
  }
}
