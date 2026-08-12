# ARCH_DECISIONS

## [ADL-001] 技术栈选择 - Spring Boot 3 & JDK 17
- **日期**: 2026-02-12
- **决策**: 放弃 JDK 20，统一使用 JDK 17 (LTS) 及 Spring Boot 3.2.4。
- **动机**: 确保项目长期支持的稳定性与生态兼容性。
- **后效**: 所有模块必须遵循 JDK 17 语法。

## [ADL-002] 服务发现方案 - Nacos 2.3.2
- **日期**: 2026-02-12
- **决策**: 使用 Nacos 作为注册中心与配置中心。
- **动机**: Spring Cloud Alibaba 生态深度集成，支持高效的服务发现与动态配置。

## [ADL-003] 多租户方案 - 字段隔离 (ID-based)
- **日期**: 2026-02-12 (Planned)
- **决策**: 采用核心表增加 `tenant_id` 字段的逻辑隔离方案。
- **动机**: 实施成本低，易于维护，适合初期演进。后续若有需求可升级为 schema 隔离。

## [ADL-004] 响应封装 - 统一 Result 对象
- **日期**: 2026-02-12
- **决策**: 在 `erp-common` 中定义 `R<T>` 和 `ResultCode`。
- **动机**: 标准化 API 输出，便于前后端解耦。

## [ADL-005] 服务边界 - 禁止跨服务直接读库
- **日期**: 2026-07-25
- **决策**: 任何模块不得直接查询其他模块拥有的数据表，一律通过 `erp-*-client` 内部接口获取。
- **背景**: `erp-system` 曾用原生 SQL 直查 `inv_stock_balance` / `inv_inbound_order` /
  `inv_outbound_order` 做仓库停用前的引用校验，形成"微服务共享数据库、互读对方表"的
  分布式单体反模式，也是 `inv_*` 表在两份初始化脚本中重复出现的根因。
- **落地**: 库存占用统计收归 `erp-business`，经
  `/business/internal/inventory/warehouses/{id}/reference-usage` 对外提供；
  `InternalBusinessClient` 新增对应方法；删除越界的 `InventoryReferenceMapper`。
- **后效**: 表的归属边界即服务边界；新增跨模块读取需求时先加内部接口，不得直连。

## [ADL-006] 财务独立为 erp-finance 微服务
- **日期**: 2026-07-25
- **决策**: 采购到付款闭环中的应付与凭证能力，作为独立可部署服务 `erp-finance`（端口 9096，
  网关前缀 `/finance/**`）落地，而非并入 `erp-business`。
- **动机**: 财务与业务的生命周期、权限与数据敏感度不同；独立后应付生成成为真正的跨服务
  最终一致性场景（本地消息表 + 重试补偿），服务边界与既有 system / business / workflow 同构。
- **代价**: 需新增 `erp-finance`、`erp-finance-contract`、`erp-finance-client` 三个模块
  与约 12 个骨架文件，并同步网关路由与 Nacos 配置；本地开发多启一个进程。
- **约束**: 遵循 [ADL-005]，`erp-business` 只能经 `erp-finance-client` 访问财务数据。
