-- 20260319_01: 已由 20260319_02_restructure_workbench_workflow_menu.sql 统一接管
-- 保留空脚本占位，避免历史环境重复执行旧迁移逻辑

UPDATE `sys_menu`
SET `remark` = `remark`
WHERE 1 = 0;
