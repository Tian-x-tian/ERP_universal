-- 2026-04-14 出勤核心：系统签到、汇总、异常、定位规则、请假、加班
CREATE TABLE IF NOT EXISTS `hr_attendance_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '出勤记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `work_date` date NOT NULL COMMENT '出勤日期',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `authority_flag` char(1) DEFAULT 'N' COMMENT '是否权威来源',
  `external_biz_no` varchar(128) DEFAULT NULL COMMENT '外部业务号',
  `sign_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `sign_out_time` datetime DEFAULT NULL COMMENT '签退时间',
  `sign_in_latitude` decimal(10,6) DEFAULT NULL COMMENT '签到纬度',
  `sign_in_longitude` decimal(10,6) DEFAULT NULL COMMENT '签到经度',
  `sign_out_latitude` decimal(10,6) DEFAULT NULL COMMENT '签退纬度',
  `sign_out_longitude` decimal(10,6) DEFAULT NULL COMMENT '签退经度',
  `sign_in_address` varchar(255) DEFAULT NULL COMMENT '签到地址',
  `sign_out_address` varchar(255) DEFAULT NULL COMMENT '签退地址',
  `sign_in_in_range` char(1) DEFAULT NULL COMMENT '签到是否范围内',
  `sign_out_in_range` char(1) DEFAULT NULL COMMENT '签退是否范围内',
  `device_source` varchar(64) DEFAULT NULL COMMENT '设备来源',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_hr_attendance_record_emp_day` (`tenant_id`, `employee_id`, `work_date`, `source_type`),
  KEY `idx_hr_attendance_record_dept_day` (`tenant_id`, `dept_id`, `work_date`),
  KEY `idx_hr_attendance_record_external` (`tenant_id`, `external_biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤原子记录';

CREATE TABLE IF NOT EXISTS `hr_attendance_day_summary` (
  `summary_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日汇总ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `work_date` date NOT NULL COMMENT '出勤日期',
  `month_code` varchar(16) NOT NULL COMMENT '月份编码',
  `authority_record_id` bigint(20) DEFAULT NULL COMMENT '权威记录ID',
  `primary_source_type` varchar(32) DEFAULT NULL COMMENT '主来源类型',
  `sign_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `sign_out_time` datetime DEFAULT NULL COMMENT '签退时间',
  `actual_minutes` int(11) DEFAULT 0 COMMENT '实出勤分钟',
  `attendance_days` decimal(10,2) DEFAULT 0 COMMENT '出勤天数',
  `leave_minutes` int(11) DEFAULT 0 COMMENT '请假分钟',
  `leave_days` decimal(10,2) DEFAULT 0 COMMENT '请假天数',
  `overtime_minutes` int(11) DEFAULT 0 COMMENT '加班分钟',
  `late_count` int(11) DEFAULT 0 COMMENT '迟到次数',
  `early_leave_count` int(11) DEFAULT 0 COMMENT '早退次数',
  `missing_card_count` int(11) DEFAULT 0 COMMENT '缺卡次数',
  `absenteeism_days` decimal(10,2) DEFAULT 0 COMMENT '旷工天数',
  `abnormal_count` int(11) DEFAULT 0 COMMENT '异常次数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `uk_hr_attendance_day_summary` (`tenant_id`, `employee_id`, `work_date`),
  KEY `idx_hr_attendance_day_dept` (`tenant_id`, `dept_id`, `work_date`),
  KEY `idx_hr_attendance_day_month` (`tenant_id`, `employee_id`, `month_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工日出勤汇总';

CREATE TABLE IF NOT EXISTS `hr_attendance_month_summary` (
  `summary_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '月汇总ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `month_code` varchar(16) NOT NULL COMMENT '月份编码',
  `attendance_days` decimal(10,2) DEFAULT 0 COMMENT '出勤天数',
  `actual_minutes` int(11) DEFAULT 0 COMMENT '实出勤分钟',
  `leave_minutes` int(11) DEFAULT 0 COMMENT '请假分钟',
  `leave_days` decimal(10,2) DEFAULT 0 COMMENT '请假天数',
  `overtime_minutes` int(11) DEFAULT 0 COMMENT '加班分钟',
  `late_count` int(11) DEFAULT 0 COMMENT '迟到次数',
  `early_leave_count` int(11) DEFAULT 0 COMMENT '早退次数',
  `missing_card_count` int(11) DEFAULT 0 COMMENT '缺卡次数',
  `absenteeism_days` decimal(10,2) DEFAULT 0 COMMENT '旷工天数',
  `abnormal_count` int(11) DEFAULT 0 COMMENT '异常次数',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `uk_hr_attendance_month_summary` (`tenant_id`, `employee_id`, `month_code`),
  KEY `idx_hr_attendance_month_dept` (`tenant_id`, `dept_id`, `month_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工月出勤汇总';

CREATE TABLE IF NOT EXISTS `hr_attendance_exception` (
  `exception_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '异常ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `work_date` date NOT NULL COMMENT '出勤日期',
  `record_id` bigint(20) DEFAULT NULL COMMENT '出勤记录ID',
  `exception_type` varchar(32) NOT NULL COMMENT '异常类型',
  `exception_message` varchar(500) DEFAULT NULL COMMENT '异常说明',
  `source_type` varchar(32) DEFAULT NULL COMMENT '来源类型',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`exception_id`),
  KEY `idx_hr_attendance_exception_emp_day` (`tenant_id`, `employee_id`, `work_date`),
  KEY `idx_hr_attendance_exception_dept_day` (`tenant_id`, `dept_id`, `work_date`, `exception_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤异常记录';

CREATE TABLE IF NOT EXISTS `hr_attendance_location_rule` (
  `rule_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `center_latitude` decimal(10,6) DEFAULT NULL COMMENT '中心纬度',
  `center_longitude` decimal(10,6) DEFAULT NULL COMMENT '中心经度',
  `radius_meters` int(11) DEFAULT 300 COMMENT '允许半径米',
  `enabled_flag` char(1) DEFAULT 'Y' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  KEY `idx_hr_attendance_rule_dept` (`tenant_id`, `dept_id`, `enabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤定位规则';

CREATE TABLE IF NOT EXISTS `hr_attendance_leave_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '请假单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `order_no` varchar(64) NOT NULL COMMENT '单据编号',
  `leave_type` varchar(32) NOT NULL COMMENT '请假类型',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `leave_minutes` int(11) DEFAULT 0 COMMENT '请假分钟',
  `leave_days` decimal(10,2) DEFAULT 0 COMMENT '请假天数',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `workflow_instance_no` varchar(64) DEFAULT NULL COMMENT '流程实例号',
  `reason` varchar(500) DEFAULT NULL COMMENT '请假原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_hr_attendance_leave_order_no` (`tenant_id`, `order_no`),
  KEY `idx_hr_attendance_leave_emp` (`tenant_id`, `employee_id`, `status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤请假单';

CREATE TABLE IF NOT EXISTS `hr_attendance_overtime_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '加班单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `employee_id` bigint(20) NOT NULL COMMENT '员工ID',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `order_no` varchar(64) NOT NULL COMMENT '单据编号',
  `overtime_type` varchar(32) NOT NULL COMMENT '加班类型',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `overtime_minutes` int(11) DEFAULT 0 COMMENT '加班分钟',
  `status` varchar(32) NOT NULL COMMENT '单据状态',
  `process_key` varchar(64) DEFAULT NULL COMMENT '流程标识',
  `workflow_instance_no` varchar(64) DEFAULT NULL COMMENT '流程实例号',
  `reason` varchar(500) DEFAULT NULL COMMENT '加班原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_hr_attendance_overtime_order_no` (`tenant_id`, `order_no`),
  KEY `idx_hr_attendance_overtime_emp` (`tenant_id`, `employee_id`, `status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤加班单';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, parent_menu.menu_id, button_def.order_num, parent_menu.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), '出勤核心按钮权限'
FROM (
  SELECT '出勤签到' AS menu_name, 5 AS order_num, 'business:hr:attendance:sign' AS perms
  UNION ALL SELECT '个人出勤查询', 6, 'business:hr:attendance:personal'
  UNION ALL SELECT '部门出勤汇总', 7, 'business:hr:attendance:dept'
  UNION ALL SELECT '公司出勤汇总', 8, 'business:hr:attendance:company'
  UNION ALL SELECT '请假单管理', 9, 'business:hr:attendance:leave'
  UNION ALL SELECT '请假提交审批', 10, 'business:hr:attendance:leave:submit'
  UNION ALL SELECT '加班单管理', 11, 'business:hr:attendance:overtime'
  UNION ALL SELECT '加班提交审批', 12, 'business:hr:attendance:overtime:submit'
) button_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/business/hr/attendance' AND parent_menu.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.parent_id = parent_menu.menu_id
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', admin_role.role_id, menu_item.menu_id
FROM `sys_role` admin_role
INNER JOIN `sys_menu` menu_item ON menu_item.perms IN (
  'business:hr:attendance:sign',
  'business:hr:attendance:personal',
  'business:hr:attendance:dept',
  'business:hr:attendance:company',
  'business:hr:attendance:leave',
  'business:hr:attendance:leave:submit',
  'business:hr:attendance:overtime',
  'business:hr:attendance:overtime:submit'
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

