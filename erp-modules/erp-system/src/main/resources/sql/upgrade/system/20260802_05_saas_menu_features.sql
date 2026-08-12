SET @feature_column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'feature_key'
);
SET @feature_column_sql := IF(
  @feature_column_exists = 0,
  'ALTER TABLE sys_menu ADD COLUMN feature_key varchar(128) DEFAULT NULL COMMENT ''Stable SaaS feature key'' AFTER perms',
  'SELECT 1'
);
PREPARE feature_column_statement FROM @feature_column_sql;
EXECUTE feature_column_statement;
DEALLOCATE PREPARE feature_column_statement;

SET @feature_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND INDEX_NAME = 'idx_sys_menu_feature_key'
);
SET @feature_index_sql := IF(
  @feature_index_exists = 0,
  'ALTER TABLE sys_menu ADD KEY idx_sys_menu_feature_key (feature_key)',
  'SELECT 1'
);
PREPARE feature_index_statement FROM @feature_index_sql;
EXECUTE feature_index_statement;
DEALLOCATE PREPARE feature_index_statement;

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
  `menu_type`, `visible`, `status`, `perms`, `feature_key`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'SaaS管理', platform_menu.menu_id, 99, '/platform/saas', NULL, 1,
  'M', '0', '0', NULL, 'platform.saas.manage', 'Management', 'saas-bootstrap', UTC_TIMESTAMP(3),
  'Platform SaaS administration'
FROM `sys_menu` platform_menu
WHERE platform_menu.path = '/platform'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/platform/saas');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
  `menu_type`, `visible`, `status`, `perms`, `feature_key`, `icon`, `create_by`, `create_time`, `remark`)
SELECT child.menu_name, parent.menu_id, child.order_num, child.path, child.component, 1,
  'C', '0', '0', child.perms, 'platform.saas.manage', child.icon,
  'saas-bootstrap', UTC_TIMESTAMP(3), 'Platform SaaS administration page'
FROM (
  SELECT '租户与开通' menu_name, 1 order_num, '/platform/saas/tenants' path,
    '/views/platform/saas/tenants/index' component, 'saas:tenant:list' perms, 'OfficeBuilding' icon
  UNION ALL SELECT '套餐与授权', 2, '/platform/saas/plans',
    '/views/platform/saas/plans/index', 'saas:plan:list', 'SetUp'
  UNION ALL SELECT '域名管理', 3, '/platform/saas/domains',
    '/views/platform/saas/domains/index', 'saas:domain:list', 'Link'
  UNION ALL SELECT '部署实例', 4, '/platform/saas/deployments',
    '/views/platform/saas/deployments/index', 'saas:deployment:list', 'Connection'
  UNION ALL SELECT '用量汇总', 5, '/platform/saas/usage',
    '/views/platform/saas/usage/index', 'saas:usage:list', 'DataAnalysis'
) child
INNER JOIN `sys_menu` parent ON parent.path = '/platform/saas'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` existing WHERE existing.path = child.path);

UPDATE `sys_menu`
SET `feature_key` = 'platform.saas.manage',
    `update_by` = 'saas-bootstrap',
    `update_time` = UTC_TIMESTAMP(3)
WHERE (`path` = '/platform/saas' OR `path` LIKE '/platform/saas/%')
  AND (`feature_key` IS NULL OR `feature_key` <> 'platform.saas.manage');

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT role_item.tenant_id, role_item.role_id, menu_item.menu_id
FROM `sys_role` role_item
INNER JOIN `sys_menu` menu_item
  ON menu_item.path = '/platform/saas' OR menu_item.path LIKE '/platform/saas/%'
LEFT JOIN `sys_role_menu` existing
  ON existing.tenant_id = role_item.tenant_id
  AND existing.role_id = role_item.role_id
  AND existing.menu_id = menu_item.menu_id
WHERE role_item.tenant_id = '000000'
  AND role_item.role_key = 'admin'
  AND existing.menu_id IS NULL;
