# 系统增量 SQL 规范

1. 文件名使用日期前缀：`yyyyMMdd_nn_description.sql`，例如 `20260318_01_wf_business_binding.sql`。
2. 脚本必须幂等，可重复执行，不依赖清库。
3. 新增脚本后无需修改历史脚本，按日期持续追加。
4. 应用启动时由 `SystemSqlUpgradeRunner` 自动扫描 `classpath:sql/upgrade/system/*.sql`。
5. 已执行脚本记录在表 `sys_sql_upgrade_log`，同名脚本仅执行一次。
6. 自 2026-03-22 起，workflow 相关结构、菜单、权限、绑定升级脚本统一迁移到 `erp-workflow` 的 `sql/upgrade/workflow/`，system 目录不再承载 workflow 升级逻辑。
