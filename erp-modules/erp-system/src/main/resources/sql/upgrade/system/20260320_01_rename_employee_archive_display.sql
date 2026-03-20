
-- 2026-03-20 员工档案名称统一
UPDATE `sys_menu`
SET `menu_name` = '员工档案', `remark` = '兼容旧员工主数据菜单名称'
WHERE `path` = '/system/mdm/employee';

UPDATE `sys_wf_definition`
SET `process_name` = '员工档案审批流程',
    `remark` = CASE
      WHEN `remark` = '系统初始化员工主数据审批流程定义' THEN '系统初始化员工档案审批流程定义'
      WHEN `remark` = '升级脚本补齐员工主数据审批流程定义' THEN '升级脚本补齐员工档案审批流程定义'
      ELSE `remark`
    END
WHERE `process_key` = 'mdm_employee';

UPDATE `sys_wf_instance`
SET `process_name` = '员工档案审批流程'
WHERE `process_key` = 'mdm_employee'
  AND `process_name` = '员工主数据审批流程';
