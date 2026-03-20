-- 2026-03-21 按模块补齐基础演示数据（仅在目标模块无数据时插入）
-- 目标：针对“无数据模块”提供可直接验证的首批数据，已有数据模块不覆盖。

-- ============================================================
-- 0) 公共基础字典补齐（主数据字典页）
-- ============================================================

INSERT INTO `mdm_settle_method` (`tenant_id`, `settle_code`, `settle_name`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'CASH', '现金结算', 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_settle_method`
  WHERE `tenant_id` = '000000' AND `settle_code` = 'CASH'
);

INSERT INTO `mdm_settle_method` (`tenant_id`, `settle_code`, `settle_name`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'BANK_TRANSFER', '银行转账', 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_settle_method`
  WHERE `tenant_id` = '000000' AND `settle_code` = 'BANK_TRANSFER'
);

INSERT INTO `mdm_tax_rate` (`tenant_id`, `tax_code`, `tax_name`, `tax_rate`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'VAT13', '增值税13%', 0.1300, 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_tax_rate`
  WHERE `tenant_id` = '000000' AND `tax_code` = 'VAT13'
);

INSERT INTO `mdm_tax_rate` (`tenant_id`, `tax_code`, `tax_name`, `tax_rate`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'VAT9', '增值税9%', 0.0900, 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_tax_rate`
  WHERE `tenant_id` = '000000' AND `tax_code` = 'VAT9'
);

INSERT INTO `mdm_currency` (`tenant_id`, `currency_code`, `currency_name`, `symbol`, `precision_scale`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'CNY', '人民币', '¥', 2, 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_currency`
  WHERE `tenant_id` = '000000' AND `currency_code` = 'CNY'
);

INSERT INTO `mdm_uom` (`tenant_id`, `uom_code`, `uom_name`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'PCS', '件', 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_uom`
  WHERE `tenant_id` = '000000' AND `uom_code` = 'PCS'
);

INSERT INTO `mdm_uom` (`tenant_id`, `uom_code`, `uom_name`, `status`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'KG', '千克', 'ACTIVE', 'system', NOW(), '模块初始化数据'
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_uom`
  WHERE `tenant_id` = '000000' AND `uom_code` = 'KG'
);

-- ============================================================
-- 1) 组织/成本中心/项目（组织成本项目模块）
-- ============================================================

INSERT INTO `mdm_org` (`tenant_id`, `org_code`, `org_name`, `org_type`, `parent_id`, `ancestors`, `status`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '000000', 'ORG-HQ', '总部组织', 'HEADQUARTER', 0, '0', 'ACTIVE', 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_org`
  WHERE `tenant_id` = '000000' AND `org_code` = 'ORG-HQ'
);

INSERT INTO `mdm_cost_center` (`tenant_id`, `cc_code`, `cc_name`, `org_id`, `parent_id`, `status`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '000000', 'CC-ADMIN', '行政成本中心', org_ref.org_id, 0, 'ACTIVE', 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM (
  SELECT `org_id` FROM `mdm_org` WHERE `tenant_id` = '000000' AND `org_code` = 'ORG-HQ' LIMIT 1
) org_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_cost_center`
  WHERE `tenant_id` = '000000' AND `cc_code` = 'CC-ADMIN'
);

-- ============================================================
-- 2) 客户/供应商/物料/仓库/员工（主数据各模块）
-- ============================================================

INSERT INTO `mdm_customer` (
  `tenant_id`, `customer_code`, `customer_name`, `short_name`, `customer_type`, `tax_no`, `invoice_title`,
  `default_currency`, `default_tax_rate`, `credit_limit`, `credit_days`,
  `contact_name`, `contact_phone`, `contact_email`,
  `province`, `city`, `district`, `detail_address`,
  `settle_method_id`, `org_id`, `status`, `effective_time`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', 'CUS-001', '上海示例客户有限公司', '示例客户', 'ENTERPRISE', '91310000DEMO0001X', '上海示例客户有限公司',
  'CNY', 0.1300, 500000.00, 30,
  '张三', '13800000001', 'customer@example.com',
  '上海市', '上海市', '浦东新区', '张江高科技园区1号',
  settle_ref.settle_method_id, org_ref.org_id, 'ACTIVE', NOW(), 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM
  (SELECT `settle_method_id` FROM `mdm_settle_method` WHERE `tenant_id` = '000000' AND `settle_code` = 'BANK_TRANSFER' LIMIT 1) settle_ref,
  (SELECT `org_id` FROM `mdm_org` WHERE `tenant_id` = '000000' AND `org_code` = 'ORG-HQ' LIMIT 1) org_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_customer`
  WHERE `tenant_id` = '000000' AND `customer_code` = 'CUS-001'
);

INSERT INTO `mdm_supplier` (
  `tenant_id`, `supplier_code`, `supplier_name`, `short_name`, `supply_category`, `tax_no`,
  `default_currency`, `default_tax_rate`, `lead_time_days`, `quality_level`, `bank_account_info`,
  `contact_name`, `contact_phone`, `contact_email`, `address`,
  `status`, `effective_time`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', 'SUP-001', '上海示例供应链有限公司', '示例供应商', 'RAW_MATERIAL', '91310000DEMO0002Y',
  'CNY', 0.1300, 7, 'A', '中国银行上海分行 6222 **** **** 8888',
  '李四', '13800000002', 'supplier@example.com', '上海市闵行区示例路88号',
  'ACTIVE', NOW(), 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_supplier`
  WHERE `tenant_id` = '000000' AND `supplier_code` = 'SUP-001'
);

INSERT INTO `mdm_warehouse` (
  `tenant_id`, `wh_code`, `wh_name`, `wh_type`, `org_id`, `address`, `manager_emp_id`, `allow_negative_stock`,
  `status`, `effective_time`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', 'WH-SH-01', '上海一号仓', 'NORMAL', org_ref.org_id, '上海市青浦区仓储大道18号', NULL, 'N',
  'ACTIVE', NOW(), 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM (
  SELECT `org_id` FROM `mdm_org` WHERE `tenant_id` = '000000' AND `org_code` = 'ORG-HQ' LIMIT 1
) org_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_warehouse`
  WHERE `tenant_id` = '000000' AND `wh_code` = 'WH-SH-01'
);

INSERT INTO `mdm_item` (
  `tenant_id`, `item_code`, `item_name`, `spec_model`, `brand`, `item_type`, `category_id`, `unit_id`, `unit_convert`,
  `tax_rate_id`, `barcode`, `shelf_life_days`, `default_expiry_warn_days`, `batch_control`, `serial_control`, `costing_method`,
  `status`, `effective_time`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', 'ITEM-001', '标准螺丝 M6', 'M6*20', 'ERP-DEMO', 'MATERIAL', NULL, NULL, NULL,
  tax_ref.tax_rate_id, '6900000000001', 3650, 180, 'Y', 'N', 'MOVING_AVERAGE',
  'ACTIVE', NOW(), 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM (
  SELECT `tax_rate_id` FROM `mdm_tax_rate` WHERE `tenant_id` = '000000' AND `tax_code` = 'VAT13' LIMIT 1
) tax_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_item`
  WHERE `tenant_id` = '000000' AND `item_code` = 'ITEM-001'
);

INSERT INTO `mdm_employee` (
  `tenant_id`, `emp_code`, `emp_name`, `mobile`, `email`, `org_id`, `dept_id`, `position`, `user_id`, `cost_center_id`,
  `status`, `effective_time`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', 'EMP-001', '王小明', '13800000003', 'employee@example.com', org_ref.org_id, 1, '仓储主管', 1, cc_ref.cc_id,
  'ACTIVE', NOW(), 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM
  (SELECT `org_id` FROM `mdm_org` WHERE `tenant_id` = '000000' AND `org_code` = 'ORG-HQ' LIMIT 1) org_ref,
  (SELECT `cc_id` FROM `mdm_cost_center` WHERE `tenant_id` = '000000' AND `cc_code` = 'CC-ADMIN' LIMIT 1) cc_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_employee`
  WHERE `tenant_id` = '000000' AND `emp_code` = 'EMP-001'
);

INSERT INTO `mdm_project` (
  `tenant_id`, `project_code`, `project_name`, `manager_emp_id`, `customer_id`, `org_id`, `start_date`, `end_date`,
  `status`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', 'PRJ-001', 'ERP示例实施项目', emp_ref.employee_id, cus_ref.customer_id, org_ref.org_id,
  CURDATE(), DATE_ADD(CURDATE(), INTERVAL 180 DAY),
  'ACTIVE', 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM
  (SELECT `employee_id` FROM `mdm_employee` WHERE `tenant_id` = '000000' AND `emp_code` = 'EMP-001' LIMIT 1) emp_ref,
  (SELECT `customer_id` FROM `mdm_customer` WHERE `tenant_id` = '000000' AND `customer_code` = 'CUS-001' LIMIT 1) cus_ref,
  (SELECT `org_id` FROM `mdm_org` WHERE `tenant_id` = '000000' AND `org_code` = 'ORG-HQ' LIMIT 1) org_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_project`
  WHERE `tenant_id` = '000000' AND `project_code` = 'PRJ-001'
);

-- ============================================================
-- 3) 仓储维度（库区/库位模块）
-- ============================================================

INSERT INTO `mdm_warehouse_area` (
  `tenant_id`, `warehouse_id`, `area_code`, `area_name`, `status`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', wh_ref.warehouse_id, 'A01', '常温区A01', 'ACTIVE', 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM (
  SELECT `warehouse_id` FROM `mdm_warehouse` WHERE `tenant_id` = '000000' AND `wh_code` = 'WH-SH-01' LIMIT 1
) wh_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_warehouse_area`
  WHERE `tenant_id` = '000000' AND `warehouse_id` = wh_ref.warehouse_id AND `area_code` = 'A01'
);

INSERT INTO `mdm_warehouse_location` (
  `tenant_id`, `warehouse_id`, `area_id`, `location_code`, `location_name`, `volume_capacity`, `weight_capacity`, `temperature_zone`, `hazardous_flag`,
  `status`, `version_no`, `del_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  '000000', area_ref.warehouse_id, area_ref.area_id, 'A01-001', 'A01-001货位', 12.5000, 1000.0000, 'NORMAL', 'N',
  'ACTIVE', 1, '0', '模块初始化数据', 'system', NOW(), 'system', NOW()
FROM (
  SELECT `area_id`, `warehouse_id`
  FROM `mdm_warehouse_area`
  WHERE `tenant_id` = '000000' AND `area_code` = 'A01'
  LIMIT 1
) area_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_warehouse_location`
  WHERE `tenant_id` = '000000' AND `warehouse_id` = area_ref.warehouse_id AND `location_code` = 'A01-001'
);

-- ============================================================
-- 4) 变更追踪（trace 模块可见首条记录）
-- ============================================================

INSERT INTO `mdm_change_log` (
  `tenant_id`, `domain_type`, `biz_id`, `change_type`, `before_json`, `after_json`, `operator`, `trace_id`, `source`, `create_time`
)
SELECT
  '000000', 'WAREHOUSE', wh_ref.warehouse_id, 'CREATE', NULL,
  CONCAT('{"whCode":"WH-SH-01","whName":"上海一号仓","status":"ACTIVE"}'),
  'system', CONCAT('TRACE-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s')), 'SYSTEM_INIT', NOW()
FROM (
  SELECT `warehouse_id` FROM `mdm_warehouse` WHERE `tenant_id` = '000000' AND `wh_code` = 'WH-SH-01' LIMIT 1
) wh_ref
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_change_log`
  WHERE `tenant_id` = '000000' AND `domain_type` = 'WAREHOUSE' AND `biz_id` = wh_ref.warehouse_id
);
