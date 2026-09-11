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

- Integration branch: `dev` when used for ORISO feature work.
- Tenant model ops guide: `../Deployment/guides/tenant/TENANT_AND_MULTITENANCY_GUIDE.md`.
- Skim `.understand-anything/` before non-trivial changes; README Java version may lag `pom.xml` — trust the POM.
- Changing tenant resolution affects Admin and other services — call out cross-repo impact.

## Done

Touched tests pass; package succeeds for PR-bound work; no secrets in the diff. Task notes: `docs/agent-tasks/YYYY-MM-DD_short-name/` if needed.

## AI agent delivery rules

Binding for every AI coding agent working in this repository. Canonical text and
rationale: `ORISO-Docs/oriso-platform/coding-standards.mdx` (section "AI agent
delivery rules"). Summary:

- **An agent never merges its own pull request.** Not on green CI, not on "finish
  it", not for chores or test-only changes. Delivery ends at: verified → PR open
  with evidence and a reviewer test plan → reviewers requested → issue
  `In review`. Merge only on an explicit, per-PR instruction naming that PR.
- **Request reviewers in the same step that opens the PR.** A PR without
  requested reviewers is not open for review.
- **"Pre-Dev is free" means the server, not the branch.** Deploying images,
  mutating config or data and running E2E on the Pre-Dev server needs no
  approval; the `dev` *branch* is review-gated like any shared branch.
- **Restore what you borrowed.** Record image reference *and* `imagePullPolicy`
  before swapping anything on Pre-Dev, put both back before reporting done, and
  say so in the report.
- **State where it was verified** in every PR body — environment and image, or
  plainly "local only".
