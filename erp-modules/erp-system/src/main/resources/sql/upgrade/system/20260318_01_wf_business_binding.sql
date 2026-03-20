-- 20260318_01: 员工审批动作绑定与默认流程补齐

CREATE TABLE IF NOT EXISTS `sys_wf_business_binding` (
  `binding_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `domain_type` varchar(64) NOT NULL COMMENT '业务域类型',
  `action_code` varchar(32) NOT NULL COMMENT '业务动作编码',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `is_default` char(1) DEFAULT '0' COMMENT '是否默认（0否 1是）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `priority` int(11) DEFAULT 100 COMMENT '优先级，数值越小优先',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`binding_id`),
  UNIQUE KEY `uk_wf_business_binding` (`tenant_id`,`domain_type`,`action_code`,`process_key`),
  KEY `idx_wf_business_action` (`tenant_id`,`domain_type`,`action_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务动作绑定表';

INSERT INTO `sys_wf_definition` (`definition_id`, `tenant_id`, `process_key`, `process_name`, `category`, `version`, `status`, `form_schema`, `model_content`, `publish_by`, `publish_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT next_data.next_id,
       '000000',
       'mdm_employee_onboard',
       '员工入职审批流程',
       'custom',
       1,
       '1',
       '{\"version\":1,\"fields\":[{\"fieldCode\":\"empCode\",\"fieldLabel\":\"员工编码\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"系统自动带出\",\"options\":[]},{\"fieldCode\":\"empName\",\"fieldLabel\":\"员工姓名\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入员工姓名\",\"options\":[]},{\"fieldCode\":\"position\",\"fieldLabel\":\"岗位\",\"componentType\":\"input\",\"required\":false,\"placeholder\":\"请输入岗位\",\"options\":[]},{\"fieldCode\":\"action\",\"fieldLabel\":\"审批动作\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"入职\",\"options\":[]}],\"nodePermissions\":{}}',
       '{\"startNodeKey\":\"START_EMPLOYEE_1\",\"nodes\":[{\"nodeKey\":\"START_EMPLOYEE_1\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":120},{\"nodeKey\":\"APPROVAL_EMPLOYEE_2\",\"nodeName\":\"员工入职审批\",\"nodeType\":\"approval\",\"assigneeType\":\"DIRECT_LEADER\",\"approveStrategy\":\"ALL\",\"x\":320,\"y\":120},{\"nodeKey\":\"END_EMPLOYEE_3\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":620,\"y\":120}],\"edges\":[{\"from\":\"START_EMPLOYEE_1\",\"to\":\"APPROVAL_EMPLOYEE_2\"},{\"from\":\"APPROVAL_EMPLOYEE_2\",\"to\":\"END_EMPLOYEE_3\"}]}',
       'system',
       NOW(),
       '日期增量脚本补齐员工入职审批流程定义',
       'system',
       NOW(),
       'system',
       NOW()
FROM (SELECT COALESCE(MAX(`definition_id`), 0) + 1 AS next_id FROM `sys_wf_definition`) next_data
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_definition`
    WHERE `tenant_id` = '000000'
      AND `process_key` = 'mdm_employee_onboard'
      AND `version` = 1
);

INSERT INTO `sys_wf_definition` (`definition_id`, `tenant_id`, `process_key`, `process_name`, `category`, `version`, `status`, `form_schema`, `model_content`, `publish_by`, `publish_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT next_data.next_id,
       '000000',
       'mdm_employee_change',
       '员工变更审批流程',
       'custom',
       1,
       '1',
       '{\"version\":1,\"fields\":[{\"fieldCode\":\"empCode\",\"fieldLabel\":\"员工编码\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"系统自动带出\",\"options\":[]},{\"fieldCode\":\"empName\",\"fieldLabel\":\"员工姓名\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入员工姓名\",\"options\":[]},{\"fieldCode\":\"position\",\"fieldLabel\":\"岗位\",\"componentType\":\"input\",\"required\":false,\"placeholder\":\"请输入岗位\",\"options\":[]},{\"fieldCode\":\"action\",\"fieldLabel\":\"审批动作\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"变更\",\"options\":[]}],\"nodePermissions\":{}}',
       '{\"startNodeKey\":\"START_EMPLOYEE_1\",\"nodes\":[{\"nodeKey\":\"START_EMPLOYEE_1\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":120},{\"nodeKey\":\"APPROVAL_EMPLOYEE_2\",\"nodeName\":\"员工变更审批\",\"nodeType\":\"approval\",\"assigneeType\":\"DIRECT_LEADER\",\"approveStrategy\":\"ALL\",\"x\":320,\"y\":120},{\"nodeKey\":\"END_EMPLOYEE_3\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":620,\"y\":120}],\"edges\":[{\"from\":\"START_EMPLOYEE_1\",\"to\":\"APPROVAL_EMPLOYEE_2\"},{\"from\":\"APPROVAL_EMPLOYEE_2\",\"to\":\"END_EMPLOYEE_3\"}]}',
       'system',
       NOW(),
       '日期增量脚本补齐员工变更审批流程定义',
       'system',
       NOW(),
       'system',
       NOW()
FROM (SELECT COALESCE(MAX(`definition_id`), 0) + 1 AS next_id FROM `sys_wf_definition`) next_data
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_definition`
    WHERE `tenant_id` = '000000'
      AND `process_key` = 'mdm_employee_change'
      AND `version` = 1
);

INSERT INTO `sys_wf_definition` (`definition_id`, `tenant_id`, `process_key`, `process_name`, `category`, `version`, `status`, `form_schema`, `model_content`, `publish_by`, `publish_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT next_data.next_id,
       '000000',
       'mdm_employee_leave',
       '员工离职审批流程',
       'custom',
       1,
       '1',
       '{\"version\":1,\"fields\":[{\"fieldCode\":\"empCode\",\"fieldLabel\":\"员工编码\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"系统自动带出\",\"options\":[]},{\"fieldCode\":\"empName\",\"fieldLabel\":\"员工姓名\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"请输入员工姓名\",\"options\":[]},{\"fieldCode\":\"position\",\"fieldLabel\":\"岗位\",\"componentType\":\"input\",\"required\":false,\"placeholder\":\"请输入岗位\",\"options\":[]},{\"fieldCode\":\"action\",\"fieldLabel\":\"审批动作\",\"componentType\":\"input\",\"required\":true,\"placeholder\":\"离职\",\"options\":[]}],\"nodePermissions\":{}}',
       '{\"startNodeKey\":\"START_EMPLOYEE_1\",\"nodes\":[{\"nodeKey\":\"START_EMPLOYEE_1\",\"nodeName\":\"开始节点\",\"nodeType\":\"start\",\"x\":40,\"y\":120},{\"nodeKey\":\"APPROVAL_EMPLOYEE_2\",\"nodeName\":\"员工离职审批\",\"nodeType\":\"approval\",\"assigneeType\":\"DIRECT_LEADER\",\"approveStrategy\":\"ALL\",\"x\":320,\"y\":120},{\"nodeKey\":\"END_EMPLOYEE_3\",\"nodeName\":\"结束节点\",\"nodeType\":\"end\",\"x\":620,\"y\":120}],\"edges\":[{\"from\":\"START_EMPLOYEE_1\",\"to\":\"APPROVAL_EMPLOYEE_2\"},{\"from\":\"APPROVAL_EMPLOYEE_2\",\"to\":\"END_EMPLOYEE_3\"}]}',
       'system',
       NOW(),
       '日期增量脚本补齐员工离职审批流程定义',
       'system',
       NOW(),
       'system',
       NOW()
FROM (SELECT COALESCE(MAX(`definition_id`), 0) + 1 AS next_id FROM `sys_wf_definition`) next_data
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_definition`
    WHERE `tenant_id` = '000000'
      AND `process_key` = 'mdm_employee_leave'
      AND `version` = 1
);

INSERT INTO `sys_wf_business_binding` (`tenant_id`, `domain_type`, `action_code`, `process_key`, `is_default`, `status`, `priority`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '000000', 'EMPLOYEE', 'ONBOARD', 'mdm_employee_onboard', '1', '0', 10, '员工入职审批流程默认绑定', 'system', NOW(), 'system', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_business_binding`
    WHERE `tenant_id` = '000000'
      AND `domain_type` = 'EMPLOYEE'
      AND `action_code` = 'ONBOARD'
      AND `process_key` = 'mdm_employee_onboard'
);

INSERT INTO `sys_wf_business_binding` (`tenant_id`, `domain_type`, `action_code`, `process_key`, `is_default`, `status`, `priority`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '000000', 'EMPLOYEE', 'CHANGE', 'mdm_employee_change', '1', '0', 10, '员工变更审批流程默认绑定', 'system', NOW(), 'system', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_business_binding`
    WHERE `tenant_id` = '000000'
      AND `domain_type` = 'EMPLOYEE'
      AND `action_code` = 'CHANGE'
      AND `process_key` = 'mdm_employee_change'
);

INSERT INTO `sys_wf_business_binding` (`tenant_id`, `domain_type`, `action_code`, `process_key`, `is_default`, `status`, `priority`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '000000', 'EMPLOYEE', 'LEAVE', 'mdm_employee_leave', '1', '0', 10, '员工离职审批流程默认绑定', 'system', NOW(), 'system', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_business_binding`
    WHERE `tenant_id` = '000000'
      AND `domain_type` = 'EMPLOYEE'
      AND `action_code` = 'LEAVE'
      AND `process_key` = 'mdm_employee_leave'
);

INSERT INTO `sys_wf_business_binding` (`tenant_id`, `domain_type`, `action_code`, `process_key`, `is_default`, `status`, `priority`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '000000', 'EMPLOYEE', 'ONBOARD', 'mdm_employee', '0', '0', 90, '员工入职审批 legacy 兼容绑定', 'system', NOW(), 'system', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_wf_business_binding`
    WHERE `tenant_id` = '000000'
      AND `domain_type` = 'EMPLOYEE'
      AND `action_code` = 'ONBOARD'
      AND `process_key` = 'mdm_employee'
);
