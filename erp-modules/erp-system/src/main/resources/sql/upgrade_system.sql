-- ERP System Upgrade Script
-- 用途：在保留历史数据的场景下补齐一期底座依赖字段与表结构
-- 版本：2026-03-07

SET NAMES utf8mb4;

-- 1) 补齐租户字段，避免多租户插件查询 sys_user_role/sys_role_menu 报错
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_user_role` ADD COLUMN `tenant_id` varchar(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user_role'
      AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_user_role`
SET `tenant_id` = '000000'
WHERE `tenant_id` IS NULL OR `tenant_id` = '';

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_role_menu` ADD COLUMN `tenant_id` varchar(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_role_menu'
      AND COLUMN_NAME = 'tenant_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_role_menu`
SET `tenant_id` = '000000'
WHERE `tenant_id` IS NULL OR `tenant_id` = '';

-- 2) 补齐组织架构增强字段
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_dept` ADD COLUMN `company_id` bigint(20) DEFAULT NULL COMMENT ''公司ID''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_dept'
      AND COLUMN_NAME = 'company_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `sys_company` (
  `company_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公司ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `company_code` varchar(64) NOT NULL COMMENT '公司编码',
  `company_name` varchar(128) NOT NULL COMMENT '公司名称',
  `parent_company_id` bigint(20) DEFAULT 0 COMMENT '父公司ID',
  `ancestors` varchar(255) DEFAULT '' COMMENT '祖级列表',
  `leader` varchar(64) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`company_id`),
  UNIQUE KEY `idx_company_tenant_code` (`tenant_id`, `company_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';

CREATE TABLE IF NOT EXISTS `sys_post` (
  `post_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `post_name` varchar(64) NOT NULL COMMENT '岗位名称',
  `post_sort` int(4) DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '岗位状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `idx_post_tenant_code` (`tenant_id`, `post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

CREATE TABLE IF NOT EXISTS `sys_user_post` (
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`),
  KEY `idx_user_post_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和岗位关联表';

CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`),
  KEY `idx_role_dept_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和部门关联表';

-- 3) 补齐审计日志表
CREATE TABLE IF NOT EXISTS `sys_audit_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人账号',
  `operation_type` varchar(32) DEFAULT NULL COMMENT '操作类型',
  `request_method` varchar(16) DEFAULT NULL COMMENT '请求方法',
  `request_uri` varchar(500) DEFAULT NULL COMMENT '请求URI',
  `request_ip` varchar(64) DEFAULT NULL COMMENT '请求IP',
  `request_params` text COMMENT '请求参数',
  `response_code` int(11) DEFAULT NULL COMMENT '响应状态码',
  `success_flag` char(1) DEFAULT '1' COMMENT '是否成功（1成功 0失败）',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `cost_time` bigint(20) DEFAULT NULL COMMENT '耗时毫秒',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_audit_tenant_time` (`tenant_id`, `operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 4) 补齐审计日志菜单（系统管理）
SET @audit_menu_id = (
    SELECT menu_id
    FROM sys_menu
    WHERE path = '/system/audit-log'
    LIMIT 1
);

SET @audit_menu_id = IFNULL(@audit_menu_id, (
    SELECT IFNULL(MAX(menu_id), 0) + 1
    FROM sys_menu
));

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @audit_menu_id, '审计日志', 2, 10, '/system/audit-log', '/views/platform/audit-log/index', 1, 'C', '0', '0', 'system:audit:list', NULL, 'system', NOW(), '升级脚本补齐菜单'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu
    WHERE path = '/system/audit-log'
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @audit_menu_id
WHERE EXISTS (
    SELECT 1
    FROM sys_role
    WHERE role_id = 1
)
AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu
    WHERE role_id = 1
      AND menu_id = @audit_menu_id
);

-- 5) 补齐登录日志、消息待办、区域主数据、编码规则表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `info_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_login_tenant_time` (`tenant_id`, `login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS `sys_notice` (
  `notice_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `title` varchar(255) NOT NULL COMMENT '消息标题',
  `notice_type` varchar(32) NOT NULL COMMENT '消息类型',
  `source` varchar(64) DEFAULT NULL COMMENT '消息来源',
  `business_no` varchar(64) DEFAULT NULL COMMENT '关联业务单号',
  `content` text COMMENT '消息内容',
  `receiver_user_id` bigint(20) NOT NULL COMMENT '接收人用户ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_receiver` (`tenant_id`, `receiver_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息通知表';

CREATE TABLE IF NOT EXISTS `sys_todo_task` (
  `todo_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `process_name` varchar(100) NOT NULL COMMENT '流程名称',
  `node_name` varchar(100) DEFAULT NULL COMMENT '当前节点',
  `business_no` varchar(64) DEFAULT NULL COMMENT '业务单号',
  `priority` char(1) DEFAULT 'M' COMMENT '优先级（H高 M中 L低）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0待处理 1处理中 2已完成）',
  `assignee_user_id` bigint(20) NOT NULL COMMENT '办理人用户ID',
  `due_time` datetime DEFAULT NULL COMMENT '截止时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `finish_time` datetime DEFAULT NULL COMMENT '办结时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`todo_id`),
  KEY `idx_todo_assignee` (`tenant_id`, `assignee_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程待办任务表';

CREATE TABLE IF NOT EXISTS `sys_region` (
  `region_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '区域ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `region_code` varchar(64) NOT NULL COMMENT '区域编码',
  `region_name` varchar(128) NOT NULL COMMENT '区域名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父区域ID',
  `ancestors` varchar(255) DEFAULT '' COMMENT '祖级列表',
  `region_level` int(2) DEFAULT 1 COMMENT '区域层级',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`region_id`),
  UNIQUE KEY `idx_region_tenant_code` (`tenant_id`, `region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域主数据表';

CREATE TABLE IF NOT EXISTS `sys_code_rule` (
  `rule_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `prefix` varchar(32) DEFAULT NULL COMMENT '编码前缀',
  `date_pattern` varchar(32) DEFAULT 'yyyyMMdd' COMMENT '日期格式',
  `seq_length` int(4) DEFAULT 4 COMMENT '流水位数',
  `current_seq` bigint(20) DEFAULT 0 COMMENT '当前流水值',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `idx_code_rule_tenant_code` (`tenant_id`, `rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编码规则表';

-- 6) 初始化区域与编码规则基础数据
INSERT INTO `sys_region` (`region_id`, `tenant_id`, `region_code`, `region_name`, `parent_id`, `ancestors`, `region_level`, `order_num`, `status`, `create_time`)
SELECT 1, '000000', 'CN', '中国', 0, '0', 1, 1, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_region` WHERE `region_id` = 1);

INSERT INTO `sys_region` (`region_id`, `tenant_id`, `region_code`, `region_name`, `parent_id`, `ancestors`, `region_level`, `order_num`, `status`, `create_time`)
SELECT 2, '000000', 'CN-BJ', '北京市', 1, '0,1', 2, 1, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_region` WHERE `region_id` = 2);

INSERT INTO `sys_region` (`region_id`, `tenant_id`, `region_code`, `region_name`, `parent_id`, `ancestors`, `region_level`, `order_num`, `status`, `create_time`)
SELECT 3, '000000', 'CN-SH', '上海市', 1, '0,1', 2, 2, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_region` WHERE `region_id` = 3);

INSERT INTO `sys_code_rule` (`rule_id`, `tenant_id`, `rule_code`, `rule_name`, `prefix`, `date_pattern`, `seq_length`, `current_seq`, `status`, `create_time`)
SELECT 1, '000000', 'ORG_DEPT', '部门编码', 'DP', 'yyyyMMdd', 4, 12, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_code_rule` WHERE `rule_id` = 1);

INSERT INTO `sys_code_rule` (`rule_id`, `tenant_id`, `rule_code`, `rule_name`, `prefix`, `date_pattern`, `seq_length`, `current_seq`, `status`, `create_time`)
SELECT 2, '000000', 'WF_BIZ', '流程业务单号', 'WF', 'yyyyMMdd', 5, 25, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_code_rule` WHERE `rule_id` = 2);

INSERT INTO `sys_code_rule` (`rule_id`, `tenant_id`, `rule_code`, `rule_name`, `prefix`, `date_pattern`, `seq_length`, `current_seq`, `status`, `create_time`)
SELECT 3, '000000', 'ATTACH', '附件编码', 'AT', 'yyyyMM', 4, 16, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_code_rule` WHERE `rule_id` = 3);

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `title`, `notice_type`, `source`, `business_no`, `content`, `receiver_user_id`, `status`, `create_time`)
SELECT 1, '000000', '流程引擎已发布新版本，请核查审批节点配置', '系统公告', '流程引擎', NULL, '流程引擎发布 v2.0.1，请检查关键审批流配置。', 1, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_notice` WHERE `notice_id` = 1);

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `title`, `notice_type`, `source`, `business_no`, `content`, `receiver_user_id`, `status`, `create_time`)
SELECT 2, '000000', '导入任务 IM20260307-01 执行完成', '审批通知', '导入导出中心', 'IM20260307-01', '导入任务执行完成，请查看结果。', 1, '1', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_notice` WHERE `notice_id` = 2);

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `title`, `notice_type`, `source`, `business_no`, `content`, `receiver_user_id`, `status`, `create_time`)
SELECT 3, '000000', '报表中心出现数据延迟预警', '预警提醒', '报表中心', NULL, '近30分钟内报表数据刷新延迟超过阈值。', 1, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_notice` WHERE `notice_id` = 3);

INSERT INTO `sys_todo_task` (`todo_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
SELECT 1, '000000', '请假审批', '部门负责人审批', 'LV-20260307-001', 'H', '0', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), '请及时处理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_todo_task` WHERE `todo_id` = 1);

INSERT INTO `sys_todo_task` (`todo_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
SELECT 2, '000000', '采购申请', '财务复核', 'PO-20260307-018', 'M', '1', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '处理中'
WHERE NOT EXISTS (SELECT 1 FROM `sys_todo_task` WHERE `todo_id` = 2);

INSERT INTO `sys_todo_task` (`todo_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
SELECT 3, '000000', '合同归档', '档案确认', 'CT-20260306-021', 'L', '0', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), '待签收'
WHERE NOT EXISTS (SELECT 1 FROM `sys_todo_task` WHERE `todo_id` = 3);

-- 7) 补齐平台菜单与系统新增菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '登录日志', 2, 11, '/system/login-log', '/views/system/login-log/index', 1, 'C', '1', '0', 'system:loginLog:list', NULL, 'system', NOW(), '升级脚本补齐菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/login-log');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '区域主数据', 2, 12, '/system/region', '/views/system/region/index', 1, 'C', '1', '0', 'system:region:list', NULL, 'system', NOW(), '升级脚本补齐菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/region');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '平台底座', 0, 3, '/platform', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '升级脚本补齐目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/platform');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '消息待办中心',
       (SELECT menu_id FROM sys_menu WHERE path = '/platform' LIMIT 1),
       1,
       '/platform/todo-center',
       '/views/platform/todo-center/index',
       1,
       'C',
       '0',
       '0',
       'system:todo:list',
       NULL,
       'system',
       NOW(),
       '升级脚本补齐菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/platform/todo-center');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '编码规则',
       (SELECT menu_id FROM sys_menu WHERE path = '/platform' LIMIT 1),
       2,
       '/platform/code-rule',
       '/views/platform/code-rule/index',
       1,
       'C',
       '0',
       '0',
       'system:codeRule:list',
       NULL,
       'system',
       NOW(),
       '升级脚本补齐菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/platform/code-rule');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.path IN ('/system/login-log', '/system/region', '/platform', '/platform/todo-center', '/platform/code-rule')
  AND EXISTS (SELECT 1 FROM sys_role WHERE role_id = 1)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = 1
        AND rm.menu_id = m.menu_id
  );
