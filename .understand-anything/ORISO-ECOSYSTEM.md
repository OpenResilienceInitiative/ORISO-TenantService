# ORISO Ecosystem Connections

## Navigation

- [Service role](#service-role)
- [Inbound dependencies](#inbound-dependencies)
- [Outbound dependencies](#outbound-dependencies)
- [Shared identity model](#shared-identity-model)
- [Operational relationships](#operational-relationships)

## Service Role

TenantService is the tenant registry for the ORISO ecosystem. It answers which tenants exist, how they are branded, which features are enabled, which legal content is active, and how a request maps to tenant context.

## Inbound Dependencies

Likely consumers are frontend/admin surfaces and other backend services that need tenant metadata. The repository itself exposes these inbound surfaces through `api/tenantservice.yaml`, especially:

- `/tenantadmin` for platform tenant administration.
- `/tenantadmin/search` for admin search and pagination.
- `/tenant` and `/tenant/{id}` for authenticated tenant reads.
- `/tenant/public/**` for unauthenticated restricted tenant metadata.
- `/tenant/access` for tenant access checks.

## Outbound Dependencies

TenantService calls three ORISO service families through generated OpenAPI clients:

- ConsultingTypeService: default consulting type settings are created during tenant creation and patched on selected tenant setting changes.
- ApplicationSettingsService: single-domain deployments read central settings and can persist the main tenant subdomain.
- UserAdminService: tenant admin search enriches tenant rows with admin email data.

`services/agencyadminservice.yaml` is referenced by schemas inside `services/useradminservice.yaml`, but this repo does not show a direct generated AgencyAdmin client execution in `pom.xml`.

## Shared Identity Model

The service expects Keycloak JWTs. Realm roles are mapped into local authorities:

- `tenant-admin` maps to broad tenant-management permissions.
- `single-tenant-admin` can update the current tenant with narrower field permissions.
- `restricted-agency-admin` and `restricted-consultant-admin` can read tenant data.

The JWT tenant claim is central. `AuthorisationService.findTenantIdInAccessToken` accepts numeric and string claim values and throws when the claim is missing or invalid.

## Operational Relationships

- MariaDB stores tenant records and JSON settings/content.
- Liquibase changelogs describe tenant table history, but runtime profiles disable Liquibase.
- Docker and deployment scripts package `TenantService.jar` and roll it into a Kubernetes development deployment.
- Runtime configuration should use service DNS names through environment variables, not hardcoded cluster IPs.
