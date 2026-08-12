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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inv_outbound_order_line` (`tenant_id`,`order_id`,`line_no`),
  KEY `idx_inv_outbound_line_item` (`tenant_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单行';

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
  `file_size` bigint(20) NOT NULL DEFAULT 0 COMMENT '文件字节数',
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

CREATE TABLE IF NOT EXISTS `biz_saas_storage_object` (
  `storage_object_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '存储对象ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `object_key` varchar(512) NOT NULL COMMENT '对象存储键',
  `byte_size` bigint(20) NOT NULL COMMENT '对象字节数',
  `status` varchar(16) NOT NULL COMMENT 'UPLOADING, ACTIVE, ORPHANED, or DELETED',
  `quota_reference_key` varchar(128) DEFAULT NULL COMMENT '本地配额预留引用',
  `last_error` varchar(128) DEFAULT NULL COMMENT '最近补偿错误类型',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime(3) NOT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime(3) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`storage_object_id`),
  UNIQUE KEY `uk_biz_saas_storage_object` (`tenant_id`, `object_key`),
  KEY `idx_biz_saas_storage_status` (`tenant_id`, `status`, `update_time`),
  CONSTRAINT `ck_biz_saas_storage_size` CHECK (`byte_size` >= 0),
  CONSTRAINT `ck_biz_saas_storage_status` CHECK (`status` IN ('UPLOADING', 'ACTIVE', 'ORPHANED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户对象存储用量台账';

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
SELECT '000000', admin_role.role_id, menu_item.menu_id
FROM `sys_role` admin_role
INNER JOIN `sys_menu` menu_item ON (
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
WHERE admin_role.tenant_id = '000000'
  AND admin_role.role_key = 'admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_menu` existed_role_menu
    WHERE existed_role_menu.tenant_id = '000000'
      AND existed_role_menu.role_id = admin_role.role_id
      AND existed_role_menu.menu_id = menu_item.menu_id
  );

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
  UNIQUE KEY `uk_inv_transfer_bill_no` (`tenant_id`,`bill_no`),
  KEY `idx_inv_transfer_order_status` (`tenant_id`,`status`,`update_time`)
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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `mdm_item` ADD COLUMN `default_expiry_warn_days` int(11) DEFAULT NULL COMMENT ''默认临期预警天数'' AFTER `shelf_life_days`',
            'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mdm_item'
    AND COLUMN_NAME = 'default_expiry_warn_days'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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


-- 2026-04-14 出勤核心初始化结构

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

-- 2026-04-14 出勤菜单拆分初始化
-- 2026-04-14 出勤菜单拆分：签到进入顶级工作台，HR 出勤管理聚焦数据与集成
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '签到', workbench_menu.menu_id, 1, '/workbench/attendance', '/views/workbench/attendance/index', 1, 'C', '0', '0', 'business:hr:attendance:list', 'Timer', 'system', NOW(), '个人签到签退入口'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) workbench_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/attendance');

UPDATE `sys_menu`
SET `menu_name` = '签到',
    `parent_id` = (SELECT workbench_menu.menu_id FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) workbench_menu),
    `order_num` = 1,
    `component` = '/views/workbench/attendance/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'business:hr:attendance:list',
    `icon` = 'Timer',
    `remark` = '个人签到签退入口'
WHERE `path` = '/workbench/attendance';

UPDATE `sys_menu`
SET `order_num` = 2
WHERE `path` = '/workbench/system-notice'
  AND `menu_type` = 'C';

UPDATE `sys_menu`
SET `order_num` = 3
WHERE `path` = '/workbench/process-todo'
  AND `menu_type` = 'C';

UPDATE `sys_menu`
SET `menu_name` = '出勤管理',
    `component` = '/views/business/hr/attendance/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'business:hr:attendance:list',
    `icon` = 'Timer',
    `remark` = '出勤数据与签到集成管理'
WHERE `path` = '/business/hr/attendance'
  AND `parent_id` IN (SELECT parent_menu.menu_id FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll') parent_menu)
  AND `menu_type` = 'C';

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT source_role_menu.tenant_id, source_role_menu.role_id, workbench_attendance.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN `sys_menu` workbench_attendance ON workbench_attendance.path = '/workbench/attendance'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = source_role_menu.tenant_id
      AND existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = workbench_attendance.menu_id
WHERE ((source_menu.path = '/business/hr/attendance' AND source_menu.menu_type = 'C') OR source_menu.perms = 'business:hr:attendance:sign')
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id, child_role_menu.role_id, workbench_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` workbench_menu ON workbench_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = child_role_menu.tenant_id
      AND existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = workbench_menu.menu_id
WHERE child_menu.path = '/workbench/attendance'
  AND existed_role_menu.menu_id IS NULL;




-- 2026-04-14 出勤菜单修复初始化
-- 2026-04-14 出勤菜单修复：清理因按钮复用 path 导致的重复菜单，并重建按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '出勤管理', parent_menu.menu_id, 4, '/business/hr/attendance', '/views/business/hr/attendance/index', 1, 'C', '0', '0', 'business:hr:attendance:list', 'Timer', 'system', NOW(), '出勤数据与签到集成管理'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll' LIMIT 1) parent_menu
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.path = '/business/hr/attendance'
    AND existed_menu.menu_type = 'C'
    AND existed_menu.parent_id = parent_menu.menu_id
);

DELETE role_menu
FROM `sys_role_menu` role_menu
INNER JOIN `sys_menu` menu_item ON menu_item.menu_id = role_menu.menu_id
WHERE menu_item.path = '/business/hr/attendance'
  AND menu_item.menu_id <> (
    SELECT keep_menu.menu_id
    FROM (
      SELECT MIN(menu_id) AS menu_id
      FROM `sys_menu`
      WHERE `path` = '/business/hr/attendance'
        AND `menu_type` = 'C'
        AND `parent_id` IN (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll')
    ) keep_menu
  );

DELETE menu_item
FROM `sys_menu` menu_item
WHERE menu_item.path = '/business/hr/attendance'
  AND menu_item.menu_id <> (
    SELECT keep_menu.menu_id
    FROM (
      SELECT MIN(menu_id) AS menu_id
      FROM `sys_menu`
      WHERE `path` = '/business/hr/attendance'
        AND `menu_type` = 'C'
        AND `parent_id` IN (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll')
    ) keep_menu
  );

UPDATE `sys_menu`
SET `menu_name` = '出勤管理',
    `parent_id` = (SELECT parent_menu.menu_id FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/human-resource/attendance-payroll' LIMIT 1) parent_menu),
    `order_num` = 4,
    `component` = '/views/business/hr/attendance/index',
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'business:hr:attendance:list',
    `icon` = 'Timer',
    `remark` = '出勤数据与签到集成管理'
WHERE `path` = '/business/hr/attendance'
  AND `menu_type` = 'C';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, attendance_menu.menu_id, button_def.order_num, attendance_menu.path, NULL, 1, 'F', '0', '0', button_def.perms, NULL, 'system', NOW(), '出勤按钮权限'
FROM (
  SELECT '出勤管理查询' AS menu_name, 1 AS order_num, 'business:hr:attendance:list' AS perms
  UNION ALL SELECT '出勤管理配置', 2, 'business:hr:attendance:config'
  UNION ALL SELECT '出勤推送', 3, 'business:hr:attendance:push'
  UNION ALL SELECT '出勤重试', 4, 'business:hr:attendance:retry'
  UNION ALL SELECT '出勤签到', 5, 'business:hr:attendance:sign'
  UNION ALL SELECT '个人出勤查询', 6, 'business:hr:attendance:personal'
  UNION ALL SELECT '部门出勤汇总', 7, 'business:hr:attendance:dept'
  UNION ALL SELECT '公司出勤汇总', 8, 'business:hr:attendance:company'
  UNION ALL SELECT '请假单管理', 9, 'business:hr:attendance:leave'
  UNION ALL SELECT '请假提交审批', 10, 'business:hr:attendance:leave:submit'
  UNION ALL SELECT '加班单管理', 11, 'business:hr:attendance:overtime'
  UNION ALL SELECT '加班提交审批', 12, 'business:hr:attendance:overtime:submit'
) button_def
INNER JOIN `sys_menu` attendance_menu ON attendance_menu.path = '/business/hr/attendance' AND attendance_menu.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed_menu
  WHERE existed_menu.parent_id = attendance_menu.menu_id
    AND existed_menu.menu_type = 'F'
    AND existed_menu.perms = button_def.perms
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT attendance_role.tenant_id, attendance_role.role_id, button_menu.menu_id
FROM `sys_role_menu` attendance_role
INNER JOIN `sys_menu` attendance_menu ON attendance_menu.menu_id = attendance_role.menu_id
INNER JOIN `sys_menu` button_menu ON button_menu.parent_id = attendance_menu.menu_id AND button_menu.menu_type = 'F'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = attendance_role.tenant_id
      AND existed_role_menu.role_id = attendance_role.role_id
      AND existed_role_menu.menu_id = button_menu.menu_id
WHERE attendance_menu.path = '/business/hr/attendance'
  AND attendance_menu.menu_type = 'C'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT attendance_role.tenant_id, attendance_role.role_id, workbench_attendance.menu_id
FROM `sys_role_menu` attendance_role
INNER JOIN `sys_menu` attendance_menu ON attendance_menu.menu_id = attendance_role.menu_id
INNER JOIN `sys_menu` workbench_attendance ON workbench_attendance.path = '/workbench/attendance'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = attendance_role.tenant_id
      AND existed_role_menu.role_id = attendance_role.role_id
      AND existed_role_menu.menu_id = workbench_attendance.menu_id
WHERE attendance_menu.path = '/business/hr/attendance'
  AND attendance_menu.menu_type = 'C'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role.tenant_id, child_role.role_id, workbench_menu.menu_id
FROM `sys_role_menu` child_role
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role.menu_id
INNER JOIN `sys_menu` workbench_menu ON workbench_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.tenant_id = child_role.tenant_id
      AND existed_role_menu.role_id = child_role.role_id
      AND existed_role_menu.menu_id = workbench_menu.menu_id
WHERE child_menu.path = '/workbench/attendance'
  AND existed_role_menu.menu_id IS NULL;

-- ========== 采购到付款 阶段一（采购申请 + 采购订单）==========
CREATE TABLE IF NOT EXISTS `pur_requisition` (
  `requisition_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `req_no` varchar(64) NOT NULL COMMENT '申请单号',
  `req_title` varchar(200) DEFAULT NULL COMMENT '申请事由',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '申请部门ID',
  `applicant_id` bigint(20) DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人姓名',
  `apply_date` date DEFAULT NULL COMMENT '申请日期',
  `expect_date` date DEFAULT NULL COMMENT '期望到货日期',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT/SUBMITTED/APPROVED/REJECTED/CONVERTED/CANCELLED）',
  `total_amount` decimal(18,4) DEFAULT 0.0000 COMMENT '预估总金额',
  `process_key` varchar(64) DEFAULT NULL COMMENT '审批流程标识',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`requisition_id`),
  UNIQUE KEY `uk_pur_requisition_no` (`tenant_id`,`req_no`),
  KEY `idx_pur_requisition_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请表';

CREATE TABLE IF NOT EXISTS `pur_requisition_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `requisition_id` bigint(20) NOT NULL COMMENT '申请ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `line_no` int(11) NOT NULL DEFAULT 1 COMMENT '行号',
  `item_id` bigint(20) DEFAULT NULL COMMENT '物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '物料编码',
  `item_name` varchar(200) DEFAULT NULL COMMENT '物料名称',
  `spec` varchar(200) DEFAULT NULL COMMENT '规格型号',
  `uom` varchar(32) DEFAULT NULL COMMENT '计量单位',
  `qty` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '申请数量',
  `est_price` decimal(18,4) DEFAULT 0.0000 COMMENT '预估单价',
  `est_amount` decimal(18,4) DEFAULT 0.0000 COMMENT '预估金额',
  `expect_date` date DEFAULT NULL COMMENT '期望到货日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_pur_requisition_line_head` (`requisition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请行表';

CREATE TABLE IF NOT EXISTS `pur_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `supplier_id` bigint(20) DEFAULT NULL COMMENT '供应商ID',
  `supplier_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `supplier_name` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  `requisition_id` bigint(20) DEFAULT NULL COMMENT '来源申请ID',
  `requisition_no` varchar(64) DEFAULT NULL COMMENT '来源申请单号',
  `order_date` date DEFAULT NULL COMMENT '订单日期',
  `expect_date` date DEFAULT NULL COMMENT '期望到货日期',
  `currency_code` varchar(16) DEFAULT 'CNY' COMMENT '币种',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT/PENDING_APPROVAL/APPROVED/PARTIAL_RECEIVED/RECEIVED/CLOSED/CANCELLED）',
  `total_qty` decimal(18,4) DEFAULT 0.0000 COMMENT '订单总数量',
  `total_amount` decimal(18,4) DEFAULT 0.0000 COMMENT '订单总金额（含税）',
  `tax_amount` decimal(18,4) DEFAULT 0.0000 COMMENT '税额',
  `process_key` varchar(64) DEFAULT NULL COMMENT '审批流程标识',
  `idempotency_no` varchar(64) DEFAULT NULL COMMENT '幂等号',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_pur_order_no` (`tenant_id`,`order_no`),
  KEY `idx_pur_order_status` (`tenant_id`,`status`),
  KEY `idx_pur_order_supplier` (`tenant_id`,`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单表';

CREATE TABLE IF NOT EXISTS `pur_order_line` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `line_no` int(11) NOT NULL DEFAULT 1 COMMENT '行号',
  `item_id` bigint(20) DEFAULT NULL COMMENT '物料ID',
  `item_code` varchar(64) DEFAULT NULL COMMENT '物料编码',
  `item_name` varchar(200) DEFAULT NULL COMMENT '物料名称',
  `spec` varchar(200) DEFAULT NULL COMMENT '规格型号',
  `uom` varchar(32) DEFAULT NULL COMMENT '计量单位',
  `qty` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '订购数量',
  `price` decimal(18,4) DEFAULT 0.0000 COMMENT '单价',
  `amount` decimal(18,4) DEFAULT 0.0000 COMMENT '金额',
  `tax_rate` decimal(9,4) DEFAULT 0.0000 COMMENT '税率',
  `received_qty` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '已收数量（阶段二收货累加）',
  `billed_qty` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '已开票数量（阶段三应付累加）',
  `line_status` varchar(32) NOT NULL DEFAULT 'OPEN' COMMENT '行状态（OPEN/CLOSED）',
  `version_no` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本（防超收）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_pur_order_line_head` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单行表';

-- 采购订单免审批金额阈值（低于该金额提交后直接生效）
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '采购订单审批阈值', 'purchase.order.approval.threshold', '10000', 'Y', 'system', NOW(), '采购订单含税总金额达到该值时需要走审批流程'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'purchase.order.approval.threshold');

-- 采购管理菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT menu_def.menu_name, 0, menu_def.order_num, menu_def.path, menu_def.component, 1, 'C', '0', '0',
       menu_def.perms, menu_def.icon, 'system', NOW(), '采购管理菜单'
FROM (
  SELECT '采购申请' AS menu_name, 1 AS order_num, '/purchase/requisition' AS path,
         '/views/purchase/requisition/index' AS component, 'business:pur:req:list' AS perms, 'Tickets' AS icon
  UNION ALL SELECT '采购订单', 2, '/purchase/order', '/views/purchase/order/index', 'business:pur:order:list', 'Document'
) menu_def
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` existed WHERE existed.path = menu_def.path);

-- 采购按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT button_def.menu_name, menu_parent.menu_id, button_def.order_num, button_def.path, NULL, 1, 'F', '0', '0',
       button_def.perms, NULL, 'system', NOW(), '采购按钮权限'
FROM (
  SELECT '采购申请新增' AS menu_name, 1 AS order_num, '/purchase/requisition' AS path, 'business:pur:req:add' AS perms
  UNION ALL SELECT '采购申请修改', 2, '/purchase/requisition', 'business:pur:req:edit'
  UNION ALL SELECT '采购申请删除', 3, '/purchase/requisition', 'business:pur:req:remove'
  UNION ALL SELECT '采购申请提交', 4, '/purchase/requisition', 'business:pur:req:submit'
  UNION ALL SELECT '采购申请转订单', 5, '/purchase/requisition', 'business:pur:req:convert'
  UNION ALL SELECT '采购订单新增', 1, '/purchase/order', 'business:pur:order:add'
  UNION ALL SELECT '采购订单修改', 2, '/purchase/order', 'business:pur:order:edit'
  UNION ALL SELECT '采购订单删除', 3, '/purchase/order', 'business:pur:order:remove'
  UNION ALL SELECT '采购订单提交', 4, '/purchase/order', 'business:pur:order:submit'
  UNION ALL SELECT '采购订单取消', 5, '/purchase/order', 'business:pur:order:cancel'
) button_def
INNER JOIN `sys_menu` menu_parent ON menu_parent.path = button_def.path AND menu_parent.menu_type = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu` existed
  WHERE existed.perms = button_def.perms AND existed.menu_type = 'F'
);

-- 授权管理员角色
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT role_item.tenant_id, role_item.role_id, menu_item.menu_id
FROM `sys_role` role_item
INNER JOIN `sys_menu` menu_item ON menu_item.perms LIKE 'business:pur:%'
LEFT JOIN `sys_role_menu` existed ON existed.role_id = role_item.role_id AND existed.menu_id = menu_item.menu_id
WHERE role_item.role_key = 'admin' AND existed.menu_id IS NULL;
