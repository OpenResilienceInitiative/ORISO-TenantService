package com.vi.tenantservice.api.controller;

import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.vi.tenantservice.useradminservice.generated.web.model.TenantDeletionResultDTO;
import java.time.LocalDateTime;
import java.util.List;
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
 * ORISO-Admin#1525: deleting a Träger marks it and empties it, it does not delete the row. On
 * staging the old {@code DELETE /tenant/{id}} removed the row at once and left agencies,
 * counsellors and advice seekers pointing at a Träger that no longer existed ("Unknown Tenant").
 * End-to-end through the real controller, security, facade and database.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class TenantDeletionIT {

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
  void deleteTenant_Should_markAndSwitchOffTheTraeger_AndKeepItsRow() throws Exception {
    when(userAdminService.markTenantContentsForDeletion(TENANT))
        .thenReturn(cascadeResult().consultants(5L).agencies(3L));

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.complete").value(true))
        .andExpect(jsonPath("$.consultants").value(5))
        .andExpect(jsonPath("$.agencies").value(3));

    var tenant = tenantRepository.findById(TENANT).orElseThrow();
    assertThat(tenant.getDeleteDate()).isNotNull();
    assertThat(tenant.getActive()).isFalse();
  }

  @Test
  void deleteTenant_Should_keepTheFirstDate_When_theTraegerIsDeletedAgain() throws Exception {
    when(userAdminService.markTenantContentsForDeletion(TENANT)).thenReturn(cascadeResult());
    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isAccepted());
    var firstDate = tenantRepository.findById(TENANT).orElseThrow().getDeleteDate();

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isAccepted());

    assertThat(tenantRepository.findById(TENANT).orElseThrow().getDeleteDate())
        .isEqualTo(firstDate);
    Mockito.verify(userAdminService, Mockito.times(2)).markTenantContentsForDeletion(TENANT);
  }

  @Test
  void deleteTenant_Should_keepTheMarkAndAnswerServiceUnavailable_When_UserServiceIsDown()
      throws Exception {
    when(userAdminService.markTenantContentsForDeletion(TENANT))
        .thenThrow(new ResourceAccessException("UserService unreachable"));

    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(platformAdmin()))
        .andExpect(status().isServiceUnavailable());

    assertThat(tenantRepository.findById(TENANT).orElseThrow().getDeleteDate()).isNotNull();
  }

  @Test
  void deleteTenant_Should_beForbidden_When_aTraegerAdminDeletesTheirOwnTraeger() throws Exception {
    mockMvc
        .perform(delete(TENANT_RESOURCE_SLASH + TENANT).with(traegerAdmin()))
        .andExpect(status().isForbidden());

    assertThat(tenantRepository.findById(TENANT).orElseThrow().getDeleteDate()).isNull();
    Mockito.verifyNoInteractions(userAdminService);
  }

  private static TenantDeletionResultDTO cascadeResult() {
    return new TenantDeletionResultDTO()
        .complete(true)
        .invites(0L)
        .consultants(0L)
        .adviceSeekers(0L)
        .agencyAdmins(0L)
        .tenantAdmins(0L)
        .agencies(0L)
        .failures(List.of());
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

  private RequestPostProcessor traegerAdmin() {
    givenAuthoritiesOfRole(TENANT_ADMIN);
    when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(true);
    when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(TENANT));
    when(authorisationService.getUserId()).thenReturn("traeger-admin-user");
    when(authorisationService.getUsername()).thenReturn("traegeradmin");
    return authentication(
        new AuthenticationMockBuilder().withUserRole(TENANT_ADMIN.getValue()).build());
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
