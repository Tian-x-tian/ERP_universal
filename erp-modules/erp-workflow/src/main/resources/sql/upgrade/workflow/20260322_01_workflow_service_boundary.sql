-- 20260322_01: workflow 服务边界收口，迁移菜单/权限并补齐实例边界字段

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_wf_instance` ADD COLUMN `owner_service` varchar(32) DEFAULT NULL COMMENT ''所属业务服务'' AFTER `category`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_wf_instance'
      AND COLUMN_NAME = 'owner_service'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_wf_instance` ADD COLUMN `domain_type` varchar(64) DEFAULT NULL COMMENT ''业务域类型'' AFTER `business_type`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_wf_instance'
      AND COLUMN_NAME = 'domain_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_wf_instance` ADD COLUMN `action_code` varchar(32) DEFAULT NULL COMMENT ''业务动作编码'' AFTER `domain_type`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_wf_instance'
      AND COLUMN_NAME = 'action_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_wf_instance` ADD COLUMN `idempotency_key` varchar(128) DEFAULT NULL COMMENT ''幂等键'' AFTER `action_code`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_wf_instance'
      AND COLUMN_NAME = 'idempotency_key'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_wf_instance` ADD KEY `idx_wf_inst_business_status` (`tenant_id`, `business_type`, `business_no`, `status`)',
              'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_wf_instance'
      AND INDEX_NAME = 'idx_wf_inst_business_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_wf_instance` ADD KEY `idx_wf_inst_idempotency` (`tenant_id`, `idempotency_key`)',
              'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_wf_instance'
      AND INDEX_NAME = 'idx_wf_inst_idempotency'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '流程中心', 0, 6, '/workflow-center', NULL, 1, 'M', '0', '0', NULL, 'Connection', 'system', NOW(), 'workflow 服务菜单目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workflow-center');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '流程定义',
       parent_menu.menu_id,
       1,
       '/workflow-center/definition',
       '/views/platform/workflow/index',
       1,
       'C',
       '0',
       '0',
       'workflow:definition:list',
       NULL,
       'system',
       NOW(),
       'workflow 服务菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center' LIMIT 1) parent_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workflow-center/definition');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '流程实例',
       parent_menu.menu_id,
       2,
       '/workflow-center/instance',
       '/views/platform/workflow/index',
       1,
       'C',
       '0',
       '0',
       'workflow:instance:list',
       NULL,
       'system',
       NOW(),
       'workflow 服务菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center' LIMIT 1) parent_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workflow-center/instance');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '待办事项',
       workbench_menu.menu_id,
       2,
       '/workbench/process-todo',
       '/views/platform/todo-center/index',
       1,
       'C',
       '0',
       '0',
       'workflow:todo:list',
       NULL,
       'system',
       NOW(),
       'workflow 服务菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) workbench_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/process-todo');

UPDATE `sys_menu`
SET `perms` = 'workflow:todo:list',
    `component` = '/views/platform/todo-center/index',
    `path` = '/workbench/process-todo',
    `menu_name` = '待办事项'
WHERE `path` IN ('/platform/todo-center', '/workbench/message/process-todo')
   OR `perms` = 'system:todo:list';

UPDATE `sys_menu`
SET `perms` = 'workflow:definition:list',
    `path` = '/workflow-center/definition',
    `component` = '/views/platform/workflow/index',
    `menu_name` = '流程定义'
WHERE `path` = '/platform/workflow'
   OR `perms` = 'system:workflow:definition:list';

UPDATE `sys_menu`
SET `perms` = 'workflow:instance:list',
    `path` = '/workflow-center/instance',
    `component` = '/views/platform/workflow/index',
    `menu_name` = '流程实例'
WHERE `perms` = 'system:workflow:instance:list';

UPDATE `sys_menu` SET `perms` = 'workflow:todo:claim' WHERE `perms` = 'system:todo:claim';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:finish' WHERE `perms` = 'system:todo:finish';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:handle' WHERE `perms` = 'system:todo:handle';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:form' WHERE `perms` = 'system:todo:form';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:approve' WHERE `perms` = 'system:todo:approve';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:reject' WHERE `perms` = 'system:todo:reject';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:transfer' WHERE `perms` = 'system:todo:transfer';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:return' WHERE `perms` = 'system:todo:return';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:addSign' WHERE `perms` = 'system:todo:addSign';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:removeSign' WHERE `perms` = 'system:todo:removeSign';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:delegate' WHERE `perms` = 'system:todo:delegate';
UPDATE `sys_menu` SET `perms` = 'workflow:todo:remind' WHERE `perms` = 'system:todo:remind';

UPDATE `sys_menu` SET `perms` = 'workflow:definition:query' WHERE `perms` = 'system:workflow:definition:query';
UPDATE `sys_menu` SET `perms` = 'workflow:definition:add' WHERE `perms` = 'system:workflow:definition:add';
UPDATE `sys_menu` SET `perms` = 'workflow:definition:edit' WHERE `perms` = 'system:workflow:definition:edit';
UPDATE `sys_menu` SET `perms` = 'workflow:definition:remove' WHERE `perms` = 'system:workflow:definition:remove';
UPDATE `sys_menu` SET `perms` = 'workflow:definition:publish' WHERE `perms` = 'system:workflow:definition:publish';
UPDATE `sys_menu` SET `perms` = 'workflow:definition:design' WHERE `perms` = 'system:workflow:definition:design';
UPDATE `sys_menu` SET `perms` = 'workflow:definition:template' WHERE `perms` = 'system:workflow:definition:template';
UPDATE `sys_menu` SET `perms` = 'workflow:instance:query' WHERE `perms` = 'system:workflow:instance:query';
UPDATE `sys_menu` SET `perms` = 'workflow:instance:start' WHERE `perms` = 'system:workflow:instance:start';
UPDATE `sys_menu` SET `perms` = 'workflow:instance:withdraw' WHERE `perms` = 'system:workflow:instance:withdraw';
UPDATE `sys_menu` SET `perms` = 'workflow:instance:report' WHERE `perms` = 'system:workflow:instance:report';
UPDATE `sys_menu` SET `perms` = 'workflow:instance:sla' WHERE `perms` = 'system:workflow:instance:sla';
UPDATE `sys_menu` SET `perms` = 'workflow:list' WHERE `perms` = 'system:workflow:list';
UPDATE `sys_menu` SET `perms` = 'workflow:query' WHERE `perms` = 'system:workflow:query';
UPDATE `sys_menu` SET `perms` = 'workflow:add' WHERE `perms` = 'system:workflow:add';
UPDATE `sys_menu` SET `perms` = 'workflow:edit' WHERE `perms` = 'system:workflow:edit';
UPDATE `sys_menu` SET `perms` = 'workflow:remove' WHERE `perms` = 'system:workflow:remove';
UPDATE `sys_menu` SET `perms` = 'workflow:publish' WHERE `perms` = 'system:workflow:publish';
UPDATE `sys_menu` SET `perms` = 'workflow:start' WHERE `perms` = 'system:workflow:start';
UPDATE `sys_menu` SET `perms` = 'workflow:handle' WHERE `perms` = 'system:workflow:handle';
UPDATE `sys_menu` SET `perms` = 'workflow:design' WHERE `perms` = 'system:workflow:design';
UPDATE `sys_menu` SET `perms` = 'workflow:withdraw' WHERE `perms` = 'system:workflow:withdraw';
UPDATE `sys_menu` SET `perms` = 'workflow:form' WHERE `perms` = 'system:workflow:form';
UPDATE `sys_menu` SET `perms` = 'workflow:return' WHERE `perms` = 'system:workflow:return';
UPDATE `sys_menu` SET `perms` = 'workflow:addSign' WHERE `perms` = 'system:workflow:addSign';
UPDATE `sys_menu` SET `perms` = 'workflow:removeSign' WHERE `perms` = 'system:workflow:removeSign';
UPDATE `sys_menu` SET `perms` = 'workflow:delegate' WHERE `perms` = 'system:workflow:delegate';
UPDATE `sys_menu` SET `perms` = 'workflow:remind' WHERE `perms` = 'system:workflow:remind';

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', 1, menu.menu_id
FROM `sys_menu` menu
LEFT JOIN `sys_role_menu` role_menu
       ON role_menu.tenant_id = '000000'
      AND role_menu.role_id = 1
      AND role_menu.menu_id = menu.menu_id
WHERE (
        menu.path IN ('/workflow-center', '/workflow-center/definition', '/workflow-center/instance', '/workbench/process-todo')
        OR menu.perms LIKE 'workflow:%'
      )
  AND role_menu.menu_id IS NULL;
