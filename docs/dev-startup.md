# 本地启动清单

## 当前机器状态

- Redis：本机 `127.0.0.1:6379` 已有 Windows 服务在运行。
- MySQL：本机 `127.0.0.1:3306` 当前不可用；共享库 `192.168.0.22:3306` 网络可达，但写入共享库前必须单独确认。
- Nacos：本机 `127.0.0.1:8848` 当前不可用。
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

单服务调试可以不启动 Nacos，但必须关闭发现和配置检查：

```powershell
$env:NACOS_DISCOVERY_ENABLED = 'false'
$env:NACOS_CONFIG_ENABLED = 'false'
$env:ERP_INTERNAL_AUTH_SIGNATURE_SECRET = '<至少 32 字节的本地开发密钥>'
$env:ERP_SAAS_TENANT_ASSERTION_SIGNATURE_SECRET = '<至少 32 字节的本地开发密钥>'
```

需要数据库的服务还要指定 MySQL：

```powershell
$env:MYSQL_HOST = '<mysql-host>'
$env:MYSQL_PORT = '3306'
$env:MYSQL_DATABASE = 'erp_system'
$env:MYSQL_USERNAME = '<mysql-user>'
$env:MYSQL_PASSWORD = '<mysql-password>'
```

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
