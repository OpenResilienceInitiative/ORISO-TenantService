# ORISO Ecosystem Notes: ORISO-TenantService

This graph was generated for `ORISO-TenantService` only. It does not analyze sibling repositories.

## Local Role Evidence

- Purpose: Spring Boot service that owns ORISO tenant records, tenant settings, legal content, subdomain resolution, tenant admin controls, and tenant-specific integration with ConsultingType, ApplicationSettings, and UserAdmin services.
- Languages: batch, dockerfile, java, javascript, json, markdown, properties, shell, sql, unknown, xml, yaml
- Frameworks/tools: Docker, Spring Boot, Spring Security, OAuth2 Resource Server, Keycloak, Spring Data JPA, MariaDB, Liquibase, OpenAPI Generator, FreeMarker, Ehcache
- API/service-related files: 12
- Auth/tenant/security-related files: 27
- Database-related files: 47
- Deployment/operation-related files: 9

## Integration Clues

- `api/tenantservice.yaml` - config, yaml
- `services/agencyadminservice.yaml` - config, yaml
- `services/applicationsettingsservice.yml` - config, yaml
- `services/consultingtypeservice.yaml` - config, yaml
- `services/useradminservice.yaml` - config, yaml
- `src/main/java/com/vi/tenantservice/api/controller/ExceptionHandlerAdvice.java` - code, java
- `src/main/java/com/vi/tenantservice/api/controller/TenantController.java` - code, java
- `src/main/java/com/vi/tenantservice/api/controller/TenantDtoMapper.java` - code, java
- `src/main/java/com/vi/tenantservice/api/controller/VersionController.java` - code, java
- `src/test/java/com/vi/tenantservice/api/controller/ActuatorControllerIT.java` - code, java
- `src/test/java/com/vi/tenantservice/api/controller/AuthenticationMockBuilder.java` - code, java
- `src/test/java/com/vi/tenantservice/api/controller/TenantControllerIT.java` - code, java
- `src/main/java/com/vi/tenantservice/api/authorisation/Authority.java` - code, java
- `src/main/java/com/vi/tenantservice/api/authorisation/RoleAuthorizationAuthorityMapper.java` - code, java
- `src/main/java/com/vi/tenantservice/api/authorisation/UserRole.java` - code, java
- `src/main/java/com/vi/tenantservice/api/exception/TenantAuthorisationException.java` - code, java
- `src/main/java/com/vi/tenantservice/api/facade/TenantFacadeAuthorisationService.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminAllowedPermissionTogglesSettings.java` - code, java
- `src/main/java/com/vi/tenantservice/api/service/httpheader/SecurityHeaderSupplier.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/AccessTokenTenantResolver.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/CookieTenantResolver.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/HttpUrlUtils.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/SubdomainExtractor.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/SubdomainTenantResolver.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/TenantHeaderSupplier.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/TenantResolver.java` - code, java
- `src/main/java/com/vi/tenantservice/api/tenant/TenantResolverService.java` - code, java
- `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java` - code, java
- `src/main/java/com/vi/tenantservice/config/security/JwtAuthConverter.java` - code, java
- `src/main/java/com/vi/tenantservice/config/security/JwtAuthConverterProperties.java` - code, java

## Platform Relationships

- Owns tenant identity, subdomains, settings, legal content, licensing limits, and tenant admin controls.
- Calls ConsultingTypeService, ApplicationSettingsService, and UserAdminService through generated contracts and wrapper services.
- Provides restricted public tenant metadata before full authenticated tenant context is available.
