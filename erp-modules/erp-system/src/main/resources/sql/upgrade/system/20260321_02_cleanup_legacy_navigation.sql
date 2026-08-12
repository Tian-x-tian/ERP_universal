-- 20260321_02: 清理旧导航兼容路径，统一菜单到前端蓝图正式路径

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工作台', 0, 5, '/workbench', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '工作台目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '通知管理', 2, 10, '/system/notice-manage', '/views/system/notice/index', 1, 'C', '0', '0', 'system:notice:list', NULL, 'system', NOW(), '通知管理正式菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/notice-manage');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT source_role_menu.tenant_id,
       source_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/system/notice-manage'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE source_menu.path = '/system/notice'
  AND existed_role_menu.menu_id IS NULL;

UPDATE `sys_menu`
SET `parent_id` = (SELECT target_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/system/notice-manage' LIMIT 1) target_menu)
WHERE `parent_id` = (SELECT source_menu.menu_id
                     FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/system/notice' LIMIT 1) source_menu);

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path = '/system/notice';

DELETE FROM `sys_menu`
WHERE `path` = '/system/notice';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '数据权限', 15, 2, '/system/data-permission', '/views/platform/data-scope/index', 1, 'C', '0', '0', 'system:dataScope:view', NULL, 'system', NOW(), '数据权限正式菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/data-permission');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT source_role_menu.tenant_id,
       source_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/system/data-permission'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE source_menu.path = '/platform/data-scope'
  AND existed_role_menu.menu_id IS NULL;

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path = '/platform/data-scope';

DELETE FROM `sys_menu`
WHERE `path` = '/platform/data-scope';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '编码规则', 15, 3, '/system/code-rule', '/views/platform/code-rule/index', 1, 'C', '0', '0', 'system:codeRule:list', NULL, 'system', NOW(), '编码规则正式菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/code-rule');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT source_role_menu.tenant_id,
       source_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/system/code-rule'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE source_menu.path = '/platform/code-rule'
  AND existed_role_menu.menu_id IS NULL;

UPDATE `sys_menu`
SET `parent_id` = (SELECT target_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/system/code-rule' LIMIT 1) target_menu)
WHERE `parent_id` = (SELECT source_menu.menu_id
                     FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/platform/code-rule' LIMIT 1) source_menu);

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path = '/platform/code-rule';

DELETE FROM `sys_menu`
WHERE `path` = '/platform/code-rule';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '系统消息',
       workbench_menu.menu_id,
       1,
       '/workbench/system-notice',
       '/views/system/notice/index',
       1,
       'C',
       '0',
       '0',
       'system:message:list',
       NULL,
       'system',
       NOW(),
       '工作台系统消息菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) workbench_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/system-notice');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT source_role_menu.tenant_id,
       source_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/workbench/system-notice'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE source_menu.path IN ('/workbench/message/system-notice', '/workbench/notice')
  AND existed_role_menu.menu_id IS NULL;

UPDATE `sys_menu`
SET `parent_id` = (SELECT target_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/system-notice' LIMIT 1) target_menu)
WHERE `parent_id` IN (
  SELECT source_menu.menu_id
  FROM (
    SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/message/system-notice'
    UNION ALL
    SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/notice'
  ) source_menu
);

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path IN ('/workbench/message/system-notice', '/workbench/notice');

DELETE FROM `sys_menu`
WHERE `path` IN ('/workbench/message/system-notice', '/workbench/notice');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       parent_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = parent_menu.menu_id
WHERE child_menu.path = '/workbench/system-notice'
  AND existed_role_menu.menu_id IS NULL;

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path = '/workbench/message';

DELETE FROM `sys_menu`
WHERE `path` = '/workbench/message';
