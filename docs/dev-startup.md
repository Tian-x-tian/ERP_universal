# 本地启动清单

## 当前机器状态

- Redis：本机 `127.0.0.1:6379` 已有 Windows 服务在运行。
- MySQL：共享库 `192.168.0.22:3306` 网络可达；本机 `127.0.0.1:33317` 已用 `D:\software\mysql-8.0.17-winx64` 启动，数据目录为 `D:\workspace\ERP\.tmp\mysql-33317-data`。后端服务 YAML 默认 MySQL host 为 `192.168.0.22`，仍可用 `MYSQL_HOST` 覆盖。本机 Nacos 中七个服务的配置目前统一指向 `192.168.0.22:3306`：`erp-saas-control` 用 `erp_saas_control` 库，其余用 `erp_system` 库。
- Nacos：本机 `127.0.0.1:8848` 可访问，默认组已有全部七份配置（含 `erp-saas-control.yml`）。
- Docker：当前 PATH 中不可用，不能直接使用 `deploy/saas/docker-compose.dedicated.yml` 的 bundled infra。

## 完整联调需要启动的服务

| 顺序 | 服务 | 端口 | 必需性 | 说明 |
|---|---:|---:|---|---|
| 1 | MySQL 8 | 3306 | 必需 | `erp-system`、`erp-business`、`erp-workflow`、`erp-auth`、`erp-saas-control` 都需要。默认库为 `erp_system`，`erp-saas-control` 默认库为 `erp_saas_control`。 |
| 2 | Nacos | 8848 | 完整联调必需 | 负责配置和服务发现。单服务本地调试可关闭。 |
| 3 | Redis | 6379 | auth 必需 | `erp-auth` 登录失败次数、限流等使用；其他后端服务不直接依赖。 |
| 4 | erp-saas-control | 9096 | SaaS/租户域名链路必需 | 网关和 system 会调用。 |
| 5 | erp-auth | 9091 | 登录必需 | JWT 签发和 token 校验。 |
| 6 | erp-system | 9092 | 主业务必需 | RBAC、租户、字典、MDM、系统配置。 |
| 7 | erp-business | 9093 | HR/库存业务必需 | 业务侧接口。 |
| 8 | erp-workflow | 9094 | 审批流必需 | 流程引擎和回调。 |
| 9 | erp-ai | 9095 | AI 功能必需 | `/system/ai/**` 路由会优先转发到这里。 |
| 10 | erp-gateway | 9090 | 前端入口必需 | 所有前端 API 通过网关进入。 |
| 11 | erp-ui | 9000 | 前端开发必需 | Vite `/api` 代理到 `http://127.0.0.1:9090`。 |

## 最小开发启动方式

YAML 配置已内置满足强校验要求的默认开发密钥（`dev-internal-auth-signature-secret-must-be-32bytes!` 等），本地直接启动（如 `mvn spring-boot:run`）无需额外设置环境变量。

若本地未启动 Nacos，可以关闭服务发现与远程配置检查进行单服务离线调试：

```powershell
$env:NACOS_DISCOVERY_ENABLED = 'false'
$env:NACOS_CONFIG_ENABLED = 'false'
```

如需覆盖默认密钥，可按需传入环境变量：

```powershell
$env:ERP_INTERNAL_AUTH_SIGNATURE_SECRET = '<至少 32 字节的自定义密钥>'
$env:ERP_SAAS_TENANT_ASSERTION_SIGNATURE_SECRET = '<至少 32 字节的自定义密钥>'
$env:ERP_JWT_SECRET = '<至少 64 字节的自定义密钥>'
```

需要数据库的服务还要指定 MySQL：

```powershell
$env:MYSQL_HOST = '192.168.0.22'
$env:MYSQL_PORT = '3306'
$env:MYSQL_DATABASE = 'erp_system'
$env:MYSQL_USERNAME = '<mysql-user>'
$env:MYSQL_PASSWORD = '<mysql-password>'
```

`erp-saas-control` 默认连接库名为 `erp_saas_control`。本机 `33317` 已创建 `erp_system_test` 和 `erp_saas_control` 两个空库；共享库 `192.168.0.22:3306/erp_saas_control` 已由 `SaasControlSqlUpgradeRunner` 初始化完成，当前有 15 张 `saas_%` 表、7 条成功 upgrade log，且没有失败或运行中的 log。启动时 `SaasControlSqlUpgradeRunner` 会按文件名顺序执行 `classpath:sql/upgrade/control/*.sql`，并写入 `saas_sql_upgrade_log`；随后 `SaasControlCatalogSchemaValidationRunner` 校验表结构。`spring.sql.init.schema-locations=classpath:sql/init_control.sql` 只有在 `SQL_INIT_MODE` 打开时才由 Spring SQL init 执行。

`erp-auth` 还需要：

```powershell
$env:ERP_JWT_SECRET = '<JWT 密钥>'
$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '6379'
```

## 服务启动命令

先在根目录构建：

```powershell
D:\java\apache-maven-3.9.12\bin\mvn.cmd -s D:\workspace\ERP\project-settings.xml clean install -DskipTests
```

再进入具体服务目录启动。切换分支后如果怀疑 `target/classes` 资源旧，优先用 `clean spring-boot:run`：

```powershell
Set-Location D:\workspace\ERP\erp-gateway
D:\java\apache-maven-3.9.12\bin\mvn.cmd -s D:\workspace\ERP\project-settings.xml clean spring-boot:run
```

其他后端服务同理，分别进入：

- `D:\workspace\ERP\erp-auth`
- `D:\workspace\ERP\erp-modules\erp-system`
- `D:\workspace\ERP\erp-modules\erp-business`
- `D:\workspace\ERP\erp-modules\erp-workflow`
- `D:\workspace\ERP\erp-modules\erp-ai`
- `D:\workspace\ERP\erp-modules\erp-saas-control`

前端：

```powershell
Set-Location D:\workspace\ERP\erp-ui
npm run dev
```
