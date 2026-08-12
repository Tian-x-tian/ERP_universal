-- ----------------------------
-- Workflow service initialization
-- Depends on init_system.sql to create shared platform tables first.
-- ----------------------------

-- ----------------------------
-- 1. 流程待办任务表 (sys_todo_task)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_todo_task` (
  `todo_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `instance_id` bigint(20) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` bigint(20) DEFAULT NULL COMMENT '流程任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `process_name` varchar(100) NOT NULL COMMENT '流程名称',
  `node_name` varchar(100) DEFAULT NULL COMMENT '当前节点',
  `business_no` varchar(64) DEFAULT NULL COMMENT '业务单号',
  `priority` char(1) DEFAULT 'M' COMMENT '优先级（H高 M中 L低）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0待处理 1处理中 2已完成）',
  `assignee_user_id` bigint(20) NOT NULL COMMENT '办理人用户ID',
  `due_time` datetime DEFAULT NULL COMMENT '截止时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `finish_time` datetime DEFAULT NULL COMMENT '办结时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`todo_id`),
  KEY `idx_todo_assignee` (`tenant_id`, `assignee_user_id`, `status`),
  KEY `idx_todo_workflow` (`tenant_id`, `instance_id`, `task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程待办任务表';

-- ----------------------------
-- 2. 流程定义表 (sys_wf_definition)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_wf_definition` (
  `definition_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `process_name` varchar(128) NOT NULL COMMENT '流程名称',
  `category` varchar(64) DEFAULT 'custom' COMMENT '流程分类',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0草稿 1已发布 2停用）',
  `published_slot` tinyint(1) GENERATED ALWAYS AS (CASE WHEN `status` = '1' THEN 1 ELSE NULL END) STORED COMMENT '已发布唯一槽位',
  `form_schema` longtext COMMENT '表单结构JSON',
  `model_content` longtext COMMENT '流程设计JSON',
  `publish_by` varchar(64) DEFAULT NULL COMMENT '发布人',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`definition_id`),
  UNIQUE KEY `idx_wf_def_key_ver` (`tenant_id`, `process_key`, `version`),
  UNIQUE KEY `uk_wf_def_publish_slot` (`tenant_id`, `process_key`, `published_slot`),
  KEY `idx_wf_def_status` (`tenant_id`, `status`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- ----------------------------
-- 3. 流程业务动作绑定表 (sys_wf_business_binding)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_wf_business_binding` (
  `binding_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `domain_type` varchar(64) NOT NULL COMMENT '业务域类型',
  `action_code` varchar(32) NOT NULL COMMENT '业务动作编码',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认（0否 1是）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `priority` int(11) DEFAULT 100 COMMENT '优先级，数值越小越优先',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`binding_id`),
  UNIQUE KEY `uk_wf_business_binding` (`tenant_id`, `domain_type`, `action_code`, `process_key`),
  KEY `idx_wf_business_action` (`tenant_id`, `domain_type`, `action_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务动作绑定表';

-- ----------------------------
-- 4. 流程实例表 (sys_wf_instance)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_wf_instance` (
  `instance_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `definition_version` int(11) DEFAULT NULL COMMENT '发起时流程定义版本号',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `process_name` varchar(128) NOT NULL COMMENT '流程名称',
  `category` varchar(64) DEFAULT 'custom' COMMENT '流程分类',
  `owner_service` varchar(32) DEFAULT NULL COMMENT '所属业务服务',
  `business_no` varchar(64) NOT NULL COMMENT '业务单号',
  `business_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `domain_type` varchar(64) DEFAULT NULL COMMENT '业务域类型',
  `action_code` varchar(32) DEFAULT NULL COMMENT '业务动作编码',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `form_data` longtext COMMENT '表单数据JSON',
  `form_schema_snapshot` longtext COMMENT '发起时表单结构快照JSON',
  `model_content_snapshot` longtext COMMENT '发起时流程模型快照JSON',
  `current_node` varchar(128) DEFAULT NULL COMMENT '当前节点',
  `initiator_user_id` bigint(20) NOT NULL COMMENT '发起人用户ID',
  `initiator_user_name` varchar(64) DEFAULT NULL COMMENT '发起人账号',
  `initiator_nick_name` varchar(64) DEFAULT NULL COMMENT '发起人昵称',
  `status` char(1) DEFAULT '0' COMMENT '状态（0进行中 1已完成 2已驳回 3已撤销）',
  `start_time` datetime DEFAULT NULL COMMENT '发起时间',
  `finish_time` datetime DEFAULT NULL COMMENT '结束时间',
  `last_action` varchar(32) DEFAULT NULL COMMENT '最近动作',
  `last_action_user_id` bigint(20) DEFAULT NULL COMMENT '最近动作人ID',
  `last_action_user_name` varchar(64) DEFAULT NULL COMMENT '最近动作人账号',
  `last_action_time` datetime DEFAULT NULL COMMENT '最近动作时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`instance_id`),
  KEY `idx_wf_inst_status` (`tenant_id`, `status`, `start_time`),
  KEY `idx_wf_inst_business` (`tenant_id`, `business_no`),
  KEY `idx_wf_inst_business_status` (`tenant_id`, `business_type`, `business_no`, `status`),
  KEY `idx_wf_inst_idempotency` (`tenant_id`, `idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- ----------------------------
-- 5. 流程任务表 (sys_wf_task)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_wf_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程任务ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) DEFAULT NULL COMMENT '节点编码',
  `node_name` varchar(128) DEFAULT NULL COMMENT '节点名称',
  `candidate_user_ids` varchar(500) DEFAULT NULL COMMENT '候选办理人ID列表',
  `assignee_user_id` bigint(20) DEFAULT NULL COMMENT '办理人用户ID',
  `assignee_user_name` varchar(64) DEFAULT NULL COMMENT '办理人账号',
  `assignee_nick_name` varchar(64) DEFAULT NULL COMMENT '办理人昵称',
  `status` char(1) DEFAULT '0' COMMENT '状态（0待处理 1处理中 2已同意 3已驳回 4已转交 5已取消）',
  `action_comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `todo_id` bigint(20) DEFAULT NULL COMMENT '关联待办ID',
  `due_time` datetime DEFAULT NULL COMMENT '截止时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `finish_time` datetime DEFAULT NULL COMMENT '办结时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_wf_task_assignee` (`tenant_id`, `assignee_user_id`, `status`),
  KEY `idx_wf_task_instance` (`tenant_id`, `instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务表';

-- ----------------------------
-- 6. 流程任务动作记录表 (sys_wf_task_action)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_wf_task_action` (
  `action_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '动作记录ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `task_id` bigint(20) DEFAULT NULL COMMENT '流程任务ID',
  `definition_id` bigint(20) DEFAULT NULL COMMENT '流程定义ID',
  `node_name` varchar(128) DEFAULT NULL COMMENT '节点名称',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `action_user_id` bigint(20) DEFAULT NULL COMMENT '动作人用户ID',
  `action_user_name` varchar(64) DEFAULT NULL COMMENT '动作人账号',
  `action_nick_name` varchar(64) DEFAULT NULL COMMENT '动作人昵称',
  `from_assignee_user_id` bigint(20) DEFAULT NULL COMMENT '来源办理人ID',
  `to_assignee_user_id` bigint(20) DEFAULT NULL COMMENT '目标办理人ID',
  `action_comment` varchar(500) DEFAULT NULL COMMENT '动作意见',
  `action_time` datetime DEFAULT NULL COMMENT '动作时间',
  PRIMARY KEY (`action_id`),
  KEY `idx_wf_action_instance` (`tenant_id`, `instance_id`, `action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务动作记录表';

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 35, '流程中心', 0, 6, '/workflow-center', NULL, 1, 'M', '0', '0', NULL, 'Connection', 'system', NOW(), 'workflow 初始化目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 35 OR `path` = '/workflow-center');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 36, '流程定义', 35, 1, '/workflow-center/definition', '/views/platform/workflow/index', 1, 'C', '0', '0', 'workflow:definition:list', NULL, 'system', NOW(), 'workflow 初始化菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 36 OR `path` = '/workflow-center/definition');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 37, '流程实例', 35, 2, '/workflow-center/instance', '/views/platform/workflow/index', 1, 'C', '0', '0', 'workflow:instance:list', NULL, 'system', NOW(), 'workflow 初始化菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 37 OR `path` = '/workflow-center/instance');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 39, '待办事项', 32, 2, '/workbench/process-todo', '/views/platform/todo-center/index', 1, 'C', '0', '0', 'workflow:todo:list', NULL, 'system', NOW(), 'workflow 初始化菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 39 OR `path` = '/workbench/process-todo');

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
       'workflow 初始化按钮权限'
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

INSERT INTO `sys_wf_definition` (`definition_id`, `tenant_id`, `process_key`, `process_name`, `category`, `version`, `status`, `form_schema`, `model_content`, `publish_by`, `publish_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
  (1, '000000', 'purchase_apply', '采购审批流程', 'purchase', 1, '1', '{"fields":[{"name":"amount","label":"金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"}]}', 'system', NOW(), 'workflow 初始化流程定义', 'system', NOW(), 'system', NOW()),
  (2, '000000', 'expense_apply', '报销审批流程', 'expense', 1, '0', '{"fields":[{"name":"feeType","label":"费用类型"},{"name":"total","label":"合计金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"},{"id":"NODE_2","name":"财务复核"}]}', NULL, NULL, 'workflow 初始化流程定义', 'system', NOW(), 'system', NOW()),
  (3, '000000', 'mdm_employee', '员工档案审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"status","fieldLabel":"状态","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工资料审批","nodeType":"approval","assigneeType":"USER","assigneeUserId":1,"approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), 'workflow 初始化员工档案审批流程定义', 'system', NOW(), 'system', NOW()),
  (4, '000000', 'mdm_employee_onboard', '员工入职审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"action","fieldLabel":"审批动作","componentType":"input","required":true,"placeholder":"入职","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工入职审批","nodeType":"approval","assigneeType":"DIRECT_LEADER","approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), 'workflow 初始化员工入职审批流程定义', 'system', NOW(), 'system', NOW()),
  (5, '000000', 'mdm_employee_change', '员工变更审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"action","fieldLabel":"审批动作","componentType":"input","required":true,"placeholder":"变更","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工变更审批","nodeType":"approval","assigneeType":"DIRECT_LEADER","approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), 'workflow 初始化员工变更审批流程定义', 'system', NOW(), 'system', NOW()),
  (6, '000000', 'mdm_employee_leave', '员工离职审批流程', 'custom', 1, '1', '{"version":1,"fields":[{"fieldCode":"empCode","fieldLabel":"员工编码","componentType":"input","required":true,"placeholder":"系统自动带出","options":[]},{"fieldCode":"empName","fieldLabel":"员工姓名","componentType":"input","required":true,"placeholder":"请输入员工姓名","options":[]},{"fieldCode":"position","fieldLabel":"岗位","componentType":"input","required":false,"placeholder":"请输入岗位","options":[]},{"fieldCode":"action","fieldLabel":"审批动作","componentType":"input","required":true,"placeholder":"离职","options":[]}],"nodePermissions":{}}', '{"startNodeKey":"START_EMPLOYEE_1","nodes":[{"nodeKey":"START_EMPLOYEE_1","nodeName":"开始节点","nodeType":"start","x":40,"y":120},{"nodeKey":"APPROVAL_EMPLOYEE_2","nodeName":"员工离职审批","nodeType":"approval","assigneeType":"DIRECT_LEADER","approveStrategy":"ALL","x":320,"y":120},{"nodeKey":"END_EMPLOYEE_3","nodeName":"结束节点","nodeType":"end","x":620,"y":120}],"edges":[{"from":"START_EMPLOYEE_1","to":"APPROVAL_EMPLOYEE_2"},{"from":"APPROVAL_EMPLOYEE_2","to":"END_EMPLOYEE_3"}]}', 'system', NOW(), 'workflow 初始化员工离职审批流程定义', 'system', NOW(), 'system', NOW())
ON DUPLICATE KEY UPDATE
  `process_name` = VALUES(`process_name`),
  `category` = VALUES(`category`),
  `status` = VALUES(`status`),
  `form_schema` = VALUES(`form_schema`),
  `model_content` = VALUES(`model_content`),
  `publish_by` = VALUES(`publish_by`),
  `publish_time` = VALUES(`publish_time`),
  `remark` = VALUES(`remark`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `sys_wf_business_binding` (`binding_id`, `tenant_id`, `domain_type`, `action_code`, `process_key`, `is_default`, `status`, `priority`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
  (1, '000000', 'EMPLOYEE', 'ONBOARD', 'mdm_employee_onboard', '1', '0', 10, '员工入职审批流程默认绑定', 'system', NOW(), 'system', NOW()),
  (2, '000000', 'EMPLOYEE', 'CHANGE', 'mdm_employee_change', '1', '0', 10, '员工变更审批流程默认绑定', 'system', NOW(), 'system', NOW()),
  (3, '000000', 'EMPLOYEE', 'LEAVE', 'mdm_employee_leave', '1', '0', 10, '员工离职审批流程默认绑定', 'system', NOW(), 'system', NOW()),
  (4, '000000', 'EMPLOYEE', 'ONBOARD', 'mdm_employee', '0', '0', 90, '员工入职审批 legacy 兼容绑定', 'system', NOW(), 'system', NOW())
ON DUPLICATE KEY UPDATE
  `process_key` = VALUES(`process_key`),
  `is_default` = VALUES(`is_default`),
  `status` = VALUES(`status`),
  `priority` = VALUES(`priority`),
  `remark` = VALUES(`remark`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `sys_wf_instance` (`instance_id`, `tenant_id`, `definition_id`, `definition_version`, `process_key`, `process_name`, `category`, `owner_service`, `business_no`, `business_type`, `domain_type`, `action_code`, `idempotency_key`, `form_data`, `form_schema_snapshot`, `model_content_snapshot`, `current_node`, `initiator_user_id`, `initiator_user_name`, `initiator_nick_name`, `status`, `start_time`, `last_action`, `last_action_user_id`, `last_action_user_name`, `last_action_time`, `remark`)
VALUES
  (1, '000000', 1, 1, 'purchase_apply', '采购审批流程', 'purchase', 'system', 'PO-20260309-001', 'PURCHASE_APPLY', 'PURCHASE_APPLY', 'SUBMIT', 'PO-20260309-001', '{"amount":12000,"reason":"办公设备采购"}', '{"fields":[{"name":"amount","label":"金额"}]}', '{"nodes":[{"id":"NODE_1","name":"部门负责人审批"}]}', '部门负责人审批', 1, 'admin', '系统管理员', '0', NOW(), 'START', 1, 'admin', NOW(), 'workflow 初始化流程实例')
ON DUPLICATE KEY UPDATE
  `definition_id` = VALUES(`definition_id`),
  `definition_version` = VALUES(`definition_version`),
  `process_key` = VALUES(`process_key`),
  `process_name` = VALUES(`process_name`),
  `category` = VALUES(`category`),
  `owner_service` = VALUES(`owner_service`),
  `business_no` = VALUES(`business_no`),
  `business_type` = VALUES(`business_type`),
  `domain_type` = VALUES(`domain_type`),
  `action_code` = VALUES(`action_code`),
  `idempotency_key` = VALUES(`idempotency_key`),
  `form_data` = VALUES(`form_data`),
  `form_schema_snapshot` = VALUES(`form_schema_snapshot`),
  `model_content_snapshot` = VALUES(`model_content_snapshot`),
  `current_node` = VALUES(`current_node`),
  `initiator_user_id` = VALUES(`initiator_user_id`),
  `initiator_user_name` = VALUES(`initiator_user_name`),
  `initiator_nick_name` = VALUES(`initiator_nick_name`),
  `status` = VALUES(`status`),
  `start_time` = VALUES(`start_time`),
  `last_action` = VALUES(`last_action`),
  `last_action_user_id` = VALUES(`last_action_user_id`),
  `last_action_user_name` = VALUES(`last_action_user_name`),
  `last_action_time` = VALUES(`last_action_time`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_todo_task` (`todo_id`, `instance_id`, `task_id`, `tenant_id`, `process_name`, `node_name`, `business_no`, `priority`, `status`, `assignee_user_id`, `due_time`, `create_time`, `remark`)
VALUES
  (1, NULL, NULL, '000000', '请假审批', '部门负责人审批', 'LV-20260307-001', 'H', '0', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), '请及时处理'),
  (2, NULL, NULL, '000000', '采购申请', '财务复核', 'PO-20260307-018', 'M', '1', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), '处理中'),
  (3, NULL, NULL, '000000', '合同归档', '档案确认', 'CT-20260306-021', 'L', '0', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW(), '待签收'),
  (4, 1, 1, '000000', '采购审批流程', '部门负责人审批', 'PO-20260309-001', 'M', '0', 1, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), 'workflow 初始化待办')
ON DUPLICATE KEY UPDATE
  `instance_id` = VALUES(`instance_id`),
  `task_id` = VALUES(`task_id`),
  `process_name` = VALUES(`process_name`),
  `node_name` = VALUES(`node_name`),
  `business_no` = VALUES(`business_no`),
  `priority` = VALUES(`priority`),
  `status` = VALUES(`status`),
  `assignee_user_id` = VALUES(`assignee_user_id`),
  `due_time` = VALUES(`due_time`),
  `create_time` = VALUES(`create_time`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_wf_task` (`task_id`, `tenant_id`, `instance_id`, `definition_id`, `node_key`, `node_name`, `candidate_user_ids`, `assignee_user_id`, `assignee_user_name`, `assignee_nick_name`, `status`, `todo_id`, `due_time`, `create_time`)
VALUES
  (1, '000000', 1, 1, 'NODE_1', '部门负责人审批', '1', 1, 'admin', '系统管理员', '0', 4, DATE_ADD(NOW(), INTERVAL 2 DAY), NOW())
ON DUPLICATE KEY UPDATE
  `instance_id` = VALUES(`instance_id`),
  `definition_id` = VALUES(`definition_id`),
  `node_key` = VALUES(`node_key`),
  `node_name` = VALUES(`node_name`),
  `candidate_user_ids` = VALUES(`candidate_user_ids`),
  `assignee_user_id` = VALUES(`assignee_user_id`),
  `assignee_user_name` = VALUES(`assignee_user_name`),
  `assignee_nick_name` = VALUES(`assignee_nick_name`),
  `status` = VALUES(`status`),
  `todo_id` = VALUES(`todo_id`),
  `due_time` = VALUES(`due_time`),
  `create_time` = VALUES(`create_time`);

INSERT INTO `sys_wf_task_action` (`action_id`, `tenant_id`, `instance_id`, `task_id`, `definition_id`, `node_name`, `action_type`, `action_user_id`, `action_user_name`, `action_nick_name`, `to_assignee_user_id`, `action_comment`, `action_time`)
VALUES
  (1, '000000', 1, 1, 1, '部门负责人审批', 'START', 1, 'admin', '系统管理员', 1, '流程发起', NOW())
ON DUPLICATE KEY UPDATE
  `instance_id` = VALUES(`instance_id`),
  `task_id` = VALUES(`task_id`),
  `definition_id` = VALUES(`definition_id`),
  `node_name` = VALUES(`node_name`),
  `action_type` = VALUES(`action_type`),
  `action_user_id` = VALUES(`action_user_id`),
  `action_user_name` = VALUES(`action_user_name`),
  `action_nick_name` = VALUES(`action_nick_name`),
  `to_assignee_user_id` = VALUES(`to_assignee_user_id`),
  `action_comment` = VALUES(`action_comment`),
  `action_time` = VALUES(`action_time`);
