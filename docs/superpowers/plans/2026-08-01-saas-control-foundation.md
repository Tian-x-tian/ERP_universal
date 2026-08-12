# SaaS Control Plane Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILLS: Use `superpowers:subagent-driven-development`, `superpowers:test-driven-development`, and `schema-migration`. Do not connect to `192.168.0.22`.

**Goal:** Add a bootable `erp-saas-control` service with an independent datasource, a fail-closed security boundary for management/internal APIs, a checksum-safe SQL upgrade runner, and an idempotent control-schema foundation.

**Architecture:** The control database is a cross-tenant catalog, so `tenant_id` is a domain key rather than a request-level MyBatis filter. The module therefore uses pagination only and must not extend `TenantMybatisPlusConfigurationSupport`. Management callers authenticate through the existing signed principal and later receive exact `saas:*` authorization; internal callers use the same service-principal signature path. This task creates the service/security/migration foundation only; lifecycle, domains, entitlements, usage, provisioning, and UI follow in reviewed subplans.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Security, MyBatis-Plus, MySQL 8.0.17, Spring `ScriptUtils`, JUnit 5, Mockito.

---

### Task 1: Create a bootable control-service module

**Files:**
- Modify: `erp-modules/pom.xml`
- Create: `erp-modules/erp-saas-control/pom.xml`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/SaasControlApplication.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/ClockConfig.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/SaasControlMyBatisPlusConfig.java`
- Create: `erp-modules/erp-saas-control/src/main/resources/application.yml`
- Create: `erp-modules/erp-saas-control/src/main/resources/bootstrap/erp-saas-control.yml`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/SaasControlApplicationTest.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlMyBatisPlusConfigTest.java`

- [ ] Create the module POM and add it after `erp-saas-client` in the reactor before adding tests, so the RED is never “selected project not found”. Foundation dependencies are only web, actuator, validation, security, MyBatis-Plus, MySQL runtime, Nacos discovery/config, `erp-common-core`, and tests. Do not add load balancer, platform/internal clients, or SaaS contracts until a current class consumes them.
- [ ] Add tests asserting the application is a Spring Boot entry point, a `Clock.systemUTC()` bean exists, and the MyBatis interceptor contains pagination only with no `TenantLineInnerInterceptor`. Expected RED: production types absent.
- [ ] Implement `SaasControlApplication` with `scanBasePackages = {"com.erp.common", "com.erp.saas.control"}` and `@EnableDiscoveryClient`; set the Boot Maven plugin `mainClass` exactly. Implement application name `erp-saas-control`, port `9096`, config import `optional:nacos:erp-saas-control.yml?refreshable=true`, datasource placeholders targeting database name `erp_saas_control`, and management health endpoints matching existing services. Do not copy secrets or add default passwords beyond the repository's existing `${MYSQL_PASSWORD:123456}` local convention.
- [ ] Implement `SaasControlMyBatisPlusConfig` with only `PaginationInnerInterceptor(DbType.MYSQL)`. Never add `saas_*` tables to `TenantGlobalTables`.
- [ ] The application test disables Nacos/discovery and `erp.saas.sql.upgrade.enabled`, provides a test-only internal signature secret, and supplies a mocked `DataSource`; it must never contact external services or a database. Run `mvn -pl erp-modules/erp-saas-control -am -Dtest=SaasControlApplicationTest,SaasControlMyBatisPlusConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`.

### Task 2: Support multiple protected route groups and secure the service

**Files:**
- Modify: `erp-common/src/main/java/com/erp/common/security/servlet/InternalApiSecurityConfigurer.java`
- Modify: `erp-common/src/test/java/com/erp/common/security/servlet/InternalApiSecurityConfigurerTest.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/security/SaasAuthenticationFilter.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/SaasControlSecurityConfig.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlSecurityConfigTest.java`

- [ ] Add a common regression test proving a new `buildFilterChain(HttpSecurity, RequestMatcher, ...)` overload protects an `OrRequestMatcher`, and explicitly re-test the old String overload's positive and excluded-path behavior. Expected RED: overload absent.
- [ ] Preserve the existing String overload's direct `http.securityMatcher(String)` call. Extract only the common configuration body into a private helper; the new overload separately calls `http.securityMatcher(RequestMatcher)`. Do not delegate the old API through `AntPathRequestMatcher` and do not broaden any existing service's matcher.
- [ ] Before production code, add `SaasControlSecurityConfigTest` and filter-focused tests so RED is missing control types. Extend `InternalAuthenticationFilterSupport` through `SaasAuthenticationFilter`, pass one harmless constructor prefix, and override `requiresAuth` to match only exact `/saas` or `/saas/**` and exact `/internal/saas` or `/internal/saas/**` after removing the servlet context path. `/saasx` and `/internal/saasx` must not match.
- [ ] Configure one chain for `OrRequestMatcher('/saas/**', '/internal/saas/**')`; documentation/Swagger paths retain the common defaults. Do not create a gateway route for `/internal/saas/**`.
- [ ] Add MockMvc/security tests for both route groups: no credentials returns 401 JSON; valid signed principal reaches each test endpoint; invalid internal signature is rejected on each group; `/saasx/**`, `/internal/saasx/**`, and unrelated paths are not claimed by this chain.
- [ ] Run `mvn -pl erp-common,erp-modules/erp-saas-control -am -Dtest=InternalApiSecurityConfigurerTest,SaasControlSecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`.

### Task 3: Add checksum-safe control SQL upgrades

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/config/SaasControlSqlUpgradeRunner.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlSqlUpgradeRunnerTest.java`
- Create: `erp-modules/erp-saas-control/src/main/resources/sql/init_control.sql`
- Create: `erp-modules/erp-saas-control/src/main/resources/sql/upgrade/control/20260801_01_saas_control_foundation.sql`

- [ ] Test runner defaults: location `classpath:sql/upgrade/control/*.sql`, history table `saas_sql_upgrade_log`, order `100`, deterministic filename order, first execution recorded, same filename/same SHA-256 skipped, same filename/different checksum fails, and checksum calculation failure fails rather than returning null. Expected RED: runner absent.
- [ ] Implement the runner with the existing Spring `ScriptUtils` statement-splitting path. Compute SHA-256 before history lookup. Persist `script_name`, non-null `checksum`, `status`, `executed_at`, and sanitized `remark`; never log SQL content or credentials.
- [ ] Add identical idempotent DDL for `saas_sql_upgrade_log` to init and versioned upgrade SQL. The script contains no domain tables yet and is safe to execute repeatedly.
- [ ] Run `mvn -pl erp-modules/erp-saas-control -am -Dtest=SaasControlSqlUpgradeRunnerTest test`.

### Task 4: Validate the migration against isolated MySQL

**Files:**
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/config/SaasControlSqlMigrationIntegrationTest.java`

- [ ] Add an opt-in integration test activated only when `ERP_SAAS_TEST_JDBC_URL`, `ERP_SAAS_TEST_DB_USER`, and `ERP_SAAS_TEST_DB_PASSWORD` are supplied. On an empty schema it runs the real `SaasControlSqlUpgradeRunner` twice and verifies one history row, the exact SHA-256, and second-run skip. In isolated reset phases it executes init twice and versioned upgrade twice through JDBC + Spring `ScriptUtils`; capture and compare `information_schema.COLUMNS` and `STATISTICS` signatures to prove the Java bootstrap, init, and upgrade definitions are identical and `script_name` is unique.
- [ ] Create a disposable directory under `D:\workspace\ERP\.worktrees\saas-hybrid\.tmp\saas-mysql-8017`; resolve its absolute path and abort unless it starts with the worktree's resolved absolute path. Initialize with `D:\software\mysql-8.0.17-winx64\bin\mysqld.exe --initialize-insecure`, using that installation as `--basedir` and the validated temp directory as `--datadir`.
- [ ] Start `mysqld.exe` with `Start-Process -WindowStyle Hidden -PassThru`, an unused loopback-only port, `--bind-address=127.0.0.1`, the exact temp data directory, and an explicit pid file inside it. Poll `mysqladmin.exe --protocol=tcp --host=127.0.0.1 --port=<port> --user=root ping --silent` with a bounded timeout; verify the returned process ID still matches before database creation.
- [ ] Use the bundled `mysql.exe` to create only `erp_saas_control_test` and a temporary loopback test account, then set the three opt-in environment variables and run the integration test. Do not print the generated temporary password.
- [ ] Shut down normally with bundled `mysqladmin.exe`, wait for the exact recorded process ID to exit, and verify it is no longer running. Resolve and revalidate the absolute data-directory prefix before removing only `.tmp\saas-mysql-8017`; never target a workspace root, shared MySQL directory, or `192.168.0.22`.
- [ ] Run `mvn -pl erp-modules/erp-saas-control -am test`, full `mvn test`, and `git diff --check`.
- [ ] Inspect status for unrelated concurrent changes and commit only this task's files as `feat: add SaaS control service foundation`.

## Boundaries for the next subplans

- Catalog/domain/subscription/lifecycle schema and services are Task 3B.
- Effective entitlement, snapshot signing/key rotation, and internal resolve/snapshot APIs are Task 3C.
- Usage aggregation and provisioning callbacks are Task 3D.
- Legacy Full Access import is separate because the existing tenant API returns active tenants only; `000000` inclusion and wildcard future features must be frozen first.
- Gateway route, Nacos example, and platform menu seed are integrated after the service endpoints exist.
