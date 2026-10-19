# ORISO Ecosystem Notes: ORISO-TenantService

This graph was generated for `ORISO-TenantService` only. It does not analyze sibling repositories.

## Local Role Evidence

- Purpose: Spring Boot service that owns ORISO tenant records - identity, subdomain resolution, licensing limits, theming and branding media, feature settings, multilingual legal content, tenant admin controls, tenant id allocation, data processing agreements (DPA), platform DPIA master data, and machine translation of legal texts.
- Languages: java, sql, xml, yaml, properties, python, shell, batch, dockerfile, javascript, json, markdown
- Frameworks/tools: Docker, Spring Boot (parent 4.0.1, Java 21), Spring Security 7 / OAuth2 Resource Server, Keycloak, Spring Data JPA, MariaDB, Liquibase (env-gated, off by default), OpenAPI Generator, FreeMarker, Handlebars, Ehcache, Testcontainers
- Main sources: 137 Java files under `src/main/java`, 71 under `src/test/java`
- Database: 32 Liquibase changeset directories (`0001`-`0031`, `0010` twice) under `src/main/resources/db/changelog/changeset/`
- CI/operations: 5 GitHub workflows including an OpenAPI contract gate (`openapi-contracts.yml`) and image release (`release-image.yml`), plus Python CI/contract tooling in `scripts/` and `tests/`

## Integration Clues

- `api/tenantservice.yaml` - own REST contract (server generation)
- `services/consultingtypeservice.yaml` - generated client, wrapped by `ConsultingTypeService`
- `services/applicationsettingsservice.yml` - generated client, wrapped by `ApplicationSettingsService`
- `services/useradminservice.yaml` - generated client, wrapped by `UserAdminService`
- `services/agencyadminservice.yaml` - spec present, no client generation configured in `pom.xml`
- `src/main/java/com/vi/tenantservice/api/controller/TenantController.java` - tenant and tenantadmin endpoints with `@PreAuthorize` rules
- `src/main/java/com/vi/tenantservice/api/controller/TenantMediaController.java` - media streaming endpoint
- `src/main/java/com/vi/tenantservice/api/facade/TenantDpaFacade.java` - DPA publish/sign/status endpoints, including public token-based signing links
- `src/main/java/com/vi/tenantservice/api/facade/PlatformDpiaMasterDataFacade.java` - DPIA master data admin endpoint and public read (`/tenant/public/dpia`)
- `src/main/java/com/vi/tenantservice/api/facade/TranslationFacade.java` - LLM translation via OpenRouter/Mistral provider clients
- `src/main/java/com/vi/tenantservice/api/service/httpheader/SecurityHeaderSupplier.java` - auth header propagation to downstream services
- `src/main/java/com/vi/tenantservice/api/tenant/TenantResolverService.java` - tenant context from access token, cookie, or subdomain
- `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java` - `/tenant/public/**` permitAll, `/tenant/**` and `/tenantadmin/**` authenticated

## Platform Relationships

- Owns tenant identity, subdomains, settings, theming (including accent/signal colors and login effect), legal content, licensing limits, tenant admin controls, and tenant-id reservations.
- Owns the tenant-facing legal machinery: versioned DPAs with signature collection and audit fields, public DPA confirm/forward token links, platform DPIA master data with a public read endpoint, and legal-text templating with typed data-protection placeholders.
- Stores tenant media and serves public branding assets without authentication.
- Encrypts tenant SMTP passwords at rest (AES-256-GCM) before they reach the database.
- Calls ConsultingTypeService, ApplicationSettingsService, and UserAdminService through generated contracts and wrapper services.
- Provides restricted public tenant metadata (`/tenant/public/**`) before full authenticated tenant context is available.
