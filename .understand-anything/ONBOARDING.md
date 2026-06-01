# Developer Onboarding

## Navigation

- [First hour](#first-hour)
- [Local setup](#local-setup)
- [Reading order](#reading-order)
- [Common change paths](#common-change-paths)
- [Testing map](#testing-map)
- [Review checklist](#review-checklist)

## First Hour

Start with the graph dashboard, then open these files in order:

1. `pom.xml` - Maven build, dependency, OpenAPI Generator, Liquibase, compiler, and test configuration.
2. `api/tenantservice.yaml` - Primary TenantService REST contract for tenant admin, tenant read, restricted public read, and access-check endpoints.
3. `src/main/java/com/vi/tenantservice/api/controller/TenantController.java` - OpenAPI interface implementation and endpoint-level authorization boundary.
4. `src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java` - Central application-service facade for create, update, delete, search, restricted reads, and downstream coordination.
5. `src/main/java/com/vi/tenantservice/api/service/TenantService.java` - Tenant persistence service and default settings loader.
6. `src/main/java/com/vi/tenantservice/api/model/TenantEntity.java` - JPA mapping for the tenant table.
7. `src/main/java/com/vi/tenantservice/api/repository/TenantRepository.java` - JPA repository and custom search projection for non-technical tenants.
8. `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java` - Spring Security, JWT resource-server, route whitelist, and method-security setup.
9. `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java` - JWT tenant, username, realm-role, and authority extraction service.
10. `src/main/java/com/vi/tenantservice/api/service/consultingtype/ConsultingTypeService.java` - Downstream ConsultingTypeService integration for tenant default settings and patches.

The fastest way to understand behavior is to trace `TenantServiceFacade.createTenant`, `TenantServiceFacade.updateTenant`, and `TenantServiceFacade.getRestrictedTenantDataDeterminingTenantContext`.

## Local Setup

This is a Java 17 Spring Boot service built with Maven. The build uses `--enable-preview`; use a JDK compatible with that configuration.

Useful commands:

```bash
./mvnw test
./mvnw package
./mvnw spotless:check
```

Runtime startup requires database, Keycloak, JWT issuer/JWK, ConsultingTypeService URL, and UserAdminService URL values. `ConfigurationValidator` will fail the app early if required values are blank.

## Reading Order

1. Contract: `api/tenantservice.yaml`.
2. Entry point: `TenantServiceApplication`.
3. Controller: `TenantController`.
4. Main use cases: `TenantServiceFacade`.
5. Persistence: `TenantService`, `TenantRepository`, `TenantEntity`, and Liquibase changelogs.
6. Security: `WebSecurityConfig`, `JwtAuthConverter`, `AuthorisationService`, `Authority`.
7. Tenant context: `TenantResolverService` and the three resolver implementations.
8. External calls: `ConsultingTypeService`, `ApplicationSettingsService`, `UserAdminService`, and API client factories.
9. Conversion: `TenantConverter`, `TenantDtoMapper`, `JsonConverter`.
10. Tests covering the area you intend to change.

## Common Change Paths

Adding or changing an endpoint:

1. Update `api/tenantservice.yaml` first.
2. Regenerate/compile generated interfaces through Maven.
3. Implement or adjust `TenantController`.
4. Put orchestration in `TenantServiceFacade`, not the controller.
5. Add/update controller integration tests.

Changing tenant update behavior:

1. Read `TenantFacadeAuthorisationService` to understand allowed field changes.
2. Read `TenantFacadeChangeDetectionService` before changing settings or legal-content rules.
3. Update `TenantConverter` if DTO/entity JSON shape changes.
4. Update `TenantServiceFacadeTest`, `TenantFacadeAuthorisationServiceTest`, and converter tests.

Changing public tenant reads:

1. Read `TenantResolverService` and resolver implementations.
2. Read `SingleDomainTenantOverrideService` for single-domain behavior.
3. Verify `WebSecurityConfig` still permits only the intended public routes.

## Testing Map

- `TenantControllerIT` covers HTTP/security behavior.
- `TenantServiceFacadeTest` covers facade orchestration.
- `TenantFacadeAuthorisationServiceTest` covers role and field-change authorization.
- `TenantFacadeChangeDetectionServiceTest` covers settings/content diff behavior.
- `TenantServiceTest` covers persistence service rules.
- `TenantResolverServiceTest`, `AccessTokenTenantResolverTest`, `CookieTenantResolverTest`, and `SubdomainTenantResolverTest` cover tenant context.
- `TenantConverterTest` and `JsonConverterTest` cover mapping and JSON handling.

## Review Checklist

- Does the OpenAPI contract match the controller behavior?
- Does the change preserve tenant-specific authorization?
- Are single-domain and multi-domain deployments both considered?
- Are external ORISO calls still carrying Authorization and CSRF headers?
- Did you update tests for both allowed and denied behavior?
- Did you avoid adding more settings-default logic outside one shared place?
