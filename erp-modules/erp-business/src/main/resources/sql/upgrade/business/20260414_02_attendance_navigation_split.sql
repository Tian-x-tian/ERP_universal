-- 2026-04-14 出勤菜单拆分：签到进入顶级工作台，HR 出勤管理聚焦数据与集成
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '签到', workbench_menu.menu_id, 1, '/workbench/attendance', '/views/workbench/attendance/index', 1, 'C', '0', '0', 'business:hr:attendance:list', 'Timer', 'system', NOW(), '个人签到签退入口'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) workbench_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/attendance');

UPDATE `sys_menu`
SET `menu_name` = '签到',
    `parent_id` = (SELECT workbench_menu.menu_id FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) workbench_menu),
    `order_num` = 1,
    `component` = '/views/workbench/attendance/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'business:hr:attendance:list',
    `icon` = 'Timer',
    `remark` = '个人签到签退入口'
WHERE `path` = '/workbench/attendance';

UPDATE `sys_menu`
SET `order_num` = 2
WHERE `path` = '/workbench/system-notice'
  AND `menu_type` = 'C';

UPDATE `sys_menu`
SET `order_num` = 3
WHERE `path` = '/workbench/process-todo'
  AND `menu_type` = 'C';

UPDATE `sys_menu`
SET `menu_name` = '出勤管理',
    `component` = '/views/business/hr/attendance/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'business:hr:attendance:list',
    `icon` = 'Timer',
    `remark` = '出勤数据与签到集成管理'
WHERE `path` = '/business/hr/attendance'
  AND `parent_id` IN (SELECT parent_menu.menu_id FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll') parent_menu)
  AND `menu_type` = 'C';

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT source_role_menu.tenant_id, source_role_menu.role_id, workbench_attendance.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN `sys_menu` workbench_attendance ON workbench_attendance.path = '/workbench/attendance'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = source_role_menu.tenant_id
      AND existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = workbench_attendance.menu_id
WHERE ((source_menu.path = '/business/hr/attendance' AND source_menu.menu_type = 'C') OR source_menu.perms = 'business:hr:attendance:sign')
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id, child_role_menu.role_id, workbench_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` workbench_menu ON workbench_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = child_role_menu.tenant_id
      AND existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = workbench_menu.menu_id
WHERE child_menu.path = '/workbench/attendance'
  AND existed_role_menu.menu_id IS NULL;
