# PROJECT_CONTEXT

## 项目愿景
构建一个面向未来的、可长期演进的微服务 ERP 系统。支持多租户、高并发、DevOps 自动化，并预留 K8s 部署能力。

## 技术栈 (Technology Stack)
- **后端**: Spring Boot 3.2.4, Spring Cloud Alibaba 2023.0.1.0 (Nacos, Sentinel, Gateway)
- **数据库**: MySQL 8.x, Redis
- **持久层**: MyBatis Plus 3.5.5
- **环境**: JDK 17, Maven 3.9.12
- **认证**: Spring Security + OAuth2 (待完全集成)

## 模块结构
- `erp-parent`: 依赖管控
- `erp-common`: 核心工具与通用响应 (`R.java`)
- `erp-gateway`: 统一网关 (端口 8080)
- `erp-auth`: 认证中心 (端口 8081)
- `erp-modules`: 业务聚合
    - `erp-system`: 系统管理 (端口 8082)
    - `erp-business`: 业务模板 (端口 8083)

## 演进路线图
1. Phase 1: 基础设施与骨架搭建 (COMPLETED)
2. Phase 2: 权限体系与多租户模型 (IN_PROGRESS)
3. Phase 3: 核心业务流程与 UI (PLANNED)
4. Phase 4: DevOps 与 K8s 交付 (PLANNED)



Goal:
Enterprise HR + ERP integration platform.

Constraints:
Do not change core stack without approval.

## 数据库脚本永久规则（2026-03-18）
- 后续数据库初始化/升级脚本统一采用日期命名：`yyyyMMdd_nn_description.sql`。
- `erp-system` 脚本目录：`erp-modules/erp-system/src/main/resources/sql/upgrade/system/`。
- `erp-business` 脚本目录：`erp-modules/erp-business/src/main/resources/sql/upgrade/business/`。
- 新增日期脚本后，必须先执行一遍并验证通过，再交付代码。
- 同步把新结构与基础初始化数据追加到总初始化脚本（system 对应 `init_system.sql`，business 对应 `init_business.sql`）。
