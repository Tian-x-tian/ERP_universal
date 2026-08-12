-- 20260725_02: 采购到付款闭环 阶段一（采购申请 + 采购订单）

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
