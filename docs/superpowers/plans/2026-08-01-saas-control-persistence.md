# SaaS Control Persistence Plan

> **For agentic workers:** REQUIRED SUB-SKILLS: `superpowers:subagent-driven-development`, `superpowers:test-driven-development`, and `schema-migration`. Never connect to `192.168.0.22`.

**Goal:** Add the exact control-plane catalog persistence model, MyBatis mappings, and a fail-fast compatibility validator without implementing catalog, domain, subscription, or deployment behavior.

**Architecture:** `erp_saas_control` is a cross-tenant control database and never installs the tenant interceptor. The upgrade runner creates only additive tables. A post-upgrade validator compares `information_schema` against an immutable manifest and fails startup when a same-named legacy table has incompatible columns, indexes, or generated expressions. Concurrency uses explicit mapper CAS (`WHERE version_no = ?`); the control service keeps its pagination-only MyBatis configuration and does not use `@Version`.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL 8.0.17, Spring JDBC, JUnit 5.

---

### Task 1: Freeze the persistence contract in tests

**Files:**
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/domain/SaasControlPersistenceContractTest.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlCatalogSchemaManifestTest.java`
- Modify: `erp-modules/erp-saas-control/pom.xml`

- [ ] Add the existing `erp-saas-contract` module as a compile dependency with `<version>${project.version}</version>` because the root dependency management does not manage this module.
- [ ] Write reflection tests for the ten exact `@TableName` mappings, `Long` ASSIGN_ID primary keys, every Java field type, enum value, and generated-column `@TableField` strategies. Expected RED: model types do not exist.
- [ ] Freeze reusable contract enums `TenantLifecycleState`, `DeploymentMode`, and `SubscriptionState`; service-local enums are exactly: `PlanStatus(DRAFT, ACTIVE, RETIRED)`, `FeatureStatus(ACTIVE, INACTIVE)`, `DomainVerificationState(PENDING, VERIFIED, REVOKED)`, `DomainVerificationMethod(PLATFORM_MANUAL)`, `QuotaPeriodType(CURRENT, MONTHLY)`, `FeatureOverrideState(GRANT, DENY)`, and `DeploymentStatus(REGISTERED, HEALTHY, UNHEALTHY, DISABLED)`.
- [ ] Write manifest tests that enumerate every required table, column type/nullability/default, primary/unique/index definition, and generated expression. Expected RED: manifest does not exist.

### Task 2: Add the exact idempotent schema

**Files:**
- Modify: `erp-modules/erp-saas-control/src/main/resources/sql/init_control.sql`
- Create: `erp-modules/erp-saas-control/src/main/resources/sql/upgrade/control/20260801_02_saas_control_catalog.sql`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlCatalogSqlMigrationIntegrationTest.java`

- [ ] Every table uses `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`; identifiers are `BIGINT NOT NULL`; audit columns are `create_by/update_by VARCHAR(64) NOT NULL`, `create_time/update_time DATETIME(3) NOT NULL`; mutable aggregate roots use `version_no BIGINT NOT NULL DEFAULT 0`. Unless explicitly marked nullable/defaulted below, every business column is `NOT NULL` with no default. Timestamps are stored as UTC by application convention. No physical foreign keys are added.
- [ ] Create `saas_tenant`: `id`, required `tenant_id VARCHAR(20)`, `slug VARCHAR(64)`, `tenant_name VARCHAR(128)`, `lifecycle_state VARCHAR(32)`, nullable/default NULL `suspended_from_state VARCHAR(32)`, `archived_at`, `purge_eligible_at`, `purged_at` as `DATETIME(3)`, audit/version; uniques `uk_saas_tenant_tenant_id(tenant_id)` and `uk_saas_tenant_slug(slug)`; index `idx_saas_tenant_state_update(lifecycle_state, update_time)`.
- [ ] Create `saas_domain`: `domain_id`, required `tenant_id VARCHAR(20)`, `host VARCHAR(253)`, `verification_state VARCHAR(32)`, `verification_method VARCHAR(32)`, nullable/default NULL `verified_at/revoked_at DATETIME(3)`, generated nullable `owned_host VARCHAR(253) AS (CASE WHEN verification_state <> 'REVOKED' THEN host ELSE NULL END) STORED`, audit/version; unique `uk_saas_domain_owned_host(owned_host)`; indexes `idx_saas_domain_host(host)` and `idx_saas_domain_tenant_state(tenant_id, verification_state)`. No token/challenge secret is persisted in this manual-verification slice.
- [ ] Create `saas_plan`: `plan_id`, required `plan_code VARCHAR(64)`, `plan_version INT`, `plan_name VARCHAR(128)`, `status VARCHAR(32)`, `trial_days INT NOT NULL DEFAULT 14`, `grace_days INT NOT NULL DEFAULT 7`, nullable/default NULL `description VARCHAR(512)`, generated nullable `active_slot VARCHAR(64) AS (CASE WHEN status = 'ACTIVE' THEN plan_code ELSE NULL END) STORED`, audit/version; uniques `uk_saas_plan_code_version(plan_code, plan_version)` and `uk_saas_plan_active_slot(active_slot)`; named check `ck_saas_plan_version` requires `1..2147483647`, and `ck_saas_plan_trial_days` / `ck_saas_plan_grace_days` require `0..3650`.
- [ ] Create `saas_feature`: `feature_id`, required `feature_key VARCHAR(128)`, `feature_name VARCHAR(128)`, `status VARCHAR(32)`, nullable/default NULL `description VARCHAR(512)`, audit/version; unique `uk_saas_feature_key(feature_key)`.
- [ ] Create `saas_plan_feature`: `plan_feature_id`, required `plan_id`, `feature_id`, `granted TINYINT(1) NOT NULL DEFAULT 1`, audit; unique `uk_saas_plan_feature(plan_id, feature_id)`, index `idx_saas_plan_feature_feature(feature_id)`, and named check `ck_saas_plan_feature_granted` constraining the flag to 0/1.
- [ ] Create `saas_plan_quota`: `plan_quota_id`, required `plan_id`, `quota_key VARCHAR(64)`, nullable/default NULL signed `limit_value BIGINT` where NULL alone means unlimited, required `period_type VARCHAR(32)`, audit; unique `uk_saas_plan_quota(plan_id, quota_key)`; named check `ck_saas_plan_quota_limit` requires NULL or `limit_value >= 0`.
- [ ] Create `saas_subscription`: `subscription_id`, required `tenant_id VARCHAR(20)`, `plan_id`, `state VARCHAR(32)`, `start_at DATETIME(3)`, nullable/default NULL `end_at/grace_end_at DATETIME(3)`, `non_expiring TINYINT(1) NOT NULL DEFAULT 0`, generated nullable `current_slot VARCHAR(20) AS (CASE WHEN state IN ('TRIAL','ACTIVE','GRACE') THEN tenant_id ELSE NULL END) STORED`, audit/version; unique `uk_saas_subscription_current_slot(current_slot)`; indexes `idx_saas_subscription_tenant_state(tenant_id, state)` and `idx_saas_subscription_lifecycle_time(state, end_at, grace_end_at)`; named checks `ck_saas_subscription_non_expiring` requires flag 0/1 and `ck_saas_subscription_dates` requires both end fields NULL for non-expiring, otherwise both non-NULL with `grace_end_at >= end_at`.
- [ ] Create `saas_tenant_feature_override`: `override_id`, required `tenant_id VARCHAR(20)`, `feature_id`, `override_state VARCHAR(32)`, `effective_from DATETIME(3)`, nullable/default NULL `effective_until DATETIME(3)`, nullable/default NULL `reason VARCHAR(512)`, audit/version; unique `uk_saas_tenant_feature_window(tenant_id, feature_id, effective_from)`; index `idx_saas_tenant_feature_effective(tenant_id, effective_from, effective_until)`; named check `ck_saas_tenant_feature_window` requires end greater than start. Windows are `[effective_from, effective_until)` and history is retained.
- [ ] Create `saas_tenant_quota_override`: `override_id`, required `tenant_id VARCHAR(20)`, `quota_key VARCHAR(64)`, nullable/default NULL signed `limit_value BIGINT`, required `effective_from DATETIME(3)`, nullable/default NULL `effective_until DATETIME(3)`, nullable/default NULL `reason VARCHAR(512)`, audit/version; unique `uk_saas_tenant_quota_window(tenant_id, quota_key, effective_from)`; index `idx_saas_tenant_quota_effective(tenant_id, effective_from, effective_until)`; named check `ck_saas_tenant_quota_window` requires end greater than start and named check `ck_saas_tenant_quota_limit` requires NULL or `limit_value >= 0`. NULL limit alone means unlimited.
- [ ] Create `saas_deployment`: `deployment_id`, required `tenant_id VARCHAR(20)`, `mode VARCHAR(32)`, `status VARCHAR(32)`, `deployment_ref VARCHAR(255)`, nullable/default NULL `secret_ref VARCHAR(255)`, audit/version; unique `uk_saas_deployment_tenant(tenant_id)`. `secret_ref` is only a reference; provider grammar is enforced in a later service slice. No JDBC URL, password, token, certificate, or connection string column exists.
- [ ] Append byte-equivalent table DDL to `init_control.sql`; the upgrade remains `CREATE TABLE IF NOT EXISTS` and idempotent. The integration test executes init and upgrade twice with Spring `ScriptUtils`, then asserts exact `information_schema` columns/indexes/generated expressions.

### Task 3: Add fail-fast schema compatibility validation

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/SaasControlCatalogSchemaManifest.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/SaasControlCatalogSchemaValidator.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/SaasControlCatalogSchemaValidationRunner.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlCatalogSchemaValidatorTest.java`

- [ ] The immutable manifest freezes every column type, nullability, default, generation expression and collation; every named primary/unique/secondary index in order; every named CHECK expression; and table engine/collation. Normalize harmless expression whitespace, quoting, and case only; do not accept a semantically different expression.
- [ ] Validator reads `information_schema.COLUMNS`, `STATISTICS`, `TABLES`, `TABLE_CONSTRAINTS`, and `CHECK_CONSTRAINTS` for `DATABASE()`. Any missing/unexpected table column/index/check, wrong default/type/nullability/generation/collation, incompatible index order, engine, or table collation throws `IllegalStateException` naming only the table and structural mismatch.
- [ ] Register the validator only when `erp.saas.schema-validation.enabled=true` or missing, and run it at order 200 after the SQL upgrade runner at order 100. Unit tests assert enable/disable, ordering, and exact pass/fail behavior.
- [ ] Add real-MySQL integration negative phases for wrong column default, missing CHECK, wrong generated expression, wrong engine, and wrong collation; each first creates an intentionally incompatible table, runs the idempotent upgrade, and proves validation fails. Clean up only after the existing loopback/test-schema/datadir safety guard succeeds.

### Task 4: Add precise entities and registered mappers

**Files:**
- Create enums under `com.erp.saas.control.domain`
- Create ten entity classes under `com.erp.saas.control.domain.entity`
- Create ten mapper interfaces under `com.erp.saas.control.mapper`
- Modify: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/SaasControlApplication.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/mapper/SaasControlMapperRegistrationTest.java`
- Modify: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/SaasControlApplicationTest.java`
- Modify: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlSecurityConfigTest.java`

- [ ] Every entity uses these exact Java mappings: all primary/foreign IDs and `versionNo/limitValue` are `Long`; `planVersion/trialDays/graceDays` are `Integer`; flags are `Boolean`; `DATETIME(3)` fields are `LocalDateTime`; enum columns use the exact enum type frozen in Task 1; all remaining text is `String`. Audit fields are `String createBy/updateBy` and `LocalDateTime createTime/updateTime`.
- [ ] Each primary key is explicit: `SaasTenantEntity.id -> id`; domain/plan/feature/plan-feature/plan-quota/subscription/deployment use `domain_id/plan_id/feature_id/plan_feature_id/plan_quota_id/subscription_id/deployment_id`; both override entities use `override_id`. Every key uses `@TableId(value = "<exact_column>", type = IdType.ASSIGN_ID)`. All snake-case non-key columns use explicit `@TableField("<exact_column>")` so the reflection contract does not depend on global naming configuration. Generated `ownedHost`, `activeSlot`, and `currentSlot` remain selectable but use `insertStrategy = NEVER` and `updateStrategy = NEVER`.
- [ ] `versionNo` is a plain mapped `Long`, not `@Version`; later services must call explicit mapper CAS updates with `WHERE version_no = #{expectedVersion}` and increment in SQL.
- [ ] Each mapper extends `BaseMapper<T>` and is registered by a narrow `@MapperScan("com.erp.saas.control.mapper")`. No mapper reads `TenantContextHolder` and no tenant interceptor is installed.
- [ ] Existing application/security context tests explicitly set `erp.saas.schema-validation.enabled=false`. Mapper registration starts a context with mocked DataSource and disables Nacos, SQL upgrade, and schema validation, then proves all ten mapper beans exist. SQL/reflection tests prove generated columns are never inserted or updated.

### Task 5: Verify and commit this slice

- [ ] Run focused contract/manifest/validator/mapper tests and `mvn -pl erp-modules/erp-saas-control -am test` serially.
- [ ] The MySQL integration test is opt-in and skips unless all three existing variables `ERP_SAAS_TEST_JDBC_URL`, `ERP_SAAS_TEST_DB_USER`, and `ERP_SAAS_TEST_DB_PASSWORD` exist. In test code and before any DROP, parse the JDBC URL and require a loopback host, then query and require `DATABASE() = 'erp_saas_control_test'` and `SELECT VERSION()` beginning with `8.0.17`; failure aborts without mutation. Before each phase and final exit it drops all ten catalog tables in dependency-safe order. The external start/stop procedure separately validates the datadir and exact PID before cleanup.
- [ ] Start only the isolated loopback MySQL 8.0.17 under the worktree-safe temp directory. Run the migration integration test with `1 run / 0 skipped / 0 failures`, validate init/upgrade twice and all incompatible-schema failures, then stop the exact PID and remove only that validated temp directory. Never connect to `192.168.0.22`.
- [ ] Run `git diff --check`, inspect status, and commit only code/SQL/tests for this slice as `feat: add SaaS control persistence model`. Root full regression remains the final batch gate.
