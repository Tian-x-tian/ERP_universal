
-- 2026-03-20 员工档案名称统一
UPDATE `sys_menu`
SET `menu_name` = '员工档案', `remark` = '兼容旧员工主数据菜单名称'
WHERE `path` = '/system/mdm/employee';
