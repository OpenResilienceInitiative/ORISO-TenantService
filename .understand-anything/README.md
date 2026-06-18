# Understand-Anything Graph: ORISO-TenantService

This directory contains the Understand-Anything graph and developer notes for `ORISO-TenantService`.

## Graph

- Graph file: `.understand-anything/knowledge-graph.json`
- Generated at: `2026-06-12T03:44:12.472Z`
- Source commit: `a777c69447609d16db33ae1a1b54fd4efe5366b6`
- Files analyzed: 177
- Nodes: 647
- Edges: 1238

## Repository Purpose

Spring Boot service that owns ORISO tenant records, tenant settings, legal content, subdomain resolution, tenant admin controls, and tenant-specific integration with ConsultingType, ApplicationSettings, and UserAdmin services.

## Current Refresh Notes

The graph was regenerated from latest `dev`. The latest code includes tenant admin controls persistence, tenant admin API additions, converter/facade/service updates, Liquibase changeset `0013_tenant_admin_controls`, and corresponding tests.

## Dashboard

From this repository root:

```sh
PLUGIN_ROOT="$HOME/.understand-anything-plugin"
test -d "$PLUGIN_ROOT/packages/dashboard" || PLUGIN_ROOT="$HOME/.understand-anything/repo/understand-anything-plugin"
cd "$PLUGIN_ROOT/packages/dashboard"
GRAPH_DIR="$(pwd)" npx vite --host 127.0.0.1
```

Use the tokenized URL printed by Vite. The dashboard reads `.understand-anything/knowledge-graph.json`.

## Main Files Scanned

- `.github/actions/docker-build-push/action.yml` - config, yaml
- `.github/actions/maven-build/action.yml` - config, yaml
- `.github/workflows/ci-feature-branch.yml` - pipeline, yaml
- `.github/workflows/ci-main.yml` - pipeline, yaml
- `.github/workflows/ci-pull-request.yml` - pipeline, yaml
- `.gitignore` - code, unknown
- `.mvn/wrapper/maven-wrapper.properties` - config, properties
- `CHANGELOG.md` - docs, markdown
- `Dockerfile` - infra, dockerfile
- `LICENSE` - code, unknown
- `README.md` - docs, markdown
- `api/tenantservice.yaml` - config, yaml
- `check-version.sh` - script, shell
- `commitlint.config.js` - code, javascript
- `deploy-development.sh` - script, shell
- `docker-build.cmd` - script, batch
- `mvnw` - code, unknown
- `mvnw.cmd` - script, batch
- `package-lock.json` - config, json
- `package.json` - config, json
- `pom.xml` - config, xml
- `run-trivy.sh` - script, shell
