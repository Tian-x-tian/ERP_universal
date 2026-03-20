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
  `token_version` int(11) NOT NULL DEFAULT 0 COMMENT 'Token版本号',
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
-- 16. 操作日志表 (sys_oper_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人账号',
  `request_method` varchar(16) DEFAULT NULL COMMENT '请求方法',
  `request_uri` varchar(500) DEFAULT NULL COMMENT '请求URI',
  `request_ip` varchar(64) DEFAULT NULL COMMENT '请求IP',
  `request_params` text COMMENT '请求参数',
  `response_code` int(11) DEFAULT NULL COMMENT '响应状态码',
  `success_flag` char(1) DEFAULT '1' COMMENT '是否成功（1成功 0失败）',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `cost_time` bigint(20) DEFAULT NULL COMMENT '耗时毫秒',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_oper_tenant_time` (`tenant_id`, `operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 17. 登录日志表 (sys_login_log)
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
-- 18. 系统消息通知表 (sys_notice)
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
  `delivery_channel` varchar(16) DEFAULT 'IN_APP' COMMENT '送达渠道（IN_APP/SMS/WECOM）',
  `delivery_status` char(1) DEFAULT '2' COMMENT '送达状态（0待发送 1发送中 2已送达 3失败）',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间',
  `external_message_id` varchar(100) DEFAULT NULL COMMENT '外部消息ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_receiver` (`tenant_id`, `receiver_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息通知表';

-- ----------------------------
-- 19. 流程待办任务表 (sys_todo_task)
-- ----------------------------
DROP TABLE IF EXISTS `sys_todo_task`;
CREATE TABLE `sys_todo_task` (
  `todo_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `instance_id` bigint(20) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` bigint(20) DEFAULT NULL COMMENT '流程任务ID',
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
  KEY `idx_todo_assignee` (`tenant_id`, `assignee_user_id`, `status`),
  KEY `idx_todo_workflow` (`tenant_id`, `instance_id`, `task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程待办任务表';

-- ----------------------------
-- 20. 流程定义表 (sys_wf_definition)
-- ----------------------------
DROP TABLE IF EXISTS `sys_wf_definition`;
CREATE TABLE `sys_wf_definition` (
  `definition_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `process_name` varchar(128) NOT NULL COMMENT '流程名称',
  `category` varchar(64) DEFAULT 'custom' COMMENT '流程分类',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0草稿 1已发布 2停用）',
  `published_slot` tinyint(1) GENERATED ALWAYS AS (CASE WHEN `status` = '1' THEN 1 ELSE NULL END) STORED COMMENT '已发布唯一槽位',
  `form_schema` longtext COMMENT '表单结构JSON',
  `model_content` longtext COMMENT '流程设计JSON',
  `publish_by` varchar(64) DEFAULT NULL COMMENT '发布人',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`definition_id`),
  UNIQUE KEY `idx_wf_def_key_ver` (`tenant_id`, `process_key`, `version`),
  UNIQUE KEY `uk_wf_def_publish_slot` (`tenant_id`, `process_key`, `published_slot`),
  KEY `idx_wf_def_status` (`tenant_id`, `status`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- ----------------------------
-- 21. 流程业务动作绑定表 (sys_wf_business_binding)
-- ----------------------------
DROP TABLE IF EXISTS `sys_wf_business_binding`;
CREATE TABLE `sys_wf_business_binding` (
  `binding_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `domain_type` varchar(64) NOT NULL COMMENT '业务域类型',
  `action_code` varchar(32) NOT NULL COMMENT '业务动作编码',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认（0否 1是）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `priority` int(11) DEFAULT 100 COMMENT '优先级，数值越小越优先',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`binding_id`),
  UNIQUE KEY `uk_wf_business_binding` (`tenant_id`,`domain_type`,`action_code`,`process_key`),
  KEY `idx_wf_business_action` (`tenant_id`,`domain_type`,`action_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务动作绑定表';

-- ----------------------------
-- 22. 流程实例表 (sys_wf_instance)
-- ----------------------------
DROP TABLE IF EXISTS `sys_wf_instance`;
CREATE TABLE `sys_wf_instance` (
  `instance_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `definition_version` int(11) DEFAULT NULL COMMENT '发起时流程定义版本号',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `process_name` varchar(128) NOT NULL COMMENT '流程名称',
  `category` varchar(64) DEFAULT 'custom' COMMENT '流程分类',
  `business_no` varchar(64) NOT NULL COMMENT '业务单号',
  `business_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `form_data` longtext COMMENT '表单数据JSON',
  `form_schema_snapshot` longtext COMMENT '发起时表单结构快照JSON',
  `model_content_snapshot` longtext COMMENT '发起时流程模型快照JSON',
  `current_node` varchar(128) DEFAULT NULL COMMENT '当前节点',
  `initiator_user_id` bigint(20) NOT NULL COMMENT '发起人用户ID',
  `initiator_user_name` varchar(64) DEFAULT NULL COMMENT '发起人账号',
  `initiator_nick_name` varchar(64) DEFAULT NULL COMMENT '发起人昵称',
  `status` char(1) DEFAULT '0' COMMENT '状态（0进行中 1已完成 2已驳回 3已撤销）',
  `start_time` datetime DEFAULT NULL COMMENT '发起时间',
  `finish_time` datetime DEFAULT NULL COMMENT '结束时间',
  `last_action` varchar(32) DEFAULT NULL COMMENT '最近动作',
  `last_action_user_id` bigint(20) DEFAULT NULL COMMENT '最近动作人ID',
  `last_action_user_name` varchar(64) DEFAULT NULL COMMENT '最近动作人账号',
  `last_action_time` datetime DEFAULT NULL COMMENT '最近动作时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`instance_id`),
  KEY `idx_wf_inst_status` (`tenant_id`, `status`, `start_time`),
  KEY `idx_wf_inst_business` (`tenant_id`, `business_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- ----------------------------
-- 23. 流程任务表 (sys_wf_task)
-- ----------------------------
DROP TABLE IF EXISTS `sys_wf_task`;
CREATE TABLE `sys_wf_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) DEFAULT NULL COMMENT '节点编码',
  `node_name` varchar(128) DEFAULT NULL COMMENT '节点名称',
  `candidate_user_ids` varchar(500) DEFAULT NULL COMMENT '候选办理人ID列表',
  `assignee_user_id` bigint(20) DEFAULT NULL COMMENT '办理人用户ID',
  `assignee_user_name` varchar(64) DEFAULT NULL COMMENT '办理人账号',
  `assignee_nick_name` varchar(64) DEFAULT NULL COMMENT '办理人昵称',
  `status` char(1) DEFAULT '0' COMMENT '状态（0待处理 1处理中 2已同意 3已驳回 4已转交 5已取消）',
  `action_comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `todo_id` bigint(20) DEFAULT NULL COMMENT '关联待办ID',
  `due_time` datetime DEFAULT NULL COMMENT '截止时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `finish_time` datetime DEFAULT NULL COMMENT '办结时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_wf_task_assignee` (`tenant_id`, `assignee_user_id`, `status`),
  KEY `idx_wf_task_instance` (`tenant_id`, `instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务表';

-- ----------------------------
-- 24. 流程任务动作记录表 (sys_wf_task_action)
-- ----------------------------
DROP TABLE IF EXISTS `sys_wf_task_action`;
CREATE TABLE `sys_wf_task_action` (
  `action_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '动作记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `task_id` bigint(20) DEFAULT NULL COMMENT '流程任务ID',
  `definition_id` bigint(20) DEFAULT NULL COMMENT '流程定义ID',
  `node_name` varchar(128) DEFAULT NULL COMMENT '节点名称',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `action_user_id` bigint(20) DEFAULT NULL COMMENT '动作人用户ID',
  `action_user_name` varchar(64) DEFAULT NULL COMMENT '动作人账号',
  `action_nick_name` varchar(64) DEFAULT NULL COMMENT '动作人昵称',
  `from_assignee_user_id` bigint(20) DEFAULT NULL COMMENT '来源办理人ID',
  `to_assignee_user_id` bigint(20) DEFAULT NULL COMMENT '目标办理人ID',
  `action_comment` varchar(500) DEFAULT NULL COMMENT '动作意见',
  `action_time` datetime DEFAULT NULL COMMENT '动作时间',
  PRIMARY KEY (`action_id`),
  KEY `idx_wf_action_instance` (`tenant_id`, `instance_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务动作记录表';

-- ----------------------------
-- 24. 区域主数据表 (sys_region)
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
-- 25. 编码规则表 (sys_code_rule)
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
-- 26. 主数据管理表（MDM）
-- ----------------------------
DROP TABLE IF EXISTS `mdm_org`;
CREATE TABLE `mdm_org` (
  `org_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_code` varchar(64) NOT NULL COMMENT '组织编码',
  `org_name` varchar(128) NOT NULL COMMENT '组织名称',
  `org_type` varchar(32) DEFAULT NULL COMMENT '组织类型',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父组织ID',
  `ancestors` varchar(255) DEFAULT '0' COMMENT '祖级列表',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`org_id`),
  UNIQUE KEY `idx_mdm_org_tenant_code` (`tenant_id`, `org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM组织主数据表';

DROP TABLE IF EXISTS `mdm_cost_center`;
CREATE TABLE `mdm_cost_center` (
  `cc_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成本中心ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `cc_code` varchar(64) NOT NULL COMMENT '成本中心编码',
  `cc_name` varchar(128) NOT NULL COMMENT '成本中心名称',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父级成本中心ID',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`cc_id`),
  UNIQUE KEY `idx_mdm_cc_tenant_code` (`tenant_id`, `cc_code`),
  KEY `idx_mdm_cc_org` (`tenant_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM成本中心主数据表';

DROP TABLE IF EXISTS `mdm_project`;
CREATE TABLE `mdm_project` (
  `project_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `project_code` varchar(64) NOT NULL COMMENT '项目编码',
  `project_name` varchar(128) NOT NULL COMMENT '项目名称',
  `manager_emp_id` bigint(20) DEFAULT NULL COMMENT '项目经理员工ID',
  `customer_id` bigint(20) DEFAULT NULL COMMENT '关联客户ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '归属组织ID',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `idx_mdm_project_tenant_code` (`tenant_id`, `project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM项目主数据表';

DROP TABLE IF EXISTS `mdm_settle_method`;
CREATE TABLE `mdm_settle_method` (
  `settle_method_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结算方式ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `settle_code` varchar(64) NOT NULL COMMENT '结算方式编码',
  `settle_name` varchar(128) NOT NULL COMMENT '结算方式名称',
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`settle_method_id`),
  UNIQUE KEY `idx_mdm_settle_tenant_code` (`tenant_id`, `settle_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM结算方式字典';

DROP TABLE IF EXISTS `mdm_tax_rate`;
CREATE TABLE `mdm_tax_rate` (
  `tax_rate_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '税率ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `tax_code` varchar(64) NOT NULL COMMENT '税率编码',
  `tax_name` varchar(128) NOT NULL COMMENT '税率名称',
  `tax_rate` decimal(8,4) NOT NULL COMMENT '税率值',
  `effective_from` date DEFAULT NULL COMMENT '生效开始日期',
  `effective_to` date DEFAULT NULL COMMENT '生效结束日期',
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tax_rate_id`),
  UNIQUE KEY `idx_mdm_tax_tenant_code` (`tenant_id`, `tax_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM税率字典';

DROP TABLE IF EXISTS `mdm_currency`;
CREATE TABLE `mdm_currency` (
  `currency_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '币种ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `currency_code` varchar(64) NOT NULL COMMENT '币种编码',
  `currency_name` varchar(128) NOT NULL COMMENT '币种名称',
  `symbol` varchar(16) DEFAULT NULL COMMENT '货币符号',
  `precision_scale` int(4) DEFAULT 2 COMMENT '金额精度',
  `effective_from` date DEFAULT NULL COMMENT '生效开始日期',
  `effective_to` date DEFAULT NULL COMMENT '生效结束日期',
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`currency_id`),
  UNIQUE KEY `idx_mdm_currency_tenant_code` (`tenant_id`, `currency_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM币种字典';

DROP TABLE IF EXISTS `mdm_uom`;
CREATE TABLE `mdm_uom` (
  `uom_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '单位ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `uom_code` varchar(64) NOT NULL COMMENT '单位编码',
  `uom_name` varchar(128) NOT NULL COMMENT '单位名称',
  `base_uom_code` varchar(64) DEFAULT NULL COMMENT '基准单位编码',
  `convert_rate` decimal(18,6) DEFAULT NULL COMMENT '换算比率',
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`uom_id`),
  UNIQUE KEY `idx_mdm_uom_tenant_code` (`tenant_id`, `uom_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM计量单位字典';

DROP TABLE IF EXISTS `mdm_customer`;
CREATE TABLE `mdm_customer` (
  `customer_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `customer_code` varchar(64) NOT NULL COMMENT '客户编码',
  `customer_name` varchar(128) NOT NULL COMMENT '客户名称',
  `short_name` varchar(128) DEFAULT NULL COMMENT '客户简称',
  `customer_type` varchar(32) DEFAULT NULL COMMENT '客户类型',
  `tax_no` varchar(64) DEFAULT NULL COMMENT '税号',
  `invoice_title` varchar(255) DEFAULT NULL COMMENT '发票抬头',
  `default_currency` varchar(32) DEFAULT NULL COMMENT '默认币种编码',
  `default_tax_rate` decimal(8,4) DEFAULT NULL COMMENT '默认税率',
  `credit_limit` decimal(18,2) DEFAULT NULL COMMENT '信用额度',
  `credit_days` int(11) DEFAULT NULL COMMENT '信用天数',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系人电话',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系人邮箱',
  `province` varchar(64) DEFAULT NULL COMMENT '省份',
  `city` varchar(64) DEFAULT NULL COMMENT '城市',
  `district` varchar(64) DEFAULT NULL COMMENT '区县',
  `detail_address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `settle_method_id` bigint(20) DEFAULT NULL COMMENT '结算方式ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '归属组织ID',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `idx_mdm_customer_tenant_code` (`tenant_id`, `customer_code`),
  KEY `idx_mdm_customer_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM客户主数据表';

DROP TABLE IF EXISTS `mdm_supplier`;
CREATE TABLE `mdm_supplier` (
  `supplier_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `supplier_code` varchar(64) NOT NULL COMMENT '供应商编码',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `short_name` varchar(128) DEFAULT NULL COMMENT '供应商简称',
  `supply_category` varchar(32) DEFAULT NULL COMMENT '供应类别',
  `tax_no` varchar(64) DEFAULT NULL COMMENT '税号',
  `default_currency` varchar(32) DEFAULT NULL COMMENT '默认币种编码',
  `default_tax_rate` decimal(8,4) DEFAULT NULL COMMENT '默认税率',
  `lead_time_days` int(11) DEFAULT NULL COMMENT '供货提前期天数',
  `quality_level` varchar(32) DEFAULT NULL COMMENT '质量等级',
  `bank_account_info` varchar(255) DEFAULT NULL COMMENT '银行账号信息',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系人电话',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系人邮箱',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `idx_mdm_supplier_tenant_code` (`tenant_id`, `supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM供应商主数据表';

DROP TABLE IF EXISTS `mdm_item`;
CREATE TABLE `mdm_item` (
  `item_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '物料ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `item_code` varchar(64) NOT NULL COMMENT '物料编码',
  `item_name` varchar(128) NOT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `brand` varchar(128) DEFAULT NULL COMMENT '品牌',
  `item_type` varchar(32) DEFAULT NULL COMMENT '物料类型',
  `category_id` bigint(20) DEFAULT NULL COMMENT '物料分类ID',
  `unit_id` bigint(20) DEFAULT NULL COMMENT '主计量单位ID',
  `unit_convert` varchar(255) DEFAULT NULL COMMENT '辅助单位换算',
  `tax_rate_id` bigint(20) DEFAULT NULL COMMENT '税率ID',
  `barcode` varchar(128) DEFAULT NULL COMMENT '条码',
  `shelf_life_days` int(11) DEFAULT NULL COMMENT '保质期天数',
  `default_expiry_warn_days` int(11) DEFAULT NULL COMMENT '默认临期预警天数',
  `batch_control` char(1) DEFAULT 'N' COMMENT '批次控制（Y/N）',
  `serial_control` char(1) DEFAULT 'N' COMMENT '序列号控制（Y/N）',
  `costing_method` varchar(32) DEFAULT NULL COMMENT '计价方式',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `idx_mdm_item_tenant_code` (`tenant_id`, `item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM物料主数据表';

DROP TABLE IF EXISTS `mdm_warehouse`;
CREATE TABLE `mdm_warehouse` (
  `warehouse_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `wh_code` varchar(64) NOT NULL COMMENT '仓库编码',
  `wh_name` varchar(128) NOT NULL COMMENT '仓库名称',
  `wh_type` varchar(32) DEFAULT NULL COMMENT '仓库类型',
  `org_id` bigint(20) DEFAULT NULL COMMENT '归属组织ID',
  `address` varchar(255) DEFAULT NULL COMMENT '仓库地址',
  `manager_emp_id` bigint(20) DEFAULT NULL COMMENT '仓库负责人员工ID',
  `allow_negative_stock` char(1) DEFAULT 'N' COMMENT '允许负库存（Y/N）',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态（DRAFT/ACTIVE/DISABLED）',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `idx_mdm_wh_tenant_code` (`tenant_id`, `wh_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM仓库主数据表';

DROP TABLE IF EXISTS `mdm_employee`;
CREATE TABLE `mdm_employee` (
  `employee_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `emp_code` varchar(64) NOT NULL COMMENT '员工编码',
  `emp_name` varchar(128) NOT NULL COMMENT '员工名称',
  `mobile` varchar(32) DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `position` varchar(64) DEFAULT NULL COMMENT '岗位',
  `user_id` bigint(20) DEFAULT NULL COMMENT '账号ID',
  `cost_center_id` bigint(20) DEFAULT NULL COMMENT '成本中心ID',
  `status` varchar(16) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/LEAVE）',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `idx_mdm_emp_tenant_code` (`tenant_id`, `emp_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM员工主数据表';

DROP TABLE IF EXISTS `mdm_change_log`;
CREATE TABLE `mdm_change_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '变更日志ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `domain_type` varchar(32) NOT NULL COMMENT '主数据域类型',
  `biz_id` bigint(20) NOT NULL COMMENT '业务主键ID',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型（CREATE/UPDATE/STATUS/DELETE）',
  `before_json` longtext COMMENT '变更前快照',
  `after_json` longtext COMMENT '变更后快照',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `source` varchar(64) DEFAULT NULL COMMENT '变更来源',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_mdm_change_tenant_domain` (`tenant_id`, `domain_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM变更日志表';

DROP TABLE IF EXISTS `mdm_version`;
CREATE TABLE `mdm_version` (
  `version_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '版本记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `domain_type` varchar(32) NOT NULL COMMENT '主数据域类型',
  `biz_id` bigint(20) NOT NULL COMMENT '业务主键ID',
  `version_no` int(11) NOT NULL COMMENT '版本号',
  `status` varchar(16) DEFAULT NULL COMMENT '版本状态',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `snapshot_json` longtext COMMENT '版本快照',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `idx_mdm_version_unique` (`tenant_id`, `domain_type`, `biz_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM版本快照表';

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

INSERT INTO `sys_user` (`user_id`, `tenant_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `token_version`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
VALUES (1, '000000', 1, 'admin', '系统管理员', '00', 'admin@erp.com', '13800000000', '0', '', '$2a$10$/V6UcHU5GP.R6V9B9Iqage9GwBCI42PgHvBVfkozG3AMn9V5eUcpW', 0, '0', '0', 'system', NOW(), '默认管理员账号（密码：admin123）');

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
  (12, '通知管理', 2, 10, '/system/notice', '/views/system/notice/index', 1, 'C', '0', '0', 'system:notice:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (13, '审计日志', 2, 11, '/system/audit-log', '/views/platform/audit-log/index', 1, 'C', '0', '0', 'system:audit:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (14, '操作日志', 2, 12, '/system/oper-log', '/views/system/oper-log/index', 1, 'C', '0', '0', 'system:oper:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (20, '登录日志', 2, 13, '/system/login-log', '/views/system/login-log/index', 1, 'C', '0', '0', 'system:loginLog:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (21, '区域主数据', 2, 14, '/system/region', '/views/system/region/index', 1, 'C', '0', '0', 'system:region:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (23, '主数据管理', 0, 4, '/master-data', NULL, 1, 'M', '0', '0', NULL, 'Collection', 'system', NOW(), '系统初始化目录'),
  (24, '客户主数据', 23, 1, '/system/mdm/customer', '/views/system/mdm/customer/index', 1, 'C', '0', '0', 'system:mdm:customer:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (25, '供应商主数据', 23, 2, '/system/mdm/supplier', '/views/system/mdm/supplier/index', 1, 'C', '0', '0', 'system:mdm:supplier:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (26, '物料主数据', 23, 3, '/system/mdm/item', '/views/system/mdm/item/index', 1, 'C', '0', '0', 'system:mdm:item:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (27, '仓库主数据', 23, 4, '/system/mdm/warehouse', '/views/system/mdm/warehouse/index', 1, 'C', '0', '0', 'system:mdm:warehouse:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (28, '员工主数据', 23, 5, '/system/mdm/employee', '/views/system/mdm/employee/index', 1, 'C', '0', '0', 'system:mdm:employee:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (29, '组织成本项目', 23, 6, '/system/mdm/dimension', '/views/system/mdm/dimension/index', 1, 'C', '0', '0', 'system:mdm:org:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (30, '基础字典', 23, 7, '/system/mdm/dict', '/views/system/mdm/dict/index', 1, 'C', '0', '0', 'system:mdm:dict:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (31, '变更追踪', 23, 8, '/system/mdm/trace', '/views/system/mdm/trace/index', 1, 'C', '0', '0', 'system:mdm:trace:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (15, '平台底座', 0, 3, '/platform', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (16, '组织架构增强', 15, 1, '/platform/org', '/views/platform/org/index', 1, 'C', '0', '0', 'system:org:view', NULL, 'system', NOW(), '系统初始化菜单'),
  (17, '数据权限', 15, 2, '/platform/data-scope', '/views/platform/data-scope/index', 1, 'C', '0', '0', 'system:dataScope:view', NULL, 'system', NOW(), '系统初始化菜单'),
  (19, '编码规则', 15, 3, '/platform/code-rule', '/views/platform/code-rule/index', 1, 'C', '0', '0', 'system:codeRule:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (32, '工作台', 0, 5, '/workbench', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (33, '消息', 32, 1, '/workbench/message', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (34, '系统消息', 33, 1, '/workbench/message/system-notice', '/views/system/notice/index', 1, 'C', '0', '0', 'system:message:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (39, '流程待办', 33, 2, '/workbench/message/process-todo', '/views/platform/todo-center/index', 1, 'C', '0', '0', 'system:todo:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (35, '流程中心', 0, 6, '/workflow-center', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (36, '流程定义', 35, 1, '/workflow-center/definition', '/views/platform/workflow/index', 1, 'C', '0', '0', 'system:workflow:definition:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (37, '流程实例', 35, 2, '/workflow-center/instance', '/views/platform/workflow/index', 1, 'C', '0', '0', 'system:workflow:instance:list', NULL, 'system', NOW(), '系统初始化菜单');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_perm.menu_name,
       parent_menu.menu_id,
       button_perm.order_num,
       '',
       NULL,
       1,
       'F',
       '1',
       '0',
       button_perm.perms,
       NULL,
       'system',
       NOW(),
       '系统初始化按钮权限'
FROM (
  SELECT '/system/tenant' AS parent_path, '租户查询' AS menu_name, 1 AS order_num, 'system:tenant:query' AS perms
  UNION ALL SELECT '/system/tenant', '租户新增', 2, 'system:tenant:add'
  UNION ALL SELECT '/system/tenant', '租户修改', 3, 'system:tenant:edit'
  UNION ALL SELECT '/system/tenant', '租户删除', 4, 'system:tenant:remove'
  UNION ALL SELECT '/system/user', '用户查询', 1, 'system:user:query'
  UNION ALL SELECT '/system/user', '用户新增', 2, 'system:user:add'
  UNION ALL SELECT '/system/user', '用户修改', 3, 'system:user:edit'
  UNION ALL SELECT '/system/user', '用户删除', 4, 'system:user:remove'
  UNION ALL SELECT '/system/role', '角色查询', 1, 'system:role:query'
  UNION ALL SELECT '/system/role', '角色新增', 2, 'system:role:add'
  UNION ALL SELECT '/system/role', '角色修改', 3, 'system:role:edit'
  UNION ALL SELECT '/system/role', '角色删除', 4, 'system:role:remove'
  UNION ALL SELECT '/system/menu', '菜单查询', 1, 'system:menu:query'
  UNION ALL SELECT '/system/menu', '菜单新增', 2, 'system:menu:add'
  UNION ALL SELECT '/system/menu', '菜单修改', 3, 'system:menu:edit'
  UNION ALL SELECT '/system/menu', '菜单删除', 4, 'system:menu:remove'
  UNION ALL SELECT '/system/dept', '部门查询', 1, 'system:dept:query'
  UNION ALL SELECT '/system/dept', '部门新增', 2, 'system:dept:add'
  UNION ALL SELECT '/system/dept', '部门修改', 3, 'system:dept:edit'
  UNION ALL SELECT '/system/dept', '部门删除', 4, 'system:dept:remove'
  UNION ALL SELECT '/system/dict', '字典查询', 1, 'system:dict:query'
  UNION ALL SELECT '/system/dict', '字典新增', 2, 'system:dict:add'
  UNION ALL SELECT '/system/dict', '字典修改', 3, 'system:dict:edit'
  UNION ALL SELECT '/system/dict', '字典删除', 4, 'system:dict:remove'
  UNION ALL SELECT '/system/config', '参数查询', 1, 'system:config:query'
  UNION ALL SELECT '/system/config', '参数新增', 2, 'system:config:add'
  UNION ALL SELECT '/system/config', '参数修改', 3, 'system:config:edit'
  UNION ALL SELECT '/system/config', '参数删除', 4, 'system:config:remove'
  UNION ALL SELECT '/system/company', '公司查询', 1, 'system:company:query'
  UNION ALL SELECT '/system/company', '公司新增', 2, 'system:company:add'
  UNION ALL SELECT '/system/company', '公司修改', 3, 'system:company:edit'
  UNION ALL SELECT '/system/company', '公司删除', 4, 'system:company:remove'
  UNION ALL SELECT '/system/post', '岗位查询', 1, 'system:post:query'
  UNION ALL SELECT '/system/post', '岗位新增', 2, 'system:post:add'
  UNION ALL SELECT '/system/post', '岗位修改', 3, 'system:post:edit'
  UNION ALL SELECT '/system/post', '岗位删除', 4, 'system:post:remove'
  UNION ALL SELECT '/system/notice', '通知查询', 1, 'system:notice:query'
  UNION ALL SELECT '/system/notice', '通知新增', 2, 'system:notice:add'
  UNION ALL SELECT '/system/notice', '通知修改', 3, 'system:notice:edit'
  UNION ALL SELECT '/system/notice', '通知删除', 4, 'system:notice:remove'
  UNION ALL SELECT '/system/audit-log', '审计详情', 1, 'system:audit:query'
  UNION ALL SELECT '/system/audit-log', '审计删除', 2, 'system:audit:remove'
  UNION ALL SELECT '/system/oper-log', '操作日志详情', 1, 'system:oper:query'
  UNION ALL SELECT '/system/oper-log', '操作日志删除', 2, 'system:oper:remove'
  UNION ALL SELECT '/system/login-log', '登录日志删除', 1, 'system:loginLog:remove'
  UNION ALL SELECT '/system/region', '区域查询', 1, 'system:region:query'
  UNION ALL SELECT '/system/region', '区域新增', 2, 'system:region:add'
  UNION ALL SELECT '/system/region', '区域修改', 3, 'system:region:edit'
  UNION ALL SELECT '/system/region', '区域删除', 4, 'system:region:remove'
  UNION ALL SELECT '/system/mdm/customer', '客户查询', 1, 'system:mdm:customer:query'
  UNION ALL SELECT '/system/mdm/customer', '客户新增', 2, 'system:mdm:customer:add'
  UNION ALL SELECT '/system/mdm/customer', '客户修改', 3, 'system:mdm:customer:edit'
  UNION ALL SELECT '/system/mdm/customer', '客户停用', 4, 'system:mdm:customer:disable'
  UNION ALL SELECT '/system/mdm/customer', '客户删除', 5, 'system:mdm:customer:remove'
  UNION ALL SELECT '/system/mdm/supplier', '供应商查询', 1, 'system:mdm:supplier:query'
  UNION ALL SELECT '/system/mdm/supplier', '供应商新增', 2, 'system:mdm:supplier:add'
  UNION ALL SELECT '/system/mdm/supplier', '供应商修改', 3, 'system:mdm:supplier:edit'
  UNION ALL SELECT '/system/mdm/supplier', '供应商停用', 4, 'system:mdm:supplier:disable'
  UNION ALL SELECT '/system/mdm/supplier', '供应商删除', 5, 'system:mdm:supplier:remove'
  UNION ALL SELECT '/system/mdm/item', '物料查询', 1, 'system:mdm:item:query'
  UNION ALL SELECT '/system/mdm/item', '物料新增', 2, 'system:mdm:item:add'
  UNION ALL SELECT '/system/mdm/item', '物料修改', 3, 'system:mdm:item:edit'
  UNION ALL SELECT '/system/mdm/item', '物料停用', 4, 'system:mdm:item:disable'
  UNION ALL SELECT '/system/mdm/item', '物料删除', 5, 'system:mdm:item:remove'
  UNION ALL SELECT '/system/mdm/warehouse', '仓库查询', 1, 'system:mdm:warehouse:query'
  UNION ALL SELECT '/system/mdm/warehouse', '仓库新增', 2, 'system:mdm:warehouse:add'
  UNION ALL SELECT '/system/mdm/warehouse', '仓库修改', 3, 'system:mdm:warehouse:edit'
  UNION ALL SELECT '/system/mdm/warehouse', '仓库停用', 4, 'system:mdm:warehouse:disable'
  UNION ALL SELECT '/system/mdm/warehouse', '仓库删除', 5, 'system:mdm:warehouse:remove'
  UNION ALL SELECT '/system/mdm/employee', '员工查询', 1, 'system:mdm:employee:query'
  UNION ALL SELECT '/system/mdm/employee', '员工新增', 2, 'system:mdm:employee:add'
  UNION ALL SELECT '/system/mdm/employee', '员工修改', 3, 'system:mdm:employee:edit'
  UNION ALL SELECT '/system/mdm/employee', '员工离职', 4, 'system:mdm:employee:leave'
  UNION ALL SELECT '/system/mdm/employee', '员工删除', 5, 'system:mdm:employee:remove'
  UNION ALL SELECT '/system/mdm/dimension', '组织查询', 1, 'system:mdm:org:query'
  UNION ALL SELECT '/system/mdm/dimension', '组织新增', 2, 'system:mdm:org:add'
  UNION ALL SELECT '/system/mdm/dimension', '组织修改', 3, 'system:mdm:org:edit'
  UNION ALL SELECT '/system/mdm/dimension', '组织停用', 4, 'system:mdm:org:disable'
  UNION ALL SELECT '/system/mdm/dimension', '组织删除', 5, 'system:mdm:org:remove'
  UNION ALL SELECT '/system/mdm/dimension', '成本中心查询', 6, 'system:mdm:cc:query'
  UNION ALL SELECT '/system/mdm/dimension', '成本中心新增', 7, 'system:mdm:cc:add'
  UNION ALL SELECT '/system/mdm/dimension', '成本中心修改', 8, 'system:mdm:cc:edit'
  UNION ALL SELECT '/system/mdm/dimension', '成本中心停用', 9, 'system:mdm:cc:disable'
  UNION ALL SELECT '/system/mdm/dimension', '成本中心删除', 10, 'system:mdm:cc:remove'
  UNION ALL SELECT '/system/mdm/dimension', '项目查询', 11, 'system:mdm:project:query'
  UNION ALL SELECT '/system/mdm/dimension', '项目新增', 12, 'system:mdm:project:add'
  UNION ALL SELECT '/system/mdm/dimension', '项目修改', 13, 'system:mdm:project:edit'
  UNION ALL SELECT '/system/mdm/dimension', '项目停用', 14, 'system:mdm:project:disable'
  UNION ALL SELECT '/system/mdm/dimension', '项目删除', 15, 'system:mdm:project:remove'
  UNION ALL SELECT '/system/mdm/dict', '字典项查询', 1, 'system:mdm:dict:query'
  UNION ALL SELECT '/system/mdm/dict', '字典项新增', 2, 'system:mdm:dict:add'
  UNION ALL SELECT '/system/mdm/dict', '字典项修改', 3, 'system:mdm:dict:edit'
  UNION ALL SELECT '/system/mdm/dict', '字典项停用', 4, 'system:mdm:dict:disable'
  UNION ALL SELECT '/system/mdm/dict', '字典项删除', 5, 'system:mdm:dict:remove'
  UNION ALL SELECT '/system/mdm/trace', '追踪查询', 1, 'system:mdm:trace:query'
  UNION ALL SELECT '/workbench/message/system-notice', '消息已读', 1, 'system:message:read'
  UNION ALL SELECT '/workbench/message/process-todo', '待办签收', 1, 'system:todo:claim'
  UNION ALL SELECT '/workbench/message/process-todo', '待办办结', 2, 'system:todo:finish'
  UNION ALL SELECT '/workbench/message/process-todo', '待办处理', 3, 'system:todo:handle'
  UNION ALL SELECT '/workbench/message/process-todo', '审批表单', 4, 'system:todo:form'
  UNION ALL SELECT '/workbench/message/process-todo', '审批同意', 5, 'system:todo:approve'
  UNION ALL SELECT '/workbench/message/process-todo', '审批驳回', 6, 'system:todo:reject'
  UNION ALL SELECT '/workbench/message/process-todo', '任务转交', 7, 'system:todo:transfer'
  UNION ALL SELECT '/workbench/message/process-todo', '节点退回', 8, 'system:todo:return'
  UNION ALL SELECT '/workbench/message/process-todo', '任务加签', 9, 'system:todo:addSign'
  UNION ALL SELECT '/workbench/message/process-todo', '任务减签', 10, 'system:todo:removeSign'
  UNION ALL SELECT '/workbench/message/process-todo', '任务委派', 11, 'system:todo:delegate'
  UNION ALL SELECT '/workbench/message/process-todo', '任务催办', 12, 'system:todo:remind'
  UNION ALL SELECT '/platform/code-rule', '编码规则查询', 1, 'system:codeRule:query'
  UNION ALL SELECT '/platform/code-rule', '编码规则新增', 2, 'system:codeRule:add'
  UNION ALL SELECT '/platform/code-rule', '编码规则修改', 3, 'system:codeRule:edit'
  UNION ALL SELECT '/platform/code-rule', '编码规则删除', 4, 'system:codeRule:remove'
  UNION ALL SELECT '/platform/code-rule', '编码规则生成', 5, 'system:codeRule:generate'
  UNION ALL SELECT '/workflow-center/definition', '流程定义查询', 1, 'system:workflow:definition:query'
  UNION ALL SELECT '/workflow-center/definition', '流程定义新增', 2, 'system:workflow:definition:add'
  UNION ALL SELECT '/workflow-center/definition', '流程定义修改', 3, 'system:workflow:definition:edit'
  UNION ALL SELECT '/workflow-center/definition', '流程定义删除', 4, 'system:workflow:definition:remove'
  UNION ALL SELECT '/workflow-center/definition', '流程定义发布', 5, 'system:workflow:definition:publish'
  UNION ALL SELECT '/workflow-center/definition', '流程设计', 6, 'system:workflow:definition:design'
  UNION ALL SELECT '/workflow-center/definition', '流程模板', 7, 'system:workflow:definition:template'
  UNION ALL SELECT '/workflow-center/instance', '流程实例查询', 1, 'system:workflow:instance:query'
  UNION ALL SELECT '/workflow-center/instance', '流程发起', 2, 'system:workflow:instance:start'
  UNION ALL SELECT '/workflow-center/instance', '流程撤回', 3, 'system:workflow:instance:withdraw'
  UNION ALL SELECT '/workflow-center/instance', '流程看板', 4, 'system:workflow:instance:report'
  UNION ALL SELECT '/workflow-center/instance', 'SLA扫描', 5, 'system:workflow:instance:sla'
) button_perm
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_perm.parent_path;

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

INSERT INTO `mdm_settle_method` (`settle_method_id`, `tenant_id`, `settle_code`, `settle_name`, `status`, `create_by`, `create_time`)
VALUES
  (1, '000000', 'CASH', '现金', 'ACTIVE', 'system', NOW()),
  (2, '000000', 'BANK', '转账', 'ACTIVE', 'system', NOW()),
  (3, '000000', 'MONTHLY', '月结', 'ACTIVE', 'system', NOW()),
  (4, '000000', 'NOTE', '票据', 'ACTIVE', 'system', NOW());

INSERT INTO `mdm_tax_rate` (`tax_rate_id`, `tenant_id`, `tax_code`, `tax_name`, `tax_rate`, `status`, `create_by`, `create_time`)
VALUES
  (1, '000000', 'TAX_0', '税率0%', 0.0000, 'ACTIVE', 'system', NOW()),
  (2, '000000', 'TAX_1', '税率1%', 0.0100, 'ACTIVE', 'system', NOW()),
  (3, '000000', 'TAX_3', '税率3%', 0.0300, 'ACTIVE', 'system', NOW()),
  (4, '000000', 'TAX_6', '税率6%', 0.0600, 'ACTIVE', 'system', NOW()),
  (5, '000000', 'TAX_9', '税率9%', 0.0900, 'ACTIVE', 'system', NOW()),
  (6, '000000', 'TAX_13', '税率13%', 0.1300, 'ACTIVE', 'system', NOW());

INSERT INTO `mdm_currency` (`currency_id`, `tenant_id`, `currency_code`, `currency_name`, `symbol`, `precision_scale`, `status`, `create_by`, `create_time`)
VALUES
  (1, '000000', 'CNY', '人民币', '￥', 2, 'ACTIVE', 'system', NOW()),
  (2, '000000', 'USD', '美元', '$', 2, 'ACTIVE', 'system', NOW()),
  (3, '000000', 'EUR', '欧元', '€', 2, 'ACTIVE', 'system', NOW());

INSERT INTO `mdm_uom` (`uom_id`, `tenant_id`, `uom_code`, `uom_name`, `status`, `create_by`, `create_time`)
VALUES
  (1, '000000', 'PCS', '件', 'ACTIVE', 'system', NOW()),
  (2, '000000', 'BOX', '箱', 'ACTIVE', 'system', NOW()),
  (3, '000000', 'KG', '千克', 'ACTIVE', 'system', NOW()),
  (4, '000000', 'M', '米', 'ACTIVE', 'system', NOW());

INSERT INTO `sys_wf_definition` (`definition_id`, `tenant_id`, `process_key`, `process_name`, `category`, `version`, `status`, `form_schema`, `model_content`, `publish_by`, `publish_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
  (1, '000000', 'purchase_apply', '采购审批流程', 'purchase', 1, '1', '{"fields":[{"name":"amount","label":"金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"}]}', 'system', NOW(), '系统初始化流程定义', 'system', NOW(), 'system', NOW()),
  (2, '000000', 'expense_apply', '报销审批流程', 'expense', 1, '0', '{"fields":[{"name":"feeType","label":"费用类型"},{"name":"total","label":"合计金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"},{"id":"NODE_2","name":"财务复核"}]}', NULL, NULL, '系统初始化流程定义', 'system', NOW(), 'system', NOW()),
  (3, '000000', 'mdm_employee', '员工主数据审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"status","fieldLabel":"状态","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工资料审批","nodeType":"approval","assigneeType":"USER","assigneeUserId":1,"approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), '系统初始化员工主数据审批流程定义', 'system', NOW(), 'system', NOW()),
  (4, '000000', 'mdm_employee_onboard', '员工入职审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"action","fieldLabel":"审批动作","componentType":"input","required":true,"placeholder":"入职","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工入职审批","nodeType":"approval","assigneeType":"DIRECT_LEADER","approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), '系统初始化员工入职审批流程定义', 'system', NOW(), 'system', NOW()),
  (5, '000000', 'mdm_employee_change', '员工变更审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"action","fieldLabel":"审批动作","componentType":"input","required":true,"placeholder":"变更","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工变更审批","nodeType":"approval","assigneeType":"DIRECT_LEADER","approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), '系统初始化员工变更审批流程定义', 'system', NOW(), 'system', NOW()),
  (6, '000000', 'mdm_employee_leave', '员工离职审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"action","fieldLabel":"审批动作","componentType":"input","required":true,"placeholder":"离职","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工离职审批","nodeType":"approval","assigneeType":"DIRECT_LEADER","approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), '系统初始化员工离职审批流程定义', 'system', NOW(), 'system', NOW());

INSERT INTO `sys_wf_business_binding` (`binding_id`, `tenant_id`, `domain_type`, `action_code`, `process_key`, `is_default`, `status`, `priority`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
  (1, '000000', 'EMPLOYEE', 'ONBOARD', 'mdm_employee_onboard', '1', '0', 10, '员工入职审批流程默认绑定', 'system', NOW(), 'system', NOW()),
  (2, '000000', 'EMPLOYEE', 'CHANGE', 'mdm_employee_change', '1', '0', 10, '员工变更审批流程默认绑定', 'system', NOW(), 'system', NOW()),
  (3, '000000', 'EMPLOYEE', 'LEAVE', 'mdm_employee_leave', '1', '0', 10, '员工离职审批流程默认绑定', 'system', NOW(), 'system', NOW()),
  (4, '000000', 'EMPLOYEE', 'ONBOARD', 'mdm_employee', '0', '0', 90, '员工入职审批 legacy 兼容绑定', 'system', NOW(), 'system', NOW());

INSERT INTO `sys_wf_instance` (`instance_id`, `tenant_id`, `definition_id`, `definition_version`, `process_key`, `process_name`, `category`, `business_no`, `business_type`, `form_data`, `form_schema_snapshot`, `model_content_snapshot`, `current_node`, `initiator_user_id`, `initiator_user_name`, `initiator_nick_name`, `status`, `start_time`, `last_action`, `last_action_user_id`, `last_action_user_name`, `last_action_time`, `remark`)
VALUES
  (1, '000000', 1, 1, 'purchase_apply', '采购审批流程', 'purchase', 'PO-20260309-001', '采购申请', '{"amount":12000,"reason":"办公设备采购"}', '{"fields":[{"name":"amount","label":"金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"}]}', '部门负责人审批', 1, 'admin', '系统管理员', '0', NOW(), 'START', 1, 'admin', NOW(), '系统初始化流程实例');

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `title`, `notice_type`, `source`, `business_no`, `content`, `receiver_user_id`, `status`, `create_time`)
VALUES
  (1, '000000', '流程引擎已发布新版本，请核查审批节点配置', '系统公告', '流程引擎', NULL, '流程引擎发布 v2.0.1，请检查关键审批流配置。', 1, '0', NOW()),
  (2, '000000', '导入任务 IM20260307-01 执行完成', '审批通知', '导入导出中心', 'IM20260307-01', '导入任务执行完成，请查看结果。', 1, '1', NOW()),
  (3, '000000', '报表中心出现数据延迟预警', '预警提醒', '报表中心', NULL, '近30分钟内报表数据刷新延迟超过阈值。', 1, '0', NOW());

INSERT INTO `sys_todo_task` (`todo_id`, `instance_id`, `task_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
VALUES
  (1, NULL, NULL, '000000', '请假审批', '部门负责人审批', 'LV-20260307-001', 'H', '0', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), '请及时处理'),
  (2, NULL, NULL, '000000', '采购申请', '财务复核', 'PO-20260307-018', 'M', '1', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '处理中'),
  (3, NULL, NULL, '000000', '合同归档', '档案确认', 'CT-20260306-021', 'L', '0', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), '待签收'),
  (4, 1, 1, '000000', '采购审批流程', '部门负责人审批', 'PO-20260309-001', 'M', '0', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '流程引擎初始化待办');

INSERT INTO `sys_wf_task` (`task_id`, `tenant_id`, `instance_id`, `definition_id`, `node_key`, `node_name`, `candidate_user_ids`, `assignee_user_id`, `assignee_user_name`, `assignee_nick_name`, `status`, `todo_id`, `due_time`, `create_time`)
VALUES
  (1, '000000', 1, 1, 'NODE_1', '部门负责人审批', '1', 1, 'admin', '系统管理员', '0', 4, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());

INSERT INTO `sys_wf_task_action` (`action_id`, `tenant_id`, `instance_id`, `task_id`, `definition_id`, `node_name`, `action_type`, `action_user_id`, `action_user_name`, `action_nick_name`, `to_assignee_user_id`, `action_comment`, `action_time`)
VALUES
  (1, '000000', 1, 1, 1, '部门负责人审批', 'START', 1, 'admin', '系统管理员', 1, '流程发起', NOW());

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `mdm_warehouse` ADD COLUMN `accounting_org_id` bigint(20) DEFAULT NULL COMMENT ''账务归属组织ID'' AFTER `org_id`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_warehouse'
      AND COLUMN_NAME = 'accounting_org_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `mdm_warehouse` ADD COLUMN `volume_capacity` decimal(18,4) DEFAULT NULL COMMENT ''容量体积'' AFTER `allow_negative_stock`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_warehouse'
      AND COLUMN_NAME = 'volume_capacity'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `mdm_warehouse` ADD COLUMN `weight_capacity` decimal(18,4) DEFAULT NULL COMMENT ''容量重量'' AFTER `volume_capacity`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_warehouse'
      AND COLUMN_NAME = 'weight_capacity'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `mdm_warehouse` ADD COLUMN `temperature_zone` varchar(32) DEFAULT NULL COMMENT ''温层'' AFTER `weight_capacity`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_warehouse'
      AND COLUMN_NAME = 'temperature_zone'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `mdm_warehouse` ADD COLUMN `hazardous_flag` char(1) DEFAULT ''N'' COMMENT ''危险品标识（Y/N）'' AFTER `temperature_zone`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_warehouse'
      AND COLUMN_NAME = 'hazardous_flag'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `mdm_warehouse` ADD COLUMN `location_code_prefix` varchar(32) DEFAULT NULL COMMENT ''库位编码前缀'' AFTER `hazardous_flag`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_warehouse'
      AND COLUMN_NAME = 'location_code_prefix'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `mdm_warehouse_area` (
  `area_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库区ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `area_code` varchar(64) NOT NULL COMMENT '库区编码',
  `area_name` varchar(128) NOT NULL COMMENT '库区名称',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`area_id`),
  UNIQUE KEY `idx_mdm_wh_area_unique` (`tenant_id`, `warehouse_id`, `area_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM仓库库区表';

CREATE TABLE IF NOT EXISTS `mdm_warehouse_location` (
  `location_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库位ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `area_id` bigint(20) NOT NULL COMMENT '库区ID',
  `location_code` varchar(64) NOT NULL COMMENT '库位编码',
  `location_name` varchar(128) NOT NULL COMMENT '库位名称',
  `volume_capacity` decimal(18,4) DEFAULT NULL COMMENT '容量体积',
  `weight_capacity` decimal(18,4) DEFAULT NULL COMMENT '容量重量',
  `temperature_zone` varchar(32) DEFAULT NULL COMMENT '温层',
  `hazardous_flag` char(1) DEFAULT 'N' COMMENT '危险品标识（Y/N）',
  `status` varchar(16) DEFAULT 'DRAFT' COMMENT '状态',
  `version_no` int(11) DEFAULT 1 COMMENT '版本号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`location_id`),
  UNIQUE KEY `idx_mdm_wh_location_unique` (`tenant_id`, `warehouse_id`, `location_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MDM仓库库位表';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库区主数据', 23, 5, '/system/mdm/warehouse-area', '/views/system/mdm/warehouse-area/index', 1, 'C', '0', '0', 'system:mdm:warehouse-area:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/mdm/warehouse-area');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库位主数据', 23, 6, '/system/mdm/warehouse-location', '/views/system/mdm/warehouse-location/index', 1, 'C', '0', '0', 'system:mdm:warehouse-location:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/system/mdm/warehouse-location');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库存与仓储', 0, 5, '/inventory-manage', NULL, 1, 'M', '0', '0', NULL, 'Box', 'system', NOW(), '系统初始化目录'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/inventory-manage');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库存台账', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 1, '/business/inventory/ledger', '/views/inventory/ledger/index', 1, 'C', '0', '0', 'business:inventory:ledger:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/ledger');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '入库管理', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 2, '/business/inventory/inbound', '/views/inventory/inbound/index', 1, 'C', '0', '0', 'business:inventory:inbound:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/inbound');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '出库管理', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 3, '/business/inventory/outbound', '/views/inventory/outbound/index', 1, 'C', '0', '0', 'business:inventory:outbound:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/outbound');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, parent_menu.menu_id, button_def.order_num, button_def.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), '系统初始化按钮权限'
FROM (
  SELECT '/system/mdm/warehouse-area' AS path, '库区查询' AS menu_name, 1 AS order_num, 'system:mdm:warehouse-area:query' AS perms
  UNION ALL SELECT '/system/mdm/warehouse-area', '库区新增', 2, 'system:mdm:warehouse-area:add'
  UNION ALL SELECT '/system/mdm/warehouse-area', '库区修改', 3, 'system:mdm:warehouse-area:edit'
  UNION ALL SELECT '/system/mdm/warehouse-area', '库区停用', 4, 'system:mdm:warehouse-area:disable'
  UNION ALL SELECT '/system/mdm/warehouse-area', '库区删除', 5, 'system:mdm:warehouse-area:remove'
  UNION ALL SELECT '/system/mdm/warehouse-location', '库位查询', 1, 'system:mdm:warehouse-location:query'
  UNION ALL SELECT '/system/mdm/warehouse-location', '库位新增', 2, 'system:mdm:warehouse-location:add'
  UNION ALL SELECT '/system/mdm/warehouse-location', '库位修改', 3, 'system:mdm:warehouse-location:edit'
  UNION ALL SELECT '/system/mdm/warehouse-location', '库位停用', 4, 'system:mdm:warehouse-location:disable'
  UNION ALL SELECT '/system/mdm/warehouse-location', '库位删除', 5, 'system:mdm:warehouse-location:remove'
  UNION ALL SELECT '/business/inventory/inbound', '入库查询', 1, 'business:inventory:inbound:query'
  UNION ALL SELECT '/business/inventory/inbound', '入库新增', 2, 'business:inventory:inbound:add'
  UNION ALL SELECT '/business/inventory/inbound', '入库修改', 3, 'business:inventory:inbound:edit'
  UNION ALL SELECT '/business/inventory/inbound', '入库提交', 4, 'business:inventory:inbound:submit'
  UNION ALL SELECT '/business/inventory/inbound', '入库执行', 5, 'business:inventory:inbound:execute'
  UNION ALL SELECT '/business/inventory/inbound', '入库取消', 6, 'business:inventory:inbound:cancel'
  UNION ALL SELECT '/business/inventory/outbound', '出库查询', 1, 'business:inventory:outbound:query'
  UNION ALL SELECT '/business/inventory/outbound', '出库新增', 2, 'business:inventory:outbound:add'
  UNION ALL SELECT '/business/inventory/outbound', '出库修改', 3, 'business:inventory:outbound:edit'
  UNION ALL SELECT '/business/inventory/outbound', '出库提交', 4, 'business:inventory:outbound:submit'
  UNION ALL SELECT '/business/inventory/outbound', '出库执行', 5, 'business:inventory:outbound:execute'
  UNION ALL SELECT '/business/inventory/outbound', '出库取消', 6, 'business:inventory:outbound:cancel'
) button_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_def.path
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = button_def.path
    AND existed_menu.perms = button_def.perms
);

CREATE TABLE IF NOT EXISTS `inv_stock_balance` (
  `balance_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库存余额ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `area_id_key` bigint(20) GENERATED ALWAYS AS (ifnull(`area_id`,0)) STORED COMMENT '库区唯一键归一值',
  `location_id_key` bigint(20) GENERATED ALWAYS AS (ifnull(`location_id`,0)) STORED COMMENT '库位唯一键归一值',
  `batch_no_key` varchar(64) GENERATED ALWAYS AS (ifnull(`batch_no`,'')) STORED COMMENT '批次唯一键归一值',
  `serial_no_key` varchar(128) GENERATED ALWAYS AS (ifnull(`serial_no`,'')) STORED COMMENT '序列唯一键归一值',
  `on_hand_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '即时库存',
  `available_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '可用库存',
  `frozen_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '冻结库存',
  `in_transit_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '在途库存',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `last_txn_time` datetime DEFAULT NULL COMMENT '最后事务时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`balance_id`),
  UNIQUE KEY `uk_inv_stock_balance_dim` (`tenant_id`,`org_id`,`warehouse_id`,`area_id_key`,`location_id_key`,`item_id`,`batch_no_key`,`serial_no_key`),
  KEY `idx_inv_stock_balance_lookup` (`tenant_id`,`org_id`,`warehouse_id`,`item_id`),
  KEY `idx_inv_stock_balance_location` (`tenant_id`,`warehouse_id`,`area_id`,`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存余额表';

CREATE TABLE IF NOT EXISTS `inv_stock_txn` (
  `txn_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库存流水ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `from_area_id` bigint(20) DEFAULT NULL COMMENT '来源库区ID',
  `from_location_id` bigint(20) DEFAULT NULL COMMENT '来源库位ID',
  `to_area_id` bigint(20) DEFAULT NULL COMMENT '目标库区ID',
  `to_location_id` bigint(20) DEFAULT NULL COMMENT '目标库位ID',
  `action_type` varchar(32) NOT NULL COMMENT '事务动作',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `bill_id` bigint(20) NOT NULL COMMENT '单据ID',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `idempotency_no` varchar(64) NOT NULL COMMENT '幂等号',
  `trace_id` varchar(64) DEFAULT NULL COMMENT 'TraceId',
  `before_on_hand_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '变更前即时库存',
  `after_on_hand_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '变更后即时库存',
  `before_available_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '变更前可用库存',
  `after_available_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '变更后可用库存',
  `change_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '变更数量',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`txn_id`),
  UNIQUE KEY `uk_inv_txn_idem` (`tenant_id`,`idempotency_no`,`line_no`,`action_type`),
  KEY `idx_inv_stock_txn_query` (`tenant_id`,`bill_no`,`item_id`,`action_type`,`create_time`),
  KEY `idx_inv_stock_txn_bill` (`tenant_id`,`bill_type`,`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

CREATE TABLE IF NOT EXISTS `inv_inbound_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_inbound_bill_no` (`tenant_id`,`bill_no`),
  KEY `idx_inv_inbound_order_status` (`tenant_id`,`status`,`update_time`),
  KEY `idx_inv_inbound_order_source` (`tenant_id`,`source_order_type`,`source_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单头';

CREATE TABLE IF NOT EXISTS `inv_inbound_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '入库单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '入库单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_inbound_order_line` (`tenant_id`,`order_id`,`line_no`),
  KEY `idx_inv_inbound_line_item` (`tenant_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单行';

CREATE TABLE IF NOT EXISTS `inv_outbound_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '出库单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_outbound_bill_no` (`tenant_id`,`bill_no`),
  KEY `idx_inv_outbound_order_status` (`tenant_id`,`status`,`update_time`),
  KEY `idx_inv_outbound_order_source` (`tenant_id`,`source_order_type`,`source_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单头';

CREATE TABLE IF NOT EXISTS `inv_outbound_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '出库单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '出库单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_outbound_order_line` (`tenant_id`,`order_id`,`line_no`),
  KEY `idx_inv_outbound_line_item` (`tenant_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单行';

CREATE TABLE IF NOT EXISTS `inv_transfer_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '调拨单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '源仓库ID',
  `target_warehouse_id` bigint(20) NOT NULL COMMENT '目标仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_transfer_bill_no` (`tenant_id`,`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单头';

CREATE TABLE IF NOT EXISTS `inv_transfer_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '调拨单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '调拨单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '源库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '源库位ID',
  `target_area_id` bigint(20) DEFAULT NULL COMMENT '目标库区ID',
  `target_location_id` bigint(20) DEFAULT NULL COMMENT '目标库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_transfer_line` (`tenant_id`,`order_id`,`line_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单行';

CREATE TABLE IF NOT EXISTS `inv_stock_move_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '移库单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_stock_move_bill_no` (`tenant_id`,`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='移库单头';

CREATE TABLE IF NOT EXISTS `inv_stock_move_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '移库单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '移库单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '源库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '源库位ID',
  `target_area_id` bigint(20) DEFAULT NULL COMMENT '目标库区ID',
  `target_location_id` bigint(20) DEFAULT NULL COMMENT '目标库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_stock_move_line` (`tenant_id`,`order_id`,`line_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='移库单行';

CREATE TABLE IF NOT EXISTS `inv_stock_freeze_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '冻结解冻单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_stock_freeze_bill_no` (`tenant_id`,`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冻结解冻单头';

CREATE TABLE IF NOT EXISTS `inv_stock_freeze_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '冻结解冻单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '冻结解冻单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_stock_freeze_line` (`tenant_id`,`order_id`,`line_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冻结解冻单行';

CREATE TABLE IF NOT EXISTS `inv_stock_adjust_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库存调整单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_stock_adjust_bill_no` (`tenant_id`,`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调整单头';

CREATE TABLE IF NOT EXISTS `inv_stock_adjust_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库存调整单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '库存调整单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `adjust_type` varchar(32) DEFAULT NULL COMMENT '调整类型',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_stock_adjust_line` (`tenant_id`,`order_id`,`line_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调整单行';

CREATE TABLE IF NOT EXISTS `inv_stocktake_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `bill_no` varchar(64) NOT NULL COMMENT '单据编号',
  `bill_type` varchar(32) NOT NULL COMMENT '单据类型',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `stocktake_stage` varchar(32) DEFAULT NULL COMMENT '盘点阶段',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `source_order_type` varchar(32) DEFAULT NULL COMMENT '来源单类型',
  `source_order_id` bigint(20) DEFAULT NULL COMMENT '来源单ID',
  `source_order_no` varchar(64) DEFAULT NULL COMMENT '来源单编号',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_inv_stocktake_bill_no` (`tenant_id`,`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单头';

CREATE TABLE IF NOT EXISTS `inv_stocktake_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '盘点单行ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_id` bigint(20) NOT NULL COMMENT '盘点单ID',
  `line_no` int(11) NOT NULL COMMENT '行号',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `qty` decimal(18,4) NOT NULL COMMENT '数量',
  `snapshot_qty` decimal(18,4) DEFAULT NULL COMMENT '账面数量',
  `counted_qty` decimal(18,4) DEFAULT NULL COMMENT '实盘数量',
  `diff_qty` decimal(18,4) DEFAULT NULL COMMENT '差异数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_stocktake_line` (`tenant_id`,`order_id`,`line_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单行';

CREATE TABLE IF NOT EXISTS `inv_batch_record` (
  `batch_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '批次记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `current_qty` decimal(18,4) NOT NULL DEFAULT 0 COMMENT '当前数量',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_inv_batch_record` (`tenant_id`,`warehouse_id`,`area_id`,`location_id`,`item_id`,`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次记录表';

CREATE TABLE IF NOT EXISTS `inv_serial_record` (
  `serial_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '序列号记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库ID',
  `area_id` bigint(20) DEFAULT NULL COMMENT '库区ID',
  `location_id` bigint(20) DEFAULT NULL COMMENT '库位ID',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `serial_no` varchar(128) NOT NULL COMMENT '序列号',
  `production_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `status` varchar(32) DEFAULT NULL COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`serial_id`),
  UNIQUE KEY `uk_inv_serial_record` (`tenant_id`,`serial_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='序列号记录表';

CREATE TABLE IF NOT EXISTS `inv_stock_policy` (
  `policy_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库存策略ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_id` bigint(20) NOT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) DEFAULT NULL COMMENT '仓库ID',
  `item_id` bigint(20) NOT NULL COMMENT '物料ID',
  `min_qty` decimal(18,4) DEFAULT NULL COMMENT '最小库存',
  `max_qty` decimal(18,4) DEFAULT NULL COMMENT '最大库存',
  `safety_qty` decimal(18,4) DEFAULT NULL COMMENT '安全库存',
  `expiry_warn_days` int(11) DEFAULT NULL COMMENT '临期预警天数',
  `allow_negative` char(1) DEFAULT 'N' COMMENT '允许负库存',
  `allow_expired_outbound` char(1) DEFAULT 'N' COMMENT '允许过期出库',
  `stagnant_days` int(11) DEFAULT NULL COMMENT '呆滞预警天数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`policy_id`),
  UNIQUE KEY `uk_inv_stock_policy` (`tenant_id`,`org_id`,`warehouse_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存策略表';

CREATE TABLE IF NOT EXISTS `inv_warning_record` (
  `warning_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预警记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `warehouse_id` bigint(20) DEFAULT NULL COMMENT '仓库ID',
  `item_id` bigint(20) DEFAULT NULL COMMENT '物料ID',
  `batch_id` bigint(20) DEFAULT NULL COMMENT '批次ID',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `serial_no` varchar(128) DEFAULT NULL COMMENT '序列号',
  `warning_type` varchar(32) NOT NULL COMMENT '预警类型',
  `warning_key` varchar(128) DEFAULT NULL COMMENT '预警幂等键',
  `warning_title` varchar(255) DEFAULT NULL COMMENT '预警标题',
  `warning_message` varchar(1000) DEFAULT NULL COMMENT '预警消息',
  `warning_content` varchar(1000) DEFAULT NULL COMMENT '预警内容',
  `status` varchar(32) NOT NULL DEFAULT 'NEW' COMMENT '处理状态',
  `warning_value` decimal(18,4) DEFAULT NULL COMMENT '预警值',
  `threshold_value` decimal(18,4) DEFAULT NULL COMMENT '阈值',
  `read_by` varchar(64) DEFAULT NULL COMMENT '读取人',
  `read_time` datetime DEFAULT NULL COMMENT '读取时间',
  `closed_by` varchar(64) DEFAULT NULL COMMENT '关闭人',
  `closed_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`warning_id`),
  KEY `idx_inv_warning_record_query` (`tenant_id`,`status`,`warning_type`,`warehouse_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警记录表';

CREATE TABLE IF NOT EXISTS `inv_integration_event` (
  `event_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '集成事件ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_status` varchar(32) NOT NULL COMMENT '事件状态',
  `source_type` varchar(64) DEFAULT NULL COMMENT '来源类型',
  `source_id` bigint(20) DEFAULT NULL COMMENT '来源ID',
  `source_no` varchar(64) DEFAULT NULL COMMENT '来源单号',
  `bill_type` varchar(32) DEFAULT NULL COMMENT '单据类型',
  `bill_id` bigint(20) DEFAULT NULL COMMENT '单据ID',
  `bill_no` varchar(64) DEFAULT NULL COMMENT '单据编号',
  `source_system` varchar(64) DEFAULT NULL COMMENT '来源系统',
  `target_system` varchar(64) DEFAULT NULL COMMENT '目标系统',
  `payload_json` longtext COMMENT '事件载荷JSON',
  `payload` longtext COMMENT '事件载荷',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `message` varchar(1000) DEFAULT NULL COMMENT '处理消息',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `last_retry_time` datetime DEFAULT NULL COMMENT '最后重试时间',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`event_id`),
  KEY `idx_inv_integration_event_query` (`tenant_id`,`event_type`,`event_status`,`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存集成事件表';

CREATE TABLE IF NOT EXISTS `sys_imex_job` (
  `job_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `job_no` varchar(64) NOT NULL COMMENT '任务编号',
  `job_type` varchar(32) NOT NULL COMMENT '任务类型',
  `module_code` varchar(64) NOT NULL COMMENT '模块编码',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `status` varchar(32) NOT NULL COMMENT '任务状态',
  `progress` int(11) NOT NULL DEFAULT 0 COMMENT '进度',
  `trigger_type` varchar(32) DEFAULT NULL COMMENT '触发方式',
  `message` varchar(1000) DEFAULT NULL COMMENT '处理消息',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`job_id`),
  UNIQUE KEY `uk_sys_imex_job_no` (`tenant_id`,`job_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入导出任务表';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '调拨管理', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 4, '/business/inventory/transfer', '/views/inventory/transfer/index', 1, 'C', '0', '0', 'business:inventory:transfer:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/transfer');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '移库管理', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 5, '/business/inventory/move', '/views/inventory/move/index', 1, 'C', '0', '0', 'business:inventory:move:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/move');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '冻结解冻', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 6, '/business/inventory/freeze', '/views/inventory/freeze/index', 1, 'C', '0', '0', 'business:inventory:freeze:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/freeze');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库存调整', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 7, '/business/inventory/adjust', '/views/inventory/adjust/index', 1, 'C', '0', '0', 'business:inventory:adjust:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/adjust');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '盘点管理', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 8, '/business/inventory/stocktake', '/views/inventory/stocktake/index', 1, 'C', '0', '0', 'business:inventory:stocktake:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/stocktake');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '批次查询', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 9, '/business/inventory/batch', '/views/inventory/batch/index', 1, 'C', '0', '0', 'business:inventory:batch:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/batch');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '序列号查询', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 10, '/business/inventory/serial', '/views/inventory/serial/index', 1, 'C', '0', '0', 'business:inventory:serial:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/serial');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库存策略', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 11, '/business/inventory/policy', '/views/inventory/policy/index', 1, 'C', '0', '0', 'business:inventory:policy:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/policy');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '预警中心', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 12, '/business/inventory/warning', '/views/inventory/warning/index', 1, 'C', '0', '0', 'business:inventory:warning:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/warning');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '库存报表', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 13, '/business/inventory/report', '/views/inventory/report/index', 1, 'C', '0', '0', 'business:inventory:report:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/report');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '集成事件', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/inventory-manage' LIMIT 1), 14, '/business/inventory/integration', '/views/inventory/integration/index', 1, 'C', '0', '0', 'business:inventory:integration:list', NULL, 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/business/inventory/integration');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '导入导出中心', 0, 6, '/platform/imex', '/views/platform/imex/index', 1, 'C', '0', '0', 'system:imex:list', 'UploadFilled', 'system', NOW(), '系统初始化菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/platform/imex');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, parent_menu.menu_id, button_def.order_num, button_def.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), '系统初始化按钮权限'
FROM (
  SELECT '/business/inventory/transfer' AS path, '调拨查询' AS menu_name, 1 AS order_num, 'business:inventory:transfer:query' AS perms
  UNION ALL SELECT '/business/inventory/transfer', '调拨新增', 2, 'business:inventory:transfer:add'
  UNION ALL SELECT '/business/inventory/transfer', '调拨修改', 3, 'business:inventory:transfer:edit'
  UNION ALL SELECT '/business/inventory/transfer', '调拨提交', 4, 'business:inventory:transfer:submit'
  UNION ALL SELECT '/business/inventory/transfer', '调拨执行', 5, 'business:inventory:transfer:execute'
  UNION ALL SELECT '/business/inventory/transfer', '调拨取消', 6, 'business:inventory:transfer:cancel'
  UNION ALL SELECT '/business/inventory/move', '移库查询', 1, 'business:inventory:move:query'
  UNION ALL SELECT '/business/inventory/move', '移库新增', 2, 'business:inventory:move:add'
  UNION ALL SELECT '/business/inventory/move', '移库修改', 3, 'business:inventory:move:edit'
  UNION ALL SELECT '/business/inventory/move', '移库提交', 4, 'business:inventory:move:submit'
  UNION ALL SELECT '/business/inventory/move', '移库执行', 5, 'business:inventory:move:execute'
  UNION ALL SELECT '/business/inventory/move', '移库取消', 6, 'business:inventory:move:cancel'
  UNION ALL SELECT '/business/inventory/freeze', '冻结解冻查询', 1, 'business:inventory:freeze:query'
  UNION ALL SELECT '/business/inventory/freeze', '冻结解冻新增', 2, 'business:inventory:freeze:add'
  UNION ALL SELECT '/business/inventory/freeze', '冻结解冻修改', 3, 'business:inventory:freeze:edit'
  UNION ALL SELECT '/business/inventory/freeze', '冻结解冻提交', 4, 'business:inventory:freeze:submit'
  UNION ALL SELECT '/business/inventory/freeze', '冻结解冻执行', 5, 'business:inventory:freeze:execute'
  UNION ALL SELECT '/business/inventory/freeze', '冻结解冻取消', 6, 'business:inventory:freeze:cancel'
  UNION ALL SELECT '/business/inventory/adjust', '库存调整查询', 1, 'business:inventory:adjust:query'
  UNION ALL SELECT '/business/inventory/adjust', '库存调整新增', 2, 'business:inventory:adjust:add'
  UNION ALL SELECT '/business/inventory/adjust', '库存调整修改', 3, 'business:inventory:adjust:edit'
  UNION ALL SELECT '/business/inventory/adjust', '库存调整提交', 4, 'business:inventory:adjust:submit'
  UNION ALL SELECT '/business/inventory/adjust', '库存调整执行', 5, 'business:inventory:adjust:execute'
  UNION ALL SELECT '/business/inventory/adjust', '库存调整取消', 6, 'business:inventory:adjust:cancel'
  UNION ALL SELECT '/business/inventory/stocktake', '盘点查询', 1, 'business:inventory:stocktake:query'
  UNION ALL SELECT '/business/inventory/stocktake', '盘点新增', 2, 'business:inventory:stocktake:add'
  UNION ALL SELECT '/business/inventory/stocktake', '盘点修改', 3, 'business:inventory:stocktake:edit'
  UNION ALL SELECT '/business/inventory/stocktake', '盘点提交', 4, 'business:inventory:stocktake:submit'
  UNION ALL SELECT '/business/inventory/stocktake', '盘点确认', 5, 'business:inventory:stocktake:execute'
  UNION ALL SELECT '/business/inventory/stocktake', '盘点取消', 6, 'business:inventory:stocktake:cancel'
  UNION ALL SELECT '/business/inventory/policy', '库存策略查询', 1, 'business:inventory:policy:query'
  UNION ALL SELECT '/business/inventory/policy', '库存策略新增', 2, 'business:inventory:policy:add'
  UNION ALL SELECT '/business/inventory/policy', '库存策略修改', 3, 'business:inventory:policy:edit'
  UNION ALL SELECT '/business/inventory/policy', '库存策略删除', 4, 'business:inventory:policy:remove'
  UNION ALL SELECT '/business/inventory/warning', '预警处理', 1, 'business:inventory:warning:handle'
  UNION ALL SELECT '/business/inventory/warning', '预警扫描', 2, 'business:inventory:warning:scan'
  UNION ALL SELECT '/business/inventory/report', '报表导出', 1, 'business:inventory:report:export'
  UNION ALL SELECT '/business/inventory/integration', '事件重放', 1, 'business:inventory:integration:replay'
  UNION ALL SELECT '/platform/imex', '导入导出查询', 1, 'system:imex:query'
  UNION ALL SELECT '/platform/imex', '导出文件下载', 2, 'system:imex:download'
) button_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_def.path
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = button_def.path
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', admin_role.role_id, menu_item.menu_id
FROM `sys_role` admin_role
INNER JOIN `sys_menu` menu_item ON (
  menu_item.path IN (
    '/business/inventory/transfer', '/business/inventory/move', '/business/inventory/freeze',
    '/business/inventory/adjust', '/business/inventory/stocktake', '/business/inventory/batch',
    '/business/inventory/serial', '/business/inventory/policy', '/business/inventory/warning',
    '/business/inventory/report', '/business/inventory/integration', '/platform/imex'
  )
  OR menu_item.perms IN (
    'business:inventory:transfer:list', 'business:inventory:transfer:query', 'business:inventory:transfer:add',
    'business:inventory:transfer:edit', 'business:inventory:transfer:submit', 'business:inventory:transfer:execute',
    'business:inventory:transfer:cancel', 'business:inventory:move:list', 'business:inventory:move:query',
    'business:inventory:move:add', 'business:inventory:move:edit', 'business:inventory:move:submit',
    'business:inventory:move:execute', 'business:inventory:move:cancel', 'business:inventory:freeze:list',
    'business:inventory:freeze:query', 'business:inventory:freeze:add', 'business:inventory:freeze:edit',
    'business:inventory:freeze:submit', 'business:inventory:freeze:execute', 'business:inventory:freeze:cancel',
    'business:inventory:adjust:list', 'business:inventory:adjust:query', 'business:inventory:adjust:add',
    'business:inventory:adjust:edit', 'business:inventory:adjust:submit', 'business:inventory:adjust:execute',
    'business:inventory:adjust:cancel', 'business:inventory:stocktake:list', 'business:inventory:stocktake:query',
    'business:inventory:stocktake:add', 'business:inventory:stocktake:edit', 'business:inventory:stocktake:submit',
    'business:inventory:stocktake:execute', 'business:inventory:stocktake:cancel', 'business:inventory:batch:list',
    'business:inventory:serial:list', 'business:inventory:policy:list', 'business:inventory:policy:query',
    'business:inventory:policy:add', 'business:inventory:policy:edit', 'business:inventory:policy:remove',
    'business:inventory:warning:list', 'business:inventory:warning:handle', 'business:inventory:warning:scan',
    'business:inventory:report:list', 'business:inventory:report:export', 'business:inventory:integration:list',
    'business:inventory:integration:replay', 'system:imex:list', 'system:imex:query', 'system:imex:download'
  )
)
WHERE admin_role.tenant_id = '000000'
  AND admin_role.role_key = 'admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed_role_menu
    WHERE existed_role_menu.role_id = admin_role.role_id
      AND existed_role_menu.menu_id = menu_item.menu_id
  );

SET FOREIGN_KEY_CHECKS = 1;


-- 2026-03-20 员工档案名称统一
UPDATE `sys_menu`
SET `menu_name` = '员工档案', `remark` = '兼容旧员工主数据菜单名称'
WHERE `path` = '/system/mdm/employee';

UPDATE `sys_wf_definition`
SET `process_name` = '员工档案审批流程',
    `remark` = CASE
      WHEN `remark` = '系统初始化员工主数据审批流程定义' THEN '系统初始化员工档案审批流程定义'
      WHEN `remark` = '升级脚本补齐员工主数据审批流程定义' THEN '升级脚本补齐员工档案审批流程定义'
      ELSE `remark`
    END
WHERE `process_key` = 'mdm_employee';

UPDATE `sys_wf_instance`
SET `process_name` = '员工档案审批流程'
WHERE `process_key` = 'mdm_employee'
  AND `process_name` = '员工主数据审批流程';
