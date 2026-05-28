# Findings and Cleanup Register

## Navigation

- [High impact](#high-impact)
- [Documentation gaps](#documentation-gaps)
- [Dead or stale code](#dead-or-stale-code)
- [Risky dependencies](#risky-dependencies)
- [Unclear boundaries](#unclear-boundaries)
- [Duplicated logic](#duplicated-logic)

## High Impact

1. Security workaround in `TenantFacadeAuthorisationService`: `canAccessTenant` contains a temporary bypass that returns true for username `technical`. This should be documented with an owner and removal condition or replaced with an explicit service-account rule.
2. Dependency stack risk in `pom.xml`: Spring Boot 3.0.0 is combined with old Keycloak adapters, Springfox 3, Springdoc 2, and `javax.ws.rs`. This is a known compatibility/upgrade hotspot for Spring Boot 3 and Jakarta migration.
3. API drift: `TenantController.deleteTenant` exposes `DELETE /tenant/{id}` but the operation is not present in `api/tenantservice.yaml`.
4. Runtime drift: `application.properties` sets server port 8081 while `Dockerfile` exposes 8080.
5. Local profile drift: `application-local.properties` includes hardcoded cluster IPs despite comments instructing DNS-based configuration.

## Documentation Gaps

- Root `README.md` is deleted in the working tree and was not replaced by this analysis. `.understand-anything/README.md` now provides architecture notes, but the repository still lacks a normal project README unless that deletion is intentional.
- No explicit documentation explains single-domain versus multi-domain tenant behavior.
- No operational note explains why Liquibase is disabled in every profile while changelogs are maintained.
- No service contract note explains why ApplicationSettings client generation uses `consulting.type.service.api.url`.
- No endpoint documentation captures the handwritten DELETE route.

## Dead or Stale Code

- `TenantHeaderSupplier` is scanned as a Spring component but no active call site was found in the repository.
- `services/agencyadminservice.yaml` is referenced by `services/useradminservice.yaml` schemas but has no direct OpenAPI Generator execution or wrapper client in this repository.
- `src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/0002-changeSet.xml` exists but is not included by the scanned profile master changelog.
- `src/main/resources/liquibase.properties` references old MySQL driver and local root credentials, which do not match the current MariaDB runtime model.

## Risky Dependencies

- Keycloak adapters `16.x/19.x` are legacy dependencies in a Spring Boot 3 application.
- Springfox 3 is present alongside Springdoc 2. Running both documentation stacks increases maintenance and compatibility risk.
- `spring-boot-properties-migrator` is runtime scoped; keep it temporary and remove after property migration is done.
- `byte-buddy` is not scoped as test-only in `pom.xml`; verify whether production runtime really needs it.
- Java preview features are enabled in Maven, tests, and Docker; preview-language use raises JDK upgrade risk.

## Unclear Boundaries

- `TenantServiceFacade` is large and mixes validation, authorization delegation, tenant resolution, persistence orchestration, and external synchronization. It is still the correct starting point, but helper extraction should continue around coherent flows.
- `TenantConverter` contains business-default behavior, not just mapping. Treat converter edits as behavior changes.
- Security is split between route matchers, `permitAll`, negated request matchers, and method annotations. That needs regression tests when adding routes.
- `ApplicationSettingsApiControllerFactory` uses `consulting.type.service.api.url`; this may be intentional because the API lives with ConsultingTypeService, but the naming is unclear.

## Duplicated Logic

- `TenantConverter` and `TenantFacadeChangeDetectionService` both encode legacy feature defaults and repeated `settingsJson.contains(...)` checks.
- `deploy-development.sh` repeats the same deployment block multiple times.
- Profile properties repeat large configuration blocks across dev, local, prod, staging, and testing.

## Smaller Correctness Notes

- `ExceptionHandlerAdvice` has an `@ExceptionHandler(BadRequestException.class)` method whose parameter type is `HttpMessageNotReadableException`, which is inconsistent.
- `TenantServiceFacade.validateTenantInput` names a variable `isoCountries` while using `Locale.getISOLanguages()`.
- Invalid active language handling throws reason `ID_MUST_BE_NULL_WHEN_CREATING_TENANT`, which does not match the validation failure.
- `SpringFoxConfig.WHITE_LIST` includes `/mails/docs` while this service configures `/tenantService/docs`.
- `run-trivy.sh` starts with `rm report*.sarif` under `set -euo pipefail`; if no report exists, the script may stop early.
