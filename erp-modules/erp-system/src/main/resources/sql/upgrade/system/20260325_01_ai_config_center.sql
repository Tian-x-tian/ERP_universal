-- 20260325_01: AI 配置中心（租户隔离配置 + 审计）

CREATE TABLE IF NOT EXISTS `sys_ai_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用（1启用 0停用）',
  `model` varchar(128) DEFAULT 'gpt-5.1' COMMENT '模型编号',
  `max_history_turns` int(11) DEFAULT 12 COMMENT '最大历史轮数',
  `max_todo_items` int(11) DEFAULT 10 COMMENT '提示词注入待办上限',
  `max_notice_items` int(11) DEFAULT 10 COMMENT '提示词注入消息上限',
  `prompt_template` text COMMENT '租户提示词模板',
  `action_policy_json` longtext COMMENT '动作策略JSON',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_sys_ai_config_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI租户配置表';

CREATE TABLE IF NOT EXISTS `sys_ai_audit` (
  `audit_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '审计ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户账号',
  `question_type` varchar(64) DEFAULT NULL COMMENT '问题类型',
  `interaction_level` varchar(16) DEFAULT NULL COMMENT '交互分级（L1/L2/L3）',
  `action_key` varchar(64) DEFAULT NULL COMMENT '动作编码',
  `action_confirmed` char(1) DEFAULT '0' COMMENT '是否确认动作（1是0否）',
  `success_flag` char(1) DEFAULT '1' COMMENT '是否成功（1成功0失败）',
  `prompt_injection_flag` char(1) DEFAULT '0' COMMENT '提示词注入命中标记（1命中0未命中）',
  `sensitive_hit_flag` char(1) DEFAULT '0' COMMENT '敏感命中标记（1命中0未命中）',
  `request_excerpt` varchar(500) DEFAULT NULL COMMENT '请求摘要',
  `response_excerpt` varchar(500) DEFAULT NULL COMMENT '响应摘要',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '耗时毫秒',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`audit_id`),
  KEY `idx_sys_ai_audit_tenant_time` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI审计日志表';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'AI配置',
       COALESCE((SELECT parent_id FROM `sys_menu` WHERE `path` = '/system/config' LIMIT 1), 0),
       4,
       '/system/ai-config',
       '/views/system/ai-config/index',
       1,
       'C',
       '0',
       '0',
       'system:ai:config:list',
       NULL,
       'system',
       NOW(),
       'AI配置中心菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/ai-config');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name,
       menu_parent.menu_id,
       button_def.order_num,
       '/system/ai-config',
       NULL,
       1,
       'F',
       '0',
       '0',
       button_def.perms,
       NULL,
       'system',
       NOW(),
       'AI配置中心按钮权限'
FROM (
  SELECT 'AI配置查询' AS menu_name, 1 AS order_num, 'system:ai:config:query' AS perms
  UNION ALL SELECT 'AI配置修改', 2, 'system:ai:config:edit'
  UNION ALL SELECT 'AI策略修改', 3, 'system:ai:policy:edit'
  UNION ALL SELECT 'AI审计查看', 4, 'system:ai:audit:list'
  UNION ALL SELECT 'AI运营查看', 5, 'system:ai:ops:view'
) button_def
INNER JOIN `sys_menu` menu_parent ON menu_parent.path = '/system/ai-config'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = '/system/ai-config'
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT role_item.tenant_id,
       role_item.role_id,
       menu_item.menu_id
FROM `sys_role` role_item
INNER JOIN `sys_menu` menu_item ON (
  menu_item.path = '/system/ai-config'
  OR menu_item.perms IN (
    'system:ai:config:list',
    'system:ai:config:query',
    'system:ai:config:edit',
    'system:ai:policy:edit',
    'system:ai:audit:list',
    'system:ai:ops:view'
  )
)
LEFT JOIN `sys_role_menu` existed_role_menu ON existed_role_menu.role_id = role_item.role_id
    AND existed_role_menu.menu_id = menu_item.menu_id
WHERE role_item.role_key = 'admin'
  AND existed_role_menu.menu_id IS NULL;
