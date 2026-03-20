
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
