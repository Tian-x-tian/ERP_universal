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
  KEY `idx_wf_def_status` (`tenant_id`, `status`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- ----------------------------
-- 21. 流程实例表 (sys_wf_instance)
-- ----------------------------
DROP TABLE IF EXISTS `sys_wf_instance`;
CREATE TABLE `sys_wf_instance` (
  `instance_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `process_name` varchar(128) NOT NULL COMMENT '流程名称',
  `category` varchar(64) DEFAULT 'custom' COMMENT '流程分类',
  `business_no` varchar(64) NOT NULL COMMENT '业务单号',
  `business_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `form_data` longtext COMMENT '表单数据JSON',
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
-- 22. 流程任务表 (sys_wf_task)
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
-- 23. 流程任务动作记录表 (sys_wf_task_action)
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
  (12, '通知管理', 2, 10, '/system/notice', '/views/system/notice/index', 1, 'C', '0', '0', 'system:notice:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (13, '审计日志', 2, 11, '/system/audit-log', '/views/platform/audit-log/index', 1, 'C', '0', '0', 'system:audit:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (14, '操作日志', 2, 12, '/system/oper-log', '/views/system/oper-log/index', 1, 'C', '0', '0', 'system:oper:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (20, '登录日志', 2, 13, '/system/login-log', '/views/system/login-log/index', 1, 'C', '0', '0', 'system:loginLog:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (21, '区域主数据', 2, 14, '/system/region', '/views/system/region/index', 1, 'C', '0', '0', 'system:region:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (15, '平台底座', 0, 3, '/platform', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '系统初始化目录'),
  (16, '组织架构增强', 15, 1, '/platform/org', '/views/platform/org/index', 1, 'C', '0', '0', 'system:org:view', NULL, 'system', NOW(), '系统初始化菜单'),
  (17, '数据权限', 15, 2, '/platform/data-scope', '/views/platform/data-scope/index', 1, 'C', '0', '0', 'system:dataScope:view', NULL, 'system', NOW(), '系统初始化菜单'),
  (18, '消息待办中心', 15, 3, '/platform/todo-center', '/views/platform/todo-center/index', 1, 'C', '0', '0', 'system:todo:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (19, '编码规则', 15, 4, '/platform/code-rule', '/views/platform/code-rule/index', 1, 'C', '0', '0', 'system:codeRule:list', NULL, 'system', NOW(), '系统初始化菜单'),
  (22, '流程引擎', 15, 5, '/platform/workflow', '/views/platform/workflow/index', 1, 'C', '0', '0', 'system:workflow:list', NULL, 'system', NOW(), '系统初始化菜单');

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
  UNION ALL SELECT '/platform/todo-center', '待办处理', 1, 'system:todo:handle'
  UNION ALL SELECT '/platform/code-rule', '编码规则查询', 1, 'system:codeRule:query'
  UNION ALL SELECT '/platform/code-rule', '编码规则新增', 2, 'system:codeRule:add'
  UNION ALL SELECT '/platform/code-rule', '编码规则修改', 3, 'system:codeRule:edit'
  UNION ALL SELECT '/platform/code-rule', '编码规则删除', 4, 'system:codeRule:remove'
  UNION ALL SELECT '/platform/code-rule', '编码规则生成', 5, 'system:codeRule:generate'
  UNION ALL SELECT '/platform/workflow', '流程定义查询', 1, 'system:workflow:query'
  UNION ALL SELECT '/platform/workflow', '流程定义新增', 2, 'system:workflow:add'
  UNION ALL SELECT '/platform/workflow', '流程定义修改', 3, 'system:workflow:edit'
  UNION ALL SELECT '/platform/workflow', '流程定义删除', 4, 'system:workflow:remove'
  UNION ALL SELECT '/platform/workflow', '流程定义发布', 5, 'system:workflow:publish'
  UNION ALL SELECT '/platform/workflow', '流程发起', 6, 'system:workflow:start'
  UNION ALL SELECT '/platform/workflow', '流程处理', 7, 'system:workflow:handle'
  UNION ALL SELECT '/platform/workflow', '流程设计', 8, 'system:workflow:design'
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

INSERT INTO `sys_wf_definition` (`definition_id`, `tenant_id`, `process_key`, `process_name`, `category`, `version`, `status`, `form_schema`, `model_content`, `publish_by`, `publish_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
  (1, '000000', 'purchase_apply', '采购审批流程', 'purchaseApprove', 1, '1', '{"fields":[{"name":"amount","label":"金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"}]}', 'system', NOW(), '系统初始化流程定义', 'system', NOW(), 'system', NOW()),
  (2, '000000', 'expense_apply', '报销审批流程', 'expense', 1, '0', '{"fields":[{"name":"feeType","label":"费用类型"},{"name":"total","label":"合计金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"},{"id":"NODE_2","name":"财务复核"}]}', NULL, NULL, '系统初始化流程定义', 'system', NOW(), 'system', NOW());

INSERT INTO `sys_wf_instance` (`instance_id`, `tenant_id`, `definition_id`, `process_key`, `process_name`, `category`, `business_no`, `business_type`, `form_data`, `current_node`, `initiator_user_id`, `initiator_user_name`, `initiator_nick_name`, `status`, `start_time`, `last_action`, `last_action_user_id`, `last_action_user_name`, `last_action_time`, `remark`)
VALUES
  (1, '000000', 1, 'purchase_apply', '采购审批流程', 'purchaseApprove', 'PO-20260309-001', '采购申请', '{"amount":12000,"reason":"办公设备采购"}', '部门负责人审批', 1, 'admin', '系统管理员', '0', NOW(), 'START', 1, 'admin', NOW(), '系统初始化流程实例');

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `title`, `notice_type`, `source`, `business_no`, `content`, `receiver_user_id`, `status`, `create_time`)
VALUES
  (1, '000000', '流程引擎已发布新版本，请核查审批节点配置', '系统公告', '流程引擎', NULL, '流程引擎发布 v2.0.1，请检查关键审批流配置。', 1, '0', NOW()),
  (2, '000000', '导入任务 IM20260307-01 执行完成', '审批通知', '导入导出中心', 'IM20260307-01', '导入任务执行完成，请查看结果。', 1, '1', NOW()),
  (3, '000000', '报表中心出现数据延迟预警', '预警提醒', '报表中心', NULL, '近30分钟内报表数据刷新延迟超过阈值。', 1, '0', NOW());

INSERT INTO `sys_todo_task` (`todo_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
VALUES
  (1, '000000', '请假审批', '部门负责人审批', 'LV-20260307-001', 'H', '0', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), '请及时处理'),
  (2, '000000', '采购申请', '财务复核', 'PO-20260307-018', 'M', '1', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '处理中'),
  (3, '000000', '合同归档', '档案确认', 'CT-20260306-021', 'L', '0', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), '待签收'),
  (4, '000000', '采购审批流程', '部门负责人审批', 'PO-20260309-001', 'M', '0', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '流程引擎初始化待办');

INSERT INTO `sys_wf_task` (`task_id`, `tenant_id`, `instance_id`, `definition_id`, `node_key`, `node_name`, `candidate_user_ids`, `assignee_user_id`, `assignee_user_name`, `assignee_nick_name`, `status`, `todo_id`, `due_time`, `create_time`)
VALUES
  (1, '000000', 1, 1, 'NODE_1', '部门负责人审批', '1', 1, 'admin', '系统管理员', '0', 4, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());

INSERT INTO `sys_wf_task_action` (`action_id`, `tenant_id`, `instance_id`, `task_id`, `definition_id`, `node_name`, `action_type`, `action_user_id`, `action_user_name`, `action_nick_name`, `to_assignee_user_id`, `action_comment`, `action_time`)
VALUES
  (1, '000000', 1, 1, 1, '部门负责人审批', 'START', 1, 'admin', '系统管理员', 1, '流程发起', NOW());

SET FOREIGN_KEY_CHECKS = 1;

