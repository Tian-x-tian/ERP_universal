# ERP Hybrid SaaS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a testable hybrid SaaS foundation in which shared and dedicated ERP deployments use the same artifacts, a central control plane owns tenants and entitlements, and every runtime enforces tenant isolation, lifecycle, feature access, and quotas.

**Architecture:** Add `erp-saas-control` with a separate MySQL schema plus POJO contracts and a typed internal client. Runtime services keep signed local entitlement snapshots and enforce them independently; the gateway resolves tenant identity from trusted host mappings. Existing tenants receive an unlimited `legacy-full-access` subscription during migration.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Cloud Gateway/Nacos, MyBatis-Plus, MySQL 8, Spring `ScriptUtils`, JUnit 5/Mockito, Vue 3/TypeScript/Element Plus, Docker Compose.

---

### Task 1: Fail-closed tenant isolation

**Files:**
- Modify: `erp-common/src/main/java/com/erp/common/mybatis/TenantMybatisPlusConfigurationSupport.java`
- Create: `erp-common/src/main/java/com/erp/common/mybatis/TenantSchemaValidator.java`
- Create: `erp-common/src/test/java/com/erp/common/mybatis/TenantMybatisPlusConfigurationSupportTest.java`
- Create: `erp-common/src/test/java/com/erp/common/mybatis/TenantSchemaValidatorTest.java`
- Modify: `erp-modules/erp-{system,business,workflow}/src/main/java/**/config/MyBatisPlusConfig.java`
- Modify: `SystemSqlUpgradeRunner`, `BusinessSchemaUpgradeRunner`, and `WorkflowSqlUpgradeRunner` ordering
- Modify: each affected module's test configuration to disable startup validation only for unrelated Spring context tests

- [ ] Add `shouldIgnoreOnlyExplicitGlobalTables`, `shouldApplyTenantRuleToUnknownTableWithoutMetadataLookup`, `shouldRejectOwnedTableWithoutTenantColumn`, `shouldAcceptTenantTableAndGlobalTable`, and `shouldReportEveryInvalidTable` tests. Expected RED is current `ignoreTable` returning `true` for the unknown table and the missing `TenantSchemaValidator` type.
- [ ] Run `mvn -pl erp-common -Dtest=TenantMybatisPlusConfigurationSupportTest,TenantSchemaValidatorTest test` and verify RED.
- [ ] Replace metadata-based fail-open behavior with a normalized explicit allowlist: `sys_tenant`, `sys_menu`, `sys_dict_type`, `sys_dict_data`, `sys_config`, `sys_sql_upgrade_log`, and `biz_sql_upgrade_log`. Any other table is tenant-scoped even when metadata lookup fails.
- [ ] Make `TenantSchemaValidator` scan every `BASE TABLE` in the current schema after upgrades, reject every non-allowlisted table lacking `tenant_id`, and include all offending names in one exception. Init-SQL evidence shows all current business tables are compliant; only the seven explicit global/infrastructure tables are exempt.
- [ ] Order SQL runners at `100` and the validator at `200`. Gate validation with `erp.tenant.schema-validation.enabled` defaulting to `true`; disable it only in existing unrelated test contexts, not in the validator's own integration test.
- [ ] Re-run focused tests and `mvn -pl erp-modules/erp-system,erp-modules/erp-business,erp-modules/erp-workflow -am test`.
- [ ] Commit `security: enforce fail-closed tenant schema`.

### Task 2: SaaS contracts and typed client

**Files:**
- Modify: `pom.xml`, `erp-modules/pom.xml`
- Create: `erp-common/src/main/java/com/erp/common/security/TenantAssertionHeaders.java`
- Create: `erp-common/src/main/java/com/erp/common/security/ResolvedTenantAssertion.java`
- Create: `erp-common/src/main/java/com/erp/common/security/ResolvedTenantAssertionSignatureUtils.java`
- Create: `erp-common/src/test/java/com/erp/common/security/ResolvedTenantAssertionSignatureUtilsTest.java`
- Create: `erp-modules/erp-saas-contract/**`
- Create: `erp-modules/erp-saas-client/**`
- Test: `erp-modules/erp-saas-client/src/test/java/com/erp/saas/client/InternalSaasClientTest.java`

- [ ] Add failing tests named `shouldSignAndVerifyResolvedTenantAssertion`, `shouldRejectModifiedHost`, `shouldRejectExpiredAssertion`, and `shouldRejectFutureAssertion`. The assertion payload is separate from authenticated `AuthHeaders` and consists of normalized `tenantId`, lowercase host without port/trailing dot, uppercase HTTP method, normalized path, `issuedAt`, and a cryptographically random nonce.
- [ ] Implement HMAC-SHA256 signing with constant-time comparison and a default 30-second clock-skew/age window. This task verifies integrity and freshness only; Task 5 consumes each nonce once through Redis.
- [ ] Write failing client routing tests `shouldResolveTenantByHost`, `shouldLoadEntitlementSnapshot`, `shouldReportUsageWithIdempotencyKey`, and `shouldReportProvisioningResult`, verifying existing signed service-principal headers.
- [ ] Define enums and DTOs for lifecycle state, deployment mode, subscription state, feature grants, quota limits/usages, signed entitlement snapshots, host resolution, and idempotent provisioning requests.
- [ ] Implement `InternalSaasClient` using the existing internal client/header factory patterns; never expose persistence entities across modules.
- [ ] Run `mvn -pl erp-modules/erp-saas-client -am test` and commit `feat: add SaaS contracts and internal client`.

### Task 3: Central control service and versioned schema

**Files:**
- Create: `erp-modules/erp-saas-control/**`
- Create: `erp-modules/erp-saas-control/src/main/resources/sql/init_control.sql`
- Create: `erp-modules/erp-saas-control/src/main/resources/sql/upgrade/control/20260801_01_saas_control_baseline.sql`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/**`
- Create: `erp-modules/erp-system/src/main/resources/sql/upgrade/system/20260801_01_saas_control_menu.sql`
- Modify: gateway route and Nacos example configuration

- [ ] Write failing lifecycle, effective-entitlement, idempotent-create, domain-uniqueness, snapshot-signing, and SQL-runner tests.
- [ ] Add the control service on port `9096`, its own datasource/schema configuration, existing internal-auth filter support, and protected `/saas/**` gateway route.
- [ ] Authorize every management endpoint by requiring authenticated tenant `000000` plus an exact `saas:*` permission verified through `InternalPlatformClient`; seed the SaaS management menu/permissions for the protected platform-admin role through init and date-versioned system SQL. Tenant ID alone is never sufficient authorization.
- [ ] Add normalized tables with stable keys: `saas_tenant(tenant_id, slug)`, `saas_domain(host)`, `saas_plan(plan_code, version)`, `saas_feature(feature_key)`, `saas_plan_feature(plan_id, feature_id)`, `saas_plan_quota(plan_id, quota_key)`, `saas_subscription(tenant_id, start/end/grace)`, separate feature/quota override tables, `saas_deployment(tenant_id, mode, secret_ref)`, `saas_usage_event(event_key)`, `saas_usage_summary(tenant_id, metric_key, period_start)`, `saas_entitlement_snapshot(tenant_id, version)`, and `saas_provision_job(request_id)`. Natural/idempotency keys are unique; store secret references only.
- [ ] Expose management APIs under `/saas/tenants`, `/saas/plans`, `/saas/subscriptions`, `/saas/domains`, `/saas/deployments`, and `/saas/usage`; expose signed internal APIs for host resolution, entitlement snapshots, idempotent usage events, legacy import, and provisioning result callbacks under `/internal/saas/**`.
- [ ] Implement the exact lifecycle transitions, 14-day default trial, 7-day grace, 90-day archive retention, configurable plan overrides, immutable usage idempotency keys, and signed versioned snapshots.
- [ ] Add an idempotent legacy importer that reads current `sys_tenant` records through an existing signed internal system API and creates tenant records plus unlimited/non-expiring `legacy-full-access` subscriptions. Missing domain mappings remain pending and cannot be host-resolved until an administrator verifies a domain.
- [ ] Implement a `ScriptUtils` runner with `saas_sql_upgrade_log`; keep init and upgrade definitions aligned and scripts rerunnable. Modify `erp-modules/pom.xml` in this task—not Task 2—to add the now-existing `erp-saas-control` module to the reactor.
- [ ] Start an isolated MySQL 8.0.17 instance from `D:/software/mysql-8.0.17-winx64/bin/mysqld.exe` with its data directory under the worktree temp area. Through a JDBC + `ScriptUtils` integration harness, execute the same upgrade script directly twice (bypassing upgrade-log skip), compare `information_schema.COLUMNS` before/after, verify backfills with `<=>`, then let the runner record `saas_sql_upgrade_log`. Never use `192.168.0.22`.
- [ ] Run focused service tests, the isolated-MySQL integration test, compile the full reactor, and commit `feat: add SaaS control plane`.

### Task 4: Idempotent tenant bootstrap and local entitlement snapshot

**Files:**
- Modify: `erp-modules/erp-system` tenant/domain/service/controller code
- Modify: `erp-modules/erp-platform-contract`, `erp-modules/erp-platform-client`
- Modify: `erp-modules/erp-system/src/main/resources/sql/init_system.sql`
- Create: `erp-modules/erp-system/src/main/resources/sql/upgrade/system/20260801_02_saas_runtime.sql`

- [ ] Write failing tests for retry-safe bootstrap, default company/root department/admin role/admin account, one-time activation token, local snapshot replacement by increasing version only, and legacy migration.
- [ ] Add internal endpoints for bootstrap, entitlement sync, and quota reserve/settle/release; guard them with existing signed internal authentication.
- [ ] Persist local entitlement snapshots and quota counters. Existing tenants receive `legacy-full-access` without expiry or finite quotas.
- [ ] Generate the activation credential from 32 cryptographically random bytes, store only its SHA-256 hash, default expiry to configurable 24 hours, return the activation URL only in the successful provisioning result, never log it, and atomically mark it consumed when the administrator sets a compliant password. Expired/replayed tokens fail without revealing account existence.
- [ ] Provision through a retryable control-plane job; use an operation ID and database uniqueness to prevent duplicates after partial failure.
- [ ] When `erp.saas.mode` is `SHARED` or `DEDICATED`, make the control plane the only tenant write entry and reject legacy `/system/tenant` mutations; keep read compatibility. Default existing deployments to explicit `LEGACY` mode with a startup warning until import/domain checks pass.
- [ ] Add `feature_key` to `sys_menu`, update initialization SQL, and verify the date-versioned migration twice through `ScriptUtils` without touching `192.168.0.22`.
- [ ] Run system/client tests and commit `feat: provision SaaS tenants idempotently`.

### Task 5: Trusted host-based tenant resolution

**Files:**
- Modify: `erp-gateway/src/main/java/com/erp/gateway/filter/GatewayAuthFilter.java` and related configuration
- Modify: `erp-auth/src/main/java/com/erp/auth/controller/LoginController.java`
- Create: gateway `TenantResolutionFilter`, resolver/cache properties, and dedicated-mode resolver
- Create: auth `ResolvedTenantAssertionVerifier` and Redis-backed single-use nonce store
- Test: gateway/auth filter and controller tests

- [ ] Add failing tests `shouldStripAllExternalTenantAssertionHeaders`, `shouldResolveVerifiedHost`, `shouldRejectUnknownOrUnverifiedHost`, `shouldRejectTokenTenantHostMismatch`, `shouldRejectUnsignedLoginTenant`, `shouldRejectReplayedNonce`, and `shouldAcceptFreshSingleUseAssertion`.
- [ ] Add an earlier tenant-resolution filter. Shared mode uses cached control-plane host mappings; dedicated mode uses a configured fixed tenant/domain.
- [ ] Use the independent Task 2 resolved-tenant assertion; do not change authenticated `AuthHeaders` or `InternalAuthSignatureUtils`. Gateway signs method/path/host/tenant/time/nonce, and auth atomically consumes the nonce with Redis `SET NX` and a 60-second TTL.
- [ ] Define `erp.saas.mode=LEGACY|SHARED|DEDICATED`. `LEGACY` preserves the current header only during migration and logs a warning; `SHARED` and `DEDICATED` always strip it and trust only assertions. Cutover requires imported tenants and verified domains before switching out of `LEGACY`.
- [ ] Run gateway/auth tests plus full Maven tests and commit `security: trust tenant identity from gateway host resolution`.

### Task 6: Feature entitlement and lifecycle enforcement

**Files:**
- Modify: common servlet/security support and each runtime security configuration
- Modify: system menu selection and platform authority contracts
- Add focused tests in common/system/business/workflow/ai

- [ ] Write failing tests proving effective access equals plan features intersected with RBAC, platform admin remains isolated to tenant `000000`, and `READ_ONLY` allows query/export but rejects business writes, approvals, uploads, AI, and mutating schedulers.
- [ ] Add stable feature annotations/guards backed by the local signed snapshot; filter menu trees by `feature_key` and enforce the same feature in backend endpoints.
- [ ] Refresh snapshots every five minutes. Permit signed stale snapshots for 24 hours; after that enter read-only until refresh succeeds.
- [ ] Default state-changing HTTP operations to writes, with an explicit reviewed allowlist/annotation for read-only POST exports and system audit writes.
- [ ] Apply the same lifecycle guard to `InventoryWarningScheduler`, `InventoryIntegrationRetryScheduler`, `BusinessTenantSchedulerSupport`, and `WorkflowSlaScheduler`; skip only the affected tenant's mutating action and preserve context cleanup/other-tenant execution.
- [ ] Run affected module tests and commit `feat: enforce SaaS lifecycle and feature access`.

### Task 7: Atomic user, storage, and AI quotas

**Files:**
- Modify: system user service and quota endpoints
- Modify: business employee-document/object-storage services and SQL
- Modify: AI model client/chat flow
- Add quota concurrency and compensation tests

- [ ] Write failing boundary/concurrency tests for active-user reservations, byte reservations, failed-upload release, duplicate usage events, AI token reservation, streaming completion, and streaming interruption.
- [ ] Count enabled/non-deleted tenant users; atomically reserve before create/enable and release on disable/delete.
- [ ] Add file size/object status metadata and a tenant object ledger. Reserve declared bytes before upload, settle actual bytes, release on failure, and expose orphan reconciliation.
- [ ] Meter monthly input plus output tokens. Extend `AiModelCompletion`/the stream result so `AiOpenAiCompatibleClient` returns the protocol `usage` object already requested via `stream_options.include_usage`; parse the terminal usage-only SSE frame, reserve before invocation, and settle on success, error, cancellation, or disconnect. Providers that omit usage fail the metered request explicitly rather than silently recording zero.
- [ ] Publish idempotent usage events asynchronously to the control plane while local counters remain authoritative for blocking.
- [ ] Update both init and date-versioned SQL, verify twice via `ScriptUtils`, run focused/full Maven tests, and commit `feat: enforce tenant resource quotas`.

### Task 8: Platform SaaS console

**Repository/worktree:** `D:/workspace/ERP/.worktrees/saas-hybrid-ui` on the frontend repository's separate `codex/saas-hybrid` branch. Never edit `D:/workspace/ERP/erp-ui`, which contains user changes.

**Files:**
- Create: `src/api/saas/**`
- Create: `src/views/platform/saas/**`
- Modify: router/menu/permission integration

- [ ] Add testable TypeScript helpers for lifecycle badges, quota presentation, and form payload mapping; add the repository's lightest viable test runner only if an existing approved test dependency is present, otherwise validate through typecheck/build.
- [ ] Add platform-admin pages for tenants, plans/features/quotas, subscriptions/overrides, domains, deployments, provisioning status, and usage.
- [ ] Hide platform routes outside the management host and for non-platform admins; backend authorization remains authoritative.
- [ ] Provide loading, empty, error, success, validation, duplicate-submit protection, and accessible labels/focus behavior using existing components/styles.
- [ ] Run `npm run build` and commit `feat: add SaaS platform console` in the frontend repository.

### Task 9: Dedicated deployment and end-to-end verification

**Files:**
- Create: service Dockerfiles/build template, `deploy/saas/docker-compose.dedicated.yml`, `.env.example`, health-check/runbook documentation
- Modify: project/Nacos configuration examples and architecture docs

- [ ] Write configuration/smoke checks proving images use the same artifacts in shared and dedicated modes and dedicated mode rejects a second tenant/domain.
- [ ] Add non-secret environment templates for MySQL 8, Redis, Nacos, gateway, auth, control-plane URL, fixed tenant/domain, and secret references.
- [ ] Document manual provisioning, domain/TLS registration, health validation, backup, upgrade order, rollback/roll-forward, and 90-day purge approval.
- [ ] Add an explicit purge-confirmation API requiring tenant state `ARCHIVED`, `archived_at <= now - 90 days`, platform tenant `000000`, the dedicated `saas:tenant:purge` permission, a fresh confirmation token, and a reason. Record an immutable audit event before enqueueing an idempotent deletion job; repeated confirmation returns the same job, and early/unauthorized attempts fail.
- [ ] Add tests for fewer than 90 days, missing permission, stale/replayed confirmation, repeated confirmation, partial deletion retry, successful tenant-data/object cleanup, and final `PURGED` tombstone retention.
- [ ] Run SQL runner tests, full `mvn test`, frontend `npm run build`, `git diff --check`, and compose config validation where Docker is available.
- [ ] Dispatch final spec and code-quality reviews; fix all findings before branch completion.

## Safety and rollout constraints

- Do not execute DDL/DML against `192.168.0.22` without a new explicit user approval.
- Use expand/migrate/contract; do not remove the legacy tenant header until the host-based frontend is deployed and verified.
- Preserve the dirty frontend `master` working tree; all frontend edits happen in its separate worktree.
- Do not merge or push until all relevant verification passes and branch-finishing instructions are followed.
