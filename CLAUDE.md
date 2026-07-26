# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Multi-tenant, microservice ERP + HR platform. Backend is a Maven multi-module Spring
Cloud Alibaba project (Java 17 / Spring Boot 3.2.4); frontend `erp-ui` is a separate Vue 3
+ Element Plus + Vite app. Documentation is written in Chinese; code and identifiers are English.

## Build & run

### Backend (Maven)

Use `build.bat` (pins JDK 17 and the project settings file) — do not rely on ambient `mvn`:

```bash
./build.bat
```

It runs `mvn -s project-settings.xml clean install -DskipTests`. Equivalent manual command:

```bash
mvn -s project-settings.xml clean install -DskipTests
```

- Run all tests: `mvn -s project-settings.xml test`
- Test a single module: `mvn -s project-settings.xml -pl erp-modules/erp-system test`
- Single test class: `mvn -s project-settings.xml -pl erp-modules/erp-system test -Dtest=SystemSqlUpgradeRunnerTest`
- Run one service after building: `mvn -s project-settings.xml -pl erp-modules/erp-system spring-boot:run`

Requires JDK 17 and Maven 3.9+. Artifacts resolve through the Aliyun mirror configured in `pom.xml`.

### Frontend (erp-ui)

`erp-ui/` is gitignored (tracked as a separate repo). From `erp-ui/`:

- `npm install` then `npm run dev` — Vite dev server on **port 9000**, proxies `/api/*` → `http://127.0.0.1:9090` (the gateway), stripping the `/api` prefix.
- `npm run build` — colour guard, then `vue-tsc` type-check, then `vite build`.
- `npm run lint:colors` — the colour guard on its own.

### Frontend styling & theming (hard rule)

**Never hard-code neutral colours in page or layout styles.** Use the semantic tokens in
`src/styles/palette.css`; `npm run build` fails if you don't (`scripts/check-colors.mjs`).

- `--erp-c-*` — business pages: `surface` / `surface-2` / `page`, `fill*`, `border*`, `text-strong|text|text-2|-3|-4`, `tint-{green,orange,yellow,blue,indigo,red}`.
- `--erp-s-*` — the classic sidebar shell only (blue-tinted family), plus `--erp-s-v-*` for its four colour variants.
- Every token is defined once for light and once under `html.dark`, so **adding a token pair is the only place a colour needs a dark counterpart** — never write a parallel `html.dark` block per component.
- Translucent panels must use `--erp-c-glass*`, never a literal `rgba(255,255,255,…)`. A white glass background that stays light while its text follows the theme is what produced a white-on-white login screen once — the guard now rejects `rgba(255,255,255,α≥0.5)` on `background` for exactly this reason.
- Saturated brand/status colours (orange, red, green…) and light text sitting on coloured backgrounds are allowed as literals; the guard permits them automatically. For a genuinely decorative literal, put `eslint-disable-next-line color-token` on the line above.
- `src/styles/palette.css` and `src/styles/ui-preference.css` are the theming layer itself and are exempt. So is `src/views/login/index.vue`: it is a **fixed-theme brand page** (blue gradient + deliberately white card) that must not follow dark mode — keep its literals, and don't tokenise pages like it (see `FIXED_THEME_PAGES` in the guard).

There are **two layout shells** and both must be checked after any visual change:
`src/layout/ExecutiveLayout.vue` (top nav **plus its own left sidebar**, the default) and
`src/layout/index.vue` (classic sidebar). `layout/index.vue` doubles as the entry component and
renders one or the other based on `layoutStyle` from `useUiPreferenceStore`.

UI personalisation (theme colour, dark mode, density, radius, motion, …) is driven by that store;
it persists to `localStorage` and syncs to `/system/ui-preference`, merging
system default ‹ tenant policy ‹ personal, with tenant-locked keys forced.

### Infrastructure prerequisites

Services need **Nacos** (discovery + config, default `127.0.0.1:8848`), **MySQL 8** (default DB `erp_system`), and **Redis**. Local dev can disable Nacos with env `NACOS_DISCOVERY_ENABLED=false` / `NACOS_CONFIG_ENABLED=false`. `nacos-config-example.yml` shows the expected per-service Nacos config shape. Key env vars: `MYSQL_HOST/PORT/USERNAME/PASSWORD`, `NACOS_SERVER_ADDR`, `ERP_INTERNAL_AUTH_SIGNATURE_SECRET` (mandatory — see below), `ERP_JWT_SECRET`.

## Services & ports

All traffic enters through the gateway (9090). Path prefix routing is defined in
`erp-gateway/src/main/resources/bootstrap/erp-gateway.yml`.

| Service | Port | Gateway path prefix | Notes |
|---|---|---|---|
| erp-gateway | 9090 | — | Spring Cloud Gateway, single ingress |
| erp-auth | 9091 | `/login`, `/logout`, `/auth/**` | Token issue + authoritative verify |
| erp-system | 9092 | `/system/**` | RBAC, tenants, dicts, MDM, config, notices |
| erp-business | 9093 | `/business/**` | HR + inventory business logic |
| erp-workflow | 9094 | `/workflow/**` | Approval flows |
| erp-ai | 9095 | `/system/ai/**` | AI assistant (routed ahead of `/system/**`) |

## Architecture

### Module layout (`pom.xml` → `erp-modules/pom.xml`)

- `erp-common` (`erp-common-core`) — shared kernel: unified `R<T>` response + `ResultCode`, `ServiceException`, tenant/trace `*ContextHolder`s, JWT utils, MyBatis-Plus tenant config, and the servlet security building blocks (`InternalAuthenticationFilterSupport`, `InternalAuthSignatureUtils`, `AuthHeaders`). Depend on this before reinventing utilities.
- `erp-*-contract` (`erp-platform-contract`, `erp-workflow-contract`) — POJO-only cross-service DTOs/VOs (e.g. `PlatformUserView`). No logic.
- `erp-internal-client-core` — shared internal-call plumbing: `RestTemplate`, `InternalRequestHeaderFactory`, `InternalSystemClientProperties` (`erp.internal.*`).
- `erp-*-client` (`erp-platform-client`, `erp-workflow-client`, `erp-business-client`) — typed clients wrapping the internal HTTP endpoints, returning contract types. A service calls another service **only through these clients**, never by hand-building HTTP.
- `erp-system`, `erp-business`, `erp-workflow`, `erp-ai` — deployable Spring Boot apps.

Each deployable module follows: `controller/` (REST) → `service/` (`I*Service` interface + `service/impl/*ServiceImpl`) → `mapper/` (MyBatis-Plus) with `domain/entity` and `domain/vo`. XML mappers live under `src/main/resources/mapper/**/*.xml`.

### Authentication & cross-service trust (important)

Two distinct trust boundaries — don't confuse them:

1. **Edge (client → gateway):** The gateway's `GatewayAuthFilter` strips any incoming `X-Auth-*` headers, then for protected paths calls `erp-auth`'s `/auth/token/verify` (the single source of truth for JWT validity). On success it injects `X-Auth-*` identity headers (`AuthHeaders`) **plus an HMAC `X-Auth-Signature`** computed with `ERP_INTERNAL_AUTH_SIGNATURE_SECRET` and forwards downstream. Downstream services trust these headers only because the signature proves they came from the gateway.
2. **Internal (service → service):** Uses the `erp-*-client` modules. `InternalRequestHeaderFactory` signs requests as a synthetic service principal (`erp-service`, tenant `000000`). Endpoints consumed this way live under `/*/internal/**` (e.g. `SystemInternalController` serves `/system/internal/...`) and are the server side of the platform/business/workflow clients.

`ERP_INTERNAL_AUTH_SIGNATURE_SECRET` is required at startup for both the gateway and internal clients — they fail fast if it is missing. There is no baked-in default.

### Multi-tenancy

Field-based (`tenant_id`) logical isolation via MyBatis-Plus tenant interceptor
(`TenantMybatisPlusConfigurationSupport` in `erp-common`), with `TenantContextHolder`
carrying the current tenant (propagated across threads via transmittable-thread-local).

### Unified API contract

Every controller response is wrapped as `R<T>` (`code`/`message`/`data`/`timestamp`/`traceId`/`path`)
by the response-body advice in `erp-common`; exceptions funnel through the shared
`GlobalExceptionHandlerSupport` / `ApiErrorResponse*`. Throw `ServiceException` for business
errors rather than returning ad-hoc error shapes. `traceId` is set by `TraceIdFilterSupport`.

## Database & SQL migrations (hard rule)

Any change to DB tables, fields, indexes, seed config, dictionaries, code rules, menus, or
permission/role grants **must ship an incremental SQL script**, and the change is expected to
be executed and verified against a real DB before the code is delivered. This convention is
central to this repo — follow it strictly.

- **Naming:** `yyyyMMdd_nn_description.sql` (date-versioned).
- **Locations:** system → `erp-modules/erp-system/src/main/resources/sql/upgrade/system/`; business → `erp-modules/erp-business/src/main/resources/sql/upgrade/business/`; workflow → `.../erp-workflow/.../sql/upgrade/workflow/`.
- **Idempotent & repeatable:** scripts must be safe to re-run and must not assume a clean database (use `IF NOT EXISTS`, guarded inserts, etc.).
- **Keep full-init in sync:** append the same structure/base data to the module's total init script (`init_system.sql`, `init_business.sql`, `init_workflow.sql`) so a fresh environment initializes in one shot.
- **Auto-execution:** each module has an `ApplicationRunner` (`SystemSqlUpgradeRunner`, `BusinessSchemaUpgradeRunner`, `WorkflowSqlUpgradeRunner`) that on startup runs only not-yet-applied upgrade scripts (in filename order) and records them in `sys_sql_upgrade_log`. This is why filenames must sort correctly and scripts must be idempotent. Toggle with `erp.sql.upgrade.enabled`.
- When a legacy DB throws "table/field/menu/permission does not exist", fix it by adding an upgrade script — do not work around it in business code.

## Conventions

- Do not change the core stack (Spring Boot 3.2.4 / Spring Cloud Alibaba 2023.0.1.0 / JDK 17 / MyBatis-Plus / Nacos) without approval — see `ARCH_DECISIONS.md`.
- Java: Lombok is enabled project-wide; Hutool + fastjson2 are the default utility/JSON libs. Prefer JDK 17 idioms.
- New cross-service data goes in a `*-contract` module as a plain POJO; the calling side gets a method on the matching `*-client`; the serving side exposes it under `/<module>/internal/**`.
- Chinese is used for docs, commit messages, and Javadoc in this repo; match the surrounding style.

### Audit columns (`create_by` / `create_time` / `update_by` / `update_time`)

New entities whose table carries all four columns **must extend `BaseAuditEntity`**
(`erp-common`, `com.erp.common.mybatis`) instead of redeclaring the fields. Each deployable
module registers an `AuditMetaObjectHandler` (extending `AuditMetaObjectHandlerSupport`) that
fills them on insert/update, so **do not hand-write `setCreateBy` / `setUpdateTime` in services** —
filling only happens when the field is null, so an explicit value (data import keeping the original
operator, workflow callbacks resolving the approver) still wins.

Two limits worth knowing: auto-fill needs an entity parameter, so pure `UpdateWrapper` and
hand-written XML updates are not covered — new tables should declare
`DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE CURRENT_TIMESTAMP` as the DB-level backstop.
`erp-workflow-contract` cannot depend on `erp-common`, so its two entities carry
`@TableField(fill = ...)` on their own fields instead.

### Operation & audit logging

`OperationLogInterceptorSupport` (write requests) and `AuditLogAspectSupport` (GET requests) live
in `erp-common` (`com.erp.common.logging`) and hand an `OperationLogPayload` to an
`OperationLogRecorder`. erp-system implements it with `LocalOperationLogRecorder` (writes
`sys_oper_log` / `sys_audit_log` directly); erp-business and erp-workflow implement it with
`RemoteOperationLogRecorder`, which posts asynchronously to `/system/internal/platform/oper-log`
via `erp-platform-client` — those two services must never write the log tables themselves.
`/*/internal/**` paths are excluded from logging so service-to-service calls don't amplify.

## Reference docs

`PROJECT_CONTEXT.md` (vision/stack/roadmap), `ARCH_DECISIONS.md` (ADRs), `AGENTS.md` (the
DB-script rules, source of the section above), `docs/ops/service-health-and-rollback.md` (ops).
Note: some phase/progress notes in `PROJECT_CONTEXT.md` / `.ai-project-state.md` predate the
current module split (they mention only system/business and ports 808x) — trust this file and
the actual `pom.xml`/`application.yml` for module names and ports.
