# ORISO-TenantService Architecture

## Navigation

- [System responsibility](#system-responsibility)
- [Layer map](#layer-map)
- [API structure](#api-structure)
- [Authentication and tenant context](#authentication-and-tenant-context)
- [Data flow](#data-flow)
- [Persistence](#persistence)
- [Deployment and configuration](#deployment-and-configuration)
- [Important boundaries](#important-boundaries)

## System Responsibility

TenantService is the ORISO tenant registry. It stores tenant identity, subdomain, licensing, theming, feature settings, legal content, and content activation dates. It also answers public restricted tenant reads used before full authentication context may exist.

## Layer Map

1. API contract: `api/tenantservice.yaml` defines the TenantService HTTP surface; service specs in `services/` define generated clients.
2. Controller layer: `TenantController` adapts generated interfaces to the facade and applies `@PreAuthorize` rules.
3. Security layer: `WebSecurityConfig`, `JwtAuthConverter`, `AuthorisationService`, `Authority`, and `RoleAuthorizationAuthorityMapper` translate JWT roles into local permissions.
4. Tenant context layer: `TenantResolverService` uses `AccessTokenTenantResolver`, `CookieTenantResolver`, and `SubdomainTenantResolver`.
5. Domain facade layer: `TenantServiceFacade` orchestrates tenant use cases and downstream calls.
6. Domain service and persistence: `TenantService`, `TenantRepository`, and `TenantEntity` own MariaDB state.
7. Conversion and validation: `TenantConverter`, `TenantDtoMapper`, `JsonConverter`, `TenantInputSanitizer`, `TemplateService`, and `TemplateRenderer` shape API data.
8. External clients: `ConsultingTypeService`, `ApplicationSettingsService`, and `UserAdminService` wrap generated OpenAPI clients.
9. Runtime configuration: `application*.properties`, `ConfigurationValidator`, `Dockerfile`, and deployment scripts control startup and deployment.

## API Structure

`api/tenantservice.yaml` exposes tenant-admin endpoints under `/tenantadmin`, authenticated tenant endpoints under `/tenant`, public restricted reads under `/tenant/public/**`, and access checking at `/tenant/access`.

Key routes:

- `POST /tenantadmin` creates tenants.
- `GET /tenantadmin` lists tenants with admin data.
- `GET /tenantadmin/search` searches non-technical tenants.
- `PUT /tenantadmin/{id}` updates tenants.
- `GET /tenant/{id}` and `GET /tenant` read tenant data.
- `GET /tenant/public/{subdomain}`, `/tenant/public/id/{tenantId}`, `/tenant/public/single`, and `/tenant/public/` return restricted tenant data.
- `GET /tenant/access` checks whether the current user can access a tenant context.

`TenantController` also defines a handwritten `DELETE /tenant/{id}` endpoint that is not listed in `api/tenantservice.yaml`; document or add it to the contract before relying on generated clients.

## Authentication and Tenant Context

`WebSecurityConfig` configures stateless OAuth2 resource-server JWT authentication. Protected `/tenant` and `/tenantadmin` endpoints rely on both URL matchers and `@PreAuthorize` expressions. Public restricted tenant endpoints are explicitly `permitAll`.

`JwtAuthConverter` combines Spring JWT authorities with realm roles extracted by `AuthorisationService`. `RoleAuthorizationAuthorityMapper` maps roles such as `tenant-admin`, `single-tenant-admin`, `restricted-agency-admin`, and `restricted-consultant-admin` into local `AUTHORIZATION_*` authorities from `Authority`.

`TenantResolverService` resolves tenant context in this order:

1. `AccessTokenTenantResolver` when a request is authenticated.
2. `CookieTenantResolver` when single-domain mode is enabled and cookie hints are available.
3. `SubdomainTenantResolver` using request host/subdomain lookup.

## Data Flow

Create tenant:

1. `TenantController.createTenant` receives the generated DTO.
2. `TenantServiceFacade.createTenant` sanitizes and validates input.
3. `TenantConverter` builds `TenantEntity` and settings/content JSON.
4. `TenantService.create` persists through `TenantRepository`.
5. `ConsultingTypeService.createDefaultConsultingTypeSettings` creates default downstream settings.
6. In single-domain mode, `ApplicationSettingsService.saveMainTenantSubDomain` may persist the first non-technical tenant subdomain.

Update tenant:

1. `TenantServiceFacade.updateTenant` loads the existing tenant.
2. `TenantFacadeAuthorisationService` checks tenant access and role-specific change permissions.
3. `TenantFacadeChangeDetectionService` identifies guarded settings/content changes.
4. `TenantService.update` persists entity changes.
5. `ConsultingTypeService.patchExtendedTenantSettings` propagates supported feature changes.

Restricted read:

1. Public endpoint enters `TenantController`.
2. Facade resolves tenant by subdomain, id, single tenant, or request context.
3. `SingleDomainTenantOverrideService` applies single-domain privacy/content overrides when configured.
4. `TenantConverter` returns a restricted DTO.

## Persistence

`TenantEntity` maps the `tenant` table and `SEQUENCE_TENANT`. It stores multilingual/legal content as strings and settings as JSON. `TenantRepository.findAllExceptTechnicalByInfix` excludes tenant id `0L` from admin search.

Liquibase changelogs live under `src/main/resources/db/changelog/`. Profile master files include historical tenant schema changes, but application profiles currently set `spring.liquibase.enabled=false`, so schema ownership should be clarified operationally.

## Deployment and Configuration

`application.properties` sets the base runtime contract: MariaDB, Keycloak/OAuth2 JWT, service URLs, single-domain feature flag, Swagger path, cache config, and health probes. `ConfigurationValidator` fails startup if database, Keycloak, JWT, ConsultingType URL, or UserAdmin URL values are blank.

`Dockerfile` runs `TenantService.jar` on `eclipse-temurin:17-jre` with `--enable-preview` and JDWP enabled. The app default port is `8081`, while the Dockerfile exposes `8080`; align those before relying on container metadata.

## Important Boundaries

- `TenantServiceFacade` is the main behavior boundary. Most tenant lifecycle changes belong there or in its focused helper services.
- `TenantService` should remain persistence-focused; avoid moving authorization or external orchestration into it.
- `TenantConverter` already has significant settings/defaulting logic. Treat changes here as behavior changes, not simple mapping edits.
- Generated models and API interfaces are Maven output from OpenAPI contracts; change YAML contracts first where possible.
