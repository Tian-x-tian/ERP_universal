# ERP 服务健康检查与回滚说明

## 适用服务

- `erp-gateway`
- `erp-auth`
- `erp-system`
- `erp-business`
- `erp-workflow`
- `erp-ai`

## 健康检查入口

所有服务统一暴露以下端点：

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/info`

建议网关与部署平台优先使用：

- 存活探针：`/actuator/health/liveness`
- 就绪探针：`/actuator/health/readiness`

## 启动验收清单

每次发布后按顺序验证：

1. 服务进程已启动且端口监听成功
2. `/actuator/health/liveness` 返回 `UP`
3. `/actuator/health/readiness` 返回 `UP`
4. 网关到下游服务的核心路由返回 200/401/403 等预期业务状态，而不是 5xx
5. 关键依赖检查通过：
   - `erp-auth`：数据库、Redis
   - `erp-system`：数据库
   - `erp-business`：数据库
   - `erp-workflow`：数据库

## 告警建议

建议至少接入以下告警规则：

1. 连续 3 次存活探针失败
2. 连续 3 次就绪探针失败
3. 5 分钟内 5xx 比例超过阈值
4. 数据库连接池耗尽或连接失败
5. Redis 连接失败
6. Nacos 配置拉取或服务注册失败

## 启动失败回滚步骤

1. 立即停止本次发布批次，禁止继续扩容
2. 保留失败实例日志，记录版本号、时间点、失败端点和异常摘要
3. 将流量切回上一稳定版本
4. 回滚对应服务包或镜像版本
5. 再次验证上一版本的 `liveness` 与 `readiness`
6. 若涉及配置变更，同时回滚 Nacos 对应配置集
7. 若涉及数据库升级：
   - 先确认升级脚本是否已执行
   - 未执行完成时禁止直接重放未审查脚本
   - 必须按增量脚本记录表核对执行状态后再决定补偿或回退

## 本地烟测建议

本地联调可优先验证：

1. `NACOS_CONFIG_ENABLED=false`
2. `NACOS_DISCOVERY_ENABLED=false`
3. 显式传入 `ERP_INTERNAL_AUTH_SIGNATURE_SECRET` 与 `ERP_JWT_SECRET`
4. 逐个访问 `/actuator/health`

## 密钥要求

以下配置必须由环境变量、Nacos 或密钥中心提供，仓库内不再保留默认密钥：

- `ERP_INTERNAL_AUTH_SIGNATURE_SECRET`
- `ERP_JWT_SECRET`
- `MYSQL_PASSWORD`
- 其他对象存储或第三方集成密钥
