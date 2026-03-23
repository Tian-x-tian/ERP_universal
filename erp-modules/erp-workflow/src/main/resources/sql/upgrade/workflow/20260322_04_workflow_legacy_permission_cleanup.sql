-- 20260322_04: 清理 workflow legacy 通用权限与旧路径残影，收敛到资源化权限拓扑

DELETE role_menu
FROM `sys_role_menu` role_menu
INNER JOIN `sys_menu` menu ON menu.menu_id = role_menu.menu_id
WHERE menu.perms IN (
    'workflow:list',
    'workflow:query',
    'workflow:add',
    'workflow:edit',
    'workflow:remove',
    'workflow:publish',
    'workflow:start',
    'workflow:handle',
    'workflow:design',
    'workflow:withdraw',
    'workflow:form',
    'workflow:return',
    'workflow:addSign',
    'workflow:removeSign',
    'workflow:delegate',
    'workflow:remind'
)
   OR menu.perms LIKE 'system:workflow:%'
   OR menu.perms LIKE 'system:todo:%'
   OR menu.path IN (
    '/platform/workflow',
    '/platform/todo-center',
    '/workbench/message/process-todo',
    '/system/workflow',
    '/system/todo'
);

DELETE FROM `sys_menu`
WHERE `perms` IN (
    'workflow:list',
    'workflow:query',
    'workflow:add',
    'workflow:edit',
    'workflow:remove',
    'workflow:publish',
    'workflow:start',
    'workflow:handle',
    'workflow:design',
    'workflow:withdraw',
    'workflow:form',
    'workflow:return',
    'workflow:addSign',
    'workflow:removeSign',
    'workflow:delegate',
    'workflow:remind'
)
   OR `perms` LIKE 'system:workflow:%'
   OR `perms` LIKE 'system:todo:%'
   OR `path` IN (
    '/platform/workflow',
    '/platform/todo-center',
    '/workbench/message/process-todo',
    '/system/workflow',
    '/system/todo'
);

DELETE role_menu
FROM `sys_role_menu` role_menu
LEFT JOIN `sys_menu` menu ON menu.menu_id = role_menu.menu_id
WHERE menu.menu_id IS NULL;
