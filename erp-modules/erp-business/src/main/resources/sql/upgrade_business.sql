-- ERP Business Upgrade Script
-- 用途：在保留历史数据的场景下补齐 HR 基础模块表结构、字典、菜单与权限
-- 版本：2026-03-15

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `hr_employee_archive` (
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `cert_type` varchar(32) DEFAULT NULL COMMENT '证件类型',
  `cert_no` varchar(64) DEFAULT NULL COMMENT '证件号码',
  `gender` varchar(16) DEFAULT NULL COMMENT '性别',
  `birth_date` datetime DEFAULT NULL COMMENT '出生日期',
  `employment_type` varchar(32) DEFAULT NULL COMMENT '用工类型',
  `hire_date` datetime DEFAULT NULL COMMENT '入职日期',
  `probation_end_date` datetime DEFAULT NULL COMMENT '试用期结束日期',
  `highest_education` varchar(64) DEFAULT NULL COMMENT '最高学历',
  `emergency_contact` varchar(64) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(32) DEFAULT NULL COMMENT '紧急联系电话',
  `home_address` varchar(255) DEFAULT NULL COMMENT '家庭住址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `uk_hr_employee_archive_cert` (`tenant_id`, `cert_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工扩展档案表';

CREATE TABLE IF NOT EXISTS `hr_employee_position` (
  `position_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任职ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `post_id` bigint(20) DEFAULT NULL COMMENT '岗位ID',
  `post_name` varchar(128) DEFAULT NULL COMMENT '岗位名称',
  `position_type` varchar(32) DEFAULT NULL COMMENT '任职类型',
  `primary_flag` char(1) DEFAULT 'Y' COMMENT '是否主岗（Y/N）',
  `start_date` datetime DEFAULT NULL COMMENT '生效日期',
  `end_date` datetime DEFAULT NULL COMMENT '失效日期',
  `status` varchar(32) DEFAULT 'ACTIVE' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`position_id`),
  KEY `idx_hr_employee_position_emp` (`tenant_id`, `employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工任职关系表';

CREATE TABLE IF NOT EXISTS `hr_employee_change` (
  `change_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '异动ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `change_type` varchar(32) NOT NULL COMMENT '异动类型',
  `effective_date` datetime DEFAULT NULL COMMENT '生效日期',
  `before_snapshot` longtext COMMENT '异动前快照',
  `after_snapshot` longtext COMMENT '异动后快照',
  `status` varchar(32) DEFAULT 'DRAFT' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`change_id`),
  KEY `idx_hr_employee_change_emp` (`tenant_id`, `employee_id`, `change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工异动事件表';

CREATE TABLE IF NOT EXISTS `hr_employee_contract` (
  `contract_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号',
  `contract_type` varchar(32) DEFAULT NULL COMMENT '合同类型',
  `start_date` datetime DEFAULT NULL COMMENT '合同开始日期',
  `end_date` datetime DEFAULT NULL COMMENT '合同结束日期',
  `status` varchar(32) DEFAULT 'DRAFT' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`contract_id`),
  KEY `idx_hr_employee_contract_emp` (`tenant_id`, `employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工合同表';

CREATE TABLE IF NOT EXISTS `hr_employee_document` (
  `document_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `document_type` varchar(32) DEFAULT NULL COMMENT '档案类型',
  `document_name` varchar(128) DEFAULT NULL COMMENT '档案名称',
  `file_url` varchar(255) DEFAULT NULL COMMENT '文件地址',
  `expire_date` datetime DEFAULT NULL COMMENT '到期日期',
  `status` varchar(32) DEFAULT 'ACTIVE' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`document_id`),
  KEY `idx_hr_employee_document_emp` (`tenant_id`, `employee_id`, `document_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工电子档案表';

CREATE TABLE IF NOT EXISTS `hr_salary_field_mapping` (
  `mapping_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `direction` varchar(32) NOT NULL COMMENT '同步方向',
  `field_code` varchar(64) NOT NULL COMMENT '源字段编码',
  `field_name` varchar(128) DEFAULT NULL COMMENT '源字段名称',
  `target_field` varchar(128) NOT NULL COMMENT '目标字段',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `status` varchar(32) DEFAULT 'ACTIVE' COMMENT '状态',
  `sort_no` int(11) DEFAULT 1 COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`),
  KEY `idx_hr_salary_mapping_query` (`tenant_id`, `direction`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资字段映射表';

CREATE TABLE IF NOT EXISTS `hr_salary_sync_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '同步日志ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `direction` varchar(32) NOT NULL COMMENT '同步方向',
  `period_code` varchar(32) DEFAULT NULL COMMENT '期间编码',
  `sync_status` varchar(32) DEFAULT 'PENDING' COMMENT '同步状态',
  `request_no` varchar(64) NOT NULL COMMENT '请求号',
  `payload_json` longtext COMMENT '请求载荷',
  `response_json` longtext COMMENT '响应载荷',
  `external_status` varchar(64) DEFAULT NULL COMMENT '外部状态',
  `retry_count` int(11) DEFAULT 0 COMMENT '重试次数',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `uk_hr_salary_sync_log_request` (`tenant_id`, `request_no`),
  KEY `idx_hr_salary_sync_log_query` (`tenant_id`, `employee_id`, `period_code`, `sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资同步日志表';

CREATE TABLE IF NOT EXISTS `hr_salary_retry_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '重试任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `log_id` bigint(20) NOT NULL COMMENT '同步日志ID',
  `task_status` varchar(32) DEFAULT 'PENDING' COMMENT '任务状态',
  `retry_count` int(11) DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_hr_salary_retry_task_query` (`tenant_id`, `log_id`, `task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资重试任务表';

CREATE TABLE IF NOT EXISTS `hr_warning_record` (
  `warning_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `warning_type` varchar(32) NOT NULL COMMENT '预警类型',
  `warning_key` varchar(128) NOT NULL COMMENT '预警幂等键',
  `warning_title` varchar(255) DEFAULT NULL COMMENT '预警标题',
  `warning_content` varchar(1000) DEFAULT NULL COMMENT '预警内容',
  `expire_date` datetime DEFAULT NULL COMMENT '到期日期',
  `status` varchar(32) DEFAULT 'NEW' COMMENT '处理状态',
  `read_by` varchar(64) DEFAULT NULL COMMENT '读取人',
  `read_time` datetime DEFAULT NULL COMMENT '读取时间',
  `handled_by` varchar(64) DEFAULT NULL COMMENT '处理人',
  `handled_time` datetime DEFAULT NULL COMMENT '处理时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`warning_id`),
  UNIQUE KEY `uk_hr_warning_record_key` (`tenant_id`, `warning_key`),
  KEY `idx_hr_warning_record_query` (`tenant_id`, `employee_id`, `warning_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR预警记录表';

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT 'HR证件号唯一校验开关', 'hr.employee.cert_unique_enabled', 'true', 'N', 'system', NOW(), 'true 表示租户内证件号唯一校验开启'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_config` WHERE `config_key` = 'hr.employee.cert_unique_enabled'
);

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT 'HR证件类型', 'hr_cert_type', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'hr_cert_type'
);

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT 'HR用工类型', 'hr_employment_type', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'hr_employment_type'
);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 1, '居民身份证', 'ID_CARD', 'hr_cert_type', 'Y', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'hr_cert_type' AND `dict_value` = 'ID_CARD'
);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 2, '护照', 'PASSPORT', 'hr_cert_type', 'N', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'hr_cert_type' AND `dict_value` = 'PASSPORT'
);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 3, '港澳通行证', 'HKM_MACAO', 'hr_cert_type', 'N', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'hr_cert_type' AND `dict_value` = 'HKM_MACAO'
);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 1, '正式员工', 'FULL_TIME', 'hr_employment_type', 'Y', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'hr_employment_type' AND `dict_value` = 'FULL_TIME'
);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 2, '劳务派遣', 'DISPATCH', 'hr_employment_type', 'N', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'hr_employment_type' AND `dict_value` = 'DISPATCH'
);

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 3, '实习生', 'INTERN', 'hr_employment_type', 'N', '0', 'system', NOW(), 'HR基础字典'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'hr_employment_type' AND `dict_value` = 'INTERN'
);

INSERT INTO `sys_code_rule` (`tenant_id`, `rule_code`, `rule_name`, `prefix`, `date_pattern`, `seq_length`, `current_seq`, `status`, `create_time`, `remark`)
SELECT '000000', 'HR_EMPLOYEE', 'HR员工编码', 'HE', 'yyyyMMdd', 4, 0, '0', NOW(), 'HR基础员工编码规则'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_code_rule` WHERE `tenant_id` = '000000' AND `rule_code` = 'HR_EMPLOYEE'
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '人力资源', 0, 2, '/human-resource', NULL, 1, 'M', '0', '0', NULL, 'UserFilled', 'system', NOW(), 'HR基础目录'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `path` = '/human-resource'
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '员工档案', (SELECT menu_id FROM `sys_menu` WHERE `path` = '/human-resource' LIMIT 1), 1, '/business/hr/employee', '/views/business/hr/employee/index', 1, 'C', '0', '0', 'business:hr:employee:list', 'User', 'system', NOW(), 'HR基础菜单'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `path` = '/business/hr/employee'
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT menu_def.menu_name, parent_menu.menu_id, menu_def.order_num, menu_def.path, menu_def.component, 1, 'C', '0', '0', menu_def.perms, menu_def.icon, 'system', NOW(), 'HR扩展菜单'
FROM (
  SELECT 2 AS order_num, '/business/hr/contract' AS path, '/views/business/hr/contract/index' AS component, '合同管理' AS menu_name, 'business:hr:contract:list' AS perms, 'Tickets' AS icon
  UNION ALL SELECT 3, '/business/hr/document', '/views/business/hr/document/index', '电子档案', 'business:hr:document:list', 'FolderOpened'
  UNION ALL SELECT 4, '/business/hr/integration', '/views/business/hr/integration/index', '接口中心', 'business:hr:integration:salary', 'Connection'
  UNION ALL SELECT 5, '/business/hr/warning', '/views/business/hr/warning/index', '预警中心', 'business:hr:warning:list', 'Warning'
) menu_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/human-resource'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu WHERE existed_menu.path = menu_def.path
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, parent_menu.menu_id, button_def.order_num, button_def.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), 'HR基础按钮权限'
FROM (
  SELECT '/business/hr/employee' AS path, '员工档案查询' AS menu_name, 1 AS order_num, 'business:hr:employee:query' AS perms
  UNION ALL SELECT '/business/hr/employee', '员工档案新增', 2, 'business:hr:employee:add'
  UNION ALL SELECT '/business/hr/employee', '员工档案修改', 3, 'business:hr:employee:edit'
  UNION ALL SELECT '/business/hr/employee', '员工档案提交', 4, 'business:hr:employee:submit'
  UNION ALL SELECT '/business/hr/employee', '员工档案离职', 5, 'business:hr:employee:leave'
  UNION ALL SELECT '/business/hr/employee', '员工档案删除', 6, 'business:hr:employee:remove'
  UNION ALL SELECT '/business/hr/employee', '任职查询', 7, 'business:hr:position:list'
  UNION ALL SELECT '/business/hr/employee', '任职新增', 8, 'business:hr:position:add'
  UNION ALL SELECT '/business/hr/employee', '任职修改', 9, 'business:hr:position:edit'
  UNION ALL SELECT '/business/hr/employee', '异动查询', 10, 'business:hr:change:list'
  UNION ALL SELECT '/business/hr/employee', '异动提交', 11, 'business:hr:change:submit'
) button_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_def.path AND parent_menu.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = button_def.path
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, parent_menu.menu_id, button_def.order_num, button_def.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), 'HR扩展按钮权限'
FROM (
  SELECT '/business/hr/contract' AS path, '合同查询' AS menu_name, 1 AS order_num, 'business:hr:contract:query' AS perms
  UNION ALL SELECT '/business/hr/contract', '合同新增', 2, 'business:hr:contract:add'
  UNION ALL SELECT '/business/hr/contract', '合同修改', 3, 'business:hr:contract:edit'
  UNION ALL SELECT '/business/hr/contract', '合同删除', 4, 'business:hr:contract:remove'
  UNION ALL SELECT '/business/hr/document', '档案查询', 1, 'business:hr:document:query'
  UNION ALL SELECT '/business/hr/document', '档案新增', 2, 'business:hr:document:add'
  UNION ALL SELECT '/business/hr/document', '档案修改', 3, 'business:hr:document:edit'
  UNION ALL SELECT '/business/hr/document', '档案删除', 4, 'business:hr:document:remove'
  UNION ALL SELECT '/business/hr/employee', '员工导入', 12, 'business:hr:employee:import'
  UNION ALL SELECT '/business/hr/integration', '薪资接口配置', 1, 'business:hr:integration:salary'
  UNION ALL SELECT '/business/hr/warning', '预警扫描', 1, 'business:hr:warning:scan'
  UNION ALL SELECT '/business/hr/warning', '预警处理', 2, 'business:hr:warning:handle'
) button_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_def.path AND parent_menu.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = button_def.path
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', 1, menu_item.menu_id
FROM `sys_menu` menu_item
WHERE (
  menu_item.path IN ('/human-resource', '/business/hr/employee')
  OR menu_item.perms IN (
    'business:hr:employee:list',
    'business:hr:employee:query',
    'business:hr:employee:add',
    'business:hr:employee:edit',
    'business:hr:employee:submit',
    'business:hr:employee:leave',
    'business:hr:employee:remove',
    'business:hr:position:list',
    'business:hr:position:add',
    'business:hr:position:edit',
    'business:hr:change:list',
    'business:hr:change:submit',
    'business:hr:employee:import',
    'business:hr:contract:list',
    'business:hr:contract:query',
    'business:hr:contract:add',
    'business:hr:contract:edit',
    'business:hr:contract:remove',
    'business:hr:document:list',
    'business:hr:document:query',
    'business:hr:document:add',
    'business:hr:document:edit',
    'business:hr:document:remove',
    'business:hr:integration:salary',
    'business:hr:warning:list',
    'business:hr:warning:scan',
    'business:hr:warning:handle'
  )
  OR menu_item.path IN ('/business/hr/contract', '/business/hr/document', '/business/hr/integration', '/business/hr/warning')
)
AND NOT EXISTS (
  SELECT 1
  FROM `sys_role_menu` existed_role_menu
  WHERE existed_role_menu.role_id = 1
    AND existed_role_menu.menu_id = menu_item.menu_id
);

-- 2026-03-20 HR 第三阶段：出勤管理 / 薪酬核算 / 绩效考核
CREATE TABLE IF NOT EXISTS `hr_attendance_field_mapping` (
  `mapping_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `direction` varchar(32) NOT NULL COMMENT '同步方向',
  `field_code` varchar(64) NOT NULL COMMENT '源字段编码',
  `field_name` varchar(128) DEFAULT NULL COMMENT '源字段名称',
  `target_field` varchar(128) NOT NULL COMMENT '目标字段',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `status` varchar(32) DEFAULT 'ACTIVE' COMMENT '状态',
  `sort_no` int(11) DEFAULT 1 COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`),
  KEY `idx_hr_attendance_mapping_query` (`tenant_id`, `direction`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤字段映射表';

CREATE TABLE IF NOT EXISTS `hr_attendance_sync_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '同步日志ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `direction` varchar(32) NOT NULL COMMENT '同步方向',
  `period_code` varchar(32) DEFAULT NULL COMMENT '期间编码',
  `sync_status` varchar(32) DEFAULT 'PENDING' COMMENT '同步状态',
  `request_no` varchar(64) NOT NULL COMMENT '请求号',
  `payload_json` longtext COMMENT '请求载荷',
  `response_json` longtext COMMENT '响应载荷',
  `external_status` varchar(64) DEFAULT NULL COMMENT '外部状态',
  `retry_count` int(11) DEFAULT 0 COMMENT '重试次数',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `uk_hr_attendance_sync_log_request` (`tenant_id`, `request_no`),
  KEY `idx_hr_attendance_sync_log_query` (`tenant_id`, `employee_id`, `period_code`, `sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤同步日志表';

CREATE TABLE IF NOT EXISTS `hr_attendance_retry_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '重试任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `log_id` bigint(20) NOT NULL COMMENT '同步日志ID',
  `task_status` varchar(32) DEFAULT 'PENDING' COMMENT '任务状态',
  `retry_count` int(11) DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_hr_attendance_retry_task_query` (`tenant_id`, `log_id`, `task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤重试任务表';

CREATE TABLE IF NOT EXISTS `hr_performance_field_mapping` (
  `mapping_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `direction` varchar(32) NOT NULL COMMENT '同步方向',
  `field_code` varchar(64) NOT NULL COMMENT '源字段编码',
  `field_name` varchar(128) DEFAULT NULL COMMENT '源字段名称',
  `target_field` varchar(128) NOT NULL COMMENT '目标字段',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `status` varchar(32) DEFAULT 'ACTIVE' COMMENT '状态',
  `sort_no` int(11) DEFAULT 1 COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`mapping_id`),
  KEY `idx_hr_performance_mapping_query` (`tenant_id`, `direction`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效字段映射表';

CREATE TABLE IF NOT EXISTS `hr_performance_sync_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '同步日志ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `direction` varchar(32) NOT NULL COMMENT '同步方向',
  `period_code` varchar(32) DEFAULT NULL COMMENT '期间编码',
  `sync_status` varchar(32) DEFAULT 'PENDING' COMMENT '同步状态',
  `request_no` varchar(64) NOT NULL COMMENT '请求号',
  `payload_json` longtext COMMENT '请求载荷',
  `response_json` longtext COMMENT '响应载荷',
  `external_status` varchar(64) DEFAULT NULL COMMENT '外部状态',
  `retry_count` int(11) DEFAULT 0 COMMENT '重试次数',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `uk_hr_performance_sync_log_request` (`tenant_id`, `request_no`),
  KEY `idx_hr_performance_sync_log_query` (`tenant_id`, `employee_id`, `period_code`, `sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效同步日志表';

CREATE TABLE IF NOT EXISTS `hr_performance_retry_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '重试任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `log_id` bigint(20) NOT NULL COMMENT '同步日志ID',
  `task_status` varchar(32) DEFAULT 'PENDING' COMMENT '任务状态',
  `retry_count` int(11) DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最后错误',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_hr_performance_retry_task_query` (`tenant_id`, `log_id`, `task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效重试任务表';

UPDATE `sys_menu`
SET `menu_name` = '接口中心(兼容)', `visible` = '1', `remark` = 'HR 历史兼容菜单'
WHERE `path` = '/business/hr/integration';

UPDATE `sys_menu`
SET `order_num` = 7
WHERE `path` = '/business/hr/warning';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT menu_def.menu_name, parent_menu.menu_id, menu_def.order_num, menu_def.path, menu_def.component, 1, 'C', '0', '0', menu_def.perms, menu_def.icon, 'system', NOW(), 'HR 第三阶段菜单'
FROM (
  SELECT 4 AS order_num, '/business/hr/attendance' AS path, '/views/business/hr/attendance/index' AS component, '出勤管理' AS menu_name, 'business:hr:attendance:list' AS perms, 'Timer' AS icon
  UNION ALL SELECT 5, '/business/hr/payroll', '/views/business/hr/payroll/index', '薪酬核算', 'business:hr:payroll:list', 'Money'
  UNION ALL SELECT 6, '/business/hr/performance', '/views/business/hr/performance/index', '绩效考核', 'business:hr:performance:list', 'TrendCharts'
) menu_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/human-resource'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu WHERE existed_menu.path = menu_def.path
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, parent_menu.menu_id, button_def.order_num, button_def.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), 'HR 第三阶段按钮权限'
FROM (
  SELECT '/business/hr/attendance' AS path, '出勤管理查询' AS menu_name, 1 AS order_num, 'business:hr:attendance:list' AS perms
  UNION ALL SELECT '/business/hr/attendance', '出勤管理配置', 2, 'business:hr:attendance:config'
  UNION ALL SELECT '/business/hr/attendance', '出勤推送', 3, 'business:hr:attendance:push'
  UNION ALL SELECT '/business/hr/attendance', '出勤重试', 4, 'business:hr:attendance:retry'
  UNION ALL SELECT '/business/hr/payroll', '薪酬核算查询', 1, 'business:hr:payroll:list'
  UNION ALL SELECT '/business/hr/payroll', '薪酬核算配置', 2, 'business:hr:payroll:config'
  UNION ALL SELECT '/business/hr/payroll', '薪酬推送', 3, 'business:hr:payroll:push'
  UNION ALL SELECT '/business/hr/payroll', '薪酬重试', 4, 'business:hr:payroll:retry'
  UNION ALL SELECT '/business/hr/performance', '绩效考核查询', 1, 'business:hr:performance:list'
  UNION ALL SELECT '/business/hr/performance', '绩效考核配置', 2, 'business:hr:performance:config'
  UNION ALL SELECT '/business/hr/performance', '绩效推送', 3, 'business:hr:performance:push'
  UNION ALL SELECT '/business/hr/performance', '绩效重试', 4, 'business:hr:performance:retry'
) button_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_def.path AND parent_menu.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = button_def.path
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', admin_role.role_id, menu_item.menu_id
FROM `sys_role` admin_role
INNER JOIN `sys_menu` menu_item ON (
  menu_item.path IN ('/business/hr/attendance', '/business/hr/payroll', '/business/hr/performance')
  OR menu_item.perms IN (
    'business:hr:attendance:list',
    'business:hr:attendance:config',
    'business:hr:attendance:push',
    'business:hr:attendance:retry',
    'business:hr:payroll:list',
    'business:hr:payroll:config',
    'business:hr:payroll:push',
    'business:hr:payroll:retry',
    'business:hr:performance:list',
    'business:hr:performance:config',
    'business:hr:performance:push',
    'business:hr:performance:retry'
  )
)
WHERE admin_role.tenant_id = '000000'
  AND admin_role.role_key = 'admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed_role_menu
    WHERE existed_role_menu.tenant_id = '000000'
      AND existed_role_menu.role_id = admin_role.role_id
      AND existed_role_menu.menu_id = menu_item.menu_id
  );

