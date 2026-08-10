-- 20260801_02: AI 面板二期 —— 服务端会话存档
-- 背景：AI 会话此前只存在浏览器 localStorage 里，换设备或清缓存即丢失，也无法在面板上回放。
-- 说明：
--   1. 建表语句使用 IF NOT EXISTS，可重复执行；
--   2. 会话按 tenant_id + user_id 隔离，del_flag 做逻辑删除；
--   3. blocks_json 保存助手消息附带的指标卡/表格/图表，用于会话回放时重建可视化；
--   4. 菜单与权限点为 AI 会话管理入口，已存在则跳过。

CREATE TABLE IF NOT EXISTS `sys_ai_session` (
  `session_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `title` varchar(255) DEFAULT NULL COMMENT '会话标题',
  `model` varchar(128) DEFAULT NULL COMMENT '会话使用的模型编号',
  `message_count` int(11) NOT NULL DEFAULT 0 COMMENT '消息条数',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_sys_ai_session_owner` (`tenant_id`, `user_id`, `del_flag`, `last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS `sys_ai_message` (
  `message_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint(20) NOT NULL COMMENT '会话ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `role` varchar(16) NOT NULL COMMENT '消息角色（user/assistant）',
  `content` mediumtext COMMENT '消息内容',
  `blocks_json` longtext COMMENT '结构化区块JSON（指标卡/表格/图表）',
  `action_key` varchar(64) DEFAULT NULL COMMENT '关联动作编码',
  `prompt_tokens` int(11) DEFAULT NULL COMMENT '输入token数',
  `completion_tokens` int(11) DEFAULT NULL COMMENT '输出token数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_sys_ai_message_session` (`session_id`, `message_id`),
  KEY `idx_sys_ai_message_owner` (`tenant_id`, `user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话消息表';

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
       'AI会话与只读数据权限'
FROM (
  SELECT 'AI会话查看' AS menu_name, 6 AS order_num, 'system:ai:session:list' AS perms
  UNION ALL SELECT 'AI会话删除', 7, 'system:ai:session:remove'
) button_def
INNER JOIN `sys_menu` menu_parent ON menu_parent.path = '/system/ai-config' AND menu_parent.menu_type = 'C'
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
INNER JOIN `sys_menu` menu_item ON menu_item.perms IN (
    'system:ai:session:list',
    'system:ai:session:remove'
  )
LEFT JOIN `sys_role_menu` existed_role_menu ON existed_role_menu.role_id = role_item.role_id
    AND existed_role_menu.menu_id = menu_item.menu_id
WHERE role_item.role_key = 'admin'
  AND existed_role_menu.menu_id IS NULL;
