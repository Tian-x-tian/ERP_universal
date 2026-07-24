-- 20260725_01: UI 个性化偏好（用户个人偏好 + 租户默认策略与锁定项）

CREATE TABLE IF NOT EXISTS `sys_user_ui_preference` (
  `preference_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '偏好ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `scope_type` char(1) NOT NULL DEFAULT '1' COMMENT '作用域（0租户默认 1用户个人）',
  `user_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '用户ID（租户默认固定为0）',
  `preference_json` longtext COMMENT 'UI偏好JSON',
  `locked_keys` varchar(1000) DEFAULT NULL COMMENT '锁定项（逗号分隔，仅租户默认生效）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`preference_id`),
  UNIQUE KEY `uk_sys_user_ui_preference` (`tenant_id`,`scope_type`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户UI偏好表';

-- UI 设置菜单（前端蓝图已内置该入口，此处补齐后端菜单记录以便按钮权限挂载）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'UI设置',
       COALESCE((SELECT parent_id FROM `sys_menu` WHERE `path` = '/system/config' LIMIT 1), 0),
       6,
       '/system/theme',
       '/views/system/theme/index',
       1,
       'C',
       '0',
       '0',
       'system:ui:preference:view',
       'Monitor',
       'system',
       NOW(),
       'UI个性化设置菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/theme');

-- UI 设置按钮权限（个人偏好保存 + 租户策略管理）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name,
       menu_parent.menu_id,
       button_def.order_num,
       '/system/theme',
       NULL,
       1,
       'F',
       '0',
       '0',
       button_def.perms,
       NULL,
       'system',
       NOW(),
       'UI个性化设置按钮权限'
FROM (
  SELECT 'UI偏好保存' AS menu_name, 1 AS order_num, 'system:ui:preference:edit' AS perms
  UNION ALL SELECT 'UI租户策略查看', 2, 'system:ui:policy:query'
  UNION ALL SELECT 'UI租户策略修改', 3, 'system:ui:policy:edit'
) button_def
INNER JOIN `sys_menu` menu_parent ON menu_parent.path = '/system/theme' AND menu_parent.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = '/system/theme'
    AND existed_menu.perms = button_def.perms
);

-- 授权管理员角色
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT role_item.tenant_id,
       role_item.role_id,
       menu_item.menu_id
FROM `sys_role` role_item
INNER JOIN `sys_menu` menu_item ON menu_item.perms IN (
    'system:ui:preference:view',
    'system:ui:preference:edit',
    'system:ui:policy:query',
    'system:ui:policy:edit'
  )
LEFT JOIN `sys_role_menu` existed_role_menu ON existed_role_menu.role_id = role_item.role_id
    AND existed_role_menu.menu_id = menu_item.menu_id
WHERE role_item.role_key = 'admin'
  AND existed_role_menu.menu_id IS NULL;
