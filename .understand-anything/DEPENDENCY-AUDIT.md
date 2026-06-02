# Dependency Audit

## Navigation

- [Build and runtime](#build-and-runtime)
- [Generated API clients](#generated-api-clients)
- [Security dependencies](#security-dependencies)
- [Persistence dependencies](#persistence-dependencies)
- [Frontend/tooling dependencies](#frontendtooling-dependencies)
- [Risk register](#risk-register)

## Build and Runtime

`pom.xml` is the main dependency boundary. It targets Java 17 and enables preview features for compile, tests, and runtime. Spring Boot parent is `3.0.0`.

Core runtime dependencies include Spring Boot web, validation, HATEOAS, security, OAuth2 resource server, JPA, MariaDB, Liquibase, FreeMarker, Ehcache/JCache, OpenAPI/Swagger tooling, Keycloak, Jackson, Guava, and Lombok.

## Generated API Clients

OpenAPI Generator 6.6.0 generates:

- Server interfaces/models from `api/tenantservice.yaml`.
- ConsultingType client from `services/consultingtypeservice.yaml`.
- ApplicationSettings client from `services/applicationsettingsservice.yml`.
- UserAdmin client from `services/useradminservice.yaml`.

`services/agencyadminservice.yaml` is used as a referenced schema source by UserAdmin specs, not as a directly generated client in this repo.

## Security Dependencies

Security mixes modern Spring Security resource-server JWT support with older Keycloak adapter dependencies. The code uses `@KeycloakConfiguration` and `@EnableGlobalMethodSecurity`; both deserve review before Spring Boot/Spring Security upgrades.

Local authorities are defined in code rather than coming directly from Keycloak scopes. That makes `Authority`, `UserRole`, and `RoleAuthorizationAuthorityMapper` part of the authorization contract.

## Persistence Dependencies

MariaDB is configured through Spring Data JPA and Liquibase changelogs. Runtime profiles disable Liquibase, so schema migration ownership is likely external or manual. `liquibase.properties` appears stale and still references MySQL-era local values.

## Frontend/Tooling Dependencies

`package.json` is only release/commit tooling: standard-version, commitlint, commitizen, Husky, and dotenv. It does not define application runtime behavior.

## Risk Register

- Spring Boot 3.0.0 is old for a Boot 3 application and should be tested before upgrading because of Keycloak/Springfox/Jakarta interactions.
- Springfox 3 and Springdoc 2 are both present; pick one documentation stack if possible.
- Old Keycloak adapters are the highest security/compatibility upgrade concern.
- Java preview features should be isolated or removed before JDK upgrades.
- `spring-boot-properties-migrator` should remain temporary.
- `byte-buddy` scope should be confirmed.
- Docker uses Java 17 while `pom.xml` also declares a Java upper version of 21; keep the declared runtime and tested runtime aligned.
