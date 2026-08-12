# Fail-Closed Tenant Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Follow superpowers:test-driven-development and commit only after focused and dependent-module tests pass.

**Goal:** Ensure an unknown application table can never bypass tenant filtering because it lacks `tenant_id`, and fail service startup when the current ERP schema contains an undeclared non-tenant table.

**Architecture:** MyBatis interception will consult only an explicit normalized global-table allowlist. A separate startup runner will query `information_schema` after module SQL upgrades and report all non-allowlisted base tables missing `tenant_id` in one failure.

**Tech Stack:** Java 17, MyBatis-Plus, Spring Boot `ApplicationRunner`, Spring JDBC, JUnit 5, Mockito.

---

### Task 1: Test and implement explicit interceptor behavior

**Files:**
- Create: `erp-common/src/test/java/com/erp/common/mybatis/TenantMybatisPlusConfigurationSupportTest.java`
- Create: `erp-common/src/main/java/com/erp/common/mybatis/FailClosedTenantLineInnerInterceptor.java`
- Modify: `erp-common/src/main/java/com/erp/common/mybatis/TenantMybatisPlusConfigurationSupport.java`

- [ ] Add tests `shouldIgnoreOnlyExplicitGlobalTables`, `shouldRejectQualifiedGlobalTableNames`, `shouldRejectQualifiedSelectThroughRealParser`, `shouldRejectQualifiedInsertThroughRealParser`, `shouldRejectQualifiedUpdateThroughRealParser`, `shouldRejectQualifiedDeleteThroughRealParser`, `shouldRejectQualifiedTableInJoinOnSubquery`, `shouldRejectQualifiedTableInNestedCteAndUnion`, `shouldRejectQualifiedTableInInsertSelectAndDuplicateUpdate`, `shouldRejectQualifiedTableInUpdateAndDeleteJoinOn`, `shouldAllowQuotedLocalTableNameContainingDot`, `shouldApplyTenantRuleToUnknownTableWithoutQueryingMetadata`, and `shouldNotIgnoreNullOrBlankTableNames`. Use a subclass exposing `buildTenantLineHandler()` and the created inner interceptor, plus a mocked `DataSource` whose connection must never be requested. Parser tests must call `parserSingle` with full SQL; directly passing a qualified string to `TenantLineHandler.ignoreTable` is not sufficient because MyBatis-Plus passes only `Table#getName()`.
- [ ] Run `mvn -pl erp-common -Dtest=TenantMybatisPlusConfigurationSupportTest test`. Expected RED: `buildTenantLineHandler()` does not exist; after adding only the seam, the unknown-table assertion fails because current code calls `information_schema` and returns ignore=true.
- [ ] Extract the existing anonymous handler into `buildTenantLineHandler()`. Make `ignoreTable` return `true` only for unqualified normalized members of `globalTableCandidates()`; null, empty, whitespace-only, schema-qualified names (including another schema with a same-name global table), and all unknown names return `false`. Remove the metadata lookup/cache and `hasTenantColumn`/`queryTenantColumn` methods.
- [ ] Implement `FailClosedTenantLineInnerInterceptor extends TenantLineInnerInterceptor` with a per-call custom JSqlParser AST visitor. Override `visit(Table)` and reject when `Table#getSchemaName()` or a nonblank database/server name is present; JSqlParser creates an empty `Database` object for local tables, so object non-null alone is not qualification. Do not infer qualification from a dot inside `Table#getName()`. Extend the complete traversal to JOIN `ON` expressions in `PlainSelect`, nested `SubJoin`, `Update` (both joins and start joins), and `Delete`, and to INSERT duplicate-update/set expressions; nested subqueries recurse through the same visitor. Before delegating each of `processSelect`, `processInsert`, `processUpdate`, and `processDelete`, run the visitor on the full statement. Use this class in `buildInterceptor()`. Cover nested SubJoin ON, CTE, UNION, derived/scalar subqueries, INSERT SELECT, ON DUPLICATE UPDATE, and UPDATE/DELETE JOIN. This rejects qualified current-schema and cross-schema SQL alike; no runtime JDBC lookup or shared mutable interceptor state is allowed.
- [ ] Re-run the focused test. Expected GREEN with no JDBC interaction.

### Task 2: Test and implement startup schema validation

**Files:**
- Create: `erp-common/src/main/java/com/erp/common/mybatis/TenantSchemaValidator.java`
- Create: `erp-common/src/main/java/com/erp/common/mybatis/TenantSchemaValidationRunner.java`
- Create: `erp-common/src/main/java/com/erp/common/mybatis/TenantSchemaReadinessGate.java`
- Create: `erp-common/src/main/java/com/erp/common/mybatis/TenantSchemaReadinessFilter.java`
- Create: `erp-common/src/test/java/com/erp/common/mybatis/TenantSchemaValidatorTest.java`
- Modify: `erp-modules/erp-system/src/main/java/com/erp/system/config/MyBatisPlusConfig.java`
- Modify: `erp-modules/erp-business/src/main/java/com/erp/business/config/MyBatisPlusConfig.java`
- Modify: `erp-modules/erp-workflow/src/main/java/com/erp/workflow/config/MyBatisPlusConfig.java`

- [ ] Add tests `shouldAcceptTenantTablesAndExplicitGlobalTables`, `shouldRejectEveryUnexpectedTableWithoutTenantColumn`, `shouldNormalizeMetadataTableNames`, `shouldExposeValidationOrderAfterUpgradeRunners`, `shouldKeepTrafficClosedUntilValidationSucceeds`, and `shouldRejectTrafficWhileSchemaIsUnvalidated`. Mock `JdbcTemplate.queryForList` to return the table names that lack `tenant_id`; assert one exception contains every non-allowlisted name.
- [ ] Run `mvn -pl erp-common -Dtest=TenantSchemaValidatorTest test`. Expected RED: validator/runner types do not exist.
- [ ] Implement `TenantSchemaValidator` with one read-only query over `information_schema.TABLES` and `information_schema.COLUMNS`, limited to `TABLE_SCHEMA = DATABASE()` and `TABLE_TYPE = 'BASE TABLE'`. Filter the result through the normalized allowlist and throw one `IllegalStateException` listing sorted violations.
- [ ] Implement `TenantSchemaValidationRunner` as `ApplicationRunner` and `Ordered`, returning order `200`, with constructor-injected validator and a closed-by-default `TenantSchemaReadinessGate`; open the gate only after validation returns successfully.
- [ ] Implement `TenantSchemaReadinessFilter` to return HTTP 503 for application traffic while the gate is closed. Allow liveness only; readiness must remain unavailable. This prevents the embedded server's pre-runner socket window from serving business requests.
- [ ] Centralize the exact immutable allowlist in common code: `sys_tenant`, `sys_menu`, `sys_dict_type`, `sys_dict_data`, `sys_config`, `sys_sql_upgrade_log`, `biz_sql_upgrade_log`. In all three `MyBatisPlusConfig` classes, register the validation runner, gate, and filter under `@ConditionalOnProperty(name = "erp.tenant.schema-validation.enabled", havingValue = "true", matchIfMissing = true)`.
- [ ] Re-run both common tests. Expected GREEN.

### Task 3: Guarantee upgrade-before-validation ordering

**Files:**
- Modify: `erp-modules/erp-system/src/main/java/com/erp/system/config/SystemSqlUpgradeRunner.java`
- Modify: `erp-modules/erp-business/src/main/java/com/erp/business/config/BusinessSchemaUpgradeRunner.java`
- Modify: `erp-modules/erp-workflow/src/main/java/com/erp/workflow/config/WorkflowSqlUpgradeRunner.java`
- Create: `erp-modules/erp-system/src/test/java/com/erp/system/config/SystemSqlUpgradeRunnerTest.java`
- Modify: `erp-modules/erp-business/src/test/java/com/erp/business/config/BusinessSchemaUpgradeRunnerTest.java`
- Create: `erp-modules/erp-workflow/src/test/java/com/erp/workflow/config/WorkflowSqlUpgradeRunnerTest.java`

- [ ] Add tests asserting each SQL runner implements `Ordered` and returns `100`; run them and observe RED because the runners currently have no order contract.
- [ ] Implement `Ordered` on all three SQL runners with order `100`, keeping SQL behavior unchanged.
- [ ] Add `erp.tenant.schema-validation.enabled=false` only to test configurations that start an unrelated Spring context without an ERP schema; never change the production default.
- [ ] Run `mvn -pl "erp-modules/erp-system,erp-modules/erp-business,erp-modules/erp-workflow" -am test` and then full `mvn test`.
- [ ] Run `git diff --check`, inspect the complete diff for unrelated changes, and commit `security: enforce fail-closed tenant schema`.

## Evidence and boundaries

- Parsed init/upgrade SQL shows every current tenant-owned table contains `tenant_id`. The only declared non-tenant application tables are `sys_menu`, `sys_dict_type`, `sys_dict_data`, and `sys_config`; `sys_tenant` is globally addressed, and the two upgrade log tables are infrastructure.
- No DDL/DML or connection to `192.168.0.22` is required for this task. The validator's metadata query is read-only and covered with a mocked `JdbcTemplate`; live database startup validation belongs to deployment verification.
