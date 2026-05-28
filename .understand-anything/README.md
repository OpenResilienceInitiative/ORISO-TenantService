# ORISO-TenantService Understand-Anything Notes

> Scope: this documentation was generated from this repository only. Parent folders and sibling repositories were not analyzed.

## Navigation

- [Knowledge graph](knowledge-graph.json)
- [Architecture summary](ARCHITECTURE.md)
- [Developer onboarding](ONBOARDING.md)
- [ORISO ecosystem map](ORISO-ECOSYSTEM.md)
- [Findings and cleanup register](FINDINGS.md)
- [Dependency audit](DEPENDENCY-AUDIT.md)
- Visuals: [architecture](visuals/architecture-flow.mmd), [auth](visuals/auth-flow.mmd), [data](visuals/data-flow.mmd), [deployment](visuals/deployment-flow.mmd), [ecosystem](visuals/ecosystem-flow.mmd)

## Dashboard

Knowledge graph saved at: /Users/nikunjchampakbhairohit/Developer/freelance/Germany/Oriso-frank-client/ORISO/ORISO-TenantService/.understand-anything/knowledge-graph.json

Open the interactive dashboard from the plugin dashboard package:

```bash
cd /Users/nikunjchampakbhairohit/.understand-anything/repo/understand-anything-plugin/packages/dashboard && GRAPH_DIR="/Users/nikunjchampakbhairohit/Developer/freelance/Germany/Oriso-frank-client/ORISO/ORISO-TenantService" pnpm exec vite --host 127.0.0.1
```

Then open the local URL printed by Vite. The dashboard reads `.understand-anything/knowledge-graph.json` from this repository.

## Quick Map

ORISO-TenantService is a Spring Boot service that owns tenant records, tenant settings, tenant legal content, tenant subdomain access, and tenant-specific synchronization with other ORISO services.

- `api/tenantservice.yaml` defines the public REST API.
- `TenantController` implements generated API interfaces and protects tenant/admin operations.
- `TenantServiceFacade` is the main orchestration layer.
- `TenantService`, `TenantRepository`, and `TenantEntity` own MariaDB persistence.
- `WebSecurityConfig`, `JwtAuthConverter`, and `AuthorisationService` translate Keycloak JWT roles into local `AUTHORIZATION_*` permissions.
- `TenantResolverService` chooses tenant context from access token, cookies, or subdomain.
- `ConsultingTypeService`, `ApplicationSettingsService`, and `UserAdminService` are ORISO service clients.

## Top 10 Files

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

## Graph Stats

- Files scanned: 160
- Graph nodes: 380
- Graph edges: 623
- API operations discovered: 66
- Java declarations discovered: 98

## Read This First

For a new developer, start with the graph tour, then read `ARCHITECTURE.md`, then walk the create/update flows in `TenantServiceFacade`. The service is compact enough to understand from the facade, but the security and single-domain override rules deserve careful reading before changing behavior.
