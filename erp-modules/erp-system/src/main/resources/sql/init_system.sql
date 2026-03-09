-- ERP System Module DDL
-- Database: erp_system (Recommended)
-- Updated: 2026-03-07

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 租户管理表 (sys_tenant)
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `name` varchar(50) DEFAULT NULL COMMENT '租户名称',
  `contact_user` varchar(20) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户管理表';

-- ----------------------------
-- 2. 公司表 (sys_company)
-- ----------------------------
DROP TABLE IF EXISTS `sys_company`;
CREATE TABLE `sys_company` (
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

-- ----------------------------
-- 3. 部门表 (sys_dept)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `company_id` bigint(20) DEFAULT NULL COMMENT '公司ID',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父部门ID',
  `ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`),
  KEY `idx_dept_tenant_company` (`tenant_id`, `company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- 4. 岗位表 (sys_post)
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
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

-- ----------------------------
-- 5. 用户信息表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `idx_user_tenant` (`user_name`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ----------------------------
-- 6. 角色信息表 (sys_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(4) NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1全部 2自定义 3本部门 4本部门及以下）',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- ----------------------------
-- 7. 菜单权限表 (sys_menu)
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `is_frame` int(1) DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ----------------------------
-- 8. 用户和角色关联表 (sys_user_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_role_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- ----------------------------
-- 9. 用户和岗位关联表 (sys_user_post)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`),
  KEY `idx_user_post_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和岗位关联表';

-- ----------------------------
-- 10. 角色和菜单关联表 (sys_role_menu)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_role_menu_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- ----------------------------
-- 11. 角色和部门关联表 (sys_role_dept)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`),
  KEY `idx_role_dept_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和部门关联表';

-- ----------------------------
-- 12. 字典类型表 (sys_dict_type)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------
-- 13. 字典数据表 (sys_dict_data)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int(4) DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ----------------------------
-- 14. 参数配置表 (sys_config)
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id` int(5) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

-- ----------------------------
-- 15. 审计日志表 (sys_audit_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
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

-- ----------------------------
-- 16. 登录日志表 (sys_login_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
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

-- ----------------------------
-- 17. 系统消息通知表 (sys_notice)
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
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

-- ----------------------------
-- 18. 流程待办任务表 (sys_todo_task)
-- ----------------------------
DROP TABLE IF EXISTS `sys_todo_task`;
CREATE TABLE `sys_todo_task` (
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

-- ----------------------------
-- 19. 区域主数据表 (sys_region)
-- ----------------------------
DROP TABLE IF EXISTS `sys_region`;
CREATE TABLE `sys_region` (
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

-- ----------------------------
-- 20. 编码规则表 (sys_code_rule)
-- ----------------------------
DROP TABLE IF EXISTS `sys_code_rule`;
CREATE TABLE `sys_code_rule` (
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

-- ----------------------------
-- 21. 初始化基础数据
-- ----------------------------
INSERT INTO `sys_tenant` (`id`, `tenant_id`, `name`, `contact_user`, `contact_phone`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
VALUES (1, '000000', '默认租户', '系统管理员', '13800000000', '0', '0', 'system', NOW(), '系统初始化租户');

INSERT INTO `sys_company` (`company_id`, `tenant_id`, `company_code`, `company_name`, `parent_company_id`, `ancestors`, `leader`, `phone`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
VALUES (1, '000000', 'HQ', '总部公司', 0, '0', '管理员', '13800000000', '0', '0', 'system', NOW(), '系统初始化公司');

INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `company_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`)
VALUES (1, '000000', 1, 0, '0', '总部', 1, '管理员', '13800000000', 'admin@erp.com', '0', '0', 'system', NOW());

INSERT INTO `sys_post` (`post_id`, `tenant_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `remark`)
VALUES (1, '000000', 'CEO', '系统管理员岗位', 1, '0', 'system', NOW(), '系统初始化岗位');

INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
VALUES (1, '000000', '超级管理员', 'admin', 1, '1', '0', '0', 'system', NOW(), '系统初始化角色');

INSERT INTO `sys_user` (`user_id`, `tenant_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
VALUES (1, '000000', 1, 'admin', '系统管理员', '00', 'admin@erp.com', '13800000000', '0', '', '$2a$10$/V6UcHU5GP.R6V9B9Iqage9GwBCI42PgHvBVfkozG3AMn9V5eUcpW', '0', '0', 'system', NOW(), '默认管理员账号（密码：admin123）');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
  (1, '首页', 0, 1, '/home', '/views/home/index', 1, 'C', '0', '0', 'system:home:view', NULL, 'system', NOW(), '系统初始化菜单'),
  (2, '系统管理', 0, 2, '/system', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (3, '租户管理', 2, 1, '/system/tenant', '/views/system/tenant/index', 1, 'C', '0', '0', 'system:tenant:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (4, '用户管理', 2, 2, '/system/user', '/views/system/user/index', 1, 'C', '0', '0', 'system:user:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (5, '角色管理', 2, 3, '/system/role', '/views/system/role/index', 1, 'C', '0', '0', 'system:role:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (6, '菜单管理', 2, 4, '/system/menu', '/views/system/menu/index', 1, 'C', '0', '0', 'system:menu:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (7, '部门管理', 2, 5, '/system/dept', '/views/system/dept/index', 1, 'C', '0', '0', 'system:dept:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (8, '字典管理', 2, 6, '/system/dict', '/views/system/dict/index', 1, 'C', '0', '0', 'system:dict:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (9, '参数管理', 2, 7, '/system/config', '/views/system/config/index', 1, 'C', '0', '0', 'system:config:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (10, '公司管理', 2, 8, '/system/company', '/views/system/company/index', 1, 'C', '0', '0', 'system:company:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (11, '岗位管理', 2, 9, '/system/post', '/views/system/post/index', 1, 'C', '0', '0', 'system:post:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (12, '审计日志', 2, 10, '/system/audit-log', '/views/platform/audit-log/index', 1, 'C', '0', '0', 'system:audit:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (13, '登录日志', 2, 11, '/system/login-log', '/views/system/login-log/index', 1, 'C', '1', '0', 'system:loginLog:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (14, '区域主数据', 2, 12, '/system/region', '/views/system/region/index', 1, 'C', '1', '0', 'system:region:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (15, '平台底座', 0, 3, '/platform', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (16, '消息待办中心', 15, 1, '/platform/todo-center', '/views/platform/todo-center/index', 1, 'C', '0', '0', 'system:todo:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (17, '编码规则', 15, 2, '/platform/code-rule', '/views/platform/code-rule/index', 1, 'C', '0', '0', 'system:codeRule:list', NULL, 'system', NOW(), '系统初始化菜单');

INSERT INTO `sys_user_role` (`tenant_id`, `user_id`, `role_id`) VALUES ('000000', 1, 1);
INSERT INTO `sys_user_post` (`tenant_id`, `user_id`, `post_id`) VALUES ('000000', 1, 1);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', 1, menu_id
FROM sys_menu;

INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) VALUES
  (1, '系统开关', 'sys_normal_disable', '0', 'system', NOW(), '系统初始化字典类型'),
  (2, '用户性别', 'sys_user_sex', '0', 'system', NOW(), '系统初始化字典类型');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
  (1, 1, '正常', '0', 'sys_normal_disable', 'Y', '0', 'system', NOW(), '系统初始化字典数据'),
  (2, 2, '停用', '1', 'sys_normal_disable', 'N', '0', 'system', NOW(), '系统初始化字典数据'),
  (3, 1, '男', '0', 'sys_user_sex', 'Y', '0', 'system', NOW(), '系统初始化字典数据'),
  (4, 2, '女', '1', 'sys_user_sex', 'N', '0', 'system', NOW(), '系统初始化字典数据'),
  (5, 3, '未知', '2', 'sys_user_sex', 'N', '0', 'system', NOW(), '系统初始化字典数据');

INSERT INTO `sys_region` (`region_id`, `tenant_id`, `region_code`, `region_name`, `parent_id`, `ancestors`, `region_level`, `order_num`, `status`, `create_time`)
VALUES
  (1, '000000', 'CN', '中国', 0, '0', 1, 1, '0', NOW()),
  (2, '000000', 'CN-BJ', '北京市', 1, '0,1', 2, 1, '0', NOW()),
  (3, '000000', 'CN-SH', '上海市', 1, '0,1', 2, 2, '0', NOW());

INSERT INTO `sys_code_rule` (`rule_id`, `tenant_id`, `rule_code`, `rule_name`, `prefix`, `date_pattern`, `seq_length`, `current_seq`, `status`, `create_time`)
VALUES
  (1, '000000', 'ORG_DEPT', '部门编码', 'DP', 'yyyyMMdd', 4, 12, '0', NOW()),
  (2, '000000', 'WF_BIZ', '流程业务单号', 'WF', 'yyyyMMdd', 5, 25, '0', NOW()),
  (3, '000000', 'ATTACH', '附件编码', 'AT', 'yyyyMM', 4, 16, '0', NOW());

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `title`, `notice_type`, `source`, `business_no`, `content`, `receiver_user_id`, `status`, `create_time`)
VALUES
  (1, '000000', '流程引擎已发布新版本，请核查审批节点配置', '系统公告', '流程引擎', NULL, '流程引擎发布 v2.0.1，请检查关键审批流配置。', 1, '0', NOW()),
  (2, '000000', '导入任务 IM20260307-01 执行完成', '审批通知', '导入导出中心', 'IM20260307-01', '导入任务执行完成，请查看结果。', 1, '1', NOW()),
  (3, '000000', '报表中心出现数据延迟预警', '预警提醒', '报表中心', NULL, '近30分钟内报表数据刷新延迟超过阈值。', 1, '0', NOW());

INSERT INTO `sys_todo_task` (`todo_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
VALUES
  (1, '000000', '请假审批', '部门负责人审批', 'LV-20260307-001', 'H', '0', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), '请及时处理'),
  (2, '000000', '采购申请', '财务复核', 'PO-20260307-018', 'M', '1', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '处理中'),
  (3, '000000', '合同归档', '档案确认', 'CT-20260306-021', 'L', '0', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), '待签收');

SET FOREIGN_KEY_CHECKS = 1;

