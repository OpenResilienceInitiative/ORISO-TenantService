# ORISO TenantService

TenantService is the ORISO tenant registry. It stores tenant identity, subdomain,
licensing limits, theming, feature settings, multilingual legal content, and
content activation dates. It also exposes restricted public tenant metadata used
before a caller has a full authenticated tenant context.

## Navigation

- [Architecture](#architecture)
- [API](#api)
- [Authentication and tenant context](#authentication-and-tenant-context)
- [Configuration](#configuration)
- [Local development](#local-development)
- [Build and test](#build-and-test)
- [Deployment](#deployment)
- [Developer documentation](#developer-documentation)

## Architecture

This is a Java 17 Spring Boot 3 service built with Maven.

Primary layers:

1. `api/tenantservice.yaml` defines the public TenantService REST contract.
2. `TenantController` implements generated API interfaces and applies endpoint authorization.
3. `TenantServiceFacade` orchestrates tenant create, update, delete, search, restricted reads, and downstream synchronization.
4. `TenantService`, `TenantRepository`, and `TenantEntity` own MariaDB persistence.
5. `WebSecurityConfig`, `JwtAuthConverter`, and `AuthorisationService` map Keycloak JWTs to local authorities.
6. `TenantResolverService` resolves tenant context from access token, cookies, or subdomain.
7. `ConsultingTypeService`, `ApplicationSettingsService`, and `UserAdminService` call other ORISO services through generated OpenAPI clients.

## API

The OpenAPI contract is in `api/tenantservice.yaml`.

Main endpoint groups:

- `/tenantadmin` - tenant administration.
- `/tenantadmin/search` - paginated tenant admin search.
- `/tenant` and `/tenant/{id}` - authenticated tenant reads.
- `/tenant/public/**` - restricted public tenant metadata.
- `/tenant/access` - checks whether the current user can access a tenant context.

`TenantController` also contains a handwritten `DELETE /tenant/{id}` endpoint. If generated clients need this operation, add it to `api/tenantservice.yaml`.

## Authentication and Tenant Context

The service uses Keycloak/OAuth2 JWT authentication through Spring Security.

Role and permission flow:

1. `WebSecurityConfig` configures stateless JWT resource-server authentication.
2. `JwtAuthConverter` reads realm roles from the JWT.
3. `RoleAuthorizationAuthorityMapper` maps ORISO roles into local `AUTHORIZATION_*` authorities.
4. `@PreAuthorize` rules on `TenantController` protect tenant and tenant-admin operations.

Tenant context resolution order:

1. `AccessTokenTenantResolver` reads tenant id from the authenticated JWT.
2. `CookieTenantResolver` reads cookie hints in single-domain mode.
3. `SubdomainTenantResolver` resolves tenant by request subdomain.

## Configuration

Base configuration lives in `src/main/resources/application.properties`.
Profile overrides live in:

- `src/main/resources/application-local.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-staging.properties`
- `src/main/resources/application-prod.properties`
- `src/main/resources/application-testing.properties`

Required runtime values are validated by `ConfigurationValidator`:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KEYCLOAK_AUTH_SERVER_URL`
- `KEYCLOAK_REALM`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`
- `CONSULTING_TYPE_SERVICE_API_URL`
- `USER_SERVICE_API_URL`

Use Kubernetes DNS names for service URLs and database hosts. Avoid hardcoded
cluster IPs because they break when services or pods are recreated.

## Local Development

For the full mixed local frontend/UserService/TenantService setup, see
[`documentation/local-development.md`](documentation/local-development.md).

Prerequisites:

- JDK 17
- Maven wrapper from this repository
- MariaDB reachable through the configured datasource URL
- Keycloak realm and JWT issuer/JWK configuration
- ConsultingTypeService and UserAdminService URLs

Run locally:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Health check:

```bash
curl http://localhost:8081/actuator/health
```

## Build and Test

Run tests:

```bash
./mvnw test
```

Build the jar:

```bash
./mvnw package
```

Check formatting:

```bash
./mvnw spotless:check
```

The Maven compiler, Surefire, and Docker runtime use `--enable-preview`.
Keep the tested JDK and deployment JDK aligned when changing Java versions.

## Deployment

`Dockerfile` runs `TenantService.jar` on `eclipse-temurin:17-jre`.

Development deployment helper:

```bash
./deploy-development.sh
```

Operational notes:

- Application default port is `8081`.
- The Dockerfile currently exposes `8080`; align this before relying on container metadata.
- Liquibase changelogs exist under `src/main/resources/db/changelog/`, but profiles currently disable Liquibase.
- `run-trivy.sh` is available for image/security scanning.

## Developer Documentation

Understand-Anything documentation and graph artifacts are stored in `.understand-anything/`.

Start here:

- `.understand-anything/README.md`
- `.understand-anything/ARCHITECTURE.md`
- `.understand-anything/ONBOARDING.md`
- `.understand-anything/FINDINGS.md`
- `.understand-anything/DEPENDENCY-AUDIT.md`
- `.understand-anything/ORISO-ECOSYSTEM.md`
- `.understand-anything/knowledge-graph.json`

Open the graph dashboard:

```bash
cd /Users/nikunjchampakbhairohit/.understand-anything/repo/understand-anything-plugin/packages/dashboard
GRAPH_DIR="/Users/nikunjchampakbhairohit/Developer/freelance/Germany/Oriso-frank-client/ORISO/ORISO-TenantService" pnpm exec vite --host 127.0.0.1
```

Then open the local URL printed by Vite.

## Important Files

- `pom.xml` - dependencies, OpenAPI generation, Liquibase plugin, Java compiler, and test configuration.
- `api/tenantservice.yaml` - primary REST API contract.
- `src/main/java/com/vi/tenantservice/api/controller/TenantController.java` - HTTP entrypoint.
- `src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java` - main tenant orchestration layer.
- `src/main/java/com/vi/tenantservice/api/service/TenantService.java` - tenant persistence service.
- `src/main/java/com/vi/tenantservice/api/model/TenantEntity.java` - JPA tenant table mapping.
- `src/main/java/com/vi/tenantservice/api/repository/TenantRepository.java` - tenant repository and search query.
- `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java` - security configuration.
- `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java` - JWT claims and authority extraction.
- `src/main/java/com/vi/tenantservice/api/service/consultingtype/ConsultingTypeService.java` - downstream ConsultingType integration.
