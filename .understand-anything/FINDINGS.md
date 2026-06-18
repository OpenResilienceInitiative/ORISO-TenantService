# Findings: ORISO-TenantService

## Generation Summary

- Generated from latest dev commit: `a777c69447609d16db33ae1a1b54fd4efe5366b6`
- Files analyzed: 177
- Category breakdown: {"config":24,"pipeline":3,"code":105,"docs":2,"infra":1,"script":5,"data":37}
- Graph nodes: 647
- Graph edges: 1238

## Notable Current-Code Changes Reflected

- Tenant admin controls are represented through `TenantAdminControlsEntity`, `TenantAdminControlsRepository`, `TenantAdminControlsService`, API additions, and tests.
- The `0013_tenant_admin_controls` Liquibase changeset and SQL migration are included in the data/persistence layer.
- Tenant converter, facade, and service updates are represented in the tenant lifecycle flow.
- Security, JWT authority mapping, and tenant context resolution remain explicit graph layers.

## Review Notes

- The graph was regenerated from the current repository only.
- Scratch directories `.understand-anything/intermediate/` and `.understand-anything/tmp/` were not created by this generator.
- Validate with `python3 -m json.tool .understand-anything/knowledge-graph.json > /dev/null`.
