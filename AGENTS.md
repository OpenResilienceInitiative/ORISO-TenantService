# AGENTS.md — ORISO-TenantService

Load workspace parent `../AGENTS.md` first (`PROJECT_ORISO_ROOT` = parent of this repo).

## Stack

Java **21**, Spring Boot **4.0.1**, Maven Wrapper **3.9.15**. Owns tenants, tenant settings, multitenancy resolution (`/service/tenant*`).

## Commands

```bash
./mvnw -B test
./mvnw -B package -DskipTests
./mvnw -B spotless:check
```

From workspace: `REPO=ORISO-TenantService ../scripts/harness/verify-fast.sh` (or `verify-full.sh`).

CI: `./mvnw -B test` then `./mvnw -B package -DskipTests` on Java 21.

## Context

- Integration branch: `pre-dev` when used for ORISO feature work.
- Tenant model ops guide: `../Deployment/guides/tenant/TENANT_AND_MULTITENANCY_GUIDE.md`.
- Skim `.understand-anything/` before non-trivial changes; README Java version may lag `pom.xml` — trust the POM.
- Changing tenant resolution affects Admin and other services — call out cross-repo impact.

## Done

Touched tests pass; package succeeds for PR-bound work; no secrets in the diff. Task notes: `docs/agent-tasks/YYYY-MM-DD_short-name/` if needed.
