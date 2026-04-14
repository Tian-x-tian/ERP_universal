-- 2026-04-14 出勤菜单修复：清理因按钮复用 path 导致的重复菜单，并重建按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '出勤管理', parent_menu.menu_id, 4, '/business/hr/attendance', '/views/business/hr/attendance/index', 1, 'C', '0', '0', 'business:hr:attendance:list', 'Timer', 'system', NOW(), '出勤数据与签到集成管理'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll' LIMIT 1) parent_menu
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = '/business/hr/attendance'
    AND existed_menu.menu_type = 'C'
    AND existed_menu.parent_id = parent_menu.menu_id
);

DELETE role_menu
FROM `sys_role_menu` role_menu
INNER JOIN `sys_menu` menu_item ON menu_item.menu_id = role_menu.menu_id
WHERE menu_item.path = '/business/hr/attendance'
  AND menu_item.menu_id <> (
    SELECT keep_menu.menu_id
    FROM (
      SELECT MIN(menu_id) AS menu_id
      FROM `sys_menu`
      WHERE `path` = '/business/hr/attendance'
        AND `menu_type` = 'C'
        AND `parent_id` IN (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll')
    ) keep_menu
  );

DELETE menu_item
FROM `sys_menu` menu_item
WHERE menu_item.path = '/business/hr/attendance'
  AND menu_item.menu_id <> (
    SELECT keep_menu.menu_id
    FROM (
      SELECT MIN(menu_id) AS menu_id
      FROM `sys_menu`
      WHERE `path` = '/business/hr/attendance'
        AND `menu_type` = 'C'
        AND `parent_id` IN (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll')
    ) keep_menu
  );

UPDATE `sys_menu`
SET `menu_name` = '出勤管理',
    `parent_id` = (SELECT parent_menu.menu_id FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll' LIMIT 1) parent_menu),
    `order_num` = 4,
    `component` = '/views/business/hr/attendance/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'business:hr:attendance:list',
    `icon` = 'Timer',
    `remark` = '出勤数据与签到集成管理'
WHERE `path` = '/business/hr/attendance'
  AND `menu_type` = 'C';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, attendance_menu.menu_id, button_def.order_num, attendance_menu.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), '出勤按钮权限'
FROM (
  SELECT '出勤管理查询' AS menu_name, 1 AS order_num, 'business:hr:attendance:list' AS perms
  UNION ALL SELECT '出勤管理配置', 2, 'business:hr:attendance:config'
  UNION ALL SELECT '出勤推送', 3, 'business:hr:attendance:push'
  UNION ALL SELECT '出勤重试', 4, 'business:hr:attendance:retry'
  UNION ALL SELECT '出勤签到', 5, 'business:hr:attendance:sign'
  UNION ALL SELECT '个人出勤查询', 6, 'business:hr:attendance:personal'
  UNION ALL SELECT '部门出勤汇总', 7, 'business:hr:attendance:dept'
  UNION ALL SELECT '公司出勤汇总', 8, 'business:hr:attendance:company'
  UNION ALL SELECT '请假单管理', 9, 'business:hr:attendance:leave'
  UNION ALL SELECT '请假提交审批', 10, 'business:hr:attendance:leave:submit'
  UNION ALL SELECT '加班单管理', 11, 'business:hr:attendance:overtime'
  UNION ALL SELECT '加班提交审批', 12, 'business:hr:attendance:overtime:submit'
) button_def
INNER JOIN `sys_menu` attendance_menu ON attendance_menu.path = '/business/hr/attendance' AND attendance_menu.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.parent_id = attendance_menu.menu_id
    AND existed_menu.menu_type = 'F'
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT attendance_role.tenant_id, attendance_role.role_id, button_menu.menu_id
FROM `sys_role_menu` attendance_role
INNER JOIN `sys_menu` attendance_menu ON attendance_menu.menu_id = attendance_role.menu_id
INNER JOIN `sys_menu` button_menu ON button_menu.parent_id = attendance_menu.menu_id AND button_menu.menu_type = 'F'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = attendance_role.tenant_id
      AND existed_role_menu.role_id = attendance_role.role_id
      AND existed_role_menu.menu_id = button_menu.menu_id
WHERE attendance_menu.path = '/business/hr/attendance'
  AND attendance_menu.menu_type = 'C'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT attendance_role.tenant_id, attendance_role.role_id, workbench_attendance.menu_id
FROM `sys_role_menu` attendance_role
INNER JOIN `sys_menu` attendance_menu ON attendance_menu.menu_id = attendance_role.menu_id
INNER JOIN `sys_menu` workbench_attendance ON workbench_attendance.path = '/workbench/attendance'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = attendance_role.tenant_id
      AND existed_role_menu.role_id = attendance_role.role_id
      AND existed_role_menu.menu_id = workbench_attendance.menu_id
WHERE attendance_menu.path = '/business/hr/attendance'
  AND attendance_menu.menu_type = 'C'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role.tenant_id, child_role.role_id, workbench_menu.menu_id
FROM `sys_role_menu` child_role
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role.menu_id
INNER JOIN `sys_menu` workbench_menu ON workbench_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = child_role.tenant_id
      AND existed_role_menu.role_id = child_role.role_id
      AND existed_role_menu.menu_id = workbench_menu.menu_id
WHERE child_menu.path = '/workbench/attendance'
  AND existed_role_menu.menu_id IS NULL;
