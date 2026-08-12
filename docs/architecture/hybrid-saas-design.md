# Hybrid SaaS Architecture Decision

## Product decisions

- Standard tenants share the application and MySQL schema with `tenant_id` isolation.
- Enterprise tenants use dedicated application instances and databases while consuming the same artifacts and upgrade scripts.
- Sales or platform administrators provision tenants; public self-registration and online payment are out of scope.
- Plans are configurable and support tenant-specific feature/quota overrides.
- Default trial is 14 days. Expiry enters a 7-day grace period and then read-only mode.
- Read-only retains login, query, export, and renewal operations while rejecting business writes, approval, upload, AI, and mutating schedules.
- Archived data is retained for 90 days and requires explicit platform-admin confirmation before physical deletion.
- Quotas cover active users, object-storage bytes, and monthly input-plus-output AI tokens. Business-document counts are unlimited.
- Existing tenants migrate to an unlimited, non-expiring `legacy-full-access` plan.

## Architecture decisions

- A new central `erp-saas-control` service and separate `erp_saas_control` schema own tenant catalog, verified domains, plans, subscriptions, overrides, deployments, usage summaries, provisioning jobs, and signed entitlement snapshots.
- Existing platform identity remains tenant `000000` with role `admin`; no second identity store is introduced.
- Stable feature keys are independent from menu IDs. Effective authorization is the intersection of plan entitlement and tenant-local RBAC.
- Shared gateways resolve tenants from subdomains or verified custom domains. Dedicated deployments use a fixed configured tenant/domain. Client tenant headers are untrusted.
- Runtime services enforce local signed snapshots and local quota counters. The control plane aggregates idempotent usage events for operations/reporting.
- Snapshots refresh every five minutes. A valid cached snapshot may be used for 24 hours during control-plane loss; after that the runtime becomes read-only.
- Dedicated infrastructure is deployed manually from versioned container/configuration templates in the first release; one-click cloud/Kubernetes provisioning is out of scope.

## Migration and safety

- Tenant interception becomes fail-closed with an explicit global-table allowlist and startup schema validation.
- Database evolution follows expand/migrate/contract. Init SQL and date-versioned upgrade SQL remain aligned and are tested through Spring `ScriptUtils` twice for idempotence.
- No SQL is executed against the shared `192.168.0.22` database without a separate explicit approval.
- The current `erp-ui` working tree contains user changes and must remain untouched; frontend work uses a separate Git worktree.
