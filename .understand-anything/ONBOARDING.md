# Onboarding Guide: ORISO-TenantService

1. Start with `README.md`, `pom.xml` (Java 21, Spring Boot parent 4.0.1 - the README's version claims are stale), and `src/main/java/com/vi/tenantservice/TenantServiceApplication.java`.
2. Open `.understand-anything/README.md` and launch the dashboard using the command shown there.
3. Follow these graph tour steps:

   - 1. Project Overview: `README.md`, `pom.xml`, `src/main/java/com/vi/tenantservice/TenantServiceApplication.java`
   - 2. Tenant API Boundary: `api/tenantservice.yaml`, `src/main/java/com/vi/tenantservice/api/controller/TenantController.java`, `src/main/java/com/vi/tenantservice/api/controller/TenantMediaController.java`
   - 3. Tenant Lifecycle Orchestration: `src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java`, `src/main/java/com/vi/tenantservice/api/service/TenantService.java`, `src/main/java/com/vi/tenantservice/api/converter/TenantConverter.java`
   - 4. Legal, DPA And DPIA: `src/main/java/com/vi/tenantservice/api/facade/TenantDpaFacade.java`, `src/main/java/com/vi/tenantservice/api/service/TenantDpaService.java`, `src/main/java/com/vi/tenantservice/api/facade/PlatformDpiaMasterDataFacade.java`, `src/main/resources/db/changelog/changeset/0015_add_tenant_dpa/`
   - 5. Tenant Admin Controls And Id Allocation: `src/main/java/com/vi/tenantservice/api/service/TenantAdminControlsService.java`, `src/main/java/com/vi/tenantservice/api/service/TenantIdAllocationService.java`, `src/main/resources/db/changelog/changeset/0024_tenant_id_reservation/`
   - 6. Auth And Tenant Context: `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java`, `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java`, `src/main/java/com/vi/tenantservice/api/tenant/TenantResolverService.java`
   - 7. External ORISO Integrations: `services/consultingtypeservice.yaml`, `services/applicationsettingsservice.yml`, `services/useradminservice.yaml`, `src/main/java/com/vi/tenantservice/api/service/consultingtype/ConsultingTypeService.java`
   - 8. Translation: `src/main/java/com/vi/tenantservice/api/facade/TranslationFacade.java`, `src/main/java/com/vi/tenantservice/api/service/translation/`
   - 9. Deployment: `.github/workflows/ci-feature-branch.yml`, `.github/workflows/ci-main.yml`, `.github/workflows/ci-pull-request.yml`, `.github/workflows/openapi-contracts.yml`, `.github/workflows/release-image.yml`, `Dockerfile`, `pom.xml`

4. For API changes, update `api/tenantservice.yaml` before generated interfaces and controller behavior; the `openapi-contracts.yml` workflow gates contract compatibility (helpers in `scripts/contracts/`).
5. For DPA/DPIA changes, inspect the facade, services, signature/version entities, and Liquibase changesets `0015`-`0031` together; public signing runs through token links under `/tenant/public/dpa/`.
6. For schema changes, note that Liquibase is profile-gated: `spring.liquibase.enabled` defaults to `false` in the base profile, to `true` in `local`/`dev` (`seed` context) and in `staging`/`prod` (`prod` context), and is `false` unconditionally in `testing`. `SPRING_LIQUIBASE_ENABLED` and `SPRING_LIQUIBASE_CONTEXTS` override every profile default except the `testing` one.
7. For auth-sensitive changes, inspect `WebSecurityConfig` (`/tenant/public/**` is permitAll, `/tenant/**` and `/tenantadmin/**` are authenticated, and `anyRequest()` is permitAll - so a new controller outside those prefixes is public unless you add a matcher), authority mapping, and tenant resolver classes before editing controller rules.
