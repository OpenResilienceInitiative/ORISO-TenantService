# Onboarding Guide: ORISO-TenantService

1. Start with `README.md`, `pom.xml`, and `src/main/java/com/vi/tenantservice/TenantServiceApplication.java`.
2. Open `.understand-anything/README.md` and launch the dashboard using the command shown there.
3. Follow these graph tour steps:

- 1. Project Overview: `README.md`, `pom.xml`, `src/main/java/com/vi/tenantservice/TenantServiceApplication.java`
- 2. Tenant API Boundary: `api/tenantservice.yaml`, `src/main/java/com/vi/tenantservice/api/controller/TenantController.java`
- 3. Tenant Lifecycle Orchestration: `src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java`, `src/main/java/com/vi/tenantservice/api/service/TenantService.java`, `src/main/java/com/vi/tenantservice/api/converter/TenantConverter.java`
- 4. Tenant Admin Controls: `src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsEntity.java`, `src/main/java/com/vi/tenantservice/api/repository/TenantAdminControlsRepository.java`, `src/main/java/com/vi/tenantservice/api/service/TenantAdminControlsService.java`, `src/main/resources/db/changelog/changeset/0013_tenant_admin_controls/0013-changeSet.xml`
- 5. Auth And Tenant Context: `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java`, `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java`
- 6. External ORISO Integrations: `services/consultingtypeservice.yaml`, `services/applicationsettingsservice.yml`, `services/useradminservice.yaml`, `src/main/java/com/vi/tenantservice/api/service/consultingtype/ConsultingTypeService.java`
- 7. Deployment: `.github/workflows/ci-feature-branch.yml`, `.github/workflows/ci-main.yml`, `.github/workflows/ci-pull-request.yml`, `Dockerfile`, `pom.xml`

4. For API changes, update `api/tenantservice.yaml` before generated interfaces and controller behavior.
5. For tenant admin controls, inspect the entity, repository, service, facade/converter use, and Liquibase changeset together.
6. For auth-sensitive changes, inspect `WebSecurityConfig`, authority mapping, and tenant resolver classes before editing controller rules.
