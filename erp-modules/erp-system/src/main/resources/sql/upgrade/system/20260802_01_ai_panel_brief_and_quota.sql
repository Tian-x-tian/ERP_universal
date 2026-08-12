-- 20260802_01: AI 面板三期 —— 每日简报缓存 + 租户配额 + 面板菜单
-- 说明：
--   1. 建表用 IF NOT EXISTS，加列先查 information_schema，整脚本可重复执行；
--   2. 配额列默认 NULL，表示「按实例配置」；显式写 0 表示该租户不限制；
--   3. sys_ai_brief 按 租户+用户+日期+类型 唯一，生成权靠这条唯一键与条件更新抢占，多实例安全；
--   4. 菜单与权限点为 AI 面板入口，已存在则跳过。

-- ---------- sys_ai_config 配额列 ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_config` ADD COLUMN `tenant_daily_request_limit` int(11) DEFAULT NULL COMMENT ''租户每日请求上限（0不限制，NULL按实例配置）'' AFTER `action_policy_json`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_config'
      AND COLUMN_NAME = 'tenant_daily_request_limit'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_config` ADD COLUMN `tenant_daily_token_limit` int(11) DEFAULT NULL COMMENT ''租户每日token上限（0不限制，NULL按实例配置）'' AFTER `tenant_daily_request_limit`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_config'
      AND COLUMN_NAME = 'tenant_daily_token_limit'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_config` ADD COLUMN `user_daily_request_limit` int(11) DEFAULT NULL COMMENT ''单用户每日请求上限（0不限制，NULL按实例配置）'' AFTER `tenant_daily_token_limit`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_config'
      AND COLUMN_NAME = 'user_daily_request_limit'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- sys_ai_brief ----------
CREATE TABLE IF NOT EXISTS `sys_ai_brief` (
  `brief_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '简报ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `brief_date` date NOT NULL COMMENT '简报日期',
  `brief_type` varchar(32) NOT NULL DEFAULT 'daily' COMMENT '简报类型',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING生成中 READY可用 FAILED失败）',
  `summary` text COMMENT '简报解读文本',
  `blocks_json` longtext COMMENT '结构化区块JSON（指标卡/表格/图表）',
  `model` varchar(128) DEFAULT NULL COMMENT '生成使用的模型编号',
  `generate_ms` bigint(20) DEFAULT NULL COMMENT '生成耗时毫秒',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`brief_id`),
  UNIQUE KEY `uk_sys_ai_brief_owner_date` (`tenant_id`,`user_id`,`brief_date`,`brief_type`),
  KEY `idx_sys_ai_brief_status` (`tenant_id`,`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI每日简报表';

-- ---------- AI 面板菜单与权限 ----------
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'AI面板',
       COALESCE((SELECT parent_id FROM `sys_menu` WHERE `path` = '/system/ai-config' AND `menu_type` = 'C' LIMIT 1), 0),
       3,
       '/system/ai-panel',
       '/views/system/ai-panel/index',
       1,
       'C',
       '0',
       '0',
       'system:ai:panel:view',
       NULL,
       'system',
       NOW(),
       'AI 面板菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/ai-panel');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT role_item.tenant_id,
       role_item.role_id,
       menu_item.menu_id
FROM `sys_role` role_item
INNER JOIN `sys_menu` menu_item ON menu_item.perms = 'system:ai:panel:view'
LEFT JOIN `sys_role_menu` existed_role_menu ON existed_role_menu.role_id = role_item.role_id
    AND existed_role_menu.menu_id = menu_item.menu_id
WHERE role_item.role_key = 'admin'
  AND existed_role_menu.menu_id IS NULL;
