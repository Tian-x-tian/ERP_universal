# SaaS Catalog, Domain, Subscription, and Lifecycle Plan

> **For agentic workers:** REQUIRED SUB-SKILLS: `superpowers:subagent-driven-development`, `superpowers:test-driven-development`, and `schema-migration`. Never connect to `192.168.0.22`.

**Goal:** Persist the control-plane tenant/catalog/domain/subscription/deployment model and implement deterministic domain resolution plus the full non-purge lifecycle through `ARCHIVED`.

**Architecture:** `erp_saas_control` is a cross-tenant catalog database and never uses request-level tenant interception. All service time comes from injected UTC `Clock`; state transitions lock the tenant and current subscription in one transaction. Database uniqueness protects host ownership, active plan versions, and current subscriptions. This batch has no public management controllers, snapshot signing, provisioning orchestration, purge, or legacy import.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL 8.0.17, Spring transactions, JUnit 5, Mockito.

---

### Task 1: Add the catalog/lifecycle schema idempotently

**Files:**
- Modify: `erp-modules/erp-saas-control/src/main/resources/sql/init_control.sql`
- Create: `erp-modules/erp-saas-control/src/main/resources/sql/upgrade/control/20260801_02_saas_catalog_lifecycle.sql`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasCatalogSqlMigrationIntegrationTest.java`

- [ ] Add a test that runs init and the new upgrade twice through JDBC + Spring `ScriptUtils` and asserts exact columns/indexes/generated columns for all ten tables below. Expected RED: tables absent.
- [ ] Create `saas_tenant`: `id BIGINT`, unique `tenant_id VARCHAR(20)`, unique `slug VARCHAR(64)`, `tenant_name VARCHAR(128)`, `lifecycle_state VARCHAR(32)`, nullable `suspended_from_state`, `archived_at`, `purge_eligible_at`, `purged_at`, audit fields, and optimistic `version_no`; index `(lifecycle_state, update_time)`.
- [ ] Create `saas_domain`: `domain_id`, `tenant_id`, normalized `host VARCHAR(253)`, `verification_state`, `verification_method`, nullable SHA-256 `verification_token_hash CHAR(64)`, `verified_at`, `revoked_at`, generated `owned_host` equal to host unless state is REVOKED, audit fields, `version_no`; unique `owned_host`, index `host`, and index `(tenant_id, verification_state)`. This preserves revoked ownership history while allowing an explicit transactional transfer to create a new current owner.
- [ ] Create global product tables: `saas_plan(plan_id, plan_code, plan_version, plan_name, status, trial_days default 14, grace_days default 7, description, active_slot generated as plan_code only when ACTIVE, audit/version)` with unique `(plan_code, plan_version)` and unique `active_slot`; `saas_feature(feature_id, feature_key unique, feature_name, status, description, audit/version)`; `saas_plan_feature(plan_feature_id, plan_id, feature_id, granted, audit)` unique `(plan_id, feature_id)`; and `saas_plan_quota(plan_quota_id, plan_id, quota_key, limit_value nullable, period_type, audit)` unique `(plan_id, quota_key)`.
- [ ] Create tenant-specific tables: `saas_subscription(subscription_id, tenant_id, plan_id, state, start_at, nullable end_at, nullable grace_end_at, non_expiring, current_slot generated as tenant_id only for TRIAL/ACTIVE/GRACE, audit/version)` with unique `current_slot` and lifecycle-time index; non-expiring requires both end fields null, while expiring requires both and `grace_end_at >= end_at`; feature/quota override tables unique by `(tenant_id, feature_id)` and `(tenant_id, quota_key)` with effective window/reason; and `saas_deployment(deployment_id, tenant_id unique, mode, status, deployment_ref, secret_ref, audit/version)`.
- [ ] All mutable tables use `DATETIME(3)` UTC-compatible timestamps, `utf8mb4`, logical references (no partial physical FK strategy), and deterministic English comments. `limit_value IS NULL` alone means unlimited. No table stores secret material, activation token/URL, password, token, or JDBC connection string.
- [ ] Append the exact same DDL to init SQL. The upgrade script uses `CREATE TABLE IF NOT EXISTS` and must not silently repair an incompatible existing table; the integration test is the compatibility guard.

### Task 2: Add precise entities, enums, and mappers

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/domain/SaasMutableEntity.java`
- Create enums: `PlanStatus.java`, `FeatureStatus.java`, `DomainVerificationState.java`, `DomainVerificationMethod.java`, `QuotaPeriodType.java`, `FeatureOverrideState.java`, `DeploymentStatus.java`
- Create entities: `SaasTenantEntity.java`, `SaasDomainEntity.java`, `SaasPlanEntity.java`, `SaasFeatureEntity.java`, `SaasPlanFeatureEntity.java`, `SaasPlanQuotaEntity.java`, `SaasSubscriptionEntity.java`, `SaasTenantFeatureOverrideEntity.java`, `SaasTenantQuotaOverrideEntity.java`, `SaasDeploymentEntity.java`
- Create mapper interfaces with matching names under `com.erp.saas.control.mapper`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/domain/SaasControlPersistenceModelTest.java`

- [ ] Reflection tests freeze every `@TableName`, primary key, Java type, enum value, `@Version` field, and generated-column exclusion. Expected RED: types absent.
- [ ] Reuse contract enums `TenantLifecycleState`, `DeploymentMode`, and `SubscriptionState`; do not create duplicate lifecycle enums in the service.
- [ ] Mappers extend `BaseMapper<T>`. Add explicit lock methods with `@Select(... FOR UPDATE)` for tenant by tenant ID, current subscription, and usage-independent transition queries. Cross-tenant reads are intentional only inside control services.
- [ ] Add mapper SQL inspection tests proving logical identifiers use bound parameters and no request `TenantContextHolder` participates.

### Task 3: Implement plan catalog and exact effective overrides

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/SaasPlanCatalogService.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/impl/SaasPlanCatalogServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasPlanCatalogServiceTest.java`

- [ ] Tests cover one active version per plan code, immutable published versions, null quota as unlimited, duplicate feature/quota rejection, effective time windows, and customer grant/deny/quota overrides. Expected RED: service absent.
- [ ] Effective feature starts from plan grant and then applies the currently effective tenant override (`GRANT` or `DENY`); final user permission intersection remains a later runtime/RBAC task.
- [ ] Effective quota starts from the plan limit and applies the current tenant override; null remains unlimited and zero remains no allocation. Unknown feature/quota keys fail closed.
- [ ] Publishing a plan version retires the prior ACTIVE version under a transaction and relies on the generated unique slot as the final concurrency guard.

### Task 4: Implement canonical domain ownership and verification

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/SaasDomainService.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/impl/SaasDomainServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/domain/DomainVerificationChallenge.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasDomainServiceTest.java`

- [ ] Normalize host by trim, lower-case `Locale.ROOT`, valid port removal, one trailing-dot removal, and `IDN.toASCII`; reject scheme, userinfo, path/query/fragment, control characters, wildcard, invalid labels, and length above 253. Exact hosts only.
- [ ] Creating a custom domain stores `PENDING` plus SHA-256 of a 32-byte `SecureRandom` token and returns the raw token only in `DomainVerificationChallenge`; never log or persist the raw token.
- [ ] Verification uses constant-time hash comparison and moves `PENDING -> VERIFIED`; revoke moves `VERIFIED -> REVOKED`. A host cannot transfer by updating tenant ID: revoke old ownership and create a new record in one explicit service flow.
- [ ] Resolve returns only VERIFIED domains whose tenant is not `ARCHIVED`, `PURGE_PENDING`, or `PURGED`. Unknown, pending, revoked, and archived all produce the same not-found result. `SUSPENDED` and `READ_ONLY` still resolve.
- [ ] Tests cover IDN/case/port/trailing dot uniqueness, duplicate cross-tenant ownership, wrong token, same not-found semantics, archived rejection, and token non-disclosure.

### Task 5: Implement subscription-driven lifecycle with an injected clock

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/TenantLifecycleService.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/impl/TenantLifecycleServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/domain/TenantSubscriptionStateMapper.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/TenantLifecycleServiceTest.java`

- [ ] Freeze mapping: no subscription for `DRAFT`, `PROVISIONING`, or `PROVISION_FAILED`; lifecycle/subscription map `TRIAL/TRIAL`, `ACTIVE/ACTIVE`, `GRACE/GRACE`, and expiration `READ_ONLY/EXPIRED`; archive cancels the subscription. `SUSPENDED` preserves subscription state and `suspended_from_state`, while restore recomputes current lifecycle from subscription dates.
- [ ] Under one transaction and row locks implement: `DRAFT -> PROVISIONING`; success creates a 14-day-by-plan trial and enters TRIAL; failure enters PROVISION_FAILED; explicit retry returns to PROVISIONING; TRIAL/ACTIVE enter GRACE exactly at `end_at`; GRACE enters READ_ONLY exactly at `grace_end_at`; renewal enters ACTIVE; explicit suspend/restore; archive sets `archived_at` and `purge_eligible_at = archived_at + 90 days`.
- [ ] Every operation is idempotent by expected current state and optimistic version; illegal transitions throw one domain exception. No direct `Instant.now()`/`LocalDateTime.now()` is allowed outside the injected UTC Clock.
- [ ] Tests cover 14-day trial, 7-day grace, exact boundaries, read-only renewal, provision retry, suspend/restore after time advances, 90-day archive eligibility, illegal transitions, duplicate requests, and concurrent version conflict.

### Task 6: Register deployments without secret material

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/SaasDeploymentService.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/impl/SaasDeploymentServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasDeploymentServiceTest.java`

- [ ] Shared and dedicated modes use the same model. One tenant has one current deployment; dedicated mode still does not disable tenant isolation in business services.
- [ ] Persist only `deployment_ref` and `secret_ref`. Reject values shaped like JDBC URLs, database passwords, bearer/JWT tokens, or complete connection strings; logs and exceptions mention only tenant/deployment identifiers.
- [ ] Tests cover idempotent registration, mode mismatch conflict, health status transitions, duplicate tenant concurrency, and secret-pattern rejection/redaction.

### Task 7: Verify and commit

- [ ] Run focused service/model tests and `mvn -pl erp-modules/erp-saas-control -am test`.
- [ ] Start the isolated MySQL 8.0.17 loopback instance using the already-approved validated temp-directory procedure. Run `SaasCatalogSqlMigrationIntegrationTest` with `1 run / 0 skipped / 0 failures`, execute init/upgrade twice, and verify `information_schema` columns, generated columns, and unique indexes.
- [ ] Shut down by exact PID, remove only the validated temp directory, and confirm no `mysqld` remains. Never connect to `192.168.0.22`.
- [ ] Run root `mvn test`, `git diff --check`, inspect status, and commit only this task as `feat: add SaaS catalog and lifecycle core`.
