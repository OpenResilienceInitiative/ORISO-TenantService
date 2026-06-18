# ORISO-TenantService Architecture

## System Responsibility

Spring Boot service that owns ORISO tenant records, tenant settings, legal content, subdomain resolution, tenant admin controls, and tenant-specific integration with ConsultingType, ApplicationSettings, and UserAdmin services.

## Architecture Layers

### Api And Routing

OpenAPI contracts, generated-client contracts, controllers, and route boundaries.

Key files:
- `src/main/java/com/vi/tenantservice/api/controller/ExceptionHandlerAdvice.java` - src/main/java/com/vi/tenantservice/api/controller/ExceptionHandlerAdvice.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".
- `src/main/java/com/vi/tenantservice/api/controller/TenantController.java` - src/main/java/com/vi/tenantservice/api/controller/TenantController.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".
- `src/main/java/com/vi/tenantservice/api/controller/TenantDtoMapper.java` - src/main/java/com/vi/tenantservice/api/controller/TenantDtoMapper.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".
- `src/main/java/com/vi/tenantservice/api/controller/VersionController.java` - src/main/java/com/vi/tenantservice/api/controller/VersionController.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".
- `src/test/java/com/vi/tenantservice/api/controller/ActuatorControllerIT.java` - src/test/java/com/vi/tenantservice/api/controller/ActuatorControllerIT.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".
- `src/test/java/com/vi/tenantservice/api/controller/AuthenticationMockBuilder.java` - src/test/java/com/vi/tenantservice/api/controller/AuthenticationMockBuilder.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".
- `src/test/java/com/vi/tenantservice/api/controller/TenantControllerIT.java` - src/test/java/com/vi/tenantservice/api/controller/TenantControllerIT.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.controller;".

### Auth And Tenant Context

OAuth2/JWT security, Keycloak role mapping, local authorities, and tenant resolution.

Key files:
- `src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java` - src/main/java/com/vi/tenantservice/config/security/AuthorisationService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.config.security;".
- `src/main/java/com/vi/tenantservice/config/security/JwtAuthConverter.java` - src/main/java/com/vi/tenantservice/config/security/JwtAuthConverter.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.config.security;".
- `src/main/java/com/vi/tenantservice/config/security/JwtAuthConverterProperties.java` - src/main/java/com/vi/tenantservice/config/security/JwtAuthConverterProperties.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.config.security;".
- `src/main/java/com/vi/tenantservice/config/security/KeycloakLogoutHandler.java` - src/main/java/com/vi/tenantservice/config/security/KeycloakLogoutHandler.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.config.security;".
- `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java` - src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.config.security;".
- `src/test/java/com/vi/tenantservice/config/security/AuthorisationServiceTest.java` - src/test/java/com/vi/tenantservice/config/security/AuthorisationServiceTest.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.config.security;".

### Tenant Domain Services

Tenant lifecycle orchestration, admin controls, downstream synchronization, conversion, and validation services.

Key files:
- `src/main/java/com/vi/tenantservice/api/converter/ConsultingTypePatchDTOConverter.java` - src/main/java/com/vi/tenantservice/api/converter/ConsultingTypePatchDTOConverter.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.converter;".
- `src/main/java/com/vi/tenantservice/api/converter/ConverterUtils.java` - src/main/java/com/vi/tenantservice/api/converter/ConverterUtils.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.converter;".
- `src/main/java/com/vi/tenantservice/api/converter/TenantConverter.java` - src/main/java/com/vi/tenantservice/api/converter/TenantConverter.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.converter;".
- `src/main/java/com/vi/tenantservice/api/facade/TenantFacadeAuthorisationService.java` - src/main/java/com/vi/tenantservice/api/facade/TenantFacadeAuthorisationService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.facade;".
- `src/main/java/com/vi/tenantservice/api/facade/TenantFacadeChangeDetectionService.java` - src/main/java/com/vi/tenantservice/api/facade/TenantFacadeChangeDetectionService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.facade;".
- `src/main/java/com/vi/tenantservice/api/facade/TenantFacadeDependentSettingsOverrideService.java` - src/main/java/com/vi/tenantservice/api/facade/TenantFacadeDependentSettingsOverrideService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.facade;".
- `src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java` - src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.facade;".
- `src/main/java/com/vi/tenantservice/api/service/ConfigurationFileLoader.java` - src/main/java/com/vi/tenantservice/api/service/ConfigurationFileLoader.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/SingleDomainTenantOverrideService.java` - src/main/java/com/vi/tenantservice/api/service/SingleDomainTenantOverrideService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/TemplateDescriptionServiceException.java` - src/main/java/com/vi/tenantservice/api/service/TemplateDescriptionServiceException.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/TemplateRenderer.java` - src/main/java/com/vi/tenantservice/api/service/TemplateRenderer.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/TemplateService.java` - src/main/java/com/vi/tenantservice/api/service/TemplateService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/TenantAdminControlsService.java` - src/main/java/com/vi/tenantservice/api/service/TenantAdminControlsService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/TenantService.java` - src/main/java/com/vi/tenantservice/api/service/TenantService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/TranslationService.java` - src/main/java/com/vi/tenantservice/api/service/TranslationService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service;".
- `src/main/java/com/vi/tenantservice/api/service/consultingtype/ApplicationSettingsService.java` - src/main/java/com/vi/tenantservice/api/service/consultingtype/ApplicationSettingsService.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.service.consultingtype;".

### Data And Persistence

Tenant entities, repositories, MariaDB schema, Liquibase changelogs, and persistence tests.

Key files:
- `src/main/java/com/vi/tenantservice/api/model/DataProtectionPlaceHolderType.java` - src/main/java/com/vi/tenantservice/api/model/DataProtectionPlaceHolderType.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminAllowedPermissionTogglesSettings.java` - src/main/java/com/vi/tenantservice/api/model/TenantAdminAllowedPermissionTogglesSettings.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsEntity.java` - src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsEntity.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsSettings.java` - src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsSettings.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantContent.java` - src/main/java/com/vi/tenantservice/api/model/TenantContent.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantEntity.java` - src/main/java/com/vi/tenantservice/api/model/TenantEntity.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantSetting.java` - src/main/java/com/vi/tenantservice/api/model/TenantSetting.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantSettings.java` - src/main/java/com/vi/tenantservice/api/model/TenantSettings.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/model/TenantSmtpSettings.java` - src/main/java/com/vi/tenantservice/api/model/TenantSmtpSettings.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.model;".
- `src/main/java/com/vi/tenantservice/api/repository/TenantAdminControlsRepository.java` - src/main/java/com/vi/tenantservice/api/repository/TenantAdminControlsRepository.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.repository;".
- `src/main/java/com/vi/tenantservice/api/repository/TenantRepository.java` - src/main/java/com/vi/tenantservice/api/repository/TenantRepository.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.repository;".
- `src/test/java/com/vi/tenantservice/api/repository/TenantRepositoryTest.java` - src/test/java/com/vi/tenantservice/api/repository/TenantRepositoryTest.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.repository;".
- `src/main/resources/db/changelog/changeset/0001_initsql/initSql.xml` - src/main/resources/db/changelog/changeset/0001_initsql/initSql.xml is a data file in ORISO-TenantService; starts with "<?xml version='1.1' encoding='UTF-8' standalone='no'?>".
- `src/main/resources/db/changelog/changeset/0001_initsql/initTables.sql` - src/main/resources/db/changelog/changeset/0001_initsql/initTables.sql is a data file in ORISO-TenantService; starts with "CREATE TABLE tenantservice.`tenant` (".
- `src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/0002-changeSet.xml` - src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/0002-changeSet.xml is a data file in ORISO-TenantService; starts with "<?xml version='1.1' encoding='UTF-8' standalone='no'?>".
- `src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/addPrivacyAndTermsAndConditionsColumns.sql` - src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/addPrivacyAndTermsAndConditionsColumns.sql is a data file in ORISO-TenantService; starts with "ALTER TABLE `tenantservice`.`tenant`".

### Configuration

Runtime, build, package, framework, and environment configuration.

Key files:
- `.github/actions/docker-build-push/action.yml` - .github/actions/docker-build-push/action.yml is a config file in ORISO-TenantService; starts with "name: Reusable Docker Build and Publish steps".
- `.github/actions/maven-build/action.yml` - .github/actions/maven-build/action.yml is a config file in ORISO-TenantService; starts with "name: Reusable Maven Build steps".
- `.mvn/wrapper/maven-wrapper.properties` - .mvn/wrapper/maven-wrapper.properties is a config file in ORISO-TenantService; starts with "wrapperVersion=3.3.4".
- `api/tenantservice.yaml` - api/tenantservice.yaml is a config file in ORISO-TenantService; starts with "openapi: 3.0.1".
- `package-lock.json` - package-lock.json is a config file in ORISO-TenantService; starts with "{".
- `package.json` - package.json is a config file in ORISO-TenantService; starts with "{".
- `pom.xml` - pom.xml is a config file in ORISO-TenantService; starts with "<?xml version='1.0' encoding='UTF-8'?>".
- `services/agencyadminservice.yaml` - services/agencyadminservice.yaml is a config file in ORISO-TenantService; starts with "openapi: 3.0.1".
- `services/applicationsettingsservice.yml` - services/applicationsettingsservice.yml is a config file in ORISO-TenantService; starts with "openapi: 3.0.1".
- `services/consultingtypeservice.yaml` - services/consultingtypeservice.yaml is a config file in ORISO-TenantService; starts with "openapi: 3.0.1".
- `services/useradminservice.yaml` - services/useradminservice.yaml is a config file in ORISO-TenantService; starts with "openapi: 3.0.1".
- `src/main/resources/application-dev.properties` - src/main/resources/application-dev.properties is a config file in ORISO-TenantService; starts with "# Logging: SLF4J (via Lombok)".
- `src/main/resources/application-local.properties` - src/main/resources/application-local.properties is a config file in ORISO-TenantService; starts with "# Logging: SLF4J (via Lombok)".
- `src/main/resources/application-prod.properties` - src/main/resources/application-prod.properties is a config file in ORISO-TenantService; starts with "# Logging: SLF4J (via Lombok)".
- `src/main/resources/application-staging.properties` - src/main/resources/application-staging.properties is a config file in ORISO-TenantService; starts with "# Logging: SLF4J (via Lombok)".
- `src/main/resources/application-testing.properties` - src/main/resources/application-testing.properties is a config file in ORISO-TenantService; starts with "# Logging: SLF4J (via Lombok)".

### Deployment And Operations

Docker, CI/CD workflows, Maven wrapper, and operational scripts.

Key files:
- `.github/workflows/ci-feature-branch.yml` - .github/workflows/ci-feature-branch.yml is a pipeline file in ORISO-TenantService; starts with "name: CI - Feature Branch".
- `.github/workflows/ci-main.yml` - .github/workflows/ci-main.yml is a pipeline file in ORISO-TenantService; starts with "name: CI - Main".
- `.github/workflows/ci-pull-request.yml` - .github/workflows/ci-pull-request.yml is a pipeline file in ORISO-TenantService; starts with "name: CI - Pull Request".
- `Dockerfile` - Dockerfile is a infra file in ORISO-TenantService; starts with "FROM eclipse-temurin:17-jre".

### Documentation

Human-facing repository documentation and Understand-Anything notes.

Key files:
- `CHANGELOG.md` - CHANGELOG.md is a docs file in ORISO-TenantService; starts with "# Changelog".
- `README.md` - README.md is a docs file in ORISO-TenantService; starts with "# ORISO TenantService".

### Application Core

Application entry point, exceptions, shared utilities, templates, and supporting code.

Key files:
- `.gitignore` - .gitignore is a code file in ORISO-TenantService; starts with "# =============================================================================".
- `LICENSE` - LICENSE is a code file in ORISO-TenantService; starts with "GNU AFFERO GENERAL PUBLIC LICENSE".
- `check-version.sh` - check-version.sh is a script file in ORISO-TenantService; starts with "#!/bin/bash".
- `commitlint.config.js` - commitlint.config.js is a code file in ORISO-TenantService; starts with "module.exports = { extends: ['@commitlint/config-conventional'] };".
- `deploy-development.sh` - deploy-development.sh is a script file in ORISO-TenantService; starts with "#!/usr/bin/env bash".
- `docker-build.cmd` - docker-build.cmd is a script file in ORISO-TenantService; starts with "docker build --no-cache -t tenantservice:development .".
- `mvnw` - mvnw is a code file in ORISO-TenantService; starts with "#!/bin/sh".
- `mvnw.cmd` - mvnw.cmd is a script file in ORISO-TenantService; starts with "<# : batch portion".
- `run-trivy.sh` - run-trivy.sh is a script file in ORISO-TenantService; starts with "rm report*.sarif".
- `spring-boot-ehcache/cache/.lock` - spring-boot-ehcache/cache/.lock is a code file in ORISO-TenantService.
- `src/main/java/com/vi/tenantservice/TenantServiceApplication.java` - src/main/java/com/vi/tenantservice/TenantServiceApplication.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice;".
- `src/main/java/com/vi/tenantservice/api/authorisation/Authority.java` - src/main/java/com/vi/tenantservice/api/authorisation/Authority.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.authorisation;".
- `src/main/java/com/vi/tenantservice/api/authorisation/RoleAuthorizationAuthorityMapper.java` - src/main/java/com/vi/tenantservice/api/authorisation/RoleAuthorizationAuthorityMapper.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.authorisation;".
- `src/main/java/com/vi/tenantservice/api/authorisation/UserRole.java` - src/main/java/com/vi/tenantservice/api/authorisation/UserRole.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.authorisation;".
- `src/main/java/com/vi/tenantservice/api/cache/CacheEventLogger.java` - src/main/java/com/vi/tenantservice/api/cache/CacheEventLogger.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.cache;".
- `src/main/java/com/vi/tenantservice/api/config/CacheManagerConfig.java` - src/main/java/com/vi/tenantservice/api/config/CacheManagerConfig.java is a code file in ORISO-TenantService; starts with "package com.vi.tenantservice.api.config;".


## Major Flows

- Tenant admin API flow: `api/tenantservice.yaml`, `TenantController`, `TenantServiceFacade`, and `TenantService`.
- Tenant admin controls flow: `TenantAdminControlsEntity`, `TenantAdminControlsRepository`, `TenantAdminControlsService`, and `0013_tenant_admin_controls` changelog.
- Auth flow: `WebSecurityConfig`, JWT conversion, authority mapping, method security, and tenant resolver services.
- Database flow: JPA entities and repositories map tenant records, settings, admin controls, and Liquibase schema changes.
- External service flow: wrapper services call ConsultingType, ApplicationSettings, and UserAdmin generated OpenAPI clients.
- Deployment flow: GitHub Actions, Maven, Dockerfile, and runtime application properties build and operate the service.

## API And Service Dependencies

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

## Authentication Relationship

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
- `src/main/java/com/vi/tenantservice/config/security/KeycloakLogoutHandler.java` - code, java
- `src/main/java/com/vi/tenantservice/config/security/WebSecurityConfig.java` - code, java

## Database Relationship

- `src/main/java/com/vi/tenantservice/api/model/DataProtectionPlaceHolderType.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminAllowedPermissionTogglesSettings.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsEntity.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantAdminControlsSettings.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantContent.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantEntity.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantSetting.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantSettings.java` - code, java
- `src/main/java/com/vi/tenantservice/api/model/TenantSmtpSettings.java` - code, java
- `src/main/java/com/vi/tenantservice/api/repository/TenantAdminControlsRepository.java` - code, java
- `src/main/java/com/vi/tenantservice/api/repository/TenantRepository.java` - code, java
- `src/main/resources/db/changelog/changeset/0001_initsql/initSql.xml` - data, xml
- `src/main/resources/db/changelog/changeset/0001_initsql/initTables.sql` - data, sql
- `src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/0002-changeSet.xml` - data, xml
- `src/main/resources/db/changelog/changeset/0002_add_privacy_and_terms_and_conditions/addPrivacyAndTermsAndConditionsColumns.sql` - data, sql
- `src/main/resources/db/changelog/changeset/0003_change_tenant_attributes/0003-changeSet.xml` - data, xml
- `src/main/resources/db/changelog/changeset/0003_change_tenant_attributes/changeTenantAttributes.sql` - data, sql
- `src/main/resources/db/changelog/changeset/0004_add_tenant_settings/0004-changeSet.xml` - data, xml
- `src/main/resources/db/changelog/changeset/0004_add_tenant_settings/addSettingTopicsInRegistrationEnabled.sql` - data, sql
- `src/main/resources/db/changelog/changeset/0005_change_tenant_settings_structure/0005-changeSet.xml` - data, xml

## Deployment Relationship

- `.github/workflows/ci-feature-branch.yml` - pipeline, yaml
- `.github/workflows/ci-main.yml` - pipeline, yaml
- `.github/workflows/ci-pull-request.yml` - pipeline, yaml
- `Dockerfile` - infra, dockerfile
- `check-version.sh` - script, shell
- `deploy-development.sh` - script, shell
- `docker-build.cmd` - script, batch
- `mvnw.cmd` - script, batch
- `run-trivy.sh` - script, shell

## ORISO Ecosystem Fit

`ORISO-TenantService` is the tenant registry for ORISO. Its local contracts and wrapper services show relationships to ConsultingTypeService, ApplicationSettingsService, UserAdminService, and tenant-aware callers that need public or authenticated tenant metadata.
