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
- `npm run build` — `vue-tsc` type-check then `vite build`.

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

## Reference docs

`PROJECT_CONTEXT.md` (vision/stack/roadmap), `ARCH_DECISIONS.md` (ADRs), `AGENTS.md` (the
DB-script rules, source of the section above), `docs/ops/service-health-and-rollback.md` (ops).
Note: some phase/progress notes in `PROJECT_CONTEXT.md` / `.ai-project-state.md` predate the
current module split (they mention only system/business and ports 808x) — trust this file and
the actual `pom.xml`/`application.yml` for module names and ports.
