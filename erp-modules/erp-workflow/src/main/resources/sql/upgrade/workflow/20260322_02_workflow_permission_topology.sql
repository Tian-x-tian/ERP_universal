-- 20260322_02: workflow 菜单权限拓扑收口，移除 legacy 通用权限并补齐角色映射

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
       'workflow 服务按钮权限'
FROM (
  SELECT '/workbench/process-todo' AS parent_path, '待办签收' AS menu_name, 1 AS order_num, 'workflow:todo:claim' AS perms
  UNION ALL SELECT '/workbench/process-todo', '待办办结', 2, 'workflow:todo:finish'
  UNION ALL SELECT '/workbench/process-todo', '待办处理', 3, 'workflow:todo:handle'
  UNION ALL SELECT '/workbench/process-todo', '审批表单', 4, 'workflow:todo:form'
  UNION ALL SELECT '/workbench/process-todo', '审批同意', 5, 'workflow:todo:approve'
  UNION ALL SELECT '/workbench/process-todo', '审批驳回', 6, 'workflow:todo:reject'
  UNION ALL SELECT '/workbench/process-todo', '任务转交', 7, 'workflow:todo:transfer'
  UNION ALL SELECT '/workbench/process-todo', '节点退回', 8, 'workflow:todo:return'
  UNION ALL SELECT '/workbench/process-todo', '任务加签', 9, 'workflow:todo:addSign'
  UNION ALL SELECT '/workbench/process-todo', '任务减签', 10, 'workflow:todo:removeSign'
  UNION ALL SELECT '/workbench/process-todo', '任务委派', 11, 'workflow:todo:delegate'
  UNION ALL SELECT '/workbench/process-todo', '任务催办', 12, 'workflow:todo:remind'
  UNION ALL SELECT '/workflow-center/definition', '流程定义查询', 1, 'workflow:definition:query'
  UNION ALL SELECT '/workflow-center/definition', '流程定义新增', 2, 'workflow:definition:add'
  UNION ALL SELECT '/workflow-center/definition', '流程定义修改', 3, 'workflow:definition:edit'
  UNION ALL SELECT '/workflow-center/definition', '流程定义删除', 4, 'workflow:definition:remove'
  UNION ALL SELECT '/workflow-center/definition', '流程定义发布', 5, 'workflow:definition:publish'
  UNION ALL SELECT '/workflow-center/definition', '流程设计', 6, 'workflow:definition:design'
  UNION ALL SELECT '/workflow-center/definition', '流程模板', 7, 'workflow:definition:template'
  UNION ALL SELECT '/workflow-center/instance', '流程实例查询', 1, 'workflow:instance:query'
  UNION ALL SELECT '/workflow-center/instance', '流程发起', 2, 'workflow:instance:start'
  UNION ALL SELECT '/workflow-center/instance', '流程撤回', 3, 'workflow:instance:withdraw'
  UNION ALL SELECT '/workflow-center/instance', '流程看板', 4, 'workflow:instance:report'
  UNION ALL SELECT '/workflow-center/instance', 'SLA扫描', 5, 'workflow:instance:sla'
) button_perm
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = button_perm.parent_path
LEFT JOIN `sys_menu` existed_menu ON existed_menu.parent_id = parent_menu.menu_id AND existed_menu.perms = button_perm.perms
WHERE existed_menu.menu_id IS NULL;

UPDATE `sys_menu`
SET `parent_id` = (SELECT todo_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workbench/process-todo' LIMIT 1) todo_menu),
    `menu_name` = CASE `perms`
                    WHEN 'workflow:todo:claim' THEN '待办签收'
                    WHEN 'workflow:todo:finish' THEN '待办办结'
                    WHEN 'workflow:todo:handle' THEN '待办处理'
                    WHEN 'workflow:todo:form' THEN '审批表单'
                    WHEN 'workflow:todo:approve' THEN '审批同意'
                    WHEN 'workflow:todo:reject' THEN '审批驳回'
                    WHEN 'workflow:todo:transfer' THEN '任务转交'
                    WHEN 'workflow:todo:return' THEN '节点退回'
                    WHEN 'workflow:todo:addSign' THEN '任务加签'
                    WHEN 'workflow:todo:removeSign' THEN '任务减签'
                    WHEN 'workflow:todo:delegate' THEN '任务委派'
                    WHEN 'workflow:todo:remind' THEN '任务催办'
                    ELSE `menu_name`
                  END,
    `order_num` = CASE `perms`
                    WHEN 'workflow:todo:claim' THEN 1
                    WHEN 'workflow:todo:finish' THEN 2
                    WHEN 'workflow:todo:handle' THEN 3
                    WHEN 'workflow:todo:form' THEN 4
                    WHEN 'workflow:todo:approve' THEN 5
                    WHEN 'workflow:todo:reject' THEN 6
                    WHEN 'workflow:todo:transfer' THEN 7
                    WHEN 'workflow:todo:return' THEN 8
                    WHEN 'workflow:todo:addSign' THEN 9
                    WHEN 'workflow:todo:removeSign' THEN 10
                    WHEN 'workflow:todo:delegate' THEN 11
                    WHEN 'workflow:todo:remind' THEN 12
                    ELSE `order_num`
                  END,
    `remark` = 'workflow 服务待办按钮权限'
WHERE `perms` IN (
  'workflow:todo:claim',
  'workflow:todo:finish',
  'workflow:todo:handle',
  'workflow:todo:form',
  'workflow:todo:approve',
  'workflow:todo:reject',
  'workflow:todo:transfer',
  'workflow:todo:return',
  'workflow:todo:addSign',
  'workflow:todo:removeSign',
  'workflow:todo:delegate',
  'workflow:todo:remind'
);

UPDATE `sys_menu`
SET `parent_id` = (SELECT definition_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/definition' LIMIT 1) definition_menu),
    `menu_name` = CASE `perms`
                    WHEN 'workflow:definition:query' THEN '流程定义查询'
                    WHEN 'workflow:definition:add' THEN '流程定义新增'
                    WHEN 'workflow:definition:edit' THEN '流程定义修改'
                    WHEN 'workflow:definition:remove' THEN '流程定义删除'
                    WHEN 'workflow:definition:publish' THEN '流程定义发布'
                    WHEN 'workflow:definition:design' THEN '流程设计'
                    WHEN 'workflow:definition:template' THEN '流程模板'
                    ELSE `menu_name`
                  END,
    `order_num` = CASE `perms`
                    WHEN 'workflow:definition:query' THEN 1
                    WHEN 'workflow:definition:add' THEN 2
                    WHEN 'workflow:definition:edit' THEN 3
                    WHEN 'workflow:definition:remove' THEN 4
                    WHEN 'workflow:definition:publish' THEN 5
                    WHEN 'workflow:definition:design' THEN 6
                    WHEN 'workflow:definition:template' THEN 7
                    ELSE `order_num`
                  END,
    `remark` = 'workflow 服务流程定义按钮权限'
WHERE `perms` IN (
  'workflow:definition:query',
  'workflow:definition:add',
  'workflow:definition:edit',
  'workflow:definition:remove',
  'workflow:definition:publish',
  'workflow:definition:design',
  'workflow:definition:template'
);

UPDATE `sys_menu`
SET `parent_id` = (SELECT instance_menu.menu_id
                   FROM (SELECT `menu_id` FROM `sys_menu` WHERE `path` = '/workflow-center/instance' LIMIT 1) instance_menu),
    `menu_name` = CASE `perms`
                    WHEN 'workflow:instance:query' THEN '流程实例查询'
                    WHEN 'workflow:instance:start' THEN '流程发起'
                    WHEN 'workflow:instance:withdraw' THEN '流程撤回'
                    WHEN 'workflow:instance:report' THEN '流程看板'
                    WHEN 'workflow:instance:sla' THEN 'SLA扫描'
                    ELSE `menu_name`
                  END,
    `order_num` = CASE `perms`
                    WHEN 'workflow:instance:query' THEN 1
                    WHEN 'workflow:instance:start' THEN 2
                    WHEN 'workflow:instance:withdraw' THEN 3
                    WHEN 'workflow:instance:report' THEN 4
                    WHEN 'workflow:instance:sla' THEN 5
                    ELSE `order_num`
                  END,
    `remark` = 'workflow 服务流程实例按钮权限'
WHERE `perms` IN (
  'workflow:instance:query',
  'workflow:instance:start',
  'workflow:instance:withdraw',
  'workflow:instance:report',
  'workflow:instance:sla'
);

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT source_role_menu.tenant_id,
       source_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
INNER JOIN (
  SELECT 'workflow:list' AS source_perm, 'workflow:definition:list' AS target_perm
  UNION ALL SELECT 'workflow:list', 'workflow:instance:list'
  UNION ALL SELECT 'workflow:list', 'workflow:definition:query'
  UNION ALL SELECT 'workflow:list', 'workflow:definition:template'
  UNION ALL SELECT 'workflow:list', 'workflow:instance:query'
  UNION ALL SELECT 'workflow:list', 'workflow:instance:report'
  UNION ALL SELECT 'workflow:query', 'workflow:definition:query'
  UNION ALL SELECT 'workflow:query', 'workflow:instance:query'
  UNION ALL SELECT 'workflow:add', 'workflow:definition:add'
  UNION ALL SELECT 'workflow:edit', 'workflow:definition:edit'
  UNION ALL SELECT 'workflow:remove', 'workflow:definition:remove'
  UNION ALL SELECT 'workflow:publish', 'workflow:definition:publish'
  UNION ALL SELECT 'workflow:design', 'workflow:definition:design'
  UNION ALL SELECT 'workflow:design', 'workflow:definition:template'
  UNION ALL SELECT 'workflow:start', 'workflow:instance:start'
  UNION ALL SELECT 'workflow:withdraw', 'workflow:instance:withdraw'
  UNION ALL SELECT 'workflow:remind', 'workflow:instance:sla'
  UNION ALL SELECT 'workflow:definition:list', 'workflow:instance:list'
  UNION ALL SELECT 'workflow:definition:list', 'workflow:definition:query'
  UNION ALL SELECT 'workflow:definition:list', 'workflow:definition:template'
  UNION ALL SELECT 'workflow:definition:list', 'workflow:instance:query'
  UNION ALL SELECT 'workflow:definition:list', 'workflow:instance:report'
  UNION ALL SELECT 'workflow:definition:query', 'workflow:instance:query'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:handle'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:claim'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:finish'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:approve'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:reject'
  UNION ALL SELECT 'workflow:handle', 'workflow:todo:transfer'
  UNION ALL SELECT 'workflow:form', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:form', 'workflow:todo:form'
  UNION ALL SELECT 'workflow:return', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:return', 'workflow:todo:return'
  UNION ALL SELECT 'workflow:addSign', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:addSign', 'workflow:todo:addSign'
  UNION ALL SELECT 'workflow:removeSign', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:removeSign', 'workflow:todo:removeSign'
  UNION ALL SELECT 'workflow:delegate', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:delegate', 'workflow:todo:delegate'
  UNION ALL SELECT 'workflow:remind', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:remind', 'workflow:todo:remind'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:list'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:claim'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:finish'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:form'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:approve'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:reject'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:transfer'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:return'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:addSign'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:removeSign'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:delegate'
  UNION ALL SELECT 'workflow:todo:handle', 'workflow:todo:remind'
) perm_mapping ON perm_mapping.source_perm = source_menu.perms
INNER JOIN `sys_menu` target_menu ON target_menu.perms = perm_mapping.target_perm
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = source_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/workflow-center/definition'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE (child_menu.perms LIKE 'workflow:definition:%' OR child_menu.path = '/workflow-center/definition')
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/workflow-center/instance'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE (child_menu.perms LIKE 'workflow:instance:%' OR child_menu.path = '/workflow-center/instance')
  AND existed_role_menu.menu_id IS NULL;

INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       target_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` target_menu ON target_menu.path = '/workbench/process-todo'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = target_menu.menu_id
WHERE (child_menu.perms LIKE 'workflow:todo:%' OR child_menu.path = '/workbench/process-todo')
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
SELECT DISTINCT child_role_menu.tenant_id,
       child_role_menu.role_id,
       parent_menu.menu_id
FROM `sys_role_menu` child_role_menu
INNER JOIN `sys_menu` child_menu ON child_menu.menu_id = child_role_menu.menu_id
INNER JOIN `sys_menu` parent_menu ON parent_menu.path = '/workbench'
LEFT JOIN `sys_role_menu` existed_role_menu
       ON existed_role_menu.role_id = child_role_menu.role_id
      AND existed_role_menu.menu_id = parent_menu.menu_id
WHERE child_menu.path = '/workbench/process-todo'
  AND existed_role_menu.menu_id IS NULL;

DELETE source_role_menu
FROM `sys_role_menu` source_role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.menu_id = source_role_menu.menu_id
WHERE source_menu.path IN ('/platform/todo-center', '/platform/workflow', '/workbench/message/process-todo')
   OR source_menu.perms IN (
        'workflow:list',
        'workflow:query',
        'workflow:add',
        'workflow:edit',
        'workflow:remove',
        'workflow:publish',
        'workflow:design',
        'workflow:start',
        'workflow:withdraw',
        'workflow:handle',
        'workflow:form',
        'workflow:return',
        'workflow:addSign',
        'workflow:removeSign',
        'workflow:delegate',
        'workflow:remind'
      );

DELETE FROM `sys_menu`
WHERE `path` IN ('/platform/todo-center', '/platform/workflow', '/workbench/message/process-todo')
   OR `perms` IN (
        'workflow:list',
        'workflow:query',
        'workflow:add',
        'workflow:edit',
        'workflow:remove',
        'workflow:publish',
        'workflow:design',
        'workflow:start',
        'workflow:withdraw',
        'workflow:handle',
        'workflow:form',
        'workflow:return',
        'workflow:addSign',
        'workflow:removeSign',
        'workflow:delegate',
        'workflow:remind'
      );
