# ORISO-TenantService Architecture

## System Responsibility

Spring Boot service that owns ORISO tenant records: identity and subdomain resolution, licensing limits, theming and branding media, feature settings, multilingual legal content, tenant admin controls, tenant id allocation/reservation, data processing agreements (DPA), platform DPIA master data, and machine translation of legal texts. It integrates with the ConsultingType, ApplicationSettings, and UserAdmin services through generated OpenAPI clients.

Build facts (from `pom.xml` and `Dockerfile`): Java 21, Spring Boot parent 4.0.1, Spring Security 7.0.1, runtime image `eclipse-temurin:21-jre`, `--enable-preview` on compiler/Surefire/runtime. (`README.md` still says Java 17 / Spring Boot 3 in places; the pom is authoritative.)

## Architecture Layers

### Api And Routing

OpenAPI contract, generated server interfaces, controllers, and route boundaries.

Key files:
- `api/tenantservice.yaml` - the REST contract; generated with the `spring` generator. Endpoint groups: `/tenantadmin/**` (CRUD, search, controls, DPIA master data, translation keys/translate, tenant-id availability/reservation, DPA publish/versions/status/sign/signatures/gate/invite, media upload), `/tenant/**` (authenticated reads, access check, group-chat author translation), `/tenant/public/**` (restricted metadata by subdomain/id/ids/single, public DPIA, public branding assets, public DPA confirm/forward), `/media/{mediaId}`.
- `src/main/java/com/vi/tenantservice/api/controller/TenantController.java` - implements the generated tenant/tenantadmin interfaces and carries the `@PreAuthorize` endpoint authorization rules (~600 lines).
- `src/main/java/com/vi/tenantservice/api/controller/TenantMediaController.java` - implements the generated `MediaApi`; streams stored tenant media.
- `src/main/java/com/vi/tenantservice/api/controller/TenantDtoMapper.java` - DTO mapping between generated models and domain objects.
- `src/main/java/com/vi/tenantservice/api/controller/ExceptionHandlerAdvice.java` - maps domain exceptions to HTTP responses.
- `src/main/java/com/vi/tenantservice/api/controller/VersionController.java` - exposes build version info.
- `src/main/java/com/vi/tenantservice/api/controller/interceptor/CorrelationIdFilter.java` - request correlation-id propagation.

### Auth And Tenant Context

OAuth2/JWT security, Keycloak role mapping, local authorities, and tenant resolution.

Key files:
- `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java` - stateless JWT resource server; `/tenant/public/**` and the Swagger whitelist are `permitAll`, `/tenant/**` and `/tenantadmin/**` require authentication.
- `src/main/java/com/vi/tenantservice/config/security/JwtAuthConverter.java` and `JwtAuthConverterProperties.java` - read realm roles from the Keycloak JWT.
- `src/main/java/com/vi/tenantservice/api/authorisation/RoleAuthorizationAuthorityMapper.java`, `Authority.java`, `UserRole.java` - map ORISO roles to local `AUTHORIZATION_*` authorities.
- `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java` - accessor for the authenticated principal and its claims.
- `src/main/java/com/vi/tenantservice/api/tenant/TenantResolverService.java` - resolution order: `AccessTokenTenantResolver` (JWT claim), `CookieTenantResolver` (single-domain mode), `SubdomainTenantResolver` + `SubdomainExtractor` (request host).
- `src/main/java/com/vi/tenantservice/api/facade/TenantFacadeAuthorisationService.java` - per-operation tenant-level authorisation checks.

### Tenant Domain Services

Tenant lifecycle orchestration, admin controls, downstream synchronization, conversion, and validation.

Key files:
- `src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java` - orchestrates create, update, delete, search, restricted reads, and downstream sync.
- `src/main/java/com/vi/tenantservice/api/facade/TenantFacadeChangeDetectionService.java` and `TenantFacadeDependentSettingsOverrideService.java` - change detection and dependent-settings overrides on update.
- `src/main/java/com/vi/tenantservice/api/service/TenantService.java` - core persistence-facing tenant service (caching via Ehcache).
- `src/main/java/com/vi/tenantservice/api/service/TenantAdminControlsService.java` - tenant admin controls (allowed permission toggles).
- `src/main/java/com/vi/tenantservice/api/service/TenantIdAllocationService.java` - tenant-id availability, next-free lookup, and reservations (`/tenantadmin/tenant-ids/**`).
- `src/main/java/com/vi/tenantservice/api/service/SingleDomainTenantOverrideService.java` - single-domain (non-subdomain) deployment override.
- `src/main/java/com/vi/tenantservice/api/converter/TenantConverter.java`, `ConsultingTypePatchDTOConverter.java`, `ConverterUtils.java` - entity/DTO conversion.
- `src/main/java/com/vi/tenantservice/api/validation/TenantInputSanitizer.java` and `InputSanitizer.java` - OWASP HTML sanitization of tenant content.
- `src/main/java/com/vi/tenantservice/api/service/SmtpPasswordEncryptionService.java` - AES-256-GCM encryption of tenant SMTP passwords at rest (#183), with `SmtpPasswordEncryptionMigration.java` for legacy plaintext values; key comes from `settings.smtp.password.encryption.secret` (blank disables with a startup warning).

### Legal, DPA And DPIA

Data processing agreement (DPA/AVV) lifecycle, platform DPIA master data, and legal-text templating - the largest growth area since mid-2026.

Key files:
- `src/main/java/com/vi/tenantservice/api/facade/TenantDpaFacade.java` - DPA orchestration behind `/tenantadmin/{id}/dpa/**` and the public confirm/forward endpoints.
- `src/main/java/com/vi/tenantservice/api/service/TenantDpaService.java`, `TenantDpaStatusService.java`, `TenantDpaRetentionService.java`, `GoverningDpaResolver.java`, `DpaSignedNoticeHintService.java` - publish, status/gate evaluation, retention, and governing-version resolution.
- `src/main/java/com/vi/tenantservice/api/service/DpaSignToken.java` - public sign-token used by `/tenant/public/dpa/confirm/{token}` and `/tenant/public/dpa/forward` (invite links; invalid tokens raise `InvalidDpaSignTokenException`).
- `src/main/java/com/vi/tenantservice/api/model/TenantDpaVersionEntity.java`, `TenantDpaSignatureEntity.java`, `TenantDpaAdminSignatureEntity.java`, `DpaSignatureStatus.java`, `TenantDpaStatus.java` - versioned DPA documents and signatures with audit fields.
- `src/main/java/com/vi/tenantservice/api/facade/PlatformDpiaMasterDataFacade.java`, `.../service/PlatformDpiaMasterDataService.java`, `.../model/PlatformDpiaMasterDataEntity.java` - platform-wide DPIA master data, editable via `/tenantadmin/dpia`, publicly readable via `GET /tenant/public/dpia`.
- `src/main/java/com/vi/tenantservice/api/service/TemplateService.java`, `TemplateRenderer.java`, `ConfigurationFileLoader.java` - FreeMarker/Handlebars rendering of legal templates; placeholders typed by `.../model/DataProtectionPlaceHolderType.java`; templates in `src/main/resources/templates/`.

### Translation

Machine translation of tenant legal texts through pluggable LLM providers.

Key files:
- `src/main/java/com/vi/tenantservice/api/facade/TranslationFacade.java` - backs `/tenantadmin/translation/keys`, `/tenantadmin/translate`, and `/tenant/translate/group-chat-author-content`.
- `src/main/java/com/vi/tenantservice/api/service/translation/TranslationProviderClient.java`, `OpenRouterClient.java`, `MistralClient.java`, `OpenAiCompatibleChatClient.java`, `ApiKeyMasker.java` - provider abstraction (OpenRouter and Mistral; API keys are stored per platform, masked on read).
- `src/main/java/com/vi/tenantservice/api/service/TranslationService.java` - resolves the current language context (default `de`, `lang` cookie).
- Defaults in `application.properties`: `translation.openrouter.*`, `translation.mistral.*`.

### Media And Branding

Tenant media upload and public branding assets.

Key files:
- `src/main/java/com/vi/tenantservice/api/service/TenantMediaService.java`, `.../model/TenantMediaEntity.java`, `.../repository/TenantMediaRepository.java` - stored media with size/content-type validation (`MediaSizeLimitExceededException`, `UnsupportedMediaContentException`).
- `src/main/java/com/vi/tenantservice/api/service/PublicBrandingAssetService.java` and `BrandingAssetDecoder.java` - serve `/tenant/public/branding/{asset}` without authentication.

### Data And Persistence

Tenant entities, repositories, MariaDB schema, and Liquibase changelogs.

Key files:
- `src/main/java/com/vi/tenantservice/api/model/TenantEntity.java` - the tenant aggregate (identity, subdomain, licensing, theming incl. accent/signal colors and login effect, content, settings JSON).
- `src/main/java/com/vi/tenantservice/api/model/TenantSettings.java`, `TenantSetting.java`, `TenantSmtpSettings.java`, `TenantContent.java`, `TenantData.java`/`TenantDataView.java`, `TenantRestrictedData.java`/`TenantRestrictedDataView.java` - settings and read models.
- `src/main/java/com/vi/tenantservice/api/model/AssignedOrSequenceIdGenerator.java` - allows explicitly assigned tenant ids, else sequence.
- `src/main/java/com/vi/tenantservice/api/model/TenantIdReservationEntity.java`, `TenantIdReservationStatus.java`, `TenantIdAllocationStatus.java` - tenant-id reservation records.
- Repositories in `src/main/java/com/vi/tenantservice/api/repository/`: `TenantRepository`, `TenantAdminControlsRepository`, `TenantDpaVersionRepository`, `TenantDpaSignatureRepository`, `TenantDpaAdminSignatureRepository`, `TenantIdReservationRepository`, `TenantMediaRepository`, `PlatformDpiaMasterDataRepository`.
- `src/main/resources/db/changelog/tenantservice-master.xml` - master changelog; changesets `0001`-`0031` under `db/changelog/changeset/`, including `0013_tenant_admin_controls`, `0015`-`0021`+`0025`/`0026`/`0030`/`0031` (DPA versions, signatures, tokens, audit fields, reservation binding), `0022_widen_tenant_settings_column`, `0023_tenant_media`, `0024_tenant_id_reservation`, `0027_tenant_theming_accent_and_signal`, `0028_platform_dpia_master_data`, `0029_tenant_theming_login_effect`.
- Liquibase execution is env-gated: `application.properties` sets `spring.liquibase.enabled=${SPRING_LIQUIBASE_ENABLED:false}` (disabled by default; the deployed cluster manages the schema separately), the `local` profile defaults it to `true` with the `seed` context, and the `testing` profile disables it outright.

### Configuration

Runtime, build, package, framework, and environment configuration.

Key files:
- `pom.xml` - Java 21, Spring Boot parent 4.0.1, Spring Security 7.0.1, Keycloak adapter 17, Liquibase 4.23.2, OpenAPI Generator 7.17.0, Testcontainers 1.21.4, Mockito 5.20, Spotless.
- `api/tenantservice.yaml` - own contract (server-side generation).
- `services/consultingtypeservice.yaml`, `services/applicationsettingsservice.yml`, `services/useradminservice.yaml` - client generation inputs; `services/agencyadminservice.yaml` is present but not wired into the generator.
- `src/main/resources/application.properties` plus profile overrides `application-{local,dev,staging,prod,testing}.properties`.
- `src/main/java/com/vi/tenantservice/config/ConfigurationValidator.java` - fails fast on missing required env values (datasource, Keycloak/JWT, downstream service URLs).
- `src/main/resources/ehcache.xml` with `.../api/config/CacheManagerConfig.java` and `.../api/cache/CacheEventLogger.java`.
- Jackson tailoring: `.../api/config/RestrictedPublicTenantJacksonConfig.java` and `DpiaMasterDataJacksonConfig.java`.

### Deployment And Operations

Docker, CI/CD workflows, contract gates, and operational scripts.

Key files:
- `.github/workflows/ci-feature-branch.yml`, `ci-pull-request.yml`, `ci-main.yml` - Maven build/test pipelines (reusable steps in `.github/actions/`).
- `.github/workflows/openapi-contracts.yml` - OpenAPI contract gate; helper scripts in `scripts/contracts/` (publish provider contracts, verify consumer contract, provider source compatibility) with Python tests in `tests/contracts/`.
- `.github/workflows/release-image.yml` - image release pipeline.
- `scripts/ci/coverage-summary.py` and `test-report-guard.py` - CI report tooling, tested in `tests/ci/`.
- `Dockerfile` - `eclipse-temurin:21-jre`, port 8081, runs `TenantService.jar` with `--enable-preview`.

### Documentation

Human-facing repository documentation and Understand-Anything notes.

Key files:
- `README.md` - service overview (partially stale on Java/Spring versions).
- `documentation/local-development.md` - mixed local frontend/UserService/TenantService setup.
- `documentation/translation-meta.md` - translation metadata notes.
- `CHANGELOG.md` - release history.

## Major Flows

- Tenant admin API flow: `api/tenantservice.yaml` -> `TenantController` -> `TenantServiceFacade` -> `TenantService`/`TenantRepository`, with `TenantInputSanitizer` and change-detection/override services on writes.
- DPA flow: admin publishes a DPA version (`/tenantadmin/{id}/dpa`), invites signers, signers confirm via public token links (`/tenant/public/dpa/confirm/{token}`, `/tenant/public/dpa/forward`); status/gate endpoints report signature state; entities carry audit fields.
- DPIA flow: `/tenantadmin/dpia` maintains platform DPIA master data; `GET /tenant/public/dpia` exposes it unauthenticated.
- Translation flow: `TranslationFacade` -> provider clients (OpenRouter/Mistral) with per-platform API keys managed via `/tenantadmin/translation/keys`.
- Media flow: `/tenantadmin/media` upload -> `TenantMediaService` -> `/media/{mediaId}` and `/tenant/public/branding/{asset}` reads.
- Tenant-id flow: availability check, next-free lookup, reservation/release via `TenantIdAllocationService` and `AssignedOrSequenceIdGenerator`.
- Auth flow: `WebSecurityConfig` -> `JwtAuthConverter` -> `RoleAuthorizationAuthorityMapper` -> `@PreAuthorize` rules; tenant context from token, cookie, or subdomain.
- Database flow: JPA entities and repositories over MariaDB; Liquibase changesets `0001`-`0031` describe the schema but only run where `SPRING_LIQUIBASE_ENABLED` is set (default on only in the `local` profile).
- Deployment flow: GitHub Actions (build, contract gate, release image), Maven, Dockerfile, and profile-based runtime properties.

## API And Service Dependencies

- `api/tenantservice.yaml` - own REST contract (spring server generation).
- `services/consultingtypeservice.yaml` - generated Java client, wrapped by `.../api/service/consultingtype/ConsultingTypeService.java`.
- `services/applicationsettingsservice.yml` - generated Java client, wrapped by `.../api/service/consultingtype/ApplicationSettingsService.java`.
- `services/useradminservice.yaml` - generated Java client, wrapped by `.../api/service/consultingtype/UserAdminService.java`.
- `services/agencyadminservice.yaml` - spec present, no client generation configured in `pom.xml`.
- Client plumbing: `.../api/config/apiclient/*ApiControllerFactory.java`, `.../api/service/httpheader/SecurityHeaderSupplier.java`, `.../api/tenant/TenantHeaderSupplier.java`.

## Database Relationship

- Core: `TenantEntity` + `TenantRepository` (tenant table, settings JSON widened by changeset `0022`).
- Admin controls: `TenantAdminControlsEntity` + repository (changeset `0013`).
- DPA: `TenantDpaVersionEntity`, `TenantDpaSignatureEntity`, `TenantDpaAdminSignatureEntity` + repositories (changesets `0015`-`0021`, `0025`, `0026`, `0030`, `0031`).
- Media: `TenantMediaEntity` + repository (changeset `0023`).
- Tenant-id reservations: `TenantIdReservationEntity` + repository (changesets `0024`, `0031`).
- DPIA: `PlatformDpiaMasterDataEntity` + repository (changeset `0028`).
- Theming columns: changesets `0027` (accent/signal colors) and `0029` (login effect).

## Deployment Relationship

- `.github/workflows/ci-feature-branch.yml`, `ci-pull-request.yml`, `ci-main.yml`, `openapi-contracts.yml`, `release-image.yml` - pipelines.
- `.github/actions/maven-build/action.yml`, `.github/actions/docker-build-push/action.yml` - reusable steps.
- `Dockerfile`, `docker-build.cmd`, `deploy-development.sh`, `check-version.sh`, `run-trivy.sh` - image build and ops scripts.
- `scripts/ci/`, `scripts/contracts/`, `tests/ci/`, `tests/contracts/` - CI gate tooling and its tests.

## ORISO Ecosystem Fit

`ORISO-TenantService` is the tenant registry for ORISO. Beyond tenant metadata it now owns the platform's tenant-facing legal machinery: DPA versioning and signature collection (including public token-based signing links), platform DPIA master data with a public read endpoint, machine translation of legal texts, and public branding assets. Its local contracts and wrapper services show relationships to ConsultingTypeService, ApplicationSettingsService, UserAdminService, and tenant-aware callers that need public or authenticated tenant metadata.
