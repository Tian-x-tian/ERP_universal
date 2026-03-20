-- 20260319_02: 审批任务迁入工作台消息，流程中心拆分为流程定义/流程实例

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工作台', 0, 5, '/workbench', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '工作台目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '消息',
       parent_menu.menu_id,
       1,
       '/workbench/message',
       NULL,
       1,
       'M',
       '0',
       '0',
       NULL,
       NULL,
       'system',
       NOW(),
       '工作台消息目录'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench' LIMIT 1) parent_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/message');

UPDATE `sys_menu`
SET `menu_name` = '流程待办',
    `parent_id` = (SELECT workbench_message.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/message' LIMIT 1) workbench_message),
    `order_num` = 2,
    `path` = '/workbench/message/process-todo',
    `component` = '/views/platform/todo-center/index',
    `is_frame` = 1,
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'system:todo:list',
    `remark` = '流程待办菜单'
WHERE `path` = '/platform/todo-center';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '流程待办',
       parent_menu.menu_id,
       2,
       '/workbench/message/process-todo',
       '/views/platform/todo-center/index',
       1,
       'C',
       '0',
       '0',
       'system:todo:list',
       NULL,
       'system',
       NOW(),
       '流程待办菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/message' LIMIT 1) parent_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/message/process-todo');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '系统消息',
       parent_menu.menu_id,
       1,
       '/workbench/message/system-notice',
       '/views/system/notice/index',
       1,
       'C',
       '0',
       '0',
       'system:message:list',
       NULL,
       'system',
       NOW(),
       '系统消息菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/message' LIMIT 1) parent_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workbench/message/system-notice');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '流程中心', 0, 6, '/workflow-center', NULL, 1, 'M', '0', '0', NULL, NULL, 'system', NOW(), '流程中心目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workflow-center');

UPDATE `sys_menu`
SET `menu_name` = '流程定义',
    `parent_id` = (SELECT workflow_center.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center' LIMIT 1) workflow_center),
    `order_num` = 1,
    `path` = '/workflow-center/definition',
    `component` = '/views/platform/workflow/index',
    `is_frame` = 1,
    `menu_type` = 'C',
    `visible` = '0',
    `status` = '0',
    `perms` = 'system:workflow:definition:list',
    `remark` = '流程定义菜单'
WHERE `path` = '/platform/workflow';

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
       'system:workflow:definition:list',
       NULL,
       'system',
       NOW(),
       '流程定义菜单'
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
       'system:workflow:instance:list',
       NULL,
       'system',
       NOW(),
       '流程实例菜单'
FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center' LIMIT 1) parent_menu
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = '/workflow-center/instance');

UPDATE `sys_menu`
SET `parent_id` = (SELECT process_todo.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/message/process-todo' LIMIT 1) process_todo),
    `menu_name` = CASE `perms`
                    WHEN 'system:todo:claim' THEN '待办签收'
                    WHEN 'system:todo:finish' THEN '待办办结'
                    WHEN 'system:todo:handle' THEN '待办处理'
                    WHEN 'system:todo:form' THEN '审批表单'
                    WHEN 'system:todo:approve' THEN '审批同意'
                    WHEN 'system:todo:reject' THEN '审批驳回'
                    WHEN 'system:todo:transfer' THEN '任务转交'
                    WHEN 'system:todo:return' THEN '节点退回'
                    WHEN 'system:todo:addSign' THEN '任务加签'
                    WHEN 'system:todo:removeSign' THEN '任务减签'
                    WHEN 'system:todo:delegate' THEN '任务委派'
                    WHEN 'system:todo:remind' THEN '任务催办'
                    ELSE `menu_name`
                  END,
    `order_num` = CASE `perms`
                    WHEN 'system:todo:claim' THEN 1
                    WHEN 'system:todo:finish' THEN 2
                    WHEN 'system:todo:handle' THEN 3
                    WHEN 'system:todo:form' THEN 4
                    WHEN 'system:todo:approve' THEN 5
                    WHEN 'system:todo:reject' THEN 6
                    WHEN 'system:todo:transfer' THEN 7
                    WHEN 'system:todo:return' THEN 8
                    WHEN 'system:todo:addSign' THEN 9
                    WHEN 'system:todo:removeSign' THEN 10
                    WHEN 'system:todo:delegate' THEN 11
                    WHEN 'system:todo:remind' THEN 12
                    ELSE `order_num`
                  END,
    `remark` = '流程待办按钮权限'
WHERE `perms` IN (
  'system:todo:claim',
  'system:todo:finish',
  'system:todo:handle',
  'system:todo:form',
  'system:todo:approve',
  'system:todo:reject',
  'system:todo:transfer',
  'system:todo:return',
  'system:todo:addSign',
  'system:todo:removeSign',
  'system:todo:delegate',
  'system:todo:remind'
);

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程定义查询',
    `order_num` = 1,
    `perms` = 'system:workflow:definition:query',
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:query';

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程定义新增',
    `order_num` = 2,
    `perms` = 'system:workflow:definition:add',
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:add';

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程定义修改',
    `order_num` = 3,
    `perms` = 'system:workflow:definition:edit',
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:edit';

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程定义删除',
    `order_num` = 4,
    `perms` = 'system:workflow:definition:remove',
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:remove';

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程定义发布',
    `order_num` = 5,
    `perms` = 'system:workflow:definition:publish',
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:publish';

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程设计',
    `order_num` = 6,
    `perms` = 'system:workflow:definition:design',
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:design';

UPDATE `sys_menu`
SET `parent_id` = (SELECT instance_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/instance' LIMIT 1) instance_menu),
    `menu_name` = '流程发起',
    `order_num` = 2,
    `perms` = 'system:workflow:instance:start',
    `remark` = '流程实例按钮权限'
WHERE `perms` = 'system:workflow:start';

UPDATE `sys_menu`
SET `parent_id` = (SELECT instance_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/instance' LIMIT 1) instance_menu),
    `menu_name` = '流程撤回',
    `order_num` = 3,
    `perms` = 'system:workflow:instance:withdraw',
    `remark` = '流程实例按钮权限'
WHERE `perms` = 'system:workflow:withdraw';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT perm_def.menu_name,
       parent_menu.menu_id,
       perm_def.order_num,
       '',
       NULL,
       1,
       'F',
       '1',
       '0',
       perm_def.perms,
       NULL,
       'system',
       NOW(),
       perm_def.remark
FROM (
  SELECT '/workbench/message/system-notice' AS parent_path, '消息已读' AS menu_name, 1 AS order_num, 'system:message:read' AS perms, '系统消息按钮权限' AS remark
  UNION ALL SELECT '/workflow-center/definition', '流程模板', 7, 'system:workflow:definition:template', '流程定义按钮权限'
  UNION ALL SELECT '/workflow-center/instance', '流程实例查询', 1, 'system:workflow:instance:query', '流程实例按钮权限'
  UNION ALL SELECT '/workflow-center/instance', '流程看板', 4, 'system:workflow:instance:report', '流程实例按钮权限'
  UNION ALL SELECT '/workflow-center/instance', 'SLA扫描', 5, 'system:workflow:instance:sla', '流程实例按钮权限'
) perm_def
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = perm_def.parent_path
LEFT JOIN `sys_menu` existed_menu ON existed_menu.perms = perm_def.perms
WHERE existed_menu.menu_id IS NULL;

UPDATE `sys_menu`
SET `parent_id` = (SELECT system_notice.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/message/system-notice' LIMIT 1) system_notice),
    `menu_name` = '消息已读',
    `order_num` = 1,
    `remark` = '系统消息按钮权限'
WHERE `perms` = 'system:message:read';

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = '流程模板',
    `order_num` = 7,
    `remark` = '流程定义按钮权限'
WHERE `perms` = 'system:workflow:definition:template';

UPDATE `sys_menu`
SET `parent_id` = (SELECT instance_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/instance' LIMIT 1) instance_menu),
    `menu_name` = CASE `perms`
                    WHEN 'system:workflow:instance:query' THEN '流程实例查询'
                    WHEN 'system:workflow:instance:report' THEN '流程看板'
                    WHEN 'system:workflow:instance:sla' THEN 'SLA扫描'
                    ELSE `menu_name`
                  END,
    `order_num` = CASE `perms`
                    WHEN 'system:workflow:instance:query' THEN 1
                    WHEN 'system:workflow:instance:report' THEN 4
                    WHEN 'system:workflow:instance:sla' THEN 5
                    ELSE `order_num`
                  END,
    `remark` = '流程实例按钮权限'
WHERE `perms` IN (
  'system:workflow:instance:query',
  'system:workflow:instance:report',
  'system:workflow:instance:sla'
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT source_role_menu.tenant_id,
       source_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN (
  SELECT 'system:todo:list' AS source_perm, 'system:message:list' AS target_perm
  UNION ALL SELECT 'system:todo:handle', 'system:message:read'
  UNION ALL SELECT 'system:workflow:list', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:handle'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:claim'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:finish'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:approve'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:reject'
  UNION ALL SELECT 'system:workflow:handle', 'system:todo:transfer'
  UNION ALL SELECT 'system:workflow:form', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:form', 'system:todo:form'
  UNION ALL SELECT 'system:workflow:return', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:return', 'system:todo:return'
  UNION ALL SELECT 'system:workflow:addSign', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:addSign', 'system:todo:addSign'
  UNION ALL SELECT 'system:workflow:removeSign', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:removeSign', 'system:todo:removeSign'
  UNION ALL SELECT 'system:workflow:delegate', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:delegate', 'system:todo:delegate'
  UNION ALL SELECT 'system:workflow:remind', 'system:todo:list'
  UNION ALL SELECT 'system:workflow:remind', 'system:todo:remind'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:list'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:claim'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:finish'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:form'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:approve'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:reject'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:transfer'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:return'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:addSign'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:removeSign'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:delegate'
  UNION ALL SELECT 'system:todo:handle', 'system:todo:remind'
  UNION ALL SELECT 'system:workflow:definition:list', 'system:workflow:instance:list'
  UNION ALL SELECT 'system:workflow:definition:list', 'system:workflow:definition:query'
  UNION ALL SELECT 'system:workflow:definition:list', 'system:workflow:definition:template'
  UNION ALL SELECT 'system:workflow:definition:list', 'system:workflow:instance:query'
  UNION ALL SELECT 'system:workflow:definition:list', 'system:workflow:instance:report'
  UNION ALL SELECT 'system:workflow:definition:query', 'system:workflow:instance:query'
  UNION ALL SELECT 'system:workflow:definition:design', 'system:workflow:definition:template'
  UNION ALL SELECT 'system:workflow:remind', 'system:workflow:instance:sla'
  UNION ALL SELECT 'system:workflow:list', 'system:workflow:definition:list'
  UNION ALL SELECT 'system:workflow:list', 'system:workflow:instance:list'
  UNION ALL SELECT 'system:workflow:list', 'system:workflow:definition:query'
  UNION ALL SELECT 'system:workflow:list', 'system:workflow:definition:template'
  UNION ALL SELECT 'system:workflow:list', 'system:workflow:instance:query'
  UNION ALL SELECT 'system:workflow:list', 'system:workflow:instance:report'
  UNION ALL SELECT 'system:workflow:query', 'system:workflow:definition:query'
  UNION ALL SELECT 'system:workflow:query', 'system:workflow:instance:query'
  UNION ALL SELECT 'system:workflow:add', 'system:workflow:definition:add'
  UNION ALL SELECT 'system:workflow:edit', 'system:workflow:definition:edit'
  UNION ALL SELECT 'system:workflow:remove', 'system:workflow:definition:remove'
  UNION ALL SELECT 'system:workflow:publish', 'system:workflow:definition:publish'
  UNION ALL SELECT 'system:workflow:design', 'system:workflow:definition:design'
  UNION ALL SELECT 'system:workflow:design', 'system:workflow:definition:template'
  UNION ALL SELECT 'system:workflow:start', 'system:workflow:instance:start'
  UNION ALL SELECT 'system:workflow:withdraw', 'system:workflow:instance:withdraw'
) perm_mapping ON perm_mapping.source_perm = source_menu.perms
INNER JOIN `sys_menu` target_menu ON target_menu.perms = perm_mapping.target_perm
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       parent_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = parent_menu.menu_id
WHERE child_menu.path LIKE '/workbench/message/%'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       parent_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/workbench/message'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = parent_menu.menu_id
WHERE child_menu.path LIKE '/workbench/message/%'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       parent_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/workflow-center'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = parent_menu.menu_id
WHERE child_menu.path LIKE '/workflow-center/%'
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT '000000', 1, target_menu.menu_id
FROM `sys_menu` target_menu
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = 1
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE (
        target_menu.path IN (
          '/workbench',
          '/workbench/message',
          '/workbench/message/system-notice',
          '/workbench/message/process-todo',
          '/workflow-center',
          '/workflow-center/definition',
          '/workflow-center/instance'
        )
        OR target_menu.perms IN (
          'system:message:read',
          'system:todo:claim',
          'system:todo:finish',
          'system:todo:handle',
          'system:todo:form',
          'system:todo:approve',
          'system:todo:reject',
          'system:todo:transfer',
          'system:todo:return',
          'system:todo:addSign',
          'system:todo:removeSign',
          'system:todo:delegate',
          'system:todo:remind',
          'system:workflow:definition:query',
          'system:workflow:definition:add',
          'system:workflow:definition:edit',
          'system:workflow:definition:remove',
          'system:workflow:definition:publish',
          'system:workflow:definition:design',
          'system:workflow:definition:template',
          'system:workflow:instance:query',
          'system:workflow:instance:start',
          'system:workflow:instance:withdraw',
          'system:workflow:instance:report',
          'system:workflow:instance:sla'
        )
      )
  AND existed_role_menu.menu_id IS NULL;

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path IN ('/platform/todo-center', '/platform/workflow')
   OR source_menu.perms IN (
        'system:workflow:handle',
        'system:workflow:form',
        'system:workflow:return',
        'system:workflow:addSign',
        'system:workflow:removeSign',
        'system:workflow:delegate',
        'system:workflow:remind',
        'system:workflow:query',
        'system:workflow:add',
        'system:workflow:edit',
        'system:workflow:remove',
        'system:workflow:publish',
        'system:workflow:design',
        'system:workflow:start',
        'system:workflow:withdraw'
      );

DELETE FROM `sys_menu`
WHERE `path` IN ('/platform/todo-center', '/platform/workflow')
   OR `perms` IN (
        'system:workflow:handle',
        'system:workflow:form',
        'system:workflow:return',
        'system:workflow:addSign',
        'system:workflow:removeSign',
        'system:workflow:delegate',
        'system:workflow:remind',
        'system:workflow:query',
        'system:workflow:add',
        'system:workflow:edit',
        'system:workflow:remove',
        'system:workflow:publish',
        'system:workflow:design',
        'system:workflow:start',
        'system:workflow:withdraw'
      );
